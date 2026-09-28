package com.plandosee.diary.todo.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.plandosee.diary.common.error.DomainRuleException;

/**
 * Parses the comma-separated tag input (ADR-07). Duplicates that differ only by case or spaces collapse to one.
 */
public final class TagNames {

    public static final int MAX_LENGTH = 50;
    /** Message argument {0}: MAX_LENGTH as a plain string (no number grouping). */
    public static final String TOO_LONG = "todo.tags.tooLong";

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
            if (name.length() > MAX_LENGTH) {
                throw new DomainRuleException("tags", TOO_LONG, String.valueOf(MAX_LENGTH));
            }
            unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
        return new ArrayList<>(unique.values());
    }

    public static String join(List<String> names) {
        return String.join(", ", names);
    }
}
