package com.org.dev.cloud.useraccount.repository;

import com.org.dev.cloud.useraccount.entity.UserPreferences;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserPreferencesRepository extends R2dbcRepository<UserPreferences, UUID> {

    Mono<UserPreferences> findByUserId(UUID userId);

    Mono<Void> deleteByUserId(UUID userId);
}
