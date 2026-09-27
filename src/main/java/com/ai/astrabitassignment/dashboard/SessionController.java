package com.ai.astrabitassignment.dashboard;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lets the Next.js frontend ask "am I signed in" without ever hitting a
 * 401/redirect - always 200, since an unauthenticated caller is an
 * expected, normal case here (every first page load), not an error.
 */
@RestController
public class SessionController {

    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return Map.of("authenticated", false);
        }
        return Map.of(
                "authenticated", true,
                "email", String.valueOf(principal.getAttribute("email")),
                "name", String.valueOf(principal.getAttribute("name"))
        );
    }
}
