package com.plandosee.diary.common.paging;

/**
 * The requested page number from the query string ({@code page}, {@code logPage}, ...). Anything that is
 * not a positive whole number is page 1; a page past the end is corrected when the total is known (PageInfo.of).
 * The value is only ever used as a number, never spliced into SQL or a URL.
 */
public final class PageRequest {

    public static final int FIRST = 1;

    private PageRequest() {
    }

    public static int parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return FIRST;
        }
        String value = raw.strip();
        if (!value.matches("[0-9]{1,9}")) {
            return FIRST;
        }
        int page = Integer.parseInt(value);
        return page < FIRST ? FIRST : page;
    }
}
