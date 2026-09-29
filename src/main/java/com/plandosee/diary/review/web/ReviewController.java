package com.plandosee.diary.review.web;

import java.util.LinkedHashMap;
import java.util.Map;
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

import com.plandosee.diary.common.domain.DurationParts;
import com.plandosee.diary.common.domain.EditOutcome;
import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.error.RetryLaterException;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.error.ServiceBusyException;
import com.plandosee.diary.common.paging.PageRequest;
import com.plandosee.diary.common.web.ConflictKeys;
import com.plandosee.diary.common.web.ConflictResponses;
import com.plandosee.diary.common.web.EditConflicts;
import com.plandosee.diary.common.web.FlashMessages;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.common.web.PageNavigation;
import com.plandosee.diary.plan.web.PlanForm;
import com.plandosee.diary.review.application.ReviewDetail;
import com.plandosee.diary.review.application.ReviewEvidencePage;
import com.plandosee.diary.review.application.ImprovementTransferredException;
import com.plandosee.diary.review.application.ReviewService;
import com.plandosee.diary.review.application.ReviewStaleException;
import com.plandosee.diary.review.application.TransferResult;
import com.plandosee.diary.review.domain.ReviewEvidence;
import com.plandosee.diary.review.domain.ReviewMetric;
import com.plandosee.diary.review.domain.ReviewRow;
import com.plandosee.diary.review.domain.ReviewSummary;

/**
 * S04 review, evidence drill-down, and improvement transfer. Each page is one service snapshot (ADR-14 C-4);
 * the transfer notice comes only from the service's TransferResult (ADR-14 C-2); the plan period rule is the
 * form-level constraint on PlanForm (ADR-14 C-3).
 */
@Controller
public class ReviewController {

    public static final String FLASH_CREATED = "flash.review.created";
    public static final String FLASH_IMPROVEMENT_SAVED = "flash.review.improvementSaved";
    public static final String FLASH_TRANSFERRED = "flash.review.transferred";
    /** ADR-18 E5: the saved improvement equals the stored one; nothing changed. */
    public static final String FLASH_IMPROVEMENT_UNCHANGED = "flash.review.improvementUnchanged";
    /**
     * ADR-18 E10: the next-plan form was submitted for a review whose improvement had already been carried to a next
     * plan. Global form error; the page is shown again (409) with the input and the existing next plan.
     */
    public static final String NEXT_PLAN_ALREADY_TRANSFERRED = "review.nextPlan.alreadyTransferred";

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Revision 1 Q5: no fields; the improvement is entered on the review page. */
    @PostMapping("/plans/{id}/reviews")
    public String create(@PathVariable("id") UUID planId, RedirectAttributes redirect) {
        UUID reviewId;
        try {
            reviewId = reviewService.create(planId, null);
        } catch (RetryLaterException ex) {
            FlashMessages.add(redirect, ConflictResponses.flashKey(ex, ConflictKeys.FLASH_RETRY));
            return "redirect:/plans/" + planId;
        }
        FlashMessages.add(redirect, FLASH_CREATED);
        return "redirect:/reviews/" + reviewId;
    }

    @GetMapping("/reviews/{id}")
    public String detail(@PathVariable("id") UUID reviewId, Model model) {
        ReviewDetail detail = reviewService.detail(reviewId);
        return detailView(model, detail, ImprovementForm.of(detail.review()));
    }

    @PutMapping("/reviews/{id}/improvement")
    public String updateImprovement(@PathVariable("id") UUID reviewId,
                                    @Valid @ModelAttribute("improvementForm") ImprovementForm form,
                                    BindingResult result, Model model, RedirectAttributes redirect,
                                    HttpServletResponse response) {
        if (result.hasErrors()) {
            return detailView(model, reviewService.detail(reviewId), form);
        }
        EditOutcome outcome;
        try {
            outcome = reviewService.updateImprovement(reviewId, form.getImprovement(), form.getVersion());
        } catch (ReviewStaleException ex) {
            // ADR-18: 409, input kept, the latest review and what differs; hidden version = latest.
            form.setVersion(ex.latestVersion());
            EditConflicts.rejectStale(result, response, model, ex, ex.latest());
            return detailView(model, reviewService.detail(reviewId), form);
        } catch (ImprovementTransferredException ex) {
            // ADR-18 E10 / 11.5.5: already carried to a next plan. 409 with the transferred improvement (review,
            // latest) and the next plan (nextPlan); the input is kept for copying.
            FormErrors.reject(result, ex);
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            ReviewDetail detail = reviewService.detail(reviewId);
            EditConflicts.showLatest(model, detail.review(), java.util.List.of());
            return detailView(model, detail, form);
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return detailAgain(model, reviewId, form);
        } catch (ServiceBusyException ex) {
            // ADR-19: out of time or connections. Shown again without reading the database, input kept (503).
            ConflictResponses.rejectForm(result, response, ex);
            return detailUnavailable(model, reviewId, form);
        } catch (RetryLaterException ex) {
            ConflictResponses.rejectForm(result, response, ex);
            return detailAgain(model, reviewId, form);
        }
        FlashMessages.add(redirect, outcome == EditOutcome.UNCHANGED ? FLASH_IMPROVEMENT_UNCHANGED : FLASH_IMPROVEMENT_SAVED);
        return "redirect:/reviews/" + reviewId;
    }

