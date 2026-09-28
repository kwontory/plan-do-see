package com.plandosee.diary.common.web;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.plandosee.diary.common.time.SeoulDates;
import com.plandosee.diary.common.time.TimeConfig;

/**
 * Common model attributes for every page (web-contract.md): the public warning, today in Asia/Seoul, and the
 * display zone so templates convert instants without calling Java classes directly (ADR-13).
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final SeoulDates seoulDates;

    public GlobalModelAdvice(SeoulDates seoulDates) {
        this.seoulDates = seoulDates;
    }

    @ModelAttribute("publicNotice")
    public String publicNotice() {
        return PublicNotice.TEXT;
    }

    @ModelAttribute("today")
    public LocalDate today() {
        return seoulDates.today();
    }

    @ModelAttribute("displayZone")
    public ZoneId displayZone() {
        return TimeConfig.SEOUL;
    }
}
