package com.lizhenqi.config;

import com.lizhenqi.entity.User;
import com.lizhenqi.service.SessionService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpSession;

/**
 * 全局模型注入：所有页面都能直接使用 ${loginUser}
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final SessionService sessionService;

    public GlobalModelAdvice(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @ModelAttribute("loginUser")
    public User loginUser(HttpSession session) {
        return sessionService.getCurrentUser(session);
    }
}
