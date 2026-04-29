package com.org.dev.cloud.useraccount.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Table("user_audit_log")
public class UserAuditLog {

    @Id
    private Long id;

    private UUID userId;

    /** Describes what happened, e.g. "REGISTERED", "PROFILE_UPDATED", "ACCOUNT_DELETED" */
    private String action;

    private String performedBy;

    private String ipAddress;

    private String details;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
