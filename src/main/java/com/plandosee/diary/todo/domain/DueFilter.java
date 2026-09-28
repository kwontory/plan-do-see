package com.plandosee.diary.todo.domain;

/**
 * ADR-06 due-state filter. Every value is judged against today in Asia/Seoul.
 */
public enum DueFilter {
    OVERDUE("지연(미완료이고 마감일이 오늘보다 앞섬)"),
    DUE_TODAY("오늘 마감"),
    UPCOMING("마감 예정(내일 이후)"),
    NO_DUE_DATE("마감일 없음");

    private final String label;

    DueFilter(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
