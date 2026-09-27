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

        // Target-typed locals, not String.valueOf(principal.getAttribute(...))
        // directly: getAttribute is <A> A getAttribute(String), and feeding
        // its result straight into the overloaded String.valueOf lets javac
        // resolve the more-specific String.valueOf(char[]) instead of
        // String.valueOf(Object) - the erased generic cast then tries to
        // cast the real String to char[] at runtime and throws.
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");

        return Map.of(
                "authenticated", true,
                "email", email != null ? email : "",
                "name", name != null ? name : ""
        );
    }
}
