package com.plandosee.diary.review.application;

import java.util.List;

import com.plandosee.diary.common.error.StaleVersionException;
import com.plandosee.diary.review.domain.ReviewRow;

/**
 * The improvement form was out of date. latest is the review as stored now; changedFields is
 * ["improvement"].
 */
public class ReviewStaleException extends StaleVersionException {

    public static final String CODE = "review.edit.staleVersion";

    private final transient ReviewRow latest;

    public ReviewStaleException(ReviewRow latest, List<String> changedFields) {
        super(CODE, latest.getVersion(), changedFields);
        this.latest = latest;
    }

    public ReviewRow latest() {
        return latest;
    }
}
