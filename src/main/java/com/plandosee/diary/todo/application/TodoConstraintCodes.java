package com.plandosee.diary.todo.application;

import java.util.List;

import org.springframework.stereotype.Component;

import com.plandosee.diary.common.domain.DateBounds;
import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.error.ConstraintCode;
import com.plandosee.diary.common.error.ConstraintCodeSource;
import com.plandosee.diary.common.error.ConstraintViolationTranslator;
import com.plandosee.diary.todo.domain.TodoRules;

/**
 * Todo and tag table constraints (V1, V5) and the code each means. TodoCommand checks the same rules first
 * (TodoRules); a blank title or tag name is caught (or dropped) before the DB, so ck_todos_title and ck_tags_name can
 * only mean "too long".
 */
@Component
public class TodoConstraintCodes implements ConstraintCodeSource {

    @Override
    public List<ConstraintCode> constraintCodes() {
        return List.of(
                new ConstraintCode("ck_todos_title", "title", FieldCodes.TITLE_MAX),
                new ConstraintCode("ck_todos_priority", "priority", FieldCodes.PRIORITY_REQUIRED),
                new ConstraintCode("ck_todos_estimated_minutes", "estimatedMinutes",
                        ConstraintViolationTranslator.VALUE_INVALID),
                new ConstraintCode("ck_tags_name", "tags", TodoRules.TAG_TOO_LONG, String.valueOf(TodoRules.TAG_NAME_MAX)),
                new ConstraintCode("ck_todos_due_date_range", "dueDate", FieldCodes.DATE_OUT_OF_RANGE,
                        DateBounds.args()));
    }
}