    @GetMapping("/reviews/{id}/evidence")
    public String evidence(@PathVariable("id") UUID reviewId,
                           @RequestParam(name = "metric", required = false) String metricKey,
                           @RequestParam(name = "page", required = false) String todoPage,
                           @RequestParam(name = "logPage", required = false) String logPage, Model model) {
        ReviewMetric metric = ReviewMetric.fromKey(metricKey).orElseThrow(() -> new NotFoundException("metric"));
        ReviewEvidencePage page = reviewService.evidencePage(reviewId, metric, PageRequest.parse(todoPage),
                PageRequest.parse(logPage));
        ReviewEvidence evidence = page.evidence();
        model.addAttribute("review", page.review());
        model.addAttribute("plan", page.plan());
        model.addAttribute("metric", metric);
        model.addAttribute("metricKey", metric.key());
        model.addAttribute("summary", evidence.summary());
        model.addAttribute("todos", evidence.todos());
        model.addAttribute("logs", evidence.logs());
        model.addAttribute("evidenceCount", evidence.evidenceCount());
        model.addAttribute("evidenceEstimatedMinutes", evidence.evidenceEstimatedMinutes());
        model.addAttribute("evidenceActualMinutes", evidence.evidenceActualMinutes());
        model.addAttribute("evidenceVarianceMinutes", evidence.evidenceVarianceMinutes());
        model.addAttribute("evidenceEstimatedDuration", evidence.evidenceEstimatedDuration());
        model.addAttribute("evidenceActualDuration", evidence.evidenceActualDuration());
        model.addAttribute("evidenceVarianceDuration", evidence.evidenceVarianceDuration());
        model.addAttribute("evidenceEmpty", evidence.empty());
        PageNavigation.addTo(model, "page", evidence.todoPage());
        PageNavigation.addTo(model, "logPage", evidence.logPage());
        return "reviews/evidence";
    }

    @GetMapping("/reviews/{id}/next-plan")
    public String nextPlanForm(@PathVariable("id") UUID reviewId, Model model) {
        ReviewDetail detail = reviewService.detail(reviewId);
        if (detail.review().isTransferred()) {
            return "redirect:/plans/" + detail.review().getNextPlanId();
        }
        model.addAttribute("planForm", new PlanForm());
        return nextPlanView(model, detail);
    }

    /**
     * A valid form always goes to the service, which decides under the review lock whether a plan is created
     * (TransferResult.created). ADR-18 E10: when the improvement had already been carried to a next plan (the page
     * was out of date, or the form was sent twice), nothing is created and the form is shown again with HTTP 409,
     * the input kept, the global error {@link #NEXT_PLAN_ALREADY_TRANSFERRED}, and the existing next plan
     * (model nextPlan). The GET still goes straight to that plan (no input to keep).
     */
    @PostMapping("/reviews/{id}/next-plan")
    public String transfer(@PathVariable("id") UUID reviewId, @Valid @ModelAttribute("planForm") PlanForm form,
                           BindingResult result, Model model, RedirectAttributes redirect,
                           HttpServletResponse response) {
        if (result.hasErrors()) {
            ReviewDetail detail = reviewService.detail(reviewId);
            if (detail.review().isTransferred()) {
                return alreadyTransferred(model, result, response, detail);
            }
            return nextPlanView(model, detail);
        }
        TransferResult transfer;
        try {
            transfer = reviewService.transferImprovement(reviewId, form.toCommand());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return nextPlanAgain(model, reviewId);
        } catch (ServiceBusyException ex) {
            // ADR-19: out of time or connections. Shown again without reading the database, input kept (503).
            ConflictResponses.rejectForm(result, response, ex);
            return nextPlanUnavailable(model, reviewId);
        } catch (RetryLaterException ex) {
            ConflictResponses.rejectForm(result, response, ex);
            return nextPlanAgain(model, reviewId);
        }
        if (!transfer.created()) {
            return alreadyTransferred(model, result, response, reviewService.detail(reviewId));
        }
        FlashMessages.add(redirect, FLASH_TRANSFERRED);
        return "redirect:/plans/" + transfer.nextPlanId();
    }

