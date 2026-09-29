package com.plandosee.diary.plan.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.plandosee.diary.common.paging.Page;
import com.plandosee.diary.common.paging.PageRequest;
import com.plandosee.diary.common.web.PageNavigation;
import com.plandosee.diary.plan.application.PlanService;
import com.plandosee.diary.plan.domain.PlanRow;

/**
 * Home page. The public warning comes from GlobalModelAdvice (publicNotice). The plan list is paged:
 * {@code plans} holds the rows of the page, {@code page} the page position (PageInfo), {@code pageNav} the page
 * links to show (PageNavigation).
 */
@Controller
public class HomeController {

    private final PlanService planService;

    public HomeController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/")
    public String index(@RequestParam(name = "page", required = false) String page, Model model) {
        Page<PlanRow> plans = planService.listPage(PageRequest.parse(page));
        model.addAttribute("plans", plans.items());
        PageNavigation.addTo(model, "page", plans.info());
        return "index";
    }
}
