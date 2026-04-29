package com.org.dev.cloud.useraccount.service;

import com.org.dev.cloud.useraccount.dto.UpdatePreferencesRequest;
import com.org.dev.cloud.useraccount.dto.UserPreferencesResponse;
import com.org.dev.cloud.useraccount.mapper.UserMapper;
import com.org.dev.cloud.useraccount.repository.UserPreferencesRepository;
import com.org.dev.cloud.useraccount.repository.UserRepository;
import com.org.dev.cloud.useraccount.exception.UserAccountExceptions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public Mono<UserPreferencesResponse> getPreferences(String externalId) {
        return resolveUserId(externalId)
                .flatMap(preferencesRepository::findByUserId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Preferences not found")))
                .map(userMapper::toPreferencesResponse);
    }

    @Transactional
    public Mono<UserPreferencesResponse> updatePreferences(UpdatePreferencesRequest request, String externalId) {
        return resolveUserId(externalId)
                .flatMap(userId -> preferencesRepository.findByUserId(userId)
                        .switchIfEmpty(Mono.error(new UserNotFoundException("Preferences not found")))
                        .flatMap(prefs -> {
                            // Only apply non-null fields (PATCH semantics)
                            if (request.getLanguage() != null)           prefs.setLanguage(request.getLanguage());
                            if (request.getTimezone() != null)           prefs.setTimezone(request.getTimezone());
                            if (request.getEmailNotifications() != null) prefs.setEmailNotifications(request.getEmailNotifications());
                            if (request.getPushNotifications() != null)  prefs.setPushNotifications(request.getPushNotifications());
                            if (request.getTheme() != null)              prefs.setTheme(request.getTheme());
                            return preferencesRepository.save(prefs);
                        }))
                .map(userMapper::toPreferencesResponse)
                .doOnSuccess(r -> log.debug("Preferences updated for externalId: {}", externalId));
    }

    private Mono<java.util.UUID> resolveUserId(String externalId) {
        return userRepository.findByExternalId(externalId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("No account found")))
                .map(user -> user.getId());
    }
}






