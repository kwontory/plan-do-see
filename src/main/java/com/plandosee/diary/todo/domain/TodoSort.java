package com.plandosee.diary.todo.domain;

/**
 * ADR-06 sort allowlist. The description is shown on screen and must match the SQL in TodoMapper.xml.
 */
public enum TodoSort {
    DUE("마감일 빠른 순(마감일 없음은 마지막) → 우선순위 높음·보통·낮음 → 먼저 만든 순 → ID 순"),
    PRIORITY("우선순위 높음·보통·낮음 → 마감일 빠른 순(마감일 없음은 마지막) → 먼저 만든 순 → ID 순"),
    CREATED("먼저 만든 순 → ID 순");

    private final String description;

    TodoSort(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    public static TodoSort fromParam(String value) {
        if (value != null) {
            for (TodoSort sort : values()) {
                if (sort.name().equals(value)) {
                    return sort;
                }
            }
        }
        return DUE;
    }
}
