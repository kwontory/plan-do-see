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

import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.execution.domain.ExecutionLogRow;
import com.plandosee.diary.plan.application.PlanCommand;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.review.domain.EvidenceQuery;
import com.plandosee.diary.review.domain.EvidenceTodo;
import com.plandosee.diary.review.domain.ReviewEvidence;
import com.plandosee.diary.review.domain.ReviewMetric;
import com.plandosee.diary.review.domain.ReviewRow;
import com.plandosee.diary.review.domain.ReviewScope;
import com.plandosee.diary.review.domain.ReviewSummary;
import com.plandosee.diary.review.infrastructure.ReviewMapper;

@Service
public class ReviewService {

    public static final int IMPROVEMENT_MAX_LENGTH = 1000;

    private final ReviewMapper reviewMapper;
    private final PlanService planService;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;

    public ReviewService(ReviewMapper reviewMapper, PlanService planService, CurrentUserProvider currentUserProvider,
                         IdGenerator idGenerator, SeoulDates seoulDates) {
        this.reviewMapper = reviewMapper;
        this.planService = planService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.seoulDates = seoulDates;
    }

    /**
     * DEC-01: a review belongs to one plan and copies the plan period for display and audit.
     */
    @Transactional
    public UUID create(UUID planId, String improvement) {
        PlanRow plan = planService.get(planId);
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

    @Transactional
    public void updateImprovement(UUID reviewId, String improvement) {
        UUID userId = currentUserProvider.currentUserId();
        ReviewRow review = reviewMapper.lockActiveOwned(userId, reviewId);
        if (review == null) {
            throw new NotFoundException("review");
        }
        if (review.isTransferred()) {
            throw new DomainRuleException("improvement", "이미 다음 계획으로 넘긴 개선점은 수정할 수 없습니다.");
        }
        if (reviewMapper.updateImprovementOwned(userId, reviewId, normalizeImprovement(improvement), now()) != 1) {
            throw new NotFoundException("review");
        }
    }

    @Transactional(readOnly = true)
    public ReviewSummary summary(UUID reviewId) {
        return summary(scope(get(reviewId)));
    }

    /**
     * Summary and evidence are read in one repeatable-read snapshot so the page never shows numbers that disagree.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewEvidence evidence(UUID reviewId, ReviewMetric metric) {
        ReviewScope scope = scope(get(reviewId));
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
     */
    @Transactional
    public TransferResult transferImprovement(UUID reviewId, PlanCommand command) {
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
            throw new DomainRuleException("improvement", "넘길 개선점이 없습니다. 회고에 개선점을 먼저 저장하세요.");
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
            throw new DomainRuleException("improvement", "개선점은 " + IMPROVEMENT_MAX_LENGTH + "자 이하로 입력하세요.");
        }
        return trimmed;
    }

    private OffsetDateTime now() {
        return seoulDates.now().atOffset(ZoneOffset.UTC);
    }
}
