package com.vertiq.auth.v1.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.vertiq.auth.v1.entity.UserAuthDetails;
import com.vertiq.auth.v1.entity.UserOidcAccount;
import com.vertiq.auth.v1.entity.UserProfile;
import com.vertiq.auth.v1.entity.UserSessionDetails;
import com.vertiq.auth.v1.model.AuthWithUserResponse;
import com.vertiq.auth.v1.model.OidcGoogleRequest;
import com.vertiq.auth.v1.model.UserInfo;
import com.vertiq.auth.v1.repository.UserAuthDetailsRepository;
import com.vertiq.auth.v1.repository.UserOidcAccountRepository;
import com.vertiq.auth.v1.repository.UserProfileRepository;
import com.vertiq.auth.v1.repository.UserSessionDetailsRepository;
import com.vertiq.auth.v1.utils.JwtUtil;

@Service
public class OidcService {

    private static final Logger logger = LoggerFactory.getLogger(OidcService.class);

    @Autowired
    private UserAuthDetailsRepository userAuthDetailsRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private UserSessionDetailsRepository userSessionDetailsRepository;
    @Autowired
    private UserOidcAccountRepository userOidcAccountRepository;

    /**
     * Verifies the Google ID token and returns the payload if valid, else
     * throws.
     *
     * @param request OidcGoogleRequest containing the idToken
     * @param clientId Your Google OAuth2 client ID
     * @return GoogleIdToken.Payload if valid
     */
    public GoogleIdToken.Payload verifyGoogleIdToken(OidcGoogleRequest request, String clientId) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build();
            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                logger.warn("Invalid Google ID token");
                throw new RuntimeException("Invalid Google ID token");
            }
            return idToken.getPayload();
        } catch (Exception e) {
            logger.error("Failed to verify Google ID token", e);
            throw new RuntimeException("Failed to verify Google ID token", e);
        }
    }

    @Transactional
    public OidcResult oidcLoginOrSignup(String googleSub, String email, String name) {
        // Check for existing OIDC account
        Optional<UserOidcAccount> oidcOpt = userOidcAccountRepository.findByProviderAndProviderUserId("google", googleSub);
        UserAuthDetails user;
        if (oidcOpt.isPresent()) {
            // Existing OIDC account, get user
            user = userAuthDetailsRepository.findById(oidcOpt.get().getUserId()).orElseThrow(() -> new RuntimeException("User not found for OIDC account"));
        } else {
            // No OIDC account, check for user by email
            Optional<UserAuthDetails> userOpt = userAuthDetailsRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                user = new UserAuthDetails();
                user.setUserId(UUID.randomUUID());
                user.setEmail(email);
                user.setEmailVerified(true);
                user.setMobileNumber("9876543201");
                user.setSource("google");
                user = userAuthDetailsRepository.save(user); // Use managed instance

                UserProfile profile = new UserProfile();
                profile.setUserAuthDetails(user); // Only set userAuthDetails, not userId
                profile.setFirstName(name);
                userProfileRepository.save(profile);
            }
            // Create OIDC account link
            UserOidcAccount oidcAccount = new UserOidcAccount();
            oidcAccount.setUserId(user.getUserId());
            oidcAccount.setProvider("google");
            oidcAccount.setProviderUserId(googleSub);
            oidcAccount.setProviderEmail(email);
            userOidcAccountRepository.save(oidcAccount);
        }
        String accessToken = JwtUtil.generateAccessToken(user.getUserId(), user.getEmail());
        String refreshToken = JwtUtil.generateRefreshToken(user.getUserId());
        UserSessionDetails session = new UserSessionDetails();
        session.setUserId(user.getUserId());
        session.setAccessTokenHash(JwtUtil.hashToken(accessToken));
        session.setRefreshTokenHash(JwtUtil.hashToken(refreshToken));
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        session.setRevoked(false);
        userSessionDetailsRepository.save(session);
        UserInfo userInfo = new UserInfo()
                .id(user.getUserId().toString())
                .email(user.getEmail())
                .firstName(userProfileRepository.findById(user.getUserId()).map(UserProfile::getFirstName).orElse(""))
                .lastName(userProfileRepository.findById(user.getUserId()).map(UserProfile::getLastName).orElse(""));
        AuthWithUserResponse response = new AuthWithUserResponse()
                .accessToken(accessToken)
                .user(userInfo);
        return new OidcResult(response, refreshToken);
    }

    public static class OidcResult {

        private final AuthWithUserResponse authWithUserResponse;
        private final String refreshToken;

        public OidcResult(AuthWithUserResponse authWithUserResponse, String refreshToken) {
            this.authWithUserResponse = authWithUserResponse;
            this.refreshToken = refreshToken;
        }

        public AuthWithUserResponse getAuthWithUserResponse() {
            return authWithUserResponse;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }

    @Transactional
    public OidcResult oidcLoginOrSignupV2(String googleSub, String email, String name) {
        Optional<UserOidcAccount> oidcOpt = userOidcAccountRepository.findByProviderAndProviderUserId("google", googleSub);
        UserAuthDetails user;
        if (oidcOpt.isPresent()) {
            user = userAuthDetailsRepository.findById(oidcOpt.get().getUserId()).orElseThrow(() -> new RuntimeException("User not found for OIDC account"));
        } else {
            Optional<UserAuthDetails> userOpt = userAuthDetailsRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                user = new UserAuthDetails();
                user.setUserId(UUID.randomUUID());
                user.setEmail(email);
                user.setEmailVerified(true);
                user.setMobileNumber("9876543201");
                user.setSource("google");
                user = userAuthDetailsRepository.save(user);
                UserProfile profile = new UserProfile();
                profile.setUserAuthDetails(user);
                profile.setFirstName(name);
                userProfileRepository.save(profile);
            }
            UserOidcAccount oidcAccount = new UserOidcAccount();
            oidcAccount.setUserId(user.getUserId());
            oidcAccount.setProvider("google");
            oidcAccount.setProviderUserId(googleSub);
            oidcAccount.setProviderEmail(email);
            userOidcAccountRepository.save(oidcAccount);
        }
        String accessToken = JwtUtil.generateAccessToken(user.getUserId(), user.getEmail());
        String refreshToken = JwtUtil.generateRefreshToken(user.getUserId());
        UserSessionDetails session = new UserSessionDetails();
        session.setUserId(user.getUserId());
        session.setAccessTokenHash(JwtUtil.hashToken(accessToken));
        session.setRefreshTokenHash(JwtUtil.hashToken(refreshToken));
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        session.setRevoked(false);
        userSessionDetailsRepository.save(session);
        // Build UserInfo and AuthWithUserResponse using generated models
        UserInfo userInfo = new UserInfo()
                .id(user.getUserId().toString())
                .email(user.getEmail());
        Optional<UserProfile> profileOpt = userProfileRepository.findById(user.getUserId());
        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            userInfo.firstName(profile.getFirstName());
            userInfo.lastName(profile.getLastName());
        }
        AuthWithUserResponse response = new AuthWithUserResponse()
                .accessToken(accessToken)
                .user(userInfo);
        return new OidcResult(response, refreshToken);
    }
}
