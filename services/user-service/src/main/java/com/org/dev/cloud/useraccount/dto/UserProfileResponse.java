package com.org.dev.cloud.useraccount.dto;

import com.org.dev.cloud.useraccount.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
