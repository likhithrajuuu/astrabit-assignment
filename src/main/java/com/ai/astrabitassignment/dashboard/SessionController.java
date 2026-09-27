package com.ai.astrabitassignment.dashboard;

import com.ai.astrabitassignment.config.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lets the Next.js frontend ask "am I signed in" without ever hitting a
 * 401/redirect - always 200, since an unauthenticated caller is an
 * expected, normal case here (every first page load), not an error.
 */
@RestController
public class SessionController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenStore tokenStore;

    public SessionController(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

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

    /** Revokes the caller's own bearer token; the frontend clears its stored copy separately. */
    @PostMapping("/api/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            tokenStore.revoke(header.substring(BEARER_PREFIX.length()));
        }
        return ResponseEntity.noContent().build();
    }
}
