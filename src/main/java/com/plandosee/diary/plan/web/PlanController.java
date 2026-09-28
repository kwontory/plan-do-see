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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.plandosee.diary.common.domain.EditOutcome;
import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.domain.EditSnapshot;
import com.plandosee.diary.common.error.RetryLaterException;
import com.plandosee.diary.common.error.ServiceBusyException;
import com.plandosee.diary.common.web.EditConflicts;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.paging.Page;
import com.plandosee.diary.common.paging.PageRequest;
import com.plandosee.diary.common.web.ConflictResponses;
import com.plandosee.diary.common.web.FlashMessages;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.plan.application.PlanStaleException;
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
    /** ADR-18 E5: the saved content equals the stored plan; nothing changed and no revision was added. */
    public static final String FLASH_UNCHANGED = "flash.plan.unchanged";

    private final PlanService planService;
    private final ReviewService reviewService;

    public PlanController(PlanService planService, ReviewService reviewService) {
        this.planService = planService;
        this.reviewService = reviewService;
    }

    @GetMapping("/plans")
    public String list(@RequestParam(name = "page", required = false) String page, Model model) {
        Page<PlanRow> plans = planService.listPage(PageRequest.parse(page));
        model.addAttribute("plans", plans.items());
        model.addAttribute("page", plans.info());
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
        } catch (RetryLaterException ex) {
            ConflictResponses.rejectForm(result, response, ex);
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
        EditOutcome outcome;
        try {
            outcome = planService.revise(planId, form.toCommand(), form.getVersion());
        } catch (PlanStaleException ex) {
            // ADR-18: 409, the input kept, the latest plan and what differs; the hidden version becomes the latest so
            // saving again is a deliberate overwrite. No DB read here: the snapshot came with the failure.
            form.setVersion(ex.latestVersion());
            EditConflicts.rejectStale(result, response, model, ex, ex.latest());
            return editView(model, ex.latest());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return editView(model, planService.get(planId));
        } catch (ServiceBusyException ex) {
            // ADR-18/ADR-19: out of time or connections. Shown again without reading the database, input kept.
            ConflictResponses.rejectForm(result, response, ex);
            EditConflicts.markSnapshotUnavailable(model);
            return editView(model, standIn(planId, form));
        } catch (RetryLaterException ex) {
            ConflictResponses.rejectForm(result, response, ex);
            return editViewWithLatest(model, planId, form);
        }
        FlashMessages.add(redirect, outcome == EditOutcome.UNCHANGED ? FLASH_UNCHANGED : FLASH_UPDATED);
        return "redirect:/plans/" + planId;
    }

    /** After a collision (ADR-15): the latest plan and what differs, or the stand-in if even the read fails. */
    private String editViewWithLatest(Model model, UUID planId, PlanForm form) {
        EditSnapshot<PlanRow> snapshot;
        try {
            snapshot = planService.latestForEdit(planId, form.toCommand());
        } catch (RetryLaterException readFailed) {
            EditConflicts.markSnapshotUnavailable(model);
            return editView(model, standIn(planId, form));
        }
        EditConflicts.showLatest(model, snapshot.latest(), snapshot.changedFields());
        return editView(model, snapshot.latest());
    }

    /**
     * The plan as far as it is known without the database: its id from the path and the submitted title (shown in
     * the breadcrumb). Everything else is empty; the page says snapshotUnavailable.
     */
    private static PlanRow standIn(UUID planId, PlanForm form) {
        PlanRow plan = new PlanRow();
        plan.setId(planId);
        plan.setTitle(form.getTitle());
        plan.setVersion(form.getVersion() == null ? 0 : form.getVersion());
        return plan;
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
