package com.plandosee.diary.todo.domain;

import java.util.UUID;

public class TagRow {

    private UUID id;
    private UUID todoId;
    private String name;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    /** Set only when loaded through a todo link. */
    public UUID getTodoId() {
        return todoId;
    }

    public void setTodoId(UUID todoId) {
        this.todoId = todoId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
