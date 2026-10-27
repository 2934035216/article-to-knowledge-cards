package com.lizhenqi.controller;

import com.lizhenqi.entity.Favorite;
import com.lizhenqi.entity.Review;
import com.lizhenqi.entity.User;
import com.lizhenqi.service.InteractionService;
import com.lizhenqi.service.SessionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

/**
 * 用户互动模块：收藏、评价、浏览记录
 */
@Controller
@RequestMapping
public class InteractionController {

    private final InteractionService interactionService;
    private final SessionService sessionService;

    public InteractionController(InteractionService interactionService, SessionService sessionService) {
        this.interactionService = interactionService;
        this.sessionService = sessionService;
    }

    // ===== 收藏 =====
    @PostMapping("/favorite/toggle")
    public String toggleFavorite(@RequestParam Long perfumeId,
                                 @RequestParam(required = false) String backUrl,
                                 HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        Favorite f = interactionService.toggleFavorite(u.getId(), perfumeId);
        if (f == null) {
            // 取消收藏成功 - 不做特殊提示
        }
        String target = backUrl != null && !backUrl.isEmpty() ? backUrl : "/perfume/detail?id=" + perfumeId;
        return "redirect:" + target;
    }

    @GetMapping("/favorites")
    public String myFavorites(HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        model.addAttribute("favorites", interactionService.myFavorites(u.getId()));
        return "perfume/favorites";
    }

    // ===== 评价 =====
    @PostMapping("/review/add")
    public String addReview(@RequestParam Long perfumeId,
                            @RequestParam int rating,
                            @RequestParam(required = false) String content,
                            HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        Review r = new Review();
        r.setUserId(u.getId());
        r.setPerfumeId(perfumeId);
        r.setRating(Math.max(1, Math.min(5, rating)));
        r.setContent(content);
        interactionService.addReview(r);
        return "redirect:/perfume/detail?id=" + perfumeId;
    }

    @GetMapping("/my-reviews")
    public String myReviews(HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        model.addAttribute("reviews", interactionService.myReviews(u.getId()));
        return "perfume/my-reviews";
    }

    @PostMapping("/review/delete")
    public String deleteReview(@RequestParam Long id, HttpSession session,
                               RedirectAttributes ra) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        interactionService.deleteReview(id);
        return "redirect:/my-reviews";
    }

    // ===== 浏览记录 =====
    @GetMapping("/browse-history")
    public String browseHistory(HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        model.addAttribute("records", interactionService.myBrowseHistory(u.getId()));
        return "perfume/browse-history";
    }

    @PostMapping("/browse-history/clear")
    public String clearBrowse(HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        interactionService.clearBrowseHistory(u.getId());
        return "redirect:/browse-history";
    }

    // ===== 公告管理（管理员） =====
    @GetMapping("/admin/notices")
    public String adminNotices(HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/";
        model.addAttribute("notices", interactionService.allNotices());
        model.addAttribute("notice", new com.lizhenqi.entity.Notice());
        return "perfume/admin-notices";
    }

    @PostMapping("/admin/notice/save")
    public String saveNotice(com.lizhenqi.entity.Notice notice, HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/";
        interactionService.saveNotice(notice);
        return "redirect:/admin/notices";
    }

    @PostMapping("/admin/notice/delete")
    public String deleteNotice(@RequestParam Long id, HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/";
        interactionService.deleteNotice(id);
        return "redirect:/admin/notices";
    }
}
