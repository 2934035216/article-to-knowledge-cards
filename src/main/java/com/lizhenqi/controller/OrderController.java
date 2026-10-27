package com.lizhenqi.controller;

import com.lizhenqi.entity.Order;
import com.lizhenqi.entity.User;
import com.lizhenqi.service.OrderService;
import com.lizhenqi.service.SessionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统管理模块：订单管理 + 数据统计
 */
@Controller
@RequestMapping
public class OrderController {

    private final OrderService orderService;
    private final SessionService sessionService;

    public OrderController(OrderService orderService, SessionService sessionService) {
        this.orderService = orderService;
        this.sessionService = sessionService;
    }

    // ===== 下单 =====
    @PostMapping("/order/create")
    public String createOrder(@RequestParam Long perfumeId,
                              @RequestParam(defaultValue = "1") int quantity,
                              @RequestParam(required = false) String receiverName,
                              @RequestParam(required = false) String receiverPhone,
                              @RequestParam(required = false) String receiverAddr,
                              HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        try {
            Order order = orderService.createOrder(u.getId(), perfumeId, quantity,
                    receiverName, receiverPhone, receiverAddr);
            return "redirect:/orders/detail?id=" + order.getId();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("perfumeId", perfumeId);
            return "redirect:/perfume/detail?id=" + perfumeId;
        }
    }

    @GetMapping("/orders")
    public String myOrders(HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        model.addAttribute("orders", orderService.myOrders(u.getId()));
        return "perfume/my-orders";
    }

    @GetMapping("/orders/detail")
    public String orderDetail(@RequestParam Long id, HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null) return "redirect:/login";
        Order order = orderService.findById(id);
        if (order == null) return "redirect:/orders";
        model.addAttribute("order", order);
        return "perfume/order-detail";
    }

    @PostMapping("/order/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status,
                               HttpSession session) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/orders";
        orderService.updateStatus(id, status);
        return "redirect:/admin/orders";
    }

    // ===== 管理员：订单管理 =====
    @GetMapping("/admin/orders")
    public String adminOrders(HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/";
        model.addAttribute("orders", orderService.allOrders());
        return "perfume/admin-orders";
    }

    // ===== 管理员：数据统计 =====
    @GetMapping("/admin/stats")
    public String stats(HttpSession session, Model model) {
        User u = sessionService.getCurrentUser(session);
        if (u == null || !u.isAdmin()) return "redirect:/";

        BigDecimal totalRev = orderService.totalRevenue();
        long totalOrders = orderService.totalOrders();
        List<Object[]> statusBreakdown = orderService.orderStatusBreakdown();

        Map<String, Object[]> statusMap = new HashMap<>();
        for (Object[] row : statusBreakdown) {
            statusMap.put((String) row[0], row);
        }

        model.addAttribute("totalRevenue", totalRev);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("statusMap", statusMap);
        return "perfume/admin-stats";
    }
}
