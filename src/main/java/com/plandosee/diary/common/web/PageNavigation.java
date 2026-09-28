package com.plandosee.diary.common.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ui.Model;

import com.plandosee.diary.common.paging.PageInfo;

/**
 * ADR-21 (보강): the page navigation of one paged list, computed on the server so the template only prints it
 * (ADR-17 F-2). A web presentation model: {@link PageInfo} stays the page arithmetic the services return, and the
 * choice of which page numbers to offer lives here, in the web layer only.
 * <ul>
 *   <li>number, totalPages, totalCount, firstItem, lastItem: as in PageInfo ("firstItem–lastItem / totalCount").</li>
 *   <li>visible: the list has at least one row (nothing about paging is shown for an empty list).</li>
 *   <li>navigable: more than one page (the page links are shown).</li>
 *   <li>hasPrevious / previousNumber, hasNext / nextNumber: the neighbouring pages (number 0 when absent).</li>
 *   <li>numbers: a window of at most {@value #WINDOW} page numbers around the current page (current − 2 … current + 2,
 *       cut at the first and last page), in ascending order; exactly one slot is current.</li>
 *   <li>showFirst / gapBefore: page 1 is outside the window and gets its own link; pages are skipped between it and
 *       the window.</li>
 *   <li>showLast / gapAfter / lastNumber: the same for the last page.</li>
 * </ul>
 * Links are built by the template from the list's base URL; {@link PageSlot#first()} marks page 1, whose link is the
 * base URL itself (an absent page parameter means page 1).
 */
public record PageNavigation(int number, int totalPages, long totalCount, long firstItem, long lastItem,
                             boolean visible, boolean navigable,
                             boolean hasPrevious, int previousNumber, boolean hasNext, int nextNumber,
                             boolean showFirst, boolean gapBefore, List<PageSlot> numbers,
                             boolean gapAfter, boolean showLast, int lastNumber) {

    /** Most page numbers shown at once around the current page. */
    public static final int WINDOW = 5;
    /** Model attribute suffix: the navigation of the list whose PageInfo is {@code <name>} is {@code <name>Nav}. */
    public static final String SUFFIX = "Nav";

    private static final int RADIUS = WINDOW / 2;

    public PageNavigation {
        numbers = List.copyOf(numbers);
    }

    /** One page number link. first: page 1 (its link is the list's base URL). */
    public record PageSlot(int number, boolean current, boolean first) {
    }

    /** The navigation for the page shown, or null for a list that is not on this page (null PageInfo). */
    public static PageNavigation of(PageInfo page) {
        if (page == null) {
            return null;
        }
        int number = page.number();
        int totalPages = page.totalPages();
        boolean visible = page.totalCount() > 0;
        List<PageSlot> numbers = new ArrayList<>();
        int from = number;
        int to = number;
        if (visible) {
            from = Math.max(1, number - RADIUS);
            to = Math.min(totalPages, number + RADIUS);
            for (int n = from; n <= to; n++) {
                numbers.add(new PageSlot(n, n == number, n == 1));
            }
        }
        return new PageNavigation(number, totalPages, page.totalCount(), page.firstItem(), page.lastItem(),
                visible, totalPages > 1,
                page.hasPrevious(), page.hasPrevious() ? number - 1 : 0,
                page.hasNext(), page.hasNext() ? number + 1 : 0,
                visible && from > 1, visible && from > 2, numbers,
                visible && to < totalPages - 1, visible && to < totalPages, totalPages);
    }

    /**
     * Puts a list's page into the model as {@code name} (PageInfo, unchanged contract) and {@code name + "Nav"}
     * (this navigation). A null page (the list is not on this screen) puts null under both names.
     */
    public static void addTo(Model model, String name, PageInfo page) {
        model.addAttribute(name, page);
        model.addAttribute(name + SUFFIX, of(page));
    }
}
