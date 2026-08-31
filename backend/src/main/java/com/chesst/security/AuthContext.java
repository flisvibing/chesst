package com.chesst.security;

import org.springframework.stereotype.Component;

@Component
public class AuthContext {
    public static Long resolve(Object userIdAttribute) {
        if (userIdAttribute instanceof Long l) return l;
        if (userIdAttribute instanceof Number n) return n.longValue();
        if (userIdAttribute instanceof String s) {
            try { return Long.valueOf(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
