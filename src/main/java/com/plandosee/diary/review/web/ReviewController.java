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

import com.plandosee.diary.common.domain.Priority;
import com.plandosee.diary.common.error.ConcurrencyConflictException;
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.web.ConflictKeys;
import com.plandosee.diary.common.web.ConflictResponses;
import com.plandosee.diary.common.web.FlashMessages;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.plan.web.PlanForm;
import com.plandosee.diary.review.application.ReviewDetail;
import com.plandosee.diary.review.application.ReviewEvidencePage;
import com.plandosee.diary.review.application.ReviewService;
import com.plandosee.diary.review.application.TransferResult;
import com.plandosee.diary.review.domain.ReviewEvidence;
import com.plandosee.diary.review.domain.ReviewMetric;
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
    public static final String FLASH_ALREADY_TRANSFERRED = "flash.review.alreadyTransferred";

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
        } catch (ConcurrencyConflictException ex) {
            FlashMessages.add(redirect, ConflictKeys.FLASH_RETRY);
            return "redirect:/plans/" + planId;
        }
        FlashMessages.add(redirect, FLASH_CREATED);
        return "redirect:/reviews/" + reviewId;
    }

    @GetMapping("/reviews/{id}")
    public String detail(@PathVariable("id") UUID reviewId, Model model) {
        ReviewDetail detail = reviewService.detail(reviewId);
        return detailView(model, detail, ImprovementForm.of(detail.review().getImprovement()));
    }

    @PutMapping("/reviews/{id}/improvement")
    public String updateImprovement(@PathVariable("id") UUID reviewId,
                                    @Valid @ModelAttribute("improvementForm") ImprovementForm form,
                                    BindingResult result, Model model, RedirectAttributes redirect,
                                    HttpServletResponse response) {
        if (result.hasErrors()) {
            return detailView(model, reviewService.detail(reviewId), form);
        }
        try {
            reviewService.updateImprovement(reviewId, form.getImprovement());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return detailView(model, reviewService.detail(reviewId), form);
        } catch (ConcurrencyConflictException ex) {
            ConflictResponses.rejectForm(result, response);
            return detailView(model, reviewService.detail(reviewId), form);
        }
        FlashMessages.add(redirect, FLASH_IMPROVEMENT_SAVED);
        return "redirect:/reviews/" + reviewId;
    }

    @GetMapping("/reviews/{id}/evidence")
    public String evidence(@PathVariable("id") UUID reviewId,
                           @RequestParam(name = "metric", required = false) String metricKey, Model model) {
        ReviewMetric metric = ReviewMetric.fromKey(metricKey).orElseThrow(() -> new NotFoundException("metric"));
        ReviewEvidencePage page = reviewService.evidencePage(reviewId, metric);
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
        model.addAttribute("evidenceEmpty", evidence.empty());
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
     * (TransferResult.created). An invalid form is shown again, unless the page it came from is already out of
     * date because the improvement was transferred meanwhile; then the user is taken to that plan, as the GET does.
     */
    @PostMapping("/reviews/{id}/next-plan")
    public String transfer(@PathVariable("id") UUID reviewId, @Valid @ModelAttribute("planForm") PlanForm form,
                           BindingResult result, Model model, RedirectAttributes redirect,
                           HttpServletResponse response) {
        if (result.hasErrors()) {
            ReviewDetail detail = reviewService.detail(reviewId);
            if (detail.review().isTransferred()) {
                FlashMessages.add(redirect, FLASH_ALREADY_TRANSFERRED);
                return "redirect:/plans/" + detail.review().getNextPlanId();
            }
            return nextPlanView(model, detail);
        }
        TransferResult transfer;
        try {
            transfer = reviewService.transferImprovement(reviewId, form.toCommand());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return nextPlanView(model, reviewService.detail(reviewId));
        } catch (ConcurrencyConflictException ex) {
            ConflictResponses.rejectForm(result, response);
            return nextPlanView(model, reviewService.detail(reviewId));
        }
        FlashMessages.add(redirect, transfer.created() ? FLASH_TRANSFERRED : FLASH_ALREADY_TRANSFERRED);
        return "redirect:/plans/" + transfer.nextPlanId();
    }

    private String detailView(Model model, ReviewDetail detail, ImprovementForm form) {
        model.addAttribute("review", detail.review());
        model.addAttribute("plan", detail.plan());
        model.addAttribute("nextPlan", detail.nextPlan());
        model.addAttribute("summary", detail.summary());
        model.addAttribute("improvementForm", form);
        model.addAttribute("metricValues", metricValues(detail.summary()));
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

    private String nextPlanView(Model model, ReviewDetail detail) {
        model.addAttribute("review", detail.review());
        model.addAttribute("plan", detail.plan());
        model.addAttribute("priorities", Priority.values());
        return "reviews/next-plan";
    }
}
