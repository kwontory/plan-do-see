package com.plandosee.diary.review.web;

import java.util.Arrays;
import java.util.UUID;

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
import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.error.NotFoundException;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.plan.web.PlanForm;
import com.plandosee.diary.review.application.ReviewService;
import com.plandosee.diary.review.application.TransferResult;
import com.plandosee.diary.review.domain.ReviewEvidence;
import com.plandosee.diary.review.domain.ReviewMetric;
import com.plandosee.diary.review.domain.ReviewRow;
import com.plandosee.diary.review.domain.ReviewSummary;

/**
 * S04 review, evidence drill-down, and improvement transfer.
 */
@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final PlanService planService;

    public ReviewController(ReviewService reviewService, PlanService planService) {
        this.reviewService = reviewService;
        this.planService = planService;
    }

    /** 개정 1 Q5: no fields; the improvement is entered on the review page. */
    @PostMapping("/plans/{id}/reviews")
    public String create(@PathVariable("id") UUID planId, RedirectAttributes redirect) {
        UUID reviewId = reviewService.create(planId, null);
        redirect.addFlashAttribute("flashMessage", "회고를 만들었습니다.");
        return "redirect:/reviews/" + reviewId;
    }

    @GetMapping("/reviews/{id}")
    public String detail(@PathVariable("id") UUID reviewId, Model model) {
        ReviewRow review = reviewService.get(reviewId);
        return detailView(model, review, ImprovementForm.of(review.getImprovement()));
    }

    @PutMapping("/reviews/{id}/improvement")
    public String updateImprovement(@PathVariable("id") UUID reviewId,
                                    @Valid @ModelAttribute("improvementForm") ImprovementForm form,
                                    BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return detailView(model, reviewService.get(reviewId), form);
        }
        try {
            reviewService.updateImprovement(reviewId, form.getImprovement());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return detailView(model, reviewService.get(reviewId), form);
        }
        redirect.addFlashAttribute("flashMessage", "개선점을 저장했습니다.");
        return "redirect:/reviews/" + reviewId;
    }

    @GetMapping("/reviews/{id}/evidence")
    public String evidence(@PathVariable("id") UUID reviewId,
                           @RequestParam(name = "metric", required = false) String metricKey, Model model) {
        ReviewMetric metric = ReviewMetric.fromKey(metricKey).orElseThrow(() -> new NotFoundException("metric"));
        ReviewRow review = reviewService.get(reviewId);
        ReviewEvidence evidence = reviewService.evidence(reviewId, metric);
        model.addAttribute("review", review);
        model.addAttribute("plan", planService.get(review.getPlanId()));
        model.addAttribute("metric", metric);
        model.addAttribute("summary", evidence.summary());
        model.addAttribute("todos", evidence.todos());
        model.addAttribute("logs", evidence.logs());
        model.addAttribute("evidenceCount", evidence.evidenceCount());
        model.addAttribute("evidenceEstimatedMinutes", evidence.evidenceEstimatedMinutes());
        model.addAttribute("evidenceActualMinutes", evidence.evidenceActualMinutes());
        return "reviews/evidence";
    }

    @GetMapping("/reviews/{id}/next-plan")
    public String nextPlanForm(@PathVariable("id") UUID reviewId, Model model) {
        ReviewRow review = reviewService.get(reviewId);
        if (review.isTransferred()) {
            return "redirect:/plans/" + review.getNextPlanId();
        }
        model.addAttribute("planForm", new PlanForm());
        return nextPlanView(model, review);
    }

    @PostMapping("/reviews/{id}/next-plan")
    public String transfer(@PathVariable("id") UUID reviewId, @Valid @ModelAttribute("planForm") PlanForm form,
                           BindingResult result, Model model, RedirectAttributes redirect) {
        ReviewRow review = reviewService.get(reviewId);
        if (review.isTransferred()) {
            redirect.addFlashAttribute("flashMessage", "이미 다음 계획으로 넘긴 회고입니다. 새 계획은 만들지 않았습니다.");
            return "redirect:/plans/" + review.getNextPlanId();
        }
        if (result.hasErrors()) {
            return nextPlanView(model, review);
        }
        TransferResult transfer;
        try {
            transfer = reviewService.transferImprovement(reviewId, form.toCommand());
        } catch (DomainRuleException ex) {
            FormErrors.reject(result, ex);
            return nextPlanView(model, reviewService.get(reviewId));
        }
        redirect.addFlashAttribute("flashMessage", transfer.created()
                ? "개선점을 다음 계획으로 넘겼습니다."
                : "이미 다음 계획으로 넘긴 회고입니다. 새 계획은 만들지 않았습니다.");
        return "redirect:/plans/" + transfer.nextPlanId();
    }

    private String detailView(Model model, ReviewRow review, ImprovementForm form) {
        ReviewSummary summary = reviewService.summary(review.getId());
        model.addAttribute("review", review);
        model.addAttribute("plan", planService.get(review.getPlanId()));
        model.addAttribute("nextPlan", planService.findOwned(review.getNextPlanId()));
        model.addAttribute("summary", summary);
        model.addAttribute("improvementForm", form);
        model.addAttribute("metrics", Arrays.stream(ReviewMetric.values())
                .map(metric -> MetricLink.of(review.getId(), metric, summary))
                .toList());
        return "reviews/detail";
    }

    private String nextPlanView(Model model, ReviewRow review) {
        model.addAttribute("review", review);
        model.addAttribute("plan", planService.get(review.getPlanId()));
        model.addAttribute("priorities", Priority.values());
        return "reviews/next-plan";
    }
}
