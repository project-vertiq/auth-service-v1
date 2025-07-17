package com.vertiq.auth.v1.service;

import java.time.LocalDateTime;
import java.util.Optional;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.vertiq.auth.v1.entity.UserAuthDetails;
import com.vertiq.auth.v1.entity.UserProfile;
import com.vertiq.auth.v1.entity.UserSessionDetails;
import com.vertiq.auth.v1.model.AuthWithUserResponse;
import com.vertiq.auth.v1.model.LoginRequest;
import com.vertiq.auth.v1.model.UserInfo;
import com.vertiq.auth.v1.repository.UserAuthDetailsRepository;
import com.vertiq.auth.v1.repository.UserProfileRepository;
import com.vertiq.auth.v1.repository.UserSessionDetailsRepository;
import com.vertiq.auth.v1.utils.JwtUtil;

@Service
public class LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

    @Autowired
    private UserAuthDetailsRepository userAuthDetailsRepository;

    @Autowired
    private UserSessionDetailsRepository userSessionDetailsRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Getter
    private String lastRefreshToken;

    public AuthWithUserResponse login(LoginRequest request) {
        logger.info("Login attempt for email: {} or phone: {}", request.getEmail(), request.getPhone());
        Optional<UserAuthDetails> userOpt = Optional.empty();
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            userOpt = userAuthDetailsRepository.findByEmail(request.getEmail());
        } else if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            userOpt = userAuthDetailsRepository.findByMobileNumber(request.getPhone());
        }
        if (userOpt.isEmpty()) {
            logger.warn("Login failed: User not found");
            throw new RuntimeException("Invalid credentials");
        }
        UserAuthDetails user = userOpt.get();
        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Login failed: Invalid password for userId: {}", user.getUserId());
            throw new RuntimeException("Invalid credentials");
        }
        // Generate JWT tokens
        String accessToken = JwtUtil.generateAccessToken(user.getUserId(), user.getEmail());
        String refreshToken = JwtUtil.generateRefreshToken(user.getUserId());
        this.lastRefreshToken = refreshToken;
        // Store session
        UserSessionDetails session = new UserSessionDetails();
        session.setUserId(user.getUserId());
        session.setAccessTokenHash(JwtUtil.hashToken(accessToken));
        session.setRefreshTokenHash(JwtUtil.hashToken(refreshToken));
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(7)); // Set as per refresh token expiry
        session.setRevoked(false);
        userSessionDetailsRepository.save(session);
        // Fetch user profile
        UserProfile profile = userProfileRepository.findById(user.getUserId()).orElse(null);
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getUserId().toString());
        userInfo.setEmail(user.getEmail());
        if (profile != null) {
            userInfo.setFirstName(profile.getFirstName());
            userInfo.setLastName(profile.getLastName());
            userInfo.setRoles(java.util.Collections.singletonList(profile.getRole()));
        } else {
            userInfo.setFirstName("");
            userInfo.setLastName("");
            userInfo.setRoles(java.util.Collections.singletonList("user"));
        }
        AuthWithUserResponse response = new AuthWithUserResponse();
        response.setAccessToken(accessToken);
        response.setUser(userInfo);
        logger.info("Login successful for userId: {}", user.getUserId());
        return response;
    }

}
