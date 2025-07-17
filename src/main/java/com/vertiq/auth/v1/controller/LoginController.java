package com.vertiq.auth.v1.controller;

import com.vertiq.auth.v1.api.LoginApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.vertiq.auth.v1.model.AuthWithUserResponse;
import com.vertiq.auth.v1.model.LoginRequest;
import com.vertiq.auth.v1.service.LoginService;

@RestController
public class LoginController implements LoginApi {

    @Autowired
    private LoginService loginService;

    @Override
    public ResponseEntity<AuthWithUserResponse> userLogin(LoginRequest loginRequest, String contentType) {
        AuthWithUserResponse response = loginService.login(loginRequest);
        // Set refresh token as HttpOnly cookie
        String refreshToken = loginService.getLastRefreshToken(); // You may need to store this in the service
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }
}
