package com.org.dev.cloud.useraccount.service;

import com.org.dev.cloud.useraccount.dto.UpdateProfileRequest;
import com.org.dev.cloud.useraccount.dto.UserProfileResponse;
import com.org.dev.cloud.useraccount.dto.UserRegistrationRequest;
import com.org.dev.cloud.useraccount.entity.User;
import com.org.dev.cloud.useraccount.entity.UserAuditLog;
import com.org.dev.cloud.useraccount.entity.UserStatus;

import com.org.dev.cloud.useraccount.mapper.UserMapper;
import com.org.dev.cloud.useraccount.repository.UserAuditLogRepository;
import com.org.dev.cloud.useraccount.repository.UserPreferencesRepository;
import com.org.dev.cloud.useraccount.repository.UserRepository;
import com.org.dev.cloud.useraccount.exception.UserAccountExceptions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final UserAuditLogRepository auditLogRepository;
    private final UserMapper userMapper;

    /**
     * Registers a new user after their first Auth0 login.
     * The externalId (Auth0 sub) is taken from the verified JWT — never from the request body.
     */
    @Transactional
    public Mono<UserProfileResponse> register(UserRegistrationRequest request, String externalId) {
        log.debug("Attempting registration for externalId: {}", externalId);

        return userRepository.existsByExternalId(externalId)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new UserAlreadyExistsException(
                                "An account already exists for this identity provider user"));
                    }
                    return userRepository.existsByEmail(request.getEmail());
                })
                .flatMap(emailExists -> {
                    if (emailExists) {
                        return Mono.error(new UserAlreadyExistsException(
                                "Email address is already registered"));
                    }
                    User newUser = userMapper.toUser(request, externalId);
                    return userRepository.save(newUser);
                })
                .flatMap(savedUser -> {
                    // Create default preferences alongside the user
                    var defaultPrefs = userMapper.defaultPreferences(savedUser);
                    return preferencesRepository.save(defaultPrefs)
                            .thenReturn(savedUser);
                })
                .flatMap(savedUser -> audit(savedUser.getId().toString(), "REGISTERED", externalId, null)
                        .thenReturn(savedUser))
                .map(userMapper::toProfileResponse)
                .doOnSuccess(r -> log.info("User registered successfully: {}", r.getId()));
    }

    /**
     * Fetches the authenticated user's own profile.
     */
    public Mono<UserProfileResponse> getMyProfile(String externalId) {
        return findActiveUserByExternalId(externalId)
                .map(userMapper::toProfileResponse);
    }

    /**
     * Partial profile update — only non-null fields in the request are applied.
     */
    @Transactional
    public Mono<UserProfileResponse> updateProfile(UpdateProfileRequest request, String externalId) {
        return findActiveUserByExternalId(externalId)
                .flatMap(user -> {
                    if (request.getFirstName() != null)  user.setFirstName(request.getFirstName());
                    if (request.getLastName() != null)   user.setLastName(request.getLastName());
                    if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
                    return userRepository.save(user);
                })
                .flatMap(updated -> audit(updated.getId().toString(), "PROFILE_UPDATED", externalId, null)
                        .thenReturn(updated))
                .map(userMapper::toProfileResponse)
                .doOnSuccess(r -> log.debug("Profile updated for user: {}", r.getId()));
    }

    /**
     * The account is marked for deletion;
     */
    @Transactional
    public Mono<Void> requestAccountDeletion(String externalId) {
        return findActiveUserByExternalId(externalId)
                .flatMap(user -> {
                    user.setStatus(UserStatus.DELETED);
                    user.setDeletedAt(Instant.now());
                    return userRepository.save(user);
                })
                .flatMap(deleted -> audit(deleted.getId().toString(), "ACCOUNT_DELETION_REQUESTED", externalId,
                        "Soft-deleted."))
                .doOnSuccess(v -> log.info("Account deletion requested for externalId: {}", externalId))
                .then();
    }

    private Mono<User> findActiveUserByExternalId(String externalId) {
        return userRepository.findByExternalId(externalId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(
                        "No account found for the authenticated user")))
                .flatMap(user -> switch (user.getStatus()) {
                    case SUSPENDED -> Mono.error(new AccountSuspendedException("This account has been suspended"));
                    case DELETED    -> Mono.error(new AccountDeletedException("This account has been deleted"));
                    case ACTIVE     -> Mono.just(user);
                });
    }

    private Mono<UserAuditLog> audit(String userId, String action, String performedBy, String details) {
        var entry = UserAuditLog.builder()
                .userId(java.util.UUID.fromString(userId))
                .action(action)
                .performedBy(performedBy)
                .details(details)
                .build();
        return auditLogRepository.save(entry)
                .doOnError(e -> log.error("Failed to write audit log for action {}: {}", action, e.getMessage()));
    }
}