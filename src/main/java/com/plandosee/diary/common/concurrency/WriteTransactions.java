package com.plandosee.diary.common.concurrency;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.plandosee.diary.common.error.ConcurrencyConflictException;
import com.plandosee.diary.common.error.ServiceBusyException;

/**
 * ADR-15/ADR-19 write-service boundary. Runs the work in one transaction and, when it fails with a
 * {@link TransientConflict}, rolls back and runs the whole transaction again after a short jittered delay
 * (at most {@code app.retry.max-retries} more times; ADR-19 sets the default to 0 because retrying means waiting,
 * so a conflict goes straight to the "press again" notice). Retrying is safe because every write transaction re-reads
 * and re-decides from scratch after a rollback, completion is guarded by the idempotency key and unique
 * constraints, and the transfer by the review row lock and the unique next_plan_id.
 * <p>
 * When a transaction is already active (a service calling another service), the work simply joins it; only the
 * outermost boundary retries, so a rolled-back inner call can never be repeated on its own.
 * <p>
 * A failure caused by the time budget ({@link BusyCause}: statement or transaction timeout, pool wait) is never
 * retried and becomes {@link ServiceBusyException}.
 * <p>
 * Logs carry the conflict kind and attempt numbers only (CLAUDE.md 5장).
 */
@Component
public class WriteTransactions {

    private static final Logger log = LoggerFactory.getLogger(WriteTransactions.class);

    private final TransactionTemplate template;
    private final int maxRetries;
    private final long baseDelayMillis;
    private final long jitterMillis;
    private final AtomicLong retries = new AtomicLong();
    private final AtomicLong exhausted = new AtomicLong();
    private final AtomicLong deadlocks = new AtomicLong();

    public WriteTransactions(PlatformTransactionManager transactionManager,
                             @Value("${app.retry.max-retries:0}") int maxRetries,
                             @Value("${app.retry.base-delay-ms:50}") long baseDelayMillis,
                             @Value("${app.retry.jitter-ms:100}") long jitterMillis) {
        this.template = new TransactionTemplate(transactionManager);
        this.maxRetries = Math.max(0, maxRetries);
        this.baseDelayMillis = Math.max(0, baseDelayMillis);
        this.jitterMillis = Math.max(0, jitterMillis);
    }

    public <T> T run(Supplier<T> work) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return work.get();
        }
        for (int attempt = 1; ; attempt++) {
            try {
                return template.execute(status -> work.get());
            } catch (RuntimeException failure) {
                Optional<BusyCause> busy = BusyCause.classify(failure);
                if (busy.isPresent()) {
                    log.warn("event=busy cause={} attempt={}", busy.get(), attempt);
                    throw new ServiceBusyException(busy.get(), failure);
                }
                Optional<TransientConflict> kind = TransientConflict.classify(failure);
                if (kind.isEmpty()) {
                    throw failure;
                }
                if (kind.get() == TransientConflict.DEADLOCK) {
                    deadlocks.incrementAndGet();
                }
                if (attempt > maxRetries) {
                    exhausted.incrementAndGet();
                    log.warn("event=transient_conflict_exhausted kind={} attempts={}", kind.get(), attempt);
                    throw new ConcurrencyConflictException(kind.get(), attempt, failure);
                }
                retries.incrementAndGet();
                log.info("event=transient_conflict_retry kind={} attempt={} maxRetries={}", kind.get(), attempt, maxRetries);
                pause(attempt, kind.get(), failure);
            }
        }
    }

    public void run(Runnable work) {
        run(() -> {
            work.run();
            return null;
        });
    }

    /** Total automatic retries since start (observability and tests). */
    public long retryCount() {
        return retries.get();
    }

    /** Total requests that failed after every retry. */
    public long exhaustedCount() {
        return exhausted.get();
    }

    /** Total deadlocks seen at this boundary, retried or not (ADR-15 CC-1 regression check). */
    public long deadlockCount() {
        return deadlocks.get();
    }

    private void pause(int attempt, TransientConflict kind, RuntimeException failure) {
        long delay = baseDelayMillis * attempt + (jitterMillis == 0 ? 0 : ThreadLocalRandom.current().nextLong(jitterMillis + 1));
        if (delay == 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ConcurrencyConflictException(kind, attempt, failure);
        }
    }
}
