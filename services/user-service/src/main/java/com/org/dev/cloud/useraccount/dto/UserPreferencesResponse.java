package com.org.dev.cloud.useraccount.dto;

import com.org.dev.cloud.useraccount.entity.UserPreferences;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.bind.annotation.BindParam;

import java.util.UUID;

@Data
@Builder
public class UserPreferencesResponse {
    private UUID id;
    private String language;
    private String timezone;
    private boolean emailNotifications;
    private boolean pushNotifications;
    private UserPreferences.Theme theme;
}
