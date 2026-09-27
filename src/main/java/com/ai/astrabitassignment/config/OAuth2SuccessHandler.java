package com.ai.astrabitassignment.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Runs instead of defaultSuccessUrl. The session Spring just created for
 * this login is useless to the frontend (different registrable domain, so
 * the cookie never comes back on its fetch calls) - so mint our own opaque
 * token in TokenStore and hand it to the frontend on the redirect URL
 * instead. The frontend picks it up and sends it back as an Authorization
 * header on every subsequent request.
 */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final TokenStore tokenStore;

    public OAuth2SuccessHandler(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String token = tokenStore.issue(email, name);

        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "?token=" + encodedToken);
    }
}
