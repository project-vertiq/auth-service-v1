package com.vertiq.auth.v1.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                .requestMatchers("/auth/v1/oidc/google", "/auth/v1/login", "/auth/v1/signup", "/auth/v1/refresh", "/auth/v1/logout").permitAll()
                .anyRequest().authenticated()
                );
        return http.build();
    }

    @Bean
    public org.springframework.web.filter.OncePerRequestFilter loggingFilter() {
        return new org.springframework.web.filter.OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws IOException, ServletException {
                logger.info("Auth Service received request: " + request.getMethod() + " " + request.getRequestURI());
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public org.springframework.security.web.AuthenticationEntryPoint loggingAuthEntryPoint() {
        return (request, response, authException) -> {
            logger.warn("Auth Service 401 Unauthorized for path: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        };
    }

    @Bean
    public org.springframework.security.web.access.AccessDeniedHandler loggingAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            logger.warn("Auth Service 403 Forbidden for path: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        };
    }
}
