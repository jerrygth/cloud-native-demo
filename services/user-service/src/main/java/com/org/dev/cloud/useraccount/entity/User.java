package com.org.dev.cloud.useraccount.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    private UUID id;

    private String externalId;

    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Instant deletedAt;

    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;


    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public boolean isActive() {
        return UserStatus.ACTIVE == this.status;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

}
