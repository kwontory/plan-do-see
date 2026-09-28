package com.plandosee.diary.common.domain;

/**
 * Result of saving an edit form (ADR-16, ADR-18 E5). UNCHANGED: the submitted content equals the stored content, so
 * nothing was written: no history row, no version change. The web layer picks the notice from this value.
 */
public enum EditOutcome {
    UPDATED,
    UNCHANGED
}
