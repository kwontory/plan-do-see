package com.plandosee.diary.todo.domain;

/**
 * Due-state filter. Every value is judged against today in Asia/Seoul.
 * Display labels live in messages.properties as enum.DueFilter.&lt;NAME&gt;.
 */
public enum DueFilter {
    OVERDUE,
    DUE_TODAY,
    UPCOMING,
    NO_DUE_DATE
}
