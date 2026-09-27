package com.ai.astrabitassignment.config;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory bearer token store standing in for the session cookie. The
 * frontend (Vercel) and backend (Render) are different registrable domains
 * in production, and Chrome blocks a cross-site cookie as third-party
 * regardless of SameSite=None - so the frontend holds one of these tokens
 * itself and sends it explicitly instead. Tokens live only as long as this
 * process; a redeploy signs everyone out, same as the old session did.
 */
@Component
public class TokenStore {

    public record Principal(String email, String name) {
    }

    private final Map<String, Principal> tokens = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(String email, String name) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, new Principal(email, name));
        return token;
    }

    public Principal resolve(String token) {
        return tokens.get(token);
    }

    public void revoke(String token) {
        tokens.remove(token);
    }
}
