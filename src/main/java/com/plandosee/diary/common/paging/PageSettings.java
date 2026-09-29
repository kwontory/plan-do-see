package com.plandosee.diary.common.paging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Page size: app.page-size (APP_PAGE_SIZE), 50 by default, 1 to 200. A value outside that range stops the
 * start-up with the property name only.
 */
@Component
public class PageSettings {

    public static final String PAGE_SIZE = "app.page-size";
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    private final int size;

    public PageSettings(@Value("${" + PAGE_SIZE + ":" + DEFAULT_SIZE + "}") int size) {
        this.size = requireSize(size);
    }

    public static int requireSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalStateException(PAGE_SIZE + " must be between 1 and " + MAX_SIZE);
        }
        return size;
    }

    public int size() {
        return size;
    }

    /** The requested page clamped against the total, at the configured size. */
    public PageInfo page(int requested, long totalCount) {
        return PageInfo.of(requested, size, totalCount);
    }
}
