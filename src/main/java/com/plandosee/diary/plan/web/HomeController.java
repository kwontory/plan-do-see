package com.plandosee.diary.plan.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.plandosee.diary.plan.application.PlanService;

/**
 * S00. The public warning comes from GlobalModelAdvice (publicNotice).
 */
@Controller
public class HomeController {

    private final PlanService planService;

    public HomeController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("plans", planService.list());
        return "index";
    }
}
