package com.ai.astrabitassignment.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final TokenStore tokenStore;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(TokenStore tokenStore, OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.tokenStore = tokenStore;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF defends against a browser automatically attaching an
                // ambient credential (a cookie) to a forged cross-site
                // request. Auth here is a Bearer token the frontend attaches
                // itself in JS - a forged request from another origin has
                // no way to read it, so there's no ambient credential left
                // for CSRF to exploit, and nothing here still depends on
                // the session cookie CSRF used to protect.
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(new BearerTokenAuthenticationFilter(tokenStore), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/interaction/**", "/actuator/**", "/api/me", "/api/logout").permitAll()
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED),
                        PathPatternRequestMatcher.pathPattern("/api/dashboard/**")
                ))
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler));

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
