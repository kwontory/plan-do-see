package com.plandosee.diary.todo.domain;

import java.util.List;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.domain.FieldRules;
import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.error.DomainRuleException;

/**
 * ADR-22: the one place for the todo field rules (DEC-07, ADR-07). TodoForm's annotations, TagNames, and
 * TodoService's entry check use these values, and they equal the V1 CHECK constraints (ck_todos_title,
 * ck_todos_estimated_minutes, ck_tags_name); ValidationRulesConsistencyTest compares them.
 */
public final class TodoRules {

    public static final int TITLE_MAX = 200;
    public static final int ESTIMATED_MINUTES_MIN = 0;
    public static final int ESTIMATED_MINUTES_MAX = 525_600;
    public static final int TAG_NAME_MAX = 50;
    /** Message argument {0}: TAG_NAME_MAX as a plain string (no number grouping). */
    public static final String TAG_TOO_LONG = "todo.tags.tooLong";

    private TodoRules() {
    }

    /**
     * Throws DomainRuleException(field, code) for the first broken rule, with the same code the form shows. Tag
     * names are the parsed list (TagNames.parse); a blank name is ignored like in the form, an over-long one is
     * {@link #TAG_TOO_LONG}.
     */
    public static void check(String title, Priority priority, int estimatedMinutes, List<String> tagNames) {
        FieldRules.requireText("title", title, TITLE_MAX, FieldCodes.TITLE_REQUIRED, FieldCodes.TITLE_MAX);
        FieldRules.require("priority", priority, FieldCodes.PRIORITY_REQUIRED);
        FieldRules.range("estimatedMinutes", estimatedMinutes, ESTIMATED_MINUTES_MIN, ESTIMATED_MINUTES_MAX,
                FieldCodes.ESTIMATED_MINUTES_MIN, FieldCodes.ESTIMATED_MINUTES_MAX);
        if (tagNames != null) {
            for (String name : tagNames) {
                checkTagName(name);
            }
        }
    }

    public static void checkTagName(String name) {
        if (name != null && name.strip().length() > TAG_NAME_MAX) {
            throw new DomainRuleException("tags", TAG_TOO_LONG, String.valueOf(TAG_NAME_MAX));
        }
    }
}
