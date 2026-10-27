package com.lizhenqi.controller;

import com.lizhenqi.entity.Perfume;
import com.lizhenqi.entity.User;
import com.lizhenqi.service.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 首页 + 导航入口
 */
@Controller
public class HomeController {

    private final SessionService sessionService;
    private final PerfumeService perfumeService;
    private final RecommendService recommendService;
    private final InteractionService interactionService;

    public HomeController(SessionService sessionService,
                          PerfumeService perfumeService,
                          RecommendService recommendService,
                          InteractionService interactionService) {
        this.sessionService = sessionService;
        this.perfumeService = perfumeService;
        this.recommendService = recommendService;
        this.interactionService = interactionService;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        model.addAttribute("loginUser", sessionService.getCurrentUser(session));
        model.addAttribute("topSales", perfumeService.topSales());
        model.addAttribute("newest", perfumeService.newest());

        // 登录用户 -> 个性化推荐
        User user = sessionService.getCurrentUser(session);
        if (user != null) {
            List<Perfume> recs = recommendService.recommend(user.getId(), 6);
            model.addAttribute("recommendations", recs);
        }

        // 右侧公告
        model.addAttribute("notices", interactionService.allNotices());
        return "perfume/index";
    }

    @GetMapping("/perfumes")
    public String perfumes(@RequestParam(required = false) Long brandId,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "12") int size,
                           Model model) {
        Page<Perfume> list = perfumeService.search(brandId, categoryId, keyword, page, size);
        model.addAttribute("perfumes", list);
        model.addAttribute("brands", perfumeService.allBrands());
        model.addAttribute("categories", perfumeService.allCategories());
        model.addAttribute("brandId", brandId);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        return "perfume/list";
    }

    @GetMapping("/perfume/detail")
    public String detail(@RequestParam Long id, HttpSession session, Model model) {
        Perfume perfume = perfumeService.findById(id);
        if (perfume == null) return "redirect:/";

        User user = sessionService.getCurrentUser(session);
        if (user != null) interactionService.recordBrowse(user.getId(), id);

        model.addAttribute("perfume", perfume);
        model.addAttribute("reviews", interactionService.reviewsByPerfume(id));
        model.addAttribute("isFavorite",
                user != null && interactionService.isFavorite(user.getId(), id));
        model.addAttribute("stats", interactionService.perfumeStats(id));
        return "perfume/detail";
    }

    // 导航到公告查看页
    @GetMapping("/notice/list")
    public String noticeList(Model model) {
        model.addAttribute("notices", interactionService.allNotices());
        return "perfume/notices";
    }

    @GetMapping("/notice/view")
    public String noticeView(@RequestParam Long id, Model model) {
        model.addAttribute("notice", interactionService.findNotice(id));
        return "perfume/notice-detail";
    }
}
