package com.vertiq.auth.v1.repository;

import com.vertiq.auth.v1.entity.UserSessionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionDetailsRepository extends JpaRepository<UserSessionDetails, UUID> {

    Optional<UserSessionDetails> findByRefreshTokenHash(String refreshTokenHash);

    Optional<UserSessionDetails> findByAccessTokenHash(String accessTokenHash);
}
