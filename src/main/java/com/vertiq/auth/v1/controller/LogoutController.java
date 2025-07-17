package com.vertiq.auth.v1.controller;

import com.vertiq.auth.v1.api.LogoutApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.vertiq.auth.v1.model.LogoutResponse;
import com.vertiq.auth.v1.service.LogoutService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class LogoutController implements LogoutApi {

    @Autowired
    private LogoutService logoutService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Override
    public ResponseEntity<LogoutResponse> userLogout(String xRequestedWith) {
        // Extract refreshToken from cookies
        String refreshToken = null;
        if (httpServletRequest.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : httpServletRequest.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        logoutService.logout(refreshToken);
        // Clear the refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        LogoutResponse response = new LogoutResponse();
        response.setMessage("Logged out");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
