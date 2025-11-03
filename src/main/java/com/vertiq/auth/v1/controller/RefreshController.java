package com.vertiq.auth.v1.controller;

import com.vertiq.auth.v1.api.RefreshApi;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.vertiq.auth.v1.model.AuthWithUserResponse;
import com.vertiq.auth.v1.service.RefreshService;

@RestController
public class RefreshController implements RefreshApi {

    @Autowired
    private RefreshService refreshService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Override
    public ResponseEntity<AuthWithUserResponse> userTokenRefresh(String xRequestedWith) {
        // Extract refreshToken from cookies
        String refreshToken = null;
        if (httpServletRequest.getCookies() != null) {
            for (Cookie cookie : httpServletRequest.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        AuthWithUserResponse response = refreshService.refresh(refreshToken);
        String newRefreshToken = refreshService.getLastRefreshToken();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}