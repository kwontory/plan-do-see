package com.plandosee.diary.plan.application;

import java.time.LocalDate;

import com.plandosee.diary.common.domain.Priority;

public record PlanCommand(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        Priority priority,
        String successCriteria,
        int estimatedMinutes) {
}
