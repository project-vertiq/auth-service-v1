package com.vertiq.auth.v1.controller;

import com.vertiq.auth.v1.api.OidcApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.vertiq.auth.v1.model.AuthWithUserResponse;
import com.vertiq.auth.v1.model.OidcGoogleRequest;
import com.vertiq.auth.v1.service.OidcService;

@RestController
public class OidcController implements OidcApi {

    @Autowired
    private OidcService oidcService;

    @Override
    public ResponseEntity<AuthWithUserResponse> userOidcLogin(OidcGoogleRequest oidcGoogleRequest, String contentType) {
        String clientId = "976754772382-lac5kapm5l7vhf2019gqaccr3p4lhsr0.apps.googleusercontent.com";
        GoogleIdToken.Payload payload = oidcService.verifyGoogleIdToken(oidcGoogleRequest, clientId);
        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // Lookup or create user, get access/refresh tokens and user info
        OidcService.OidcResult result = oidcService.oidcLoginOrSignupV2(googleSub, email, name);

        // Set refresh token as HttpOnly cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.getAuthWithUserResponse());
    }
}
