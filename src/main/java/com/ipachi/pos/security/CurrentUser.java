package com.ipachi.pos.security;


import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class CurrentUser {
    private Long id;

    public Long getId() {
        if (id == null) throw new IllegalStateException("Current user not set");
        return id;
    }
    public void setId(Long id) { this.id = id; }
}