    private String alreadyTransferred(Model model, BindingResult result, HttpServletResponse response,
                                      ReviewDetail detail) {
        result.reject(NEXT_PLAN_ALREADY_TRANSFERRED, null, NEXT_PLAN_ALREADY_TRANSFERRED);
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        model.addAttribute("nextPlan", detail.nextPlan());
        return nextPlanView(model, detail);
    }

    /** The review page after a failed save; without the database if even that read does not fit in the budget. */
    private String detailAgain(Model model, UUID reviewId, ImprovementForm form) {
        return EditConflicts.readForForm(() -> reviewService.detail(reviewId))
                .map(detail -> detailView(model, detail, form))
                .orElseGet(() -> detailUnavailable(model, reviewId, form));
    }

    /**
     * ADR-15/ADR-19: the review page from the request alone. Model: review (a stand-in holding the id from the path
     * and the submitted version; improvement, period, nextPlanId and the rest unknown), improvementForm (the input),
     * snapshotUnavailable = true. plan, nextPlan, summary and metricValues are absent (null).
     */
    private String detailUnavailable(Model model, UUID reviewId, ImprovementForm form) {
        EditConflicts.markSnapshotUnavailable(model);
        model.addAttribute("review", standIn(reviewId, form.getVersion()));
        model.addAttribute("improvementForm", form);
        return "reviews/detail";
    }

    /** The next-plan page after a failed save; without the database if even that read does not fit in the budget. */
    private String nextPlanAgain(Model model, UUID reviewId) {
        return EditConflicts.readForForm(() -> reviewService.detail(reviewId))
                .map(detail -> nextPlanView(model, detail))
                .orElseGet(() -> nextPlanUnavailable(model, reviewId));
    }

    /**
     * ADR-15/ADR-19: the next-plan page from the request alone. Model: review (a stand-in holding the id from the
     * path; the improvement to carry is unknown), planForm (the input, already in the model), priorities,
     * snapshotUnavailable = true. plan and nextPlan are absent (null).
     */
    private String nextPlanUnavailable(Model model, UUID reviewId) {
        EditConflicts.markSnapshotUnavailable(model);
        model.addAttribute("review", standIn(reviewId, null));
        model.addAttribute("priorities", Priority.values());
        return "reviews/next-plan";
    }

    /** The review as far as it is known without the database: its id and, for the improvement form, the version. */
    private static ReviewRow standIn(UUID reviewId, Integer version) {
        ReviewRow review = new ReviewRow();
        review.setId(reviewId);
        review.setVersion(version == null ? 0 : version);
        return review;
    }

    private String detailView(Model model, ReviewDetail detail, ImprovementForm form) {
        model.addAttribute("review", detail.review());
        model.addAttribute("plan", detail.plan());
        model.addAttribute("nextPlan", detail.nextPlan());
        model.addAttribute("summary", detail.summary());
        model.addAttribute("improvementForm", form);
        model.addAttribute("metricValues", metricValues(detail.summary()));
        model.addAttribute("metricDurations", metricDurations(detail.summary()));
        return "reviews/detail";
    }

    /**
     * metric key to value only. Labels, units, sign notation, card order, and evidence links are template concerns
     * (ADR-13, web-contract revision 3).
     */
    static Map<String, Long> metricValues(ReviewSummary summary) {
        Map<String, Long> values = new LinkedHashMap<>();
        for (ReviewMetric metric : ReviewMetric.values()) {
            values.put(metric.key(), metric.valueOf(summary));
        }
        return values;
    }

    /**
     * ADR-29: the time metrics (estimated, actual, variance; same keys as metricValues) as days, hours and minutes.
     * Count metrics are not in this map.
     */
    static Map<String, DurationParts> metricDurations(ReviewSummary summary) {
        Map<String, DurationParts> durations = new LinkedHashMap<>();
        durations.put(ReviewMetric.ESTIMATED.key(), summary.estimatedDuration());
        durations.put(ReviewMetric.ACTUAL.key(), summary.actualDuration());
        durations.put(ReviewMetric.VARIANCE.key(), summary.varianceDuration());
        return durations;
    }

    private String nextPlanView(Model model, ReviewDetail detail) {
        model.addAttribute("review", detail.review());
        model.addAttribute("plan", detail.plan());
        model.addAttribute("priorities", Priority.values());
        return "reviews/next-plan";
    }
}
