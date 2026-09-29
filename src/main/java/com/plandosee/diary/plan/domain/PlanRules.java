package com.plandosee.diary.plan.domain;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.domain.IntRange;
import com.plandosee.diary.common.domain.TextRule;

/**
 * The one place for the plan field rules. Only declarations: which common tool applies to
 * which field with which constant. PlanCommand checks every command with them (InputCheck), PlanForm's annotations
 * use the same constants, and they equal the CHECK constraints (V1 ck_plans_title, ck_plans_success_criteria,
 * ck_plans_estimated_minutes, ck_plans_period; V5 date ranges); ValidationRulesConsistencyTest compares them.
 * Dates use DateBounds and the period rule PlanPeriod.
 */
public final class PlanRules {

    public static final int TITLE_MAX = 200;
    public static final int SUCCESS_CRITERIA_MAX = 1000;
    public static final int ESTIMATED_MINUTES_MIN = 0;
    public static final int ESTIMATED_MINUTES_MAX = 525_600;

    public static final TextRule TITLE =
            TextRule.singleLine(TITLE_MAX, FieldCodes.TITLE_REQUIRED, FieldCodes.TITLE_MAX);
    public static final TextRule SUCCESS_CRITERIA =
            TextRule.multiLine(SUCCESS_CRITERIA_MAX, FieldCodes.SUCCESS_CRITERIA_REQUIRED, FieldCodes.SUCCESS_CRITERIA_MAX);
    public static final IntRange ESTIMATED_MINUTES = new IntRange(ESTIMATED_MINUTES_MIN, ESTIMATED_MINUTES_MAX,
            FieldCodes.ESTIMATED_MINUTES_MIN, FieldCodes.ESTIMATED_MINUTES_MAX);

    private PlanRules() {
    }
}
