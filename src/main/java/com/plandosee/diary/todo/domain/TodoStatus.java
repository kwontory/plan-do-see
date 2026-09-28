package com.plandosee.diary.todo.domain;

public enum TodoStatus {
    IN_PROGRESS("진행 중"),
    COMPLETED("완료");

    private final String label;

    TodoStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
