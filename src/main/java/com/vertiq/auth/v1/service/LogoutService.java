package com.vertiq.auth.v1.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vertiq.auth.v1.entity.UserSessionDetails;
import com.vertiq.auth.v1.repository.UserSessionDetailsRepository;
import com.vertiq.auth.v1.utils.JwtUtil;

@Service
public class LogoutService {

    private static final Logger logger = LoggerFactory.getLogger(LogoutService.class);

    @Autowired
    private UserSessionDetailsRepository userSessionDetailsRepository;

    public void logout(String refreshToken) {
        String refreshTokenHash = JwtUtil.hashToken(refreshToken);
        Optional<UserSessionDetails> sessionOpt = userSessionDetailsRepository.findByRefreshTokenHash(refreshTokenHash);
        if (sessionOpt.isEmpty() || Boolean.TRUE.equals(sessionOpt.get().getRevoked())) {
            logger.warn("Logout failed: Invalid or already revoked refresh token");
            throw new RuntimeException("Invalid or expired refresh token");
        }
        UserSessionDetails session = sessionOpt.get();
        session.setRevoked(true);
        userSessionDetailsRepository.save(session);
        logger.info("Logout successful for session id: {}", session.getId());
    }
}
