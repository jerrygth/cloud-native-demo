package com.org.dev.cloud.api_gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserInfoController {

    private final WebClient userServiceClient;

    public UserInfoController(
            @Value("${services.user-account.base-url:http://localhost:8086}") String baseUrl) {
        this.userServiceClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @GetMapping("/api/me")
    public Mono<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RegisteredOAuth2AuthorizedClient("auth0") OAuth2AuthorizedClient authorizedClient) {

        String bearerToken = "Bearer " + authorizedClient.getAccessToken().getTokenValue();

        // Parse Auth0 name claim into first/last for registration fallback.
        // Auth0's "name" claim is typically "Firstname Lastname" or the email.
        String[] nameParts = parseAuthName(oidcUser.getFullName(), oidcUser.getEmail());
        String authFirstName = nameParts[0];
        String authLastName  = nameParts[1];

        // Base identity from Auth0 — always available regardless of UserAccount state
        Map<String, Object> identity = buildIdentity(oidcUser, authFirstName, authLastName);

        return fetchAndMergeProfile(bearerToken, identity)
                .flatMap(merged -> {
                    if (Boolean.TRUE.equals(merged.remove("_needsRegistration"))) {
                        // First login — register the user, then return the identity as-is.
                        // On the NEXT call to /api/me the local profile will exist.
                        return autoRegister(bearerToken, authFirstName, authLastName,
                                oidcUser.getEmail())
                                .thenReturn(merged);
                    }
                    return Mono.just(merged);
                });
    }

    // ── FETCH PROFILE FROM USERACCOUNT SERVICE ───────────────────────
    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> fetchAndMergeProfile(
            String bearerToken, Map<String, Object> identity) {

        return userServiceClient.get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(profile -> mergeProfile(identity, profile))
                .onErrorResume(ex -> {
                    if (isNotFound(ex)) {
                        // User has no local profile — flag for auto-registration
                        Map<String, Object> withFlag = new HashMap<>(identity);
                        withFlag.put("preferences",          defaultPreferences());
                        withFlag.put("profileComplete",      false);
                        withFlag.put("_needsRegistration",   true);
                        return Mono.just(withFlag);
                    }
                    // Service unavailable — degrade gracefully, user still logged in
                    Map<String, Object> fallback = new HashMap<>(identity);
                    fallback.put("preferences",     defaultPreferences());
                    fallback.put("profileComplete", false);
                    return Mono.just(fallback);
                });
    }

    // ── MERGE AUTH0 IDENTITY WITH USERACCOUNT PROFILE ────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeProfile(
            Map<String, Object> identity, Map<String, Object> profile) {

        Map<String, Object> merged = new HashMap<>(identity);

        // ── NAME FIELDS ─────────────────────────────────────────────
        // Use UserAccount service's firstName/lastName if present,
        // otherwise the Auth0-derived values from identity are kept.
        String firstName = (String) profile.get("firstName");
        String lastName  = (String) profile.get("lastName");

        if (isPresent(firstName)) merged.put("firstName", firstName);
        if (isPresent(lastName))  merged.put("lastName",  lastName);

        // Compose displayName for convenience — React can use this directly
        // in the navbar without joining strings on the frontend.
        String displayName = composeName(
                (String) merged.get("firstName"),
                (String) merged.get("lastName"),
                oidcNameFallback(identity)
        );
        merged.put("name", displayName);

        // ── OTHER PROFILE FIELDS ─────────────────────────────────────
        String phoneNumber = (String) profile.get("phoneNumber");
        if (isPresent(phoneNumber)) merged.put("phoneNumber", phoneNumber);

        String status = profile.get("status") != null ? profile.get("status").toString() : null;
        if (isPresent(status)) merged.put("status", status);

        merged.put("profileComplete", true);

        // ── PREFERENCES ──────────────────────────────────────────────
        // If UserAccount service has preferences nested in the response,
        // include them; otherwise use defaults.
        Object prefs = profile.get("preferences");
        merged.put("preferences", prefs != null ? prefs : defaultPreferences());

        return merged;
    }

    // ── AUTO-REGISTER ON FIRST LOGIN ─────────────────────────────────
    // Calls POST /api/v1/users/register with the real field names:
    // firstName, lastName, email — matching UserRegistrationRequest exactly.
    private Mono<Void> autoRegister(String bearerToken,
                                    String firstName,
                                    String lastName,
                                    String email) {

        // Build the registration body matching UserRegistrationRequest:
        //   firstName  — @NotBlank, max 100 chars
        //   lastName   — @NotBlank, max 100 chars
        //   email      — @NotBlank, @Email
        //   phoneNumber — optional, max 30 chars (omitted here — user can add later)
        Map<String, String> body = new HashMap<>();
        body.put("firstName", truncate(firstName, 100));
        body.put("lastName",  truncate(lastName,  100));
        body.put("email",     email != null ? email : "");
        // phoneNumber intentionally omitted — optional field

        return userServiceClient.post()
                .uri("/api/v1/users/register")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(ex -> {
                    // Registration failure must not block login.
                    // User will be auto-registered on their next /api/me call.
                    System.err.println("Auto-registration failed: " + ex.getMessage());
                    return Mono.empty();
                });
    }

    // ── HELPERS ───────────────────────────────────────────────────────

    private Map<String, Object> buildIdentity(OidcUser oidcUser,
                                              String firstName,
                                              String lastName) {
        Map<String, Object> identity = new HashMap<>();
        identity.put("sub",           oidcUser.getSubject());
        identity.put("email",         nvl(oidcUser.getEmail(), ""));
        identity.put("firstName",     firstName);
        identity.put("lastName",      lastName);
        identity.put("name",          composeName(firstName, lastName, oidcUser.getFullName()));
        identity.put("picture",       nvl(oidcUser.getPicture(), ""));
        identity.put("emailVerified", oidcUser.getEmailVerified());
        return identity;
    }

    /**
     * Parse Auth0's "name" claim into [firstName, lastName].
     *
     * Auth0 "name" claim is typically:
     *   "Jerry Thomas"   → ["Jerry", "Thomas"]
     *   "jerry.thomas@gmail.com" → ["jerry.thomas", ""] (email used as name)
     *   null             → ["", ""]
     */
    private String[] parseAuthName(String fullName, String email) {
        if (fullName == null || fullName.isBlank()) {
            // Use the part before @ from email as firstName
            String localPart = email != null && email.contains("@")
                    ? email.substring(0, email.indexOf('@'))
                    : "";
            return new String[]{ localPart, "" };
        }

        // If it looks like an email address, treat it as firstName only
        if (fullName.contains("@")) {
            String localPart = fullName.substring(0, fullName.indexOf('@'));
            return new String[]{ localPart, "" };
        }

        // Split on first space: "Jerry Thomas Alan" → ["Jerry", "Thomas Alan"]
        int spaceIdx = fullName.indexOf(' ');
        if (spaceIdx == -1) {
            return new String[]{ fullName, "" };
        }
        return new String[]{
                fullName.substring(0, spaceIdx).trim(),
                fullName.substring(spaceIdx + 1).trim()
        };
    }

    private String composeName(String firstName, String lastName, String fallback) {
        if (isPresent(firstName) && isPresent(lastName)) {
            return firstName.trim() + " " + lastName.trim();
        }
        if (isPresent(firstName)) return firstName.trim();
        if (isPresent(lastName))  return lastName.trim();
        return nvl(fallback, "");
    }

    private String oidcNameFallback(Map<String, Object> identity) {
        return (String) identity.getOrDefault("name", "");
    }

    private Map<String, Object> defaultPreferences() {
        return Map.of(
                "theme",         "dark",
                "language",      "en",
                "notifications", true
        );
    }

    private boolean isNotFound(Throwable ex) {
        return ex instanceof WebClientResponseException we
                && we.getStatusCode() == HttpStatus.NOT_FOUND;
    }

    private boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }

    private String nvl(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}