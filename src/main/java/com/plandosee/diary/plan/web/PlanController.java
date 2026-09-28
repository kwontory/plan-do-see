package com.plandosee.diary.plan.web;

import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.error.ConcurrencyConflictException;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.web.ConflictResponses;
import com.plandosee.diary.common.web.FlashMessages;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.plan.domain.PlanRow;
import com.plandosee.diary.review.application.ReviewService;

/**
 * S01 plan pages.
 */
@Controller
public class PlanController {

    static final String FORM_VIEW = "plans/form";
    public static final String FLASH_CREATED = "flash.plan.created";
    public static final String FLASH_UPDATED = "flash.plan.updated";

    private final PlanService planService;
    private final ReviewService reviewService;

    public PlanController(PlanService planService, ReviewService reviewService) {
        this.planService = planService;
        this.reviewService = reviewService;
    }

    @GetMapping("/plans")
    public String list(Model model) {
        model.addAttribute("plans", planService.list());
        return "plans/list";
    }

    @GetMapping("/plans/new")
    public String newForm(Model model) {
        model.addAttribute("planForm", new PlanForm());
        return createView(model);
    }

    @PostMapping("/plans")
    public String create(@Valid @ModelAttribute("planForm") PlanForm form, BindingResult result, Model model,
                         RedirectAttributes redirect, HttpServletResponse response) {
        if (result.hasErrors()) {
            return createView(model);
        }
        UUID planId;
        try {
            planId = planService.create(form.toCommand());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return createView(model);
        } catch (ConcurrencyConflictException ex) {
            ConflictResponses.rejectForm(result, response);
            return createView(model);
        }
        FlashMessages.add(redirect, FLASH_CREATED);
        return "redirect:/plans/" + planId;
    }

    @GetMapping("/plans/{id}")
    public String detail(@PathVariable("id") UUID planId, Model model) {
        model.addAttribute("plan", planService.get(planId));
        model.addAttribute("revisions", planService.revisions(planId));
        model.addAttribute("reviews", reviewService.listForPlan(planId));
        model.addAttribute("sourceReview", reviewService.findSourceReview(planId));
        return "plans/detail";
    }

    @GetMapping("/plans/{id}/edit")
    public String editForm(@PathVariable("id") UUID planId, Model model) {
        PlanRow plan = planService.get(planId);
        model.addAttribute("planForm", PlanForm.from(plan));
        return editView(model, plan);
    }

    @PutMapping("/plans/{id}")
    public String update(@PathVariable("id") UUID planId, @Valid @ModelAttribute("planForm") PlanForm form,
                         BindingResult result, Model model, RedirectAttributes redirect,
                         HttpServletResponse response) {
        if (result.hasErrors()) {
            return editView(model, planService.get(planId));
        }
        try {
            planService.revise(planId, form.toCommand());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return editView(model, planService.get(planId));
        } catch (ConcurrencyConflictException ex) {
            ConflictResponses.rejectForm(result, response);
            return editView(model, planService.get(planId));
        }
        FlashMessages.add(redirect, FLASH_UPDATED);
        return "redirect:/plans/" + planId;
    }

    private String createView(Model model) {
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("mode", "create");
        return FORM_VIEW;
    }

    private String editView(Model model, PlanRow plan) {
        model.addAttribute("plan", plan);
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("mode", "edit");
        return FORM_VIEW;
    }
}
