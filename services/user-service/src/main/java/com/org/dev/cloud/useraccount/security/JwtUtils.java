package com.org.dev.cloud.useraccount.security;


import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    public String getExternalId(Jwt jwt) {
        return jwt.getSubject();
    }

    public String getEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }
    public String getName(Jwt jwt) {
        return jwt.getClaimAsString("name");
    }
}
