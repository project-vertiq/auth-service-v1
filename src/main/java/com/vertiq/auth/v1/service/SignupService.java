package com.vertiq.auth.v1.service;

import com.vertiq.auth.v1.entity.UserAuthDetails;
import com.vertiq.auth.v1.entity.UserProfile;
import com.vertiq.auth.v1.model.SignupRequest;
import com.vertiq.auth.v1.repository.UserAuthDetailsRepository;
import com.vertiq.auth.v1.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupService {

    private static final Logger logger = LoggerFactory.getLogger(SignupService.class);

    @Autowired
    private UserAuthDetailsRepository userAuthDetailsRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;

    @Transactional
    public void signup(SignupRequest request) {
        logger.info("Signup attempt for email: {} and mobile: {}", request.getEmail(), request.getMobileNumber());
        // Check if email or mobile already exists
        if (userAuthDetailsRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Signup failed: Email already registered - {}", request.getEmail());
            throw new RuntimeException("Email already registered");
        }
        if (userAuthDetailsRepository.findByMobileNumber(request.getMobileNumber()).isPresent()) {
            logger.warn("Signup failed: Mobile number already registered - {}", request.getMobileNumber());
            throw new RuntimeException("Mobile number already registered");
        }
        // Hash password
        String salt = BCrypt.gensalt();
        String passwordHash = BCrypt.hashpw(request.getPassword(), salt);
        // Create UserAuthDetails
        UserAuthDetails userAuth = new UserAuthDetails();
        userAuth.setEmail(request.getEmail());
        userAuth.setMobileNumber(request.getMobileNumber());
        userAuth.setPasswordHash(passwordHash);
        userAuth.setPasswordSalt(salt);
        userAuth.setSource("classic");
        userAuth = userAuthDetailsRepository.save(userAuth);
        logger.info("UserAuthDetails created for userId: {}", userAuth.getUserId());
        // Create UserProfile
        UserProfile profile = new UserProfile();
        profile.setUserAuthDetails(userAuth);
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setGender(request.getGender());
        profile.setDateOfBirth(request.getDateOfBirth());
        userProfileRepository.save(profile);
        logger.info("UserProfile created for userId: {}", profile.getUserId());
    }
}
