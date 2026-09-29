package com.plandosee.diary.todo.application;

import java.time.LocalDate;
import java.util.List;

import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.domain.InputCheck;
import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.todo.domain.TodoRules;

/**
 * Todo content for create and update (ADR-07 fields). ADR-30: a self-validating command. The constructor checks every
 * field with the common tools and TodoRules (the same codes the form shows, ADR-22) and throws one
 * DomainRuleException listing every broken rule. The title is stored normalized; tagNames becomes the list to store
 * (checked, blank and invisible names dropped, case duplicates merged, never null).
 */
public record TodoCommand(
        String title,
        LocalDate dueDate,
        Priority priority,
        int estimatedMinutes,
        List<String> tagNames) {

    public TodoCommand {
        InputCheck check = new InputCheck();
        title = check.text("title", TodoRules.TITLE, title);
        check.date("dueDate", dueDate, null);
        check.required("priority", priority, FieldCodes.PRIORITY_REQUIRED);
        check.range("estimatedMinutes", estimatedMinutes, TodoRules.ESTIMATED_MINUTES);
        tagNames = TodoRules.tagNames(check, "tags", tagNames);
        check.done();
    }
}
