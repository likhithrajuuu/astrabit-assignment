package com.ai.astrabitassignment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces Discord's interactions-endpoint contract: every request to
 * {@code /api/interaction} must carry a valid Ed25519 signature over
 * {@code timestamp + rawBody}, or it must be rejected with 401.
 *
 */
@Component
public class DiscordSignatureFilter extends OncePerRequestFilter {

    private static final String SIGNATURE_HEADER = "X-Signature-Ed25519";
    private static final String TIMESTAMP_HEADER = "X-Signature-Timestamp";
    private static final String INTERACTION_PATH = "/api/interaction";

    private final Ed25519Verifier ed25519Verifier;

    public DiscordSignatureFilter(Ed25519Verifier ed25519Verifier) {
        this.ed25519Verifier = ed25519Verifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERACTION_PATH);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

        String signature = request.getHeader(SIGNATURE_HEADER);
        String timestamp = request.getHeader(TIMESTAMP_HEADER);

        boolean valid = signature != null
                && timestamp != null
                && ed25519Verifier.verify(signature, timestamp, cachedRequest.getCachedBody());

        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }
}
