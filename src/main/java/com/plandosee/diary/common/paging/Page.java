package com.plandosee.diary.common.paging;

import java.util.List;

/**
 * ADR-21: the rows of one page, in the list's stable SQL order, and where that page sits in the whole list.
 */
public record Page<T>(List<T> items, PageInfo info) {

    public Page {
        items = List.copyOf(items);
    }
}
