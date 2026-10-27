package com.lizhenqi.controller;

import com.lizhenqi.entity.Brand;
import com.lizhenqi.entity.Category;
import com.lizhenqi.entity.Perfume;
import com.lizhenqi.entity.User;
import com.lizhenqi.service.PerfumeService;
import com.lizhenqi.service.SessionService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;

/**
 * 香水信息管理模块：商家和管理员可管理香水/品牌/分类
 */
@Controller
@RequestMapping
public class PerfumeController {

    private final PerfumeService perfumeService;
    private final SessionService sessionService;

    public PerfumeController(PerfumeService perfumeService, SessionService sessionService) {
        this.perfumeService = perfumeService;
        this.sessionService = sessionService;
    }

    private boolean canManage(HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        return u != null && (u.isMerchant() || u.isAdmin());
    }

    private void rejectOrRedirect(HttpSession session) {
        // 在调用方检查，这里只在 view 层面处理
    }

    // ===== 商家香水管理 =====
    @GetMapping("/merchant/perfumes")
    public String myPerfumes(HttpSession session,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Model model) {
        if (!canManage(session)) return "redirect:/";
        User u = sessionService.getCurrentUser(session);
        Page<Perfume> list = u.isMerchant()
                ? perfumeService.findByMerchant(u.getId(), page, size)
                : perfumeService.search(null, null, null, page, size);
        model.addAttribute("perfumes", list);
        model.addAttribute("brands", perfumeService.allBrands());
        model.addAttribute("categories", perfumeService.allCategories());
        return "perfume/merchant-perfumes";
    }

    @GetMapping("/merchant/perfume/add")
    public String addForm(HttpSession session, Model model) {
        if (!canManage(session)) return "redirect:/";
        model.addAttribute("brands", perfumeService.allBrands());
        model.addAttribute("categories", perfumeService.allCategories());
        model.addAttribute("perfume", new Perfume());
        return "perfume/perfume-form";
    }

    @PostMapping("/merchant/perfume/save")
    public String savePerfume(Perfume perfume, HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (!canManage(session)) return "redirect:/";
        if (perfume.getId() == null && u.isMerchant()) {
            perfume.setMerchantId(u.getId());
        }
        if (perfume.getStock() == null) perfume.setStock(0);
        if (perfume.getSales() == null) perfume.setSales(0);
        perfumeService.save(perfume);
        return "redirect:/merchant/perfumes";
    }

    @GetMapping("/merchant/perfume/edit")
    public String editForm(@RequestParam Long id, HttpSession session, Model model) {
        if (!canManage(session)) return "redirect:/";
        model.addAttribute("brands", perfumeService.allBrands());
        model.addAttribute("categories", perfumeService.allCategories());
        model.addAttribute("perfume", perfumeService.findById(id));
        return "perfume/perfume-form";
    }

    @PostMapping("/merchant/perfume/delete")
    public String deletePerfume(@RequestParam Long id, HttpSession session) {
        if (!canManage(session)) return "redirect:/";
        perfumeService.delete(id);
        return "redirect:/merchant/perfumes";
    }

    // ===== 品牌管理（管理员） =====
    @GetMapping("/admin/brands")
    public String brands(HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/";
        model.addAttribute("brands", perfumeService.allBrands());
        model.addAttribute("brand", new Brand());
        return "perfume/admin-brands";
    }

    @PostMapping("/admin/brand/save")
    public String saveBrand(Brand brand, HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/";
        perfumeService.saveBrand(brand);
        return "redirect:/admin/brands";
    }

    @PostMapping("/admin/brand/delete")
    public String deleteBrand(@RequestParam Long id, HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/";
        perfumeService.deleteBrand(id);
        return "redirect:/admin/brands";
    }

    // ===== 分类管理（管理员） =====
    @GetMapping("/admin/categories")
    public String categories(HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/";
        model.addAttribute("categories", perfumeService.allCategories());
        model.addAttribute("category", new Category());
        return "perfume/admin-categories";
    }

    @PostMapping("/admin/category/save")
    public String saveCategory(Category category, HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/";
        perfumeService.saveCategory(category);
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/category/delete")
    public String deleteCategory(@RequestParam Long id, HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/";
        perfumeService.deleteCategory(id);
        return "redirect:/admin/categories";
    }
}
