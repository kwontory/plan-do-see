package com.plandosee.diary.common.web;

import com.plandosee.diary.common.domain.DurationInput;
import com.plandosee.diary.common.domain.DurationParts;

/**
 * A form with the estimated time entered as three boxes: request parameters
 * {@code estimatedDays}, {@code estimatedHours}, {@code estimatedMinutesPart} (text, blank = 0). The combined value
 * and every error of the group use the one field name {@value #FIELD}, the same name the service, the DB constraint
 * mapping and the conflict {@code changedFields} use, so the template shows one error for the three boxes.
 * Validated by {@link ValidEstimatedDuration}; the rule itself is {@link DurationInput}.
 */
public interface EstimatedDurationForm {

    String FIELD = "estimatedMinutes";

    String getEstimatedDays();

    String getEstimatedHours();

    String getEstimatedMinutesPart();

    /** The feature's total limit (PlanRules / TodoRules ESTIMATED_MINUTES_MAX). */
    int estimatedMinutesMax();

    default DurationInput.Result estimatedInput() {
        return DurationInput.combine(getEstimatedDays(), getEstimatedHours(), getEstimatedMinutesPart(),
                estimatedMinutesMax());
    }

    /** Box values for a stored minute value (edit form, "load latest"): all three filled, zeros included. */
    static String[] boxes(int minutes) {
        DurationParts parts = DurationParts.of(minutes);
        return new String[] {String.valueOf(parts.days()), String.valueOf(parts.hours()),
                String.valueOf(parts.minutes())};
    }
}
