package com.vertiq.auth.v1.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vertiq.auth.v1.entity.UserAuthDetails;
import com.vertiq.auth.v1.entity.UserProfile;
import com.vertiq.auth.v1.entity.UserSessionDetails;
import com.vertiq.auth.v1.model.AuthWithUserResponse;
import com.vertiq.auth.v1.model.UserInfo;
import com.vertiq.auth.v1.repository.UserAuthDetailsRepository;
import com.vertiq.auth.v1.repository.UserProfileRepository;
import com.vertiq.auth.v1.repository.UserSessionDetailsRepository;
import com.vertiq.auth.v1.utils.JwtUtil;

import io.jsonwebtoken.Claims;

@Service
public class RefreshService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshService.class);

    @Autowired
    private UserSessionDetailsRepository userSessionDetailsRepository;
    @Autowired
    private UserAuthDetailsRepository userAuthDetailsRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    private String lastRefreshToken;

    public AuthWithUserResponse refresh(String refreshToken) {
        String refreshTokenHash = JwtUtil.hashToken(refreshToken);
        Optional<UserSessionDetails> sessionOpt = userSessionDetailsRepository.findByRefreshTokenHash(refreshTokenHash);
        if (sessionOpt.isEmpty() || Boolean.TRUE.equals(sessionOpt.get().getRevoked())) {
            logger.warn("Refresh failed: Invalid or revoked refresh token");
            throw new RuntimeException("Invalid or expired refresh token");
        }
        UserSessionDetails session = sessionOpt.get();
        // Validate JWT (signature, expiry)
        Claims claims = JwtUtil.parseToken(refreshToken);
        String userId = claims.getSubject();
        Optional<UserAuthDetails> userOpt = userAuthDetailsRepository.findById(java.util.UUID.fromString(userId));
        if (userOpt.isEmpty()) {
            logger.warn("Refresh failed: User not found for userId: {}", userId);
            throw new RuntimeException("Invalid refresh token");
        }
        UserAuthDetails user = userOpt.get();
        // Generate new tokens
        String newAccessToken = JwtUtil.generateAccessToken(user.getUserId(), user.getEmail());
        String newRefreshToken = JwtUtil.generateRefreshToken(user.getUserId());
        // Update session
        session.setAccessTokenHash(JwtUtil.hashToken(newAccessToken));
        session.setRefreshTokenHash(JwtUtil.hashToken(newRefreshToken));
        userSessionDetailsRepository.save(session);
        // Map user info
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
        response.setAccessToken(newAccessToken);
        response.setUser(userInfo);
        this.lastRefreshToken = newRefreshToken;
        return response;
    }

    public String getLastRefreshToken() {
        return lastRefreshToken;
    }
}
