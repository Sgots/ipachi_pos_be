package com.ipachi.pos.config;

import com.ipachi.pos.security.CurrentRequest;
import com.ipachi.pos.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserHeaderInterceptor implements HandlerInterceptor {

    private final CurrentRequest ctx;
    public UserHeaderInterceptor(CurrentRequest ctx) { this.ctx = ctx; }
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        String user = req.getHeader("X-User-Id");
           if (user == null || user.isBlank()) {
            resp.setStatus(400);
            return false;
        }
        try {
            ctx.setUserId(Long.parseLong(user));
                        // Terminal is optional for now
                                String term = req.getHeader("X-Terminal-Id");
                        if (term != null && !term.isBlank()) {
                                ctx.setTerminalId(Long.parseLong(term));
                          }            return true;
        } catch (NumberFormatException e) {
            resp.setStatus(400);
            return false;
        }
    }
}
