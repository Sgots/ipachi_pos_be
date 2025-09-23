// src/main/java/com/ipachi/pos/security/AuthzUtil.java
package com.ipachi.pos.security;

import com.ipachi.pos.model.RoleModule;

public final class AuthzUtil {
    private AuthzUtil() {}

    public static String key(RoleModule module, Action action) {
        return module.name() + ":" + action.name(); // e.g., "CASH_TILL:VIEW"
    }
}
