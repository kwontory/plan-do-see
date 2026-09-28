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
import com.plandosee.diary.common.domain.EditOutcome;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.paging.PageInfo;
import com.plandosee.diary.common.paging.PageRequest;
import com.plandosee.diary.common.paging.PageSettings;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.plan.application.PlanCommand;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.review.application.port.ReviewMapper;
import com.plandosee.diary.review.domain.EvidenceQuery;
import com.plandosee.diary.review.domain.EvidenceTodo;
import com.plandosee.diary.review.domain.EvidenceTotals;
import com.plandosee.diary.review.domain.ReviewEvidence;
import com.plandosee.diary.review.domain.ReviewMetric;
import com.plandosee.diary.review.domain.ReviewRow;
import com.plandosee.diary.review.domain.ReviewRules;
import com.plandosee.diary.review.domain.ReviewScope;
import com.plandosee.diary.review.domain.ReviewSummary;

@Service
public class ReviewService {

    public static final String IMPROVEMENT_ALREADY_TRANSFERRED = "review.improvement.alreadyTransferred";
    public static final String IMPROVEMENT_MISSING = "review.improvement.missing";

    private final ReviewMapper reviewMapper;
    private final PlanService planService;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;
    private final WriteTransactions writes;
    private final StatementBudget statementBudget;
    private final PageSettings pageSettings;

    public ReviewService(ReviewMapper reviewMapper, PlanService planService, CurrentUserProvider currentUserProvider,
                         IdGenerator idGenerator, SeoulDates seoulDates, WriteTransactions writes,
                         StatementBudget statementBudget, PageSettings pageSettings) {
        this.pageSettings = pageSettings;
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
        String normalized = normalizeImprovement(improvement);
        return writes.run(() -> insert(planId, normalized));
    }

