// src/main/java/com/ipachi/pos/security/Action.java
package com.ipachi.pos.security;

public enum Action {
    VIEW, CREATE, EDIT, DELETE;

    public String asSuffix() { return ":" + name(); }
}
