package com.org.dev.cloud.useraccount.repository;

import com.org.dev.cloud.useraccount.entity.User;
import com.org.dev.cloud.useraccount.entity.UserStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {

    Mono<User> findByExternalId(String externalId);

    Mono<Boolean> existsByEmail(String email);

    Mono<Boolean> existsByExternalId(String externalId);

}
