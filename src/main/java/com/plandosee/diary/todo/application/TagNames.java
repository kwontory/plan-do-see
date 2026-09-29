package com.plandosee.diary.todo.application;

import java.util.List;

import com.plandosee.diary.common.domain.InputCheck;
import com.plandosee.diary.todo.domain.TodoRules;

/**
 * Parses the comma-separated tag input of the form with the one tag-list rule (TodoRules.tagNames):
 * duplicates that differ only by case or spaces collapse to one; an over-long or broken name, or more than
 * {@link #MAX_COUNT} distinct names, is DomainRuleException on the field {@code tags}.
 */
public final class TagNames {

    public static final int MAX_LENGTH = TodoRules.TAG_NAME_MAX;
    /** Message argument {0}: MAX_LENGTH as a plain string (no number grouping). */
    public static final String TOO_LONG = TodoRules.TAG_TOO_LONG;
    public static final int MAX_COUNT = TodoRules.TAGS_MAX;
    /** Message argument {0}: MAX_COUNT as a plain string. */
    public static final String TOO_MANY = TodoRules.TAGS_TOO_MANY;

    private TagNames() {
    }

    public static List<String> parse(String raw) {
        InputCheck check = new InputCheck();
        List<String> names = TodoRules.tagNames(check, "tags", TodoRules.splitTagInput(raw));
        check.done();
        return names;
    }

    public static String join(List<String> names) {
        return String.join(", ", names);
    }
}
