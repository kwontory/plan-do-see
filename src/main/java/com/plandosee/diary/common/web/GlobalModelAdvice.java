package com.plandosee.diary.common.web;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.plandosee.diary.common.time.SeoulDates;

/**
 * Common model attributes for every page (web-contract.md): the public warning and today in Asia/Seoul.
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
}
