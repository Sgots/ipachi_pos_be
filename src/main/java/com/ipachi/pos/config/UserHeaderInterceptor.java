// src/main/java/com/ipachi/pos/config/UserHeaderInterceptor.java
package com.ipachi.pos.config;

import com.ipachi.pos.security.CurrentRequest;
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
        String biz  = req.getHeader("X-Business-Id");

        if (biz == null || biz.isBlank()) { // business is required for scoping
            resp.setStatus(400);
            return false;
        }
        try {
            ctx.setBusinessId(Long.parseLong(biz.trim()));
        } catch (NumberFormatException e) {
            resp.setStatus(400);
            return false;
        }

        // userId is still required in your current flow (for audit: who added)
        if (user == null || user.isBlank()) {
            resp.setStatus(400);
            return false;
        }
        try {
            ctx.setUserId(Long.parseLong(user.trim()));
        } catch (NumberFormatException e) {
            resp.setStatus(400);
            return false;
        }

        // Terminal is optional for now
        String term = req.getHeader("X-Terminal-Id");
        if (term != null && !term.isBlank()) {
            try { ctx.setTerminalId(Long.parseLong(term.trim())); } catch (NumberFormatException ignore) { }
        }
        return true;
    }
}
