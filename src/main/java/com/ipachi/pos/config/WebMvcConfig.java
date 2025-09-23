package com.ipachi.pos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.ipachi.pos.config.UserHeaderInterceptor;

/**
 * Register interceptors. Exclude public/auth endpoints from the user-id interceptor
 * so anonymous flows (register/login, static assets) are allowed.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final UserHeaderInterceptor userHeaderInterceptor;

    public WebMvcConfig(UserHeaderInterceptor userHeaderInterceptor) {
        this.userHeaderInterceptor = userHeaderInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userHeaderInterceptor)
                .addPathPatterns("/api/**")
                // Allow public auth endpoints and any other public API to bypass the X-User-Id requirement
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/users/setup",
                        "/api/public/**",
                        "/api/users/**",
                        "/api/me/permissions",
                        "/api/health",
                        "/api/transactions/lines",
                        "/api/metrics",
                        "/api/user-profile/picture/file/**",
                        "/api/user-profile/id-doc/file/**");
    }
}
