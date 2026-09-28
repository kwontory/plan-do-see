package com.plandosee.diary.common.web;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import com.plandosee.diary.common.error.StaleVersionException;

/**
 * ADR-18 re-shown edit forms (web-contract revision 6). Model attributes:
 * <ul>
 *   <li>{@value #LATEST}: the latest stored record (read-only "latest content" panel)</li>
 *   <li>{@value #CHANGED_FIELDS}: List of form field names whose submitted value differs from the latest record
 *       (server computed; the template only prints "changed" badges)</li>
 *   <li>{@value #SNAPSHOT_UNAVAILABLE}: true when the form is shown again without reading the database (the save
 *       ran out of its time budget or pool wait, ADR-19). Only the submitted form and the ids are reliable then;
 *       the record object in the model is a stand-in built from the path and the input.</li>
 * </ul>
 * The global error code is also the default message, so a code without text cannot break the page
 * (same as ConflictResponses).
 */
public final class EditConflicts {

    public static final String LATEST = "latest";
    public static final String CHANGED_FIELDS = "changedFields";
    public static final String SNAPSHOT_UNAVAILABLE = "snapshotUnavailable";

    private EditConflicts() {
    }

    /** HTTP 409, global error {@code <feature>.edit.staleVersion}, latest snapshot and changed fields. */
    public static void rejectStale(BindingResult result, HttpServletResponse response, Model model,
                                   StaleVersionException stale, Object latest) {
        result.reject(stale.code(), null, stale.code());
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        model.addAttribute(LATEST, latest);
        model.addAttribute(CHANGED_FIELDS, stale.changedFields());
    }

    /** Latest snapshot for a form re-shown after another kind of failure (collision, rule violation). */
    public static void showLatest(Model model, Object latest, List<String> changedFields) {
        model.addAttribute(LATEST, latest);
        model.addAttribute(CHANGED_FIELDS, List.copyOf(changedFields));
    }

    public static void markSnapshotUnavailable(Model model) {
        model.addAttribute(SNAPSHOT_UNAVAILABLE, true);
    }
}
