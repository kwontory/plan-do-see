package com.plandosee.diary.review.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.concurrency.WriteTransactions;
import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.db.StatementBudget;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.plan.application.PlanCommand;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.review.application.port.ReviewMapper;
import com.plandosee.diary.review.domain.EvidenceQuery;
import com.plandosee.diary.review.domain.EvidenceTodo;
import com.plandosee.diary.review.domain.ReviewEvidence;
import com.plandosee.diary.review.domain.ReviewMetric;
import com.plandosee.diary.review.domain.ReviewRow;
import com.plandosee.diary.review.domain.ReviewScope;
import com.plandosee.diary.review.domain.ReviewSummary;

@Service
public class ReviewService {

    public static final int IMPROVEMENT_MAX_LENGTH = 1000;
    public static final String IMPROVEMENT_ALREADY_TRANSFERRED = "review.improvement.alreadyTransferred";
    public static final String IMPROVEMENT_MISSING = "review.improvement.missing";
    /** Message argument {0}: IMPROVEMENT_MAX_LENGTH as a plain string (no number grouping). */
    public static final String IMPROVEMENT_TOO_LONG = "review.improvement.tooLong";

    private final ReviewMapper reviewMapper;
    private final PlanService planService;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;
    private final WriteTransactions writes;
    private final StatementBudget statementBudget;

    public ReviewService(ReviewMapper reviewMapper, PlanService planService, CurrentUserProvider currentUserProvider,
                         IdGenerator idGenerator, SeoulDates seoulDates, WriteTransactions writes,
                         StatementBudget statementBudget) {
        this.reviewMapper = reviewMapper;
        this.planService = planService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.seoulDates = seoulDates;
        this.writes = writes;
        this.statementBudget = statementBudget;
    }

    /**
     * DEC-01: a review belongs to one plan and copies the plan period for display and audit.
     */
    public UUID create(UUID planId, String improvement) {
        return writes.run(() -> insert(planId, improvement));
    }

    private UUID insert(UUID planId, String improvement) {
        PlanRow plan = planService.requireOwned(planId);
        OffsetDateTime now = now();
        ReviewRow review = new ReviewRow();
        review.setId(idGenerator.newId());
        review.setUserId(currentUserProvider.currentUserId());
        review.setPlanId(plan.getId());
        review.setPeriodStart(plan.getStartDate());
        review.setPeriodEnd(plan.getEndDate());
        review.setImprovement(normalizeImprovement(improvement));
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        reviewMapper.insert(review);
        return review.getId();
    }

    @Transactional(readOnly = true)
    public ReviewRow get(UUID reviewId) {
        ReviewRow review = reviewMapper.findActiveOwned(currentUserProvider.currentUserId(), reviewId);
        if (review == null) {
            throw new NotFoundException("review");
        }
        return review;
    }

    @Transactional(readOnly = true)
    public List<ReviewRow> listForPlan(UUID planId) {
        planService.get(planId);
        return reviewMapper.listForPlanOwned(currentUserProvider.currentUserId(), planId);
    }

    /**
     * The review whose improvement was carried into the given plan, or null.
     */
    @Transactional(readOnly = true)
    public ReviewRow findSourceReview(UUID nextPlanId) {
        return reviewMapper.findByNextPlanOwned(currentUserProvider.currentUserId(), nextPlanId);
    }

    /**
     * The review row lock serializes this with a concurrent transfer: either the new text is saved first and then
     * carried, or the transfer wins and this is rejected (ADR-08, ADR-15).
     */
    public void updateImprovement(UUID reviewId, String improvement) {
        writes.run(() -> updateImprovementLocked(reviewId, improvement));
    }

    private void updateImprovementLocked(UUID reviewId, String improvement) {
        UUID userId = currentUserProvider.currentUserId();
        ReviewRow review = reviewMapper.lockActiveOwned(userId, reviewId);
        if (review == null) {
            throw new NotFoundException("review");
        }
        if (review.isTransferred()) {
            throw new DomainRuleException("improvement", IMPROVEMENT_ALREADY_TRANSFERRED);
        }
        if (reviewMapper.updateImprovementOwned(userId, reviewId, normalizeImprovement(improvement), now()) != 1) {
            throw new NotFoundException("review");
        }
    }

