package com.ai.astrabitassignment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Authenticates requests off "Authorization: Bearer <token>" against
 * TokenStore, in place of the session cookie. Builds the same
 * OAuth2AuthenticationToken/OAuth2User shape Spring's own OAuth2 login
 * would put in the SecurityContext, so existing controllers using
 * @AuthenticationPrincipal OAuth2User need no changes.
 */
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final TokenStore tokenStore;

    public BearerTokenAuthenticationFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            TokenStore.Principal resolved = tokenStore.resolve(token);
            if (resolved != null) {
                OAuth2User user = new DefaultOAuth2User(
                        AuthorityUtils.NO_AUTHORITIES,
                        Map.of(
                                "email", resolved.email(),
                                "name", resolved.name() != null ? resolved.name() : ""
                        ),
                        "email"
                );
                SecurityContextHolder.getContext().setAuthentication(
                        new OAuth2AuthenticationToken(user, AuthorityUtils.NO_AUTHORITIES, "google")
                );
            }
        }
        filterChain.doFilter(request, response);
    }
}
