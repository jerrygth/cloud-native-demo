package com.org.dev.cloud.useraccount.controller;

import com.org.dev.cloud.useraccount.dto.UpdatePreferencesRequest;
import com.org.dev.cloud.useraccount.dto.UserPreferencesResponse;
import com.org.dev.cloud.useraccount.security.JwtUtils;
import com.org.dev.cloud.useraccount.service.UserPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/me/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;
    private final JwtUtils jwtUtils;

    /**
     * GET /api/v1/users/me/preferences
     */
    @GetMapping
    public Mono<UserPreferencesResponse> getPreferences(@AuthenticationPrincipal Jwt jwt) {
        return preferencesService.getPreferences(jwtUtils.getExternalId(jwt));
    }

    /**
     * PATCH /api/v1/users/me/preferences
     * Partial update — only provided fields are modified.
     */
    @PatchMapping
    public Mono<UserPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdatePreferencesRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return preferencesService.updatePreferences(request, jwtUtils.getExternalId(jwt));
    }
}
