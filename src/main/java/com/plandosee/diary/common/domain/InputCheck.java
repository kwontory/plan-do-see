package com.plandosee.diary.common.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.FieldViolation;

/**
 * ADR-30: checks every field of one command with the common rules and collects every broken rule, then
 * {@link #done()} throws one DomainRuleException listing them all (the first one is its field/code). Each field keeps
 * only its first violation, in the order the fields are checked. Used by the self-validating command records
 * (PlanCommand, TodoCommand, ExecutionCommand) and by {@link TextRule#apply}; the web layer shows the same codes.
 * <p>
 * Methods return the value to store (normalized text, the date itself) so a command stores exactly what was checked.
 */
public final class InputCheck {

    private final List<FieldViolation> violations = new ArrayList<>();
    private final Set<String> brokenFields = new HashSet<>();

    /** Records a violation unless the field already has one. */
    public void reject(String field, String code, Object... args) {
        if (brokenFields.add(field)) {
            violations.add(new FieldViolation(field, code, args));
        }
    }

    /** True while the field has no violation (cross-field rules run only on fields that passed). */
    public boolean ok(String field) {
        return !brokenFields.contains(field);
    }

    /** Text by its rule: normalized (TextInput.normalize), content, length. Returns the normalized value. */
    public String text(String field, TextRule rule, String raw) {
        String value = TextInput.normalize(raw);
        if (value == null) {
            if (rule.required()) {
                reject(field, rule.requiredCode());
            }
            return null;
        }
        String content = TextInput.contentCode(rule.lines(), value);
        if (content != null) {
            reject(field, content);
        } else if (value.length() > rule.max()) {
            reject(field, rule.maxCode(), rule.maxArgs());
        }
        return value;
    }

    public <T> T required(String field, T value, String requiredCode) {
        if (value == null) {
            reject(field, requiredCode);
        }
        return value;
    }

    public long range(String field, long value, IntRange range) {
        if (value < range.min()) {
            reject(field, range.minCode());
        } else if (value > range.max()) {
            reject(field, range.maxCode());
        }
        return value;
    }

    /** A date within DateBounds; requiredCode null for an optional date. */
    public LocalDate date(String field, LocalDate value, String requiredCode) {
        if (value == null) {
            if (requiredCode != null) {
                reject(field, requiredCode);
            }
            return null;
        }
        if (!DateBounds.contains(value)) {
            reject(field, FieldCodes.DATE_OUT_OF_RANGE, DateBounds.args());
        }
        return value;
    }

    /** A point in time whose local date in {@code zone} (Asia/Seoul for inputs, DEC-02) is within DateBounds. */
    public OffsetDateTime dateTime(String field, OffsetDateTime value, ZoneId zone, String requiredCode) {
        if (value == null) {
            reject(field, requiredCode);
            return null;
        }
        if (!DateBounds.contains(value, zone)) {
            reject(field, FieldCodes.DATE_OUT_OF_RANGE, DateBounds.args());
        }
        return value;
    }

    /** A rule over several fields that already passed their own rules; reported on {@code field}. */
    public void rule(boolean holds, String field, String code, Object... args) {
        if (!holds) {
            reject(field, code, args);
        }
    }

    public List<FieldViolation> violations() {
        return List.copyOf(violations);
    }

    /** Throws DomainRuleException with every violation, if any. */
    public void done() {
        if (!violations.isEmpty()) {
            throw new DomainRuleException(violations);
        }
    }
}