    private UUID insert(UUID planId, String normalizedImprovement) {
        PlanRow plan = planService.requireOwned(planId);
        OffsetDateTime now = now();
        ReviewRow review = new ReviewRow();
        review.setId(idGenerator.newId());
        review.setUserId(currentUserProvider.currentUserId());
        review.setPlanId(plan.getId());
        review.setPeriodStart(plan.getStartDate());
        review.setPeriodEnd(plan.getEndDate());
        review.setImprovement(normalizedImprovement);
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
    public EditOutcome updateImprovement(UUID reviewId, String improvement) {
        return updateImprovement(reviewId, improvement, null);
    }

    /**
     * ADR-18: expectedVersion is the version the form was opened with (null: no check). Under the review row lock:
     * already transferred is {@link ImprovementTransferredException} (E10); the same text as stored is UNCHANGED
     * (E5); a different version is {@link ReviewStaleException} with the latest review; otherwise the text is saved
     * and the version goes up by one. The transfer itself never changes the version (E7).
     */
    public EditOutcome updateImprovement(UUID reviewId, String improvement, Integer expectedVersion) {
        String normalized = normalizeImprovement(improvement);
        return writes.run(() -> updateImprovementLocked(reviewId, normalized, expectedVersion));
    }

    private EditOutcome updateImprovementLocked(UUID reviewId, String normalizedImprovement, Integer expectedVersion) {
        UUID userId = currentUserProvider.currentUserId();
        ReviewRow review = reviewMapper.lockActiveOwned(userId, reviewId);
        if (review == null) {
            throw new NotFoundException("review");
        }
        if (review.isTransferred()) {
            throw new ImprovementTransferredException();
        }
        if (java.util.Objects.equals(review.getImprovement(), normalizedImprovement)) {
            return EditOutcome.UNCHANGED;
        }
        if (expectedVersion != null && expectedVersion != review.getVersion()) {
            throw new ReviewStaleException(review, List.of("improvement"));
        }
        if (reviewMapper.updateImprovementOwned(userId, reviewId, normalizedImprovement, now(), review.getVersion()) != 1) {
            throw new NotFoundException("review");
        }
        return EditOutcome.UPDATED;
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
     * First page of each list.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewEvidencePage evidencePage(UUID reviewId, ReviewMetric metric) {
        return evidencePage(reviewId, metric, PageRequest.FIRST, PageRequest.FIRST);
    }

    /**
     * ADR-21: the evidence page with one page of the todo list ({@code todoPage}) and of the log list
     * ({@code logPage}). evidenceCount and the minute sums still cover the whole scope, so they match the summary
     * whatever page is shown (T06-C83).
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewEvidencePage evidencePage(UUID reviewId, ReviewMetric metric, int todoPage, int logPage) {
        statementBudget.useAggregateTimeout();
        ReviewRow review = get(reviewId);
        return new ReviewEvidencePage(review, planService.get(review.getPlanId()),
                evidence(review, metric, todoPage, logPage));
    }

    @Transactional(readOnly = true)
    public ReviewSummary summary(UUID reviewId) {
        statementBudget.useAggregateTimeout();
        return summary(scope(get(reviewId)));
    }

    /**
     * Summary and every evidence row (whole lists, not paged), read in one repeatable-read snapshot so the numbers
     * never disagree.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewEvidence evidence(UUID reviewId, ReviewMetric metric) {
        statementBudget.useAggregateTimeout();
        return evidence(get(reviewId), metric, null, null);
    }

    /**
     * Lists narrowed by the metric's filter; totals come from the same filter over the whole scope. A null page
     * reads the whole list.
     */
    private ReviewEvidence evidence(ReviewRow review, ReviewMetric metric, Integer todoPage, Integer logPage) {
        ReviewScope scope = scope(review);
        ReviewSummary summary = summary(scope);
        EvidenceQuery query = EvidenceQuery.of(scope, metric);
        boolean showsTodos = metric != ReviewMetric.ACTUAL;
        boolean showsLogs = metric == ReviewMetric.ACTUAL || metric == ReviewMetric.VARIANCE;

        EvidenceTotals todoTotals = showsTodos ? reviewMapper.evidenceTodoTotals(query) : EvidenceTotals.zero();
        PageInfo todoInfo = null;
        List<EvidenceTodo> todos = List.of();
        if (showsTodos) {
            todoInfo = pageOf(todoPage, todoTotals.getCount());
            if (todoInfo.totalCount() > 0) {
                todos = reviewMapper.evidenceTodos(todoPage == null ? query : query.page(todoInfo.limit(), todoInfo.offset()));
            }
        }

        EvidenceTotals logTotals = showsLogs ? reviewMapper.evidenceLogTotals(query) : EvidenceTotals.zero();
        PageInfo logInfo = null;
        List<ExecutionLogRow> logs = List.of();
        if (showsLogs) {
            logInfo = pageOf(logPage, logTotals.getCount());
            if (logInfo.totalCount() > 0) {
                logs = reviewMapper.evidenceLogs(logPage == null ? query : query.page(logInfo.limit(), logInfo.offset()));
            }
        } else if (metric == ReviewMetric.BLOCKED && !todos.isEmpty()) {
            logs = reviewMapper.evidenceLogs(query.forTodos(todos.stream().map(EvidenceTodo::getId).toList()));
            attachBlockerReasons(todos, logs);
        }

        boolean actual = metric == ReviewMetric.ACTUAL;
        long count = actual ? logTotals.getCount() : todoTotals.getCount();
        long estimateSum = actual ? 0 : todoTotals.getEstimatedMinutes();
        long actualSum = actual ? logTotals.getActualMinutes() : todoTotals.getActualMinutes();
        return new ReviewEvidence(metric, summary, todos, logs, Math.toIntExact(count), estimateSum, actualSum,
                todoInfo, logInfo);
    }

    private PageInfo pageOf(Integer requested, long total) {
        return requested == null ? PageInfo.whole(total) : pageSettings.page(requested, total);
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
        // ADR-22: same rule and code as ImprovementForm (validation.improvement.max; was review.improvement.tooLong).
        ReviewRules.checkImprovement(trimmed);
        return trimmed;
    }

    private OffsetDateTime now() {
        return seoulDates.now().atOffset(ZoneOffset.UTC);
    }
}
