package com.lizhenqi.controller;

import com.lizhenqi.entity.User;
import com.lizhenqi.entity.UserPreference;
import com.lizhenqi.service.InteractionService;
import com.lizhenqi.service.RecommendService;
import com.lizhenqi.service.SessionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

/**
 * 推荐模块：个人偏好管理 + 香水推荐
 */
@Controller
@RequestMapping
public class RecommendController {

    private final RecommendService recommendService;
    private final InteractionService interactionService;
    private final SessionService sessionService;

    public RecommendController(RecommendService recommendService,
                               InteractionService interactionService,
                               SessionService sessionService) {
        this.recommendService = recommendService;
        this.interactionService = interactionService;
        this.sessionService = sessionService;
    }

    @GetMapping("/recommend")
    public String recommend(HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        model.addAttribute("preference", recommendService.getPreference(u.getId()));
        model.addAttribute("brands", interactionService.allBrands());
        model.addAttribute("categories", interactionService.allCategories());
        model.addAttribute("recommendations", recommendService.recommend(u.getId(), 12));
        return "perfume/recommend";
    }

    @PostMapping("/preference/save")
    public String savePreference(@RequestParam(required = false) String brandIds,
                                 @RequestParam(required = false) String categoryIds,
                                 @RequestParam(required = false) String notes,
                                 HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        recommendService.savePreference(u.getId(), brandIds, categoryIds, notes);
        return "redirect:/recommend";
    }
}
