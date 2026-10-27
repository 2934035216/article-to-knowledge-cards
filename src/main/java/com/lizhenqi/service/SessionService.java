package com.lizhenqi.service;

import com.lizhenqi.entity.User;
import com.lizhenqi.repository.*;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * 会话服务：登录/登出/获取当前用户
 */
@Service
public class SessionService {

    public static final String SESSION_USER = "loginUser";

    private final UserRepository userRepository;

    public SessionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String username, String password, String role) {
        return userRepository.findByUsername(username)
                .filter(u -> u.getPassword().equals(password))
                .filter(u -> role == null || role.equalsIgnoreCase(u.getRole()) || "USER".equals(role))
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElse(null);
    }

    public void saveUser(HttpSession session, User user) {
        session.setAttribute(SESSION_USER, user);
    }

    public User getCurrentUser(HttpSession session) {
        Object o = session.getAttribute(SESSION_USER);
        return (User) o;
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }
}
