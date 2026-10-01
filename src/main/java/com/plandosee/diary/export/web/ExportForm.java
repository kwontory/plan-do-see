package com.plandosee.diary.export.web;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import com.plandosee.diary.export.domain.ExportRange;

/**
 * The export range form: {@code from}, {@code to} as {@code yyyy-MM-dd} (Asia/Seoul dates; the format and
 * the date range are checked by FormBindingAdvice). Required here for the field errors; every rule, required ones
 * included, is checked again by ExportRange.
 */
public class ExportForm {

    @NotNull(message = "{" + ExportRange.FROM_REQUIRED + "}")
    private LocalDate from;

    @NotNull(message = "{" + ExportRange.TO_REQUIRED + "}")
    private LocalDate to;

    static ExportForm of(ExportRange range) {
        ExportForm form = new ExportForm();
        form.setFrom(range.from());
        form.setTo(range.to());
        return form;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }
}
