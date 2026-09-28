package com.plandosee.diary.common.paging;

/**
 * ADR-21: one page of an offset-paged list, computed on the server so templates only print it.
 * <ul>
 *   <li>number: the page actually shown, 1-based. A requested page below 1 or unreadable becomes 1; a page past
 *       the end becomes the last page; an empty list is page 1.</li>
 *   <li>size: rows per page (app.page-size).</li>
 *   <li>totalCount: rows in the whole list, counted with the same conditions as the list itself.</li>
 *   <li>totalPages: 0 for an empty list.</li>
 *   <li>firstItem / lastItem: 1-based positions of the first and last row on this page ("n–m of N"), 0 when
 *       empty.</li>
 * </ul>
 */
public record PageInfo(int number, int size, long totalCount, int totalPages, boolean hasPrevious, boolean hasNext,
                       long firstItem, long lastItem) {

    /**
     * @param requested the page asked for (any int; clamped)
     * @param size      rows per page, at least 1
     * @param totalCount rows in the whole list
     */
    public static PageInfo of(int requested, int size, long totalCount) {
        if (size < 1) {
            throw new IllegalArgumentException("page size must be at least 1");
        }
        long total = Math.max(0, totalCount);
        int totalPages = (int) ((total + size - 1) / size);
        int number = Math.max(1, Math.min(requested, Math.max(1, totalPages)));
        long first = total == 0 ? 0 : (long) (number - 1) * size + 1;
        long last = total == 0 ? 0 : Math.min((long) number * size, total);
        return new PageInfo(number, size, total, totalPages, number > 1, number < totalPages, first, last);
    }

    /** A single page holding the whole list (unpaged reads that share the paged code path). */
    public static PageInfo whole(long totalCount) {
        long total = Math.max(0, totalCount);
        int size = (int) Math.max(1, Math.min(Integer.MAX_VALUE, total));
        return of(1, size, total);
    }

    /** Rows to skip for this page (SQL OFFSET). */
    public long offset() {
        return (long) (number - 1) * size;
    }

    /** Rows to read for this page (SQL LIMIT). */
    public int limit() {
        return size;
    }
}
