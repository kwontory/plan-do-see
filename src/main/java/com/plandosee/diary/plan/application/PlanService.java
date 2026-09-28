package com.plandosee.diary.plan.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.concurrency.WriteTransactions;
import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.paging.Page;
import com.plandosee.diary.common.paging.PageInfo;
import com.plandosee.diary.common.paging.PageSettings;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.plan.application.port.PlanMapper;
import com.plandosee.diary.plan.domain.PlanRules;
import com.plandosee.diary.plan.domain.PlanRevisionRow;
import com.plandosee.diary.plan.domain.PlanRow;

@Service
public class PlanService {

    private final PlanMapper planMapper;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;
    private final WriteTransactions writes;
    private final PageSettings pageSettings;

    public PlanService(PlanMapper planMapper, CurrentUserProvider currentUserProvider,
                       IdGenerator idGenerator, SeoulDates seoulDates, WriteTransactions writes,
                       PageSettings pageSettings) {
        this.planMapper = planMapper;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.seoulDates = seoulDates;
        this.writes = writes;
        this.pageSettings = pageSettings;
    }

    public UUID create(PlanCommand command) {
        return createWithImprovement(command, null);
    }

    /**
     * Used by the improvement transfer inside its transaction (joins it); the carried text never comes from the
     * client.
     */
    public UUID createWithImprovement(PlanCommand command, String carriedImprovement) {
        validate(command);
        return writes.run(() -> insert(command, carriedImprovement));
    }

    private UUID insert(PlanCommand command, String carriedImprovement) {
        OffsetDateTime now = now();
        PlanRow plan = new PlanRow();
        plan.setId(idGenerator.newId());
        plan.setUserId(currentUserProvider.currentUserId());
        apply(plan, command);
        plan.setCarriedImprovement(carriedImprovement);
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        planMapper.insert(plan);
        return plan.getId();
    }

    /**
     * Ownership check for other features (ADR-14): the owned active plan, or NotFoundException. Joins the caller's
     * transaction when there is one.
     */
    @Transactional(readOnly = true)
    public PlanRow requireOwned(UUID planId) {
        return get(planId);
    }

    @Transactional(readOnly = true)
    public PlanRow get(UUID planId) {
        PlanRow plan = planMapper.findActiveOwned(currentUserProvider.currentUserId(), planId);
        if (plan == null) {
            throw new NotFoundException("plan");
        }
        return plan;
    }

    /**
     * Owned active plan or null, for optional links such as a review's next plan.
     */
    @Transactional(readOnly = true)
    public PlanRow findOwned(UUID planId) {
        return planId == null ? null : planMapper.findActiveOwned(currentUserProvider.currentUserId(), planId);
    }

    @Transactional(readOnly = true)
    public List<PlanRow> list() {
        return planMapper.listActiveOwned(currentUserProvider.currentUserId());
    }

    /**
     * ADR-21: one page of the plan list (S00, S01). The count and the rows come from one repeatable-read snapshot,
     * so the page position always matches the rows shown; a page past the end shows the last page.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<PlanRow> listPage(int requestedPage) {
        UUID userId = currentUserProvider.currentUserId();
        PageInfo info = pageSettings.page(requestedPage, planMapper.countActiveOwned(userId));
        List<PlanRow> rows = info.totalCount() == 0 ? List.of()
                : planMapper.listActiveOwnedPage(userId, info.limit(), info.offset());
        return new Page<>(rows, info);
    }

    @Transactional(readOnly = true)
    public List<PlanRevisionRow> revisions(UUID planId) {
        get(planId);
        return planMapper.listRevisionsOwned(currentUserProvider.currentUserId(), planId);
    }

    /**
     * Snapshot of the previous values and the update of the current row commit or roll back together. The plan row
     * lock serializes concurrent revisions so revision numbers never repeat or skip (ADR-15).
     */
    public void revise(UUID planId, PlanCommand command) {
        validate(command);
        writes.run(() -> reviseLocked(planId, command));
    }

    private void reviseLocked(UUID planId, PlanCommand command) {
        UUID userId = currentUserProvider.currentUserId();
        PlanRow current = planMapper.lockActiveOwned(userId, planId);
        if (current == null) {
            throw new NotFoundException("plan");
        }
        OffsetDateTime now = now();

        PlanRevisionRow revision = new PlanRevisionRow();
        revision.setId(idGenerator.newId());
        revision.setPlanId(planId);
        revision.setRevisionNo(planMapper.nextRevisionNo(planId));
        revision.setTitle(current.getTitle());
        revision.setStartDate(current.getStartDate());
        revision.setEndDate(current.getEndDate());
        revision.setPriority(current.getPriority());
        revision.setSuccessCriteria(current.getSuccessCriteria());
        revision.setEstimatedMinutes(current.getEstimatedMinutes());
        revision.setCarriedImprovement(current.getCarriedImprovement());
        revision.setRevisedAt(now);
        planMapper.insertRevision(revision);

        apply(current, command);
        current.setUpdatedAt(now);
        if (planMapper.updateOwned(current) != 1) {
            throw new NotFoundException("plan");
        }
    }

    private void apply(PlanRow plan, PlanCommand command) {
        plan.setTitle(command.title().strip());
        plan.setStartDate(command.startDate());
        plan.setEndDate(command.endDate());
        plan.setPriority(command.priority());
        plan.setSuccessCriteria(command.successCriteria().strip());
        plan.setEstimatedMinutes(command.estimatedMinutes());
    }

    /**
     * ADR-22: the service does not trust the form. Every command is checked at the entrance with the same rules and
     * codes as PlanForm (PlanRules), so a direct call never reaches the DB constraints with a known bad value.
     */
    private void validate(PlanCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("plan command");
        }
        PlanRules.check(command.title(), command.startDate(), command.endDate(), command.priority(),
                command.successCriteria(), command.estimatedMinutes());
    }

    private OffsetDateTime now() {
        return seoulDates.now().atOffset(ZoneOffset.UTC);
    }
}
