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
import com.plandosee.diary.common.domain.EditOutcome;
import com.plandosee.diary.common.domain.EditSnapshot;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.paging.Page;
import com.plandosee.diary.common.paging.PageInfo;
import com.plandosee.diary.common.paging.PageSettings;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.plan.application.port.PlanMapper;
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
        requireCommand(command);
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
     * Ownership check for other features: the owned active plan, or NotFoundException. Joins the caller's
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
     * One page of the plan list (home and plan pages). The count and the rows come from one repeatable-read snapshot,
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

    /**
     * The latest stored plan and the PlanForm fields where the given input differs from it, for an edit form
     * shown again after a failed save.
     */
    @Transactional(readOnly = true)
    public EditSnapshot<PlanRow> latestForEdit(UUID planId, PlanCommand input) {
        PlanRow latest = get(planId);
        return new EditSnapshot<>(latest, changedFields(latest, input));
    }

    /**
     * Snapshot of the previous values and the update of the current row commit or roll back together. The plan row
     * lock serializes concurrent revisions so revision numbers never repeat or skip.
     */
    public EditOutcome revise(UUID planId, PlanCommand command) {
        return revise(planId, command, null);
    }

    /**
     * expectedVersion is the version the edit form was opened with (null: no check, internal callers).
     * Under the plan row lock, in this order:
     * <ol>
     *   <li>content equal to the stored plan: nothing is written, UNCHANGED (also when the version moved on,
     *       since there is nothing to lose)</li>
     *   <li>version differs: {@link PlanStaleException} with the latest plan and the fields that differ; nothing is
     *       written, so no revision</li>
     *   <li>otherwise the revision and the update commit together and the version goes up by one</li>
     * </ol>
     */
    public EditOutcome revise(UUID planId, PlanCommand command, Integer expectedVersion) {
        requireCommand(command);
        return writes.run(() -> reviseLocked(planId, command, expectedVersion));
    }

    private EditOutcome reviseLocked(UUID planId, PlanCommand command, Integer expectedVersion) {
        UUID userId = currentUserProvider.currentUserId();
        PlanRow current = planMapper.lockActiveOwned(userId, planId);
        if (current == null) {
            throw new NotFoundException("plan");
        }
        List<String> changed = changedFields(current, command);
        if (changed.isEmpty()) {
            return EditOutcome.UNCHANGED;
        }
        if (expectedVersion != null && expectedVersion != current.getVersion()) {
            throw new PlanStaleException(current, changed);
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
        return EditOutcome.UPDATED;
    }

    /**
     * PlanForm field names whose submitted value differs from the stored plan, in form order; "period" is added
     * when either date differs (the latest panel shows the period as one line). Empty means the same content.
     */
    static List<String> changedFields(PlanRow plan, PlanCommand command) {
        List<String> changed = new java.util.ArrayList<>();
        if (!plan.getTitle().equals(command.title())) {
            changed.add("title");
        }
        boolean start = !plan.getStartDate().equals(command.startDate());
        boolean end = !plan.getEndDate().equals(command.endDate());
        if (start) {
            changed.add("startDate");
        }
        if (end) {
            changed.add("endDate");
        }
        if (start || end) {
            changed.add("period");
        }
        if (plan.getPriority() != command.priority()) {
            changed.add("priority");
        }
        if (!plan.getSuccessCriteria().equals(command.successCriteria())) {
            changed.add("successCriteria");
        }
        if (plan.getEstimatedMinutes() != command.estimatedMinutes()) {
            changed.add("estimatedMinutes");
        }
        return changed;
    }

    private void apply(PlanRow plan, PlanCommand command) {
        plan.setTitle(command.title());
        plan.setStartDate(command.startDate());
        plan.setEndDate(command.endDate());
        plan.setPriority(command.priority());
        plan.setSuccessCriteria(command.successCriteria());
        plan.setEstimatedMinutes(command.estimatedMinutes());
    }

    /**
     * The service does not trust the form. A PlanCommand checks itself when it is built (the same
     * rules and codes as PlanForm), so only a missing command is left to reject here.
     */
    private static void requireCommand(PlanCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("plan command");
        }
    }

    private OffsetDateTime now() {
        return seoulDates.now().atOffset(ZoneOffset.UTC);
    }

    /**
     * Account deletion only (ADR-35): physically deletes every plan and plan revision of the logged-in person, soft-deleted rows included.
     * Joins the caller's transaction; the caller (AccountService) deletes in foreign-key order: execution, todo,
     * review, plan, then the person.
     */
    @Transactional
    public void deleteAllOfCurrentUser() {
        UUID userId = currentUserProvider.currentUserId();
        planMapper.deleteRevisionsOwnedBy(userId);
        planMapper.deleteAllOwnedBy(userId);
    }
}
