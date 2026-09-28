package com.plandosee.diary.common.domain;

import java.util.List;

/**
 * ADR-18: what an edit form re-shown after a failed save compares against: the latest stored record and the names
 * of the form fields whose submitted value differs from it (computed by the server; empty when nothing differs).
 */
public record EditSnapshot<T>(T latest, List<String> changedFields) {

    public EditSnapshot {
        changedFields = List.copyOf(changedFields);
    }
}
