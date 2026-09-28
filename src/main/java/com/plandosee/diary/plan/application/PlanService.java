package com.plandosee.diary.plan.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plandosee.diary.common.config.CurrentUserProvider;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.id.IdGenerator;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.plan.domain.PlanPeriod;
import com.plandosee.diary.plan.domain.PlanRevisionRow;
import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.plan.infrastructure.PlanMapper;

@Service
public class PlanService {

    private final PlanMapper planMapper;
    private final CurrentUserProvider currentUserProvider;
    private final IdGenerator idGenerator;
    private final SeoulDates seoulDates;

    public PlanService(PlanMapper planMapper, CurrentUserProvider currentUserProvider,
                       IdGenerator idGenerator, SeoulDates seoulDates) {
        this.planMapper = planMapper;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.seoulDates = seoulDates;
    }

    @Transactional
    public UUID create(PlanCommand command) {
        return createWithImprovement(command, null);
    }

    /**
     * Used by the improvement transfer inside its own transaction; the carried text never comes from the client.
     */
    @Transactional
    public UUID createWithImprovement(PlanCommand command, String carriedImprovement) {
        validate(command);
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

    @Transactional(readOnly = true)
    public List<PlanRevisionRow> revisions(UUID planId) {
        get(planId);
        return planMapper.listRevisionsOwned(currentUserProvider.currentUserId(), planId);
    }

    /**
     * Snapshot of the previous values and the update of the current row commit or roll back together.
     */
    @Transactional
    public void revise(UUID planId, PlanCommand command) {
        validate(command);
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

    private void validate(PlanCommand command) {
        PlanPeriod.check(command.startDate(), command.endDate());
    }

    private OffsetDateTime now() {
        return seoulDates.now().atOffset(ZoneOffset.UTC);
    }
}
