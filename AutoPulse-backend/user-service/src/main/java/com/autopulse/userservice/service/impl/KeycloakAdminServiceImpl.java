package com.autopulse.userservice.service.impl;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.userservice.config.KeycloakAdminProperties;
import com.autopulse.userservice.service.KeycloakAdminService;
import com.autopulse.userservice.web.dto.TokenResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminServiceImpl.class);

    private final RestClient keycloakRestClient;
    private final KeycloakAdminProperties keycloakAdminProperties;
    private final Map<String, Map<String, Object>> realmRoleCache = new ConcurrentHashMap<>();
    private volatile CachedToken cachedAdminToken;

    public KeycloakAdminServiceImpl(RestClient keycloakRestClient,
                                    KeycloakAdminProperties keycloakAdminProperties) {
        this.keycloakRestClient = keycloakRestClient;
        this.keycloakAdminProperties = keycloakAdminProperties;
    }

    @Override
    public UUID createUser(String email,
                           String firstName,
                           String lastName,
                           String rawPassword,
                           boolean enabled,
                           String realmRoleName) {
        String accessToken = getAdminAccessToken();
        Map<String, Object> payload = Map.of(
                "username", email,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", enabled,
                "emailVerified", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", rawPassword,
                        "temporary", false
                ))
        );

        UUID createdUserId;
        try {
            ResponseEntity<Void> response = keycloakRestClient.post()
                    .uri(keycloakAdminProperties.adminUsersEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            createdUserId = extractCreatedUserId(response.getHeaders().getLocation());
        } catch (RestClientResponseException ex) {
            throw mapKeycloakException("Failed to create Keycloak user", ex);
        }

        try {
            assignRealmRole(createdUserId, realmRoleName, accessToken);
        } catch (RuntimeException ex) {
            try {
                deleteUser(createdUserId);
            } catch (RuntimeException cleanupEx) {
                log.warn("Failed to clean up Keycloak user {} after role assignment failure", createdUserId, cleanupEx);
            }
            throw ex;
        }

        return createdUserId;
    }

    @Override
    public void updateUser(UUID keycloakUserId,
                           String email,
                           String firstName,
                           String lastName,
                           boolean enabled) {
        String accessToken = getAdminAccessToken();
        Map<String, Object> payload = Map.of(
                "id", keycloakUserId.toString(),
                "username", email,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", enabled,
                "emailVerified", true
        );

        try {
            keycloakRestClient.put()
                    .uri(keycloakAdminProperties.adminUserEndpoint(keycloakUserId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw mapKeycloakException("Failed to update Keycloak user", ex);
        }
    }

    @Override
    public void setEnabled(UUID keycloakUserId, boolean enabled) {
        Map<String, Object> representation = getUserRepresentation(keycloakUserId);
        representation.put("enabled", enabled);

        try {
            keycloakRestClient.put()
                    .uri(keycloakAdminProperties.adminUserEndpoint(keycloakUserId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(getAdminAccessToken()))
                    .body(representation)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw mapKeycloakException("Failed to enable/disable Keycloak user", ex);
        }
    }

    @Override
    public void resetPassword(UUID keycloakUserId, String rawPassword, boolean temporary) {
        Map<String, Object> payload = Map.of(
                "type", "password",
                "value", rawPassword,
                "temporary", temporary
        );

        try {
            keycloakRestClient.put()
                    .uri(keycloakAdminProperties.adminUserResetPasswordEndpoint(keycloakUserId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(getAdminAccessToken()))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw mapKeycloakException("Failed to reset Keycloak password", ex);
        }
    }

    @Override
    public void deleteUser(UUID keycloakUserId) {
        try {
            keycloakRestClient.delete()
                    .uri(keycloakAdminProperties.adminUserEndpoint(keycloakUserId.toString()))
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(getAdminAccessToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw mapKeycloakException("Failed to delete Keycloak user", ex);
        }
    }

    @Override
    public TokenResponse login(String username, String password) {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);

        try {
            TokenEndpointResponse response = keycloakRestClient.post()
                    .uri(keycloakAdminProperties.tokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenEndpointResponse.class);

            if (response == null) {
                throw new BadRequestException("Keycloak did not return a login response");
            }
            return response.toTokenResponse();
        } catch (RestClientResponseException ex) {
            throw mapAuthenticationException(ex);
        }
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        try {
            TokenEndpointResponse response = keycloakRestClient.post()
                    .uri(keycloakAdminProperties.tokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenEndpointResponse.class);

            if (response == null) {
                throw new BadRequestException("Keycloak did not return a refresh response");
            }
            return response.toTokenResponse();
        } catch (RestClientResponseException ex) {
            throw mapAuthenticationException(ex);
        }
    }

    @Override
    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");

        try {
            keycloakRestClient.post()
                    .uri(keycloakAdminProperties.revokeEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw mapAuthenticationException(ex);
        }
    }

    private void assignRealmRole(UUID keycloakUserId, String realmRoleName, String accessToken) {
        Map<String, Object> roleRepresentation = realmRoleCache.computeIfAbsent(realmRoleName, this::loadRoleRepresentation);
        try {
            keycloakRestClient.post()
                    .uri(keycloakAdminProperties.adminUserRealmRoleMappingsEndpoint(keycloakUserId.toString()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                    .body(List.of(roleRepresentation))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw mapKeycloakException("Failed to assign Keycloak realm role", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadRoleRepresentation(String realmRoleName) {
        try {
            Map<String, Object> response = keycloakRestClient.get()
                    .uri(keycloakAdminProperties.adminRoleEndpoint(realmRoleName))
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(getAdminAccessToken()))
                    .retrieve()
                    .body(Map.class);
            if (response == null || response.isEmpty()) {
                throw new NotFoundException("Keycloak realm role '%s' was not found".formatted(realmRoleName));
            }
            return response;
        } catch (RestClientResponseException ex) {
            throw mapKeycloakException("Failed to fetch Keycloak realm role", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUserRepresentation(UUID keycloakUserId) {
        try {
            Map<String, Object> response = keycloakRestClient.get()
                    .uri(keycloakAdminProperties.adminUserEndpoint(keycloakUserId.toString()))
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(getAdminAccessToken()))
                    .retrieve()
                    .body(Map.class);
            if (response == null || response.isEmpty()) {
                throw new NotFoundException("Keycloak user '%s' was not found".formatted(keycloakUserId));
            }
            return response;
        } catch (RestClientResponseException ex) {
            throw mapKeycloakException("Failed to fetch Keycloak user representation", ex);
        }
    }

    private String getAdminAccessToken() {
        CachedToken currentToken = cachedAdminToken;
        if (currentToken != null && currentToken.isUsable()) {
            return currentToken.accessToken();
        }

        synchronized (this) {
            currentToken = cachedAdminToken;
            if (currentToken != null && currentToken.isUsable()) {
                return currentToken.accessToken();
            }

            MultiValueMap<String, String> form = baseClientForm();
            form.add("grant_type", "client_credentials");

            try {
                TokenEndpointResponse response = keycloakRestClient.post()
                        .uri(keycloakAdminProperties.tokenEndpoint())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(TokenEndpointResponse.class);

                if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                    throw new BadRequestException("Keycloak did not return an admin access token");
                }

                cachedAdminToken = new CachedToken(
                        response.accessToken(),
                        Instant.now().plusSeconds(Math.max(30L, response.expiresIn() - 30L))
                );
                return cachedAdminToken.accessToken();
            } catch (RestClientResponseException ex) {
                throw mapKeycloakException("Failed to obtain Keycloak admin access token", ex);
            }
        }
    }

    private MultiValueMap<String, String> baseClientForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakAdminProperties.getClientId());
        form.add("client_secret", keycloakAdminProperties.getClientSecret());
        return form;
    }

    private UUID extractCreatedUserId(URI location) {
        if (location == null) {
            throw new BadRequestException("Keycloak did not return the location of the created user");
        }

        String path = location.getPath();
        int separatorIndex = path.lastIndexOf('/');
        if (separatorIndex < 0 || separatorIndex == path.length() - 1) {
            throw new BadRequestException("Could not extract created Keycloak user id from response location");
        }

        try {
            return UUID.fromString(path.substring(separatorIndex + 1));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Keycloak returned an invalid user identifier");
        }
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    private RuntimeException mapAuthenticationException(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        int status = ex.getStatusCode().value();

        if (status == 400 || status == 401) {
            if (body != null && body.contains("unauthorized_client")) {
                return new BadRequestException("Keycloak client is not configured for the requested grant type");
            }
            return new BadRequestException("Invalid credentials or token request");
        }

        return mapKeycloakException("Authentication request against Keycloak failed", ex);
    }

    private RuntimeException mapKeycloakException(String action, RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String body = ex.getResponseBodyAsString();
        log.warn("{}: status={} body={}", action, status, body);

        if (status == 404) {
            return new NotFoundException(action);
        }
        if (status == 409) {
            return new ConflictException(action + ": resource already exists");
        }
        if (status == 400 || status == 401 || status == 403) {
            return new BadRequestException(action);
        }
        return new BadRequestException(action + ": Keycloak returned status " + status);
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
        private boolean isUsable() {
            return expiresAt != null && Instant.now().isBefore(expiresAt);
        }
    }

    private record TokenEndpointResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("refresh_expires_in") long refreshExpiresIn,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("scope") String scope,
            @JsonProperty("session_state") String sessionState
    ) {
        private TokenResponse toTokenResponse() {
            return new TokenResponse(
                    accessToken,
                    refreshToken,
                    expiresIn,
                    refreshExpiresIn,
                    tokenType,
                    scope,
                    sessionState
            );
        }
    }
}