    /**
     * The review page in one read-only snapshot (ADR-14 C-4): review, plan, next plan, and summary cannot come from
     * different moments. Aggregate reads run under the wider aggregate statement timeout (ADR-19).
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewDetail detail(UUID reviewId) {
        statementBudget.useAggregateTimeout();
        ReviewRow review = get(reviewId);
        return new ReviewDetail(review, planService.get(review.getPlanId()), planService.findOwned(review.getNextPlanId()),
                summary(scope(review)));
    }

    /**
     * The evidence page in one repeatable-read snapshot: review, plan, summary, and evidence lists agree (T06-C83).
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewEvidencePage evidencePage(UUID reviewId, ReviewMetric metric) {
        statementBudget.useAggregateTimeout();
        ReviewRow review = get(reviewId);
        return new ReviewEvidencePage(review, planService.get(review.getPlanId()), evidence(review, metric));
    }

    @Transactional(readOnly = true)
    public ReviewSummary summary(UUID reviewId) {
        statementBudget.useAggregateTimeout();
        return summary(scope(get(reviewId)));
    }

    /**
     * Summary and evidence are read in one repeatable-read snapshot so the page never shows numbers that disagree.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewEvidence evidence(UUID reviewId, ReviewMetric metric) {
        statementBudget.useAggregateTimeout();
        return evidence(get(reviewId), metric);
    }

    private ReviewEvidence evidence(ReviewRow review, ReviewMetric metric) {
        ReviewScope scope = scope(review);
        ReviewSummary summary = summary(scope);
        EvidenceQuery query = EvidenceQuery.of(scope, metric);
        List<EvidenceTodo> todos = List.of();
        List<ExecutionLogRow> logs = List.of();
        switch (metric) {
            case PLANNED, COMPLETED, OVERDUE, ESTIMATED -> todos = reviewMapper.evidenceTodos(query);
            case BLOCKED -> {
                todos = reviewMapper.evidenceTodos(query);
                logs = reviewMapper.evidenceLogs(query);
                attachBlockerReasons(todos, logs);
            }
            case ACTUAL -> logs = reviewMapper.evidenceLogs(query);
            case VARIANCE -> {
                todos = reviewMapper.evidenceTodos(query);
                logs = reviewMapper.evidenceLogs(query);
            }
        }
        long estimateSum = todos.stream().mapToLong(EvidenceTodo::getEstimatedMinutes).sum();
        long actualSum = metric == ReviewMetric.ACTUAL
                ? logs.stream().mapToLong(ExecutionLogRow::getActualMinutes).sum()
                : todos.stream().mapToLong(EvidenceTodo::getActualMinutes).sum();
        int count = metric == ReviewMetric.ACTUAL ? logs.size() : todos.size();
        return new ReviewEvidence(metric, summary, todos, logs, count, estimateSum, actualSum);
    }

    /**
     * ADR-08 / T06-C33: carries the review improvement into a new plan exactly once. The review row lock
     * serializes concurrent requests; a repeated request returns the existing next plan without creating another.
     * created in the result is decided under that lock, so the caller never checks the transfer state itself.
     */
    public TransferResult transferImprovement(UUID reviewId, PlanCommand command) {
        return writes.run(() -> transferLocked(reviewId, command));
    }

    private TransferResult transferLocked(UUID reviewId, PlanCommand command) {
        UUID userId = currentUserProvider.currentUserId();
        ReviewRow review = reviewMapper.lockActiveOwned(userId, reviewId);
        if (review == null) {
            throw new NotFoundException("review");
        }
        if (review.isTransferred()) {
            return new TransferResult(review.getNextPlanId(), false);
        }
        String improvement = review.getImprovement();
        if (improvement == null || improvement.isBlank()) {
            throw new DomainRuleException("improvement", IMPROVEMENT_MISSING);
        }
        UUID nextPlanId = planService.createWithImprovement(command, improvement);
        if (reviewMapper.markTransferredOwned(userId, reviewId, nextPlanId, now()) != 1) {
            throw new IllegalStateException("review transfer update affected no row");
        }
        return new TransferResult(nextPlanId, true);
    }

    private ReviewScope scope(ReviewRow review) {
        return new ReviewScope(currentUserProvider.currentUserId(), review.getPlanId(), seoulDates.today());
    }

    private ReviewSummary summary(ReviewScope scope) {
        return ReviewSummary.of(reviewMapper.summary(scope), scope.today());
    }

    private static void attachBlockerReasons(List<EvidenceTodo> todos, List<ExecutionLogRow> blockerLogs) {
        Map<UUID, EvidenceTodo> byId = new LinkedHashMap<>();
        todos.forEach(todo -> byId.put(todo.getId(), todo));
        for (ExecutionLogRow log : blockerLogs) {
            EvidenceTodo todo = byId.get(log.getTodoId());
            if (todo != null) {
                todo.getBlockerReasons().add(log.getBlockerReason());
            }
        }
    }

    static String normalizeImprovement(String improvement) {
        if (improvement == null) {
            return null;
        }
        String trimmed = improvement.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > IMPROVEMENT_MAX_LENGTH) {
            throw new DomainRuleException("improvement", IMPROVEMENT_TOO_LONG, String.valueOf(IMPROVEMENT_MAX_LENGTH));
        }
        return trimmed;
    }

    private OffsetDateTime now() {
        return seoulDates.now().atOffset(ZoneOffset.UTC);
    }
}
