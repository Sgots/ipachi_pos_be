// src/main/java/com/ipachi/pos/security/CurrentRequest.java
package com.ipachi.pos.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class CurrentRequest {
    private Long userId;       // who is acting (audit only)
    private Long terminalId;   // optional
    private Long businessId;   // NEW: scope/owner for queries

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTerminalId() { return terminalId; }
    public void setTerminalId(Long terminalId) { this.terminalId = terminalId; }

    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
}
