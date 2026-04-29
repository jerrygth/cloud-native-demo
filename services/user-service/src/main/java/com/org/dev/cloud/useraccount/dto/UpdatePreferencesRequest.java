package com.org.dev.cloud.useraccount.dto;

import com.org.dev.cloud.useraccount.entity.UserPreferences;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePreferencesRequest {

    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language must be a valid IETF tag, e.g. 'en' or 'en-US'")
    private String language;

    @Size(max = 50)
    private String timezone;

    private Boolean emailNotifications;

    private Boolean pushNotifications;

    private UserPreferences.Theme theme;
}
