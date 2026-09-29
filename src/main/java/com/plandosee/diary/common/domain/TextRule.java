package com.plandosee.diary.common.domain;

import com.plandosee.diary.common.domain.TextInput.Lines;

/**
 * ADR-30: the rule of one text field, declared once in the feature's rules class (for example
 * {@code PlanRules.TITLE}) and applied by {@link InputCheck#text} to a command, or by {@link #apply} to a single
 * input. The form uses the same values ({@code @Size(max = …)}, {@code @PlainText(lines)}; checked by
 * ValidationRulesConsistencyTest).
 *
 * @param lines        one line or several lines ({@link TextInput#contentCode})
 * @param max          largest length after {@link TextInput#normalize}, in UTF-16 units
 * @param requiredCode code when nothing visible is left; null for an optional field (then the value becomes null)
 * @param maxCode      code when longer than max
 * @param maxArgs      message arguments of maxCode
 */
public record TextRule(Lines lines, int max, String requiredCode, String maxCode, Object... maxArgs) {

    public TextRule {
        if (lines == null || max < 1 || maxCode == null) {
            throw new IllegalArgumentException("text rule");
        }
        maxArgs = maxArgs == null ? new Object[0] : maxArgs.clone();
    }

    public static TextRule singleLine(int max, String requiredCode, String maxCode) {
        return new TextRule(Lines.SINGLE, max, requiredCode, maxCode);
    }

    public static TextRule multiLine(int max, String requiredCode, String maxCode) {
        return new TextRule(Lines.MULTI, max, requiredCode, maxCode);
    }

    public static TextRule optional(Lines lines, int max, String maxCode, Object... maxArgs) {
        return new TextRule(lines, max, null, maxCode, maxArgs);
    }

    public boolean required() {
        return requiredCode != null;
    }

    @Override
    public Object[] maxArgs() {
        return maxArgs.clone();
    }

    /**
     * The normalized value of a single input, or DomainRuleException(field, code) for the broken rule. An optional
     * field without visible text is null.
     */
    public String apply(String field, String raw) {
        InputCheck check = new InputCheck();
        String value = check.text(field, this, raw);
        check.done();
        return value;
    }
}
