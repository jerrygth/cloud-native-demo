package com.org.dev.cloud.useraccount.controller;

import com.org.dev.cloud.useraccount.dto.UpdateProfileRequest;
import com.org.dev.cloud.useraccount.dto.UserProfileResponse;
import com.org.dev.cloud.useraccount.dto.UserRegistrationRequest;
import com.org.dev.cloud.useraccount.security.JwtUtils;
import com.org.dev.cloud.useraccount.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    /**
     * POST /api/v1/users/register
     * Called once after a user's first Auth0 login to create their local profile.
     * Identity is always sourced from the validated JWT — never from the request body.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserProfileResponse> register(
            @Valid @RequestBody UserRegistrationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String externalId = jwtUtils.getExternalId(jwt);
        log.debug("Registration request from externalId: {}", externalId);
        return userService.register(request, externalId);
    }

    /**
     * GET /api/v1/users/me
     * Returns the authenticated user's own profile.
     * No userId in the path — the JWT is the identity.
     */
    @GetMapping("/me")
    public Mono<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getMyProfile(jwtUtils.getExternalId(jwt));
    }

    /**
     * PATCH /api/v1/users/me
     * Partial profile update — only provided fields are changed.
     */
    @PatchMapping("/me")
    public Mono<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return userService.updateProfile(request, jwtUtils.getExternalId(jwt));
    }

    /**
     * DELETE /api/v1/users/me
     * Initiates GDPR account deletion (soft delete).
     * Returns 204 No Content on success.
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        log.info("Account deletion initiated");
        return userService.requestAccountDeletion(jwtUtils.getExternalId(jwt));
    }
}
