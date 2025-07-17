package com.vertiq.auth.v1.controller;

import com.vertiq.auth.v1.api.SignupApi;
import com.vertiq.auth.v1.model.SignupRequest;
import com.vertiq.auth.v1.model.SignupResponse;
import com.vertiq.auth.v1.service.SignupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SignupController implements SignupApi {

    @Autowired
    private SignupService signupService;

    @Override
    public ResponseEntity<SignupResponse> userSignup(SignupRequest signupRequest, String contentType) {
        signupService.signup(signupRequest);
        SignupResponse response = new SignupResponse();
        response.setMessage("Signup successful");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
