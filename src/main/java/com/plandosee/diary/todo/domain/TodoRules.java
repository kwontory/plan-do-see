package com.plandosee.diary.todo.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.domain.InputCheck;
import com.plandosee.diary.common.domain.IntRange;
import com.plandosee.diary.common.domain.TextInput;
import com.plandosee.diary.common.domain.TextRule;
import com.plandosee.diary.common.error.FieldViolation;

/**
 * The one place for the todo field rules. Declarations of which common tool applies
 * to which field with which constant, plus the tag-list rule (merge, count) used by both the form (TagNames.parse)
 * and TodoCommand. TodoForm's annotations use the same constants, and they equal the CHECK constraints (V1
 * ck_todos_title, ck_todos_estimated_minutes, ck_tags_name; V5 ck_todos_due_date_range);
 * ValidationRulesConsistencyTest compares them.
 */
public final class TodoRules {

    public static final int TITLE_MAX = 200;
    public static final int ESTIMATED_MINUTES_MIN = 0;
    public static final int ESTIMATED_MINUTES_MAX = 525_600;
    public static final int TAG_NAME_MAX = 50;
    /** Message argument {0}: TAG_NAME_MAX as a plain string (no number grouping). */
    public static final String TAG_TOO_LONG = "todo.tags.tooLong";
    /**
     * Most tags one todo may have, counted after blanks are dropped and names that differ only by case or surrounding
     * spaces are merged. No DB constraint: a count over todo_tags rows cannot be a CHECK; TodoService writes the links
     * inside the transaction that holds the todo row lock.
     */
    public static final int TAGS_MAX = 20;
    /** Message argument {0}: TAGS_MAX as a plain string. */
    public static final String TAGS_TOO_MANY = "todo.tags.tooMany";
    /**
     * Longest search text in UTF-16 units (the length unit of every text rule). Longer text is rejected, not cut: the list is not searched and the search box shows
     * {@link #SEARCH_TOO_LONG}.
     */
    public static final int SEARCH_QUERY_MAX = 50;
    /** The search text's field: the list query parameter and TodoFilter's input. */
    public static final String SEARCH_FIELD = "q";
    /** Message argument {0}: SEARCH_QUERY_MAX as a plain string. */
    public static final String SEARCH_TOO_LONG = FieldCodes.SEARCH_TOO_LONG;

    public static final TextRule TITLE = TextRule.singleLine(TITLE_MAX, FieldCodes.TITLE_REQUIRED, FieldCodes.TITLE_MAX);
    public static final IntRange ESTIMATED_MINUTES = new IntRange(ESTIMATED_MINUTES_MIN, ESTIMATED_MINUTES_MAX,
            FieldCodes.ESTIMATED_MINUTES_MIN, FieldCodes.ESTIMATED_MINUTES_MAX);
    /** One tag name; a name with nothing visible is dropped like a blank one. */
    public static final TextRule TAG_NAME = TextRule.optional(TextInput.Lines.SINGLE, TAG_NAME_MAX, TAG_TOO_LONG,
            String.valueOf(TAG_NAME_MAX));
    /**
     * The search text: one line, at most SEARCH_QUERY_MAX; nothing visible means no search. Checked by the list query
     * (web) and by TodoFilter (the service's list command), so neither searches with a text this rule rejects.
     */
    public static final TextRule SEARCH_QUERY = TextRule.optional(TextInput.Lines.SINGLE, SEARCH_QUERY_MAX,
            SEARCH_TOO_LONG, String.valueOf(SEARCH_QUERY_MAX));

    private TodoRules() {
    }

    /**
     * The tag names to store: each checked with {@link #TAG_NAME} (blank or invisible names dropped), names that differ
     * only by case merged (the first spelling wins), at most {@link #TAGS_MAX} distinct names
     * ({@link #TAGS_TOO_MANY}). Violations are reported on {@code field}. Null is no tags.
     */
    public static List<String> tagNames(InputCheck check, String field, List<String> raw) {
        Map<String, String> unique = new LinkedHashMap<>();
        if (raw != null) {
            for (String part : raw) {
                String name = check.text(field, TAG_NAME, part);
                if (name != null && check.ok(field)) {
                    unique.putIfAbsent(tagKey(name), name);
                }
            }
        }
        check.rule(unique.size() <= TAGS_MAX, field, TAGS_TOO_MANY, String.valueOf(TAGS_MAX));
        return List.copyOf(unique.values());
    }

    /** The normalized key two tag names share when they are the same tag (DB: lower(btrim(name))). */
    public static String tagKey(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * The broken rules of a search text ({@link #SEARCH_QUERY} on {@link #SEARCH_FIELD}); empty when it may be
     * searched. Null or invisible-only text is no search and passes.
     */
    public static List<FieldViolation> searchQueryViolations(String raw) {
        InputCheck check = new InputCheck();
        check.text(SEARCH_FIELD, SEARCH_QUERY, raw);
        return check.violations();
    }

    /** The normalized search text, or DomainRuleException(SEARCH_FIELD, code) when {@link #SEARCH_QUERY} rejects it. */
    public static String searchQuery(String raw) {
        return SEARCH_QUERY.apply(SEARCH_FIELD, raw);
    }

    /** Splits the comma-separated tag input of the form into raw names. */
    public static List<String> splitTagInput(String raw) {
        return raw == null ? List.of() : new ArrayList<>(List.of(raw.split(",", -1)));
    }
}
