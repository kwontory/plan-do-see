package com.plandosee.diary.review.web;

import java.util.UUID;

import com.plandosee.diary.review.domain.ReviewMetric;
import com.plandosee.diary.review.domain.ReviewSummary;

/**
 * One summary number on S04 with the link to its evidence list. JavaBean getters keep template access simple.
 */
public class MetricLink {

    private final String key;
    private final String label;
    private final int value;
    private final String unit;
    private final String href;

    public MetricLink(String key, String label, int value, String unit, String href) {
        this.key = key;
        this.label = label;
        this.value = value;
        this.unit = unit;
        this.href = href;
    }

    public static MetricLink of(UUID reviewId, ReviewMetric metric, ReviewSummary summary) {
        return new MetricLink(metric.key(), metric.label(), metric.valueOf(summary), metric.unit(),
                "/reviews/" + reviewId + "/evidence?metric=" + metric.key());
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public int getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public String getHref() {
        return href;
    }
}
