package com.org.dev.cloud.useraccount.mapper;

import com.org.dev.cloud.useraccount.dto.UserPreferencesResponse;
import com.org.dev.cloud.useraccount.dto.UserProfileResponse;
import com.org.dev.cloud.useraccount.dto.UserRegistrationRequest;
import com.org.dev.cloud.useraccount.entity.User;
import com.org.dev.cloud.useraccount.entity.UserPreferences;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toUser(UserRegistrationRequest request, String externalId) {
        return User.builder()
                .externalId(externalId)
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .build();
    }

    public UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public UserPreferencesResponse toPreferencesResponse(UserPreferences prefs) {
        return UserPreferencesResponse.builder()
                .id(prefs.getId())
                .language(prefs.getLanguage())
                .timezone(prefs.getTimezone())
                .emailNotifications(prefs.isEmailNotifications())
                .pushNotifications(prefs.isPushNotifications())
                .theme(prefs.getTheme())
                .build();
    }

    public UserPreferences defaultPreferences(User user) {
        return UserPreferences.builder()
                .userId(user.getId())
                .build(); // all defaults are set in the builder
    }
}
