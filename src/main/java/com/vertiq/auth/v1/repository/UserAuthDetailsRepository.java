package com.vertiq.auth.v1.repository;

import com.vertiq.auth.v1.entity.UserAuthDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserAuthDetailsRepository extends JpaRepository<UserAuthDetails, UUID> {

    Optional<UserAuthDetails> findByEmail(String email);

    Optional<UserAuthDetails> findByMobileNumber(String mobileNumber);
}
