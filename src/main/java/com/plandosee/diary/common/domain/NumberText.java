package com.plandosee.diary.common.domain;

/**
 * ADR-30: whole numbers typed as text. Only ASCII digits 0-9 with an optional single leading minus are numbers; hex
 * ({@code 0x10}), {@code #10}, a plus sign, decimals, exponents, full-width or Arabic-Indic digits and surrounding
 * spaces are not (IV-07). Used by the estimated-time boxes (DurationInput) and by every Integer form field
 * (FormBindingAdvice).
 */
public final class NumberText {

    /** Most digits of an int value (2147483647). */
    public static final int INT_DIGITS = 10;

    private NumberText() {
    }

    /** The unsigned value of at most {@code maxDigits} ASCII digits, or null. */
    public static Long unsigned(String text, int maxDigits) {
        if (text == null || text.isEmpty() || text.length() > maxDigits || maxDigits > 18) {
            return null;
        }
        long value = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
            value = value * 10 + (c - '0');
        }
        return value;
    }

    /** An int written as {@code -?[0-9]{1,10}} within the int range, or null. */
    public static Integer parseInt(String text) {
        if (text == null) {
            return null;
        }
        boolean negative = text.startsWith("-");
        Long magnitude = unsigned(negative ? text.substring(1) : text, INT_DIGITS);
        if (magnitude == null) {
            return null;
        }
        long value = negative ? -magnitude : magnitude;
        return value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ? null : (int) value;
    }
}
