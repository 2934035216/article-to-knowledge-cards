package com.lizhenqi.controller;

import com.lizhenqi.entity.User;
import com.lizhenqi.service.SessionService;
import com.lizhenqi.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

/**
 * 用户管理模块：登录/注册/个人信息/账号状态
 */
@Controller
@RequestMapping
public class UserController {

    private final SessionService sessionService;
    private final UserService userService;

    public UserController(SessionService sessionService, UserService userService) {
        this.sessionService = sessionService;
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String role, Model model) {
        model.addAttribute("role", role != null ? role : "USER");
        return "perfume/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam(required = false, defaultValue = "USER") String role,
                          HttpSession session, Model model) {
        User user = sessionService.login(username, password, role);
        if (user == null) {
            model.addAttribute("error", "账号、密码或角色不匹配，或账号已被禁用");
            model.addAttribute("role", role);
            return "perfume/login";
        }
        sessionService.saveUser(session, user);
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        sessionService.logout(session);
        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(required = false, defaultValue = "USER") String role,
                               Model model) {
        model.addAttribute("role", role);
        return "perfume/register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam(required = false) String nickname,
                             @RequestParam(defaultValue = "USER") String role,
                             Model model) {
        if (userService.exists(username)) {
            model.addAttribute("error", "账号已存在");
            model.addAttribute("role", role);
            return "perfume/register";
        }
        User user = "MERCHANT".equalsIgnoreCase(role)
                ? userService.registerMerchant(username, password, nickname)
                : userService.registerUser(username, password, nickname);
        return "redirect:/login?role=" + user.getRole();
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User user = sessionService.getCurrentUser(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "perfume/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String nickname,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String newPassword,
                                HttpSession session) {
        User user = sessionService.getCurrentUser(session);
        if (user == null) return "redirect:/login";
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(newPassword);
        }
        User saved = userService.save(user);
        sessionService.saveUser(session, saved);
        return "redirect:/profile";
    }

    // ===== 管理员：用户管理 =====
    @GetMapping("/admin/users")
    public String userList(HttpSession session, Model model) {
        User admin = sessionService.getCurrentUser(session);
        if (admin == null || !admin.isAdmin()) return "redirect:/";
        model.addAttribute("users", userService.listAll());
        return "perfume/admin-users";
    }

    @PostMapping("/admin/user/{id}/ban")
    public String banUser(@PathVariable Long id, HttpSession session) {
        User admin = sessionService.getCurrentUser(session);
        if (admin == null || !admin.isAdmin()) return "redirect:/";
        userService.ban(id);
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/user/{id}/unban")
    public String unbanUser(@PathVariable Long id, HttpSession session) {
        User admin = sessionService.getCurrentUser(session);
        if (admin == null || !admin.isAdmin()) return "redirect:/";
        userService.unban(id);
        return "redirect:/admin/users";
    }
}
