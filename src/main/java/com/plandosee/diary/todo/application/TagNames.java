package com.plandosee.diary.todo.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.plandosee.diary.todo.domain.TodoRules;

/**
 * Parses the comma-separated tag input (ADR-07). Duplicates that differ only by case or spaces collapse to one; more
 * than {@link #MAX_COUNT} distinct names is rejected with {@link #TOO_MANY} (ADR-22 amendment 2).
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
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String part : raw.split(",")) {
            String name = part.strip();
            if (name.isEmpty()) {
                continue;
            }
            TodoRules.checkTagName(name);
            unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
            TodoRules.checkTagCount(unique.size());
        }
        return new ArrayList<>(unique.values());
    }

    public static String join(List<String> names) {
        return String.join(", ", names);
    }
}
