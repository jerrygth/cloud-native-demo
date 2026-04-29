package com.org.dev.cloud.useraccount.repository;

import com.org.dev.cloud.useraccount.entity.UserAuditLog;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface UserAuditLogRepository extends R2dbcRepository<UserAuditLog, Long> {

    Flux<UserAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

}
