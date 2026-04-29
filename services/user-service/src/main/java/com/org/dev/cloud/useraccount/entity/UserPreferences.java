package com.org.dev.cloud.useraccount.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Table("user_preferences")
public class UserPreferences {

    @Id
    private UUID id;

    private UUID userId;

    @Builder.Default
    private String language = "en";

    @Builder.Default
    private String timezone = "UTC";

    @Builder.Default
    private boolean emailNotifications = true;

    @Builder.Default
    private boolean pushNotifications = true;

    @Builder.Default
    private Theme theme = Theme.LIGHT;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum Theme {
        LIGHT, DARK, SYSTEM
    }
}
