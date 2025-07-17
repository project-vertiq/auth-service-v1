package com.vertiq.auth.v1.repository;

import com.vertiq.auth.v1.entity.UserOidcAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserOidcAccountRepository extends JpaRepository<UserOidcAccount, UUID> {

    Optional<UserOidcAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<UserOidcAccount> findByUserIdAndProvider(UUID userId, String provider);
}
