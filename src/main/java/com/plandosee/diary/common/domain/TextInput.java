package com.plandosee.diary.common.domain;

/**
 * ADR-30: the one place that decides what submitted text means, used by the web binding (FormBindingAdvice), the
 * form constraint {@code @PlainText} and every command check (TextRule, InputCheck).
 * <ul>
 *   <li>{@link #normalize}: CRLF and lone CR become LF, surrounding whitespace is stripped, and a value without a
 *       single visible character is null (so a title of only NBSP or zero-width spaces is "required", IV-11).</li>
 *   <li>{@link #contentCode}: a single-line value may not contain a line break
 *       ({@link FieldCodes#TEXT_LINE_BREAK}) or any other control character; a multi-line value may contain LF and TAB
 *       but no other control character ({@link FieldCodes#TEXT_CONTROL_CHAR}, NUL included, IV-09, IV-10).</li>
 * </ul>
 * Lengths are counted in UTF-16 units ({@code String.length()}), like the form's {@code @Size} (ADR-22, IV-15).
 */
public final class TextInput {

    /** Kind of input box: one line (text input) or several lines (textarea). */
    public enum Lines {
        SINGLE,
        MULTI
    }

    private TextInput() {
    }

    /** CRLF and lone CR to LF; null stays null. */
    public static String normalizeLineBreaks(String value) {
        if (value == null || value.indexOf('\r') < 0) {
            return value;
        }
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    /** Line breaks to LF, then stripped; null when nothing visible is left. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = normalizeLineBreaks(raw).strip();
        return hasVisible(value) ? value : null;
    }

    /** True when at least one character is neither whitespace, a format or control character, nor a blank filler. */
    public static boolean hasVisible(String value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            if (!invisible(cp)) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    /**
     * The content rule code broken by an already normalized value, or null when it is fine (null and empty are fine:
     * the required rule reports those).
     */
    public static String contentCode(Lines lines, String value) {
        if (value == null) {
            return null;
        }
        String control = null;
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            i += Character.charCount(cp);
            if (lines == Lines.SINGLE && lineBreak(cp)) {
                return FieldCodes.TEXT_LINE_BREAK;
            }
            if (control == null && Character.getType(cp) == Character.CONTROL && !allowedControl(lines, cp)) {
                control = FieldCodes.TEXT_CONTROL_CHAR;
            }
        }
        return control;
    }

    /**
     * At most {@code max} UTF-16 units, never splitting a surrogate pair (the cut moves one unit left instead).
     */
    public static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        int end = max;
        if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static boolean lineBreak(int cp) {
        return cp == '\n' || cp == '\r' || cp == 0x85 || cp == 0x2028 || cp == 0x2029;
    }

    private static boolean allowedControl(Lines lines, int cp) {
        return lines == Lines.MULTI && (cp == '\n' || cp == '\t');
    }

    private static boolean invisible(int cp) {
        if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
            return true;
        }
        int type = Character.getType(cp);
        if (type == Character.FORMAT || type == Character.CONTROL) {
            return true;
        }
        return switch (cp) {
            // Hangul fillers, braille blank, combining grapheme joiner: render as nothing.
            case 0x115F, 0x1160, 0x3164, 0xFFA0, 0x2800, 0x034F -> true;
            default -> false;
        };
    }
}
