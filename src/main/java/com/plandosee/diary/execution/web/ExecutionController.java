package com.plandosee.diary.execution.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.execution.application.ExecutionService;
import com.plandosee.diary.todo.web.TodoPageModels;

/**
 * S03 execution record. A rejected range (end before start) re-renders the detail page with a field error.
 */
@Controller
public class ExecutionController {

    private final ExecutionService executionService;
    private final TodoPageModels pages;

    public ExecutionController(ExecutionService executionService, TodoPageModels pages) {
        this.executionService = executionService;
        this.pages = pages;
    }

    @PostMapping("/todos/{id}/executions")
    public String record(@PathVariable("id") UUID todoId, @Valid @ModelAttribute("executionForm") ExecutionForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return pages.detail(model, todoId, form);
        }
        try {
            executionService.record(todoId, form.startedAtInSeoul(), form.endedAtInSeoul(), form.getBlockerReason());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return pages.detail(model, todoId, form);
        }
        redirect.addFlashAttribute("flashMessage", "실행 기록을 저장했습니다. 계획의 예상 시간은 바뀌지 않습니다.");
        return "redirect:/todos/" + todoId;
    }
}
