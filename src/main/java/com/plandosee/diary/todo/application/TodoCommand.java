package com.plandosee.diary.todo.application;

import java.time.LocalDate;
import java.util.List;

import com.plandosee.diary.common.domain.Priority;

public record TodoCommand(
        String title,
        LocalDate dueDate,
        Priority priority,
        int estimatedMinutes,
        List<String> tagNames) {
}
