package com.plandosee.diary.common.web;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.plandosee.diary.common.domain.DateBounds;
import com.plandosee.diary.common.domain.DurationInput;
import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.common.time.TimeConfig;

/**
 * Common model attributes for every page: today in Asia/Seoul and the display zone so templates convert instants
 * without calling Java classes directly.
 * The accepted date range for the {@code min}/{@code max} attributes of every date and datetime-local box
 * ({@code dateInputMin}, {@code dateInputMax}: yyyy-MM-dd, four-digit years; the template adds the time part), and
 * the box limits of the estimated-time input ({@code durationInputLimits}). Browser attributes are a convenience;
 * the server checks the same constants.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final SeoulDates seoulDates;

    public GlobalModelAdvice(SeoulDates seoulDates) {
        this.seoulDates = seoulDates;
    }

    @ModelAttribute("today")
    public LocalDate today() {
        return seoulDates.today();
    }

    @ModelAttribute("dateInputMin")
    public String dateInputMin() {
        return DateBounds.MIN_TEXT;
    }

    @ModelAttribute("dateInputMax")
    public String dateInputMax() {
        return DateBounds.MAX_TEXT;
    }

    @ModelAttribute("durationInputLimits")
    public DurationInputLimits durationInputLimits() {
        return DurationInputLimits.INSTANCE;
    }

    /** Largest value of each estimated-time box (DurationInput); the smallest is 0. */
    public record DurationInputLimits(int daysMax, int hoursMax, int minutesMax) {

        static final DurationInputLimits INSTANCE =
                new DurationInputLimits(DurationInput.DAYS_MAX, DurationInput.HOURS_MAX, DurationInput.MINUTES_MAX);
    }

    @ModelAttribute("displayZone")
    public ZoneId displayZone() {
        return TimeConfig.SEOUL;
    }
}
