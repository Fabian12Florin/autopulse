package com.autopulse.userservice.service.impl;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.userservice.config.KeycloakAdminProperties;
import com.autopulse.userservice.testutil.TestDataFactory;
import com.autopulse.userservice.web.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakAdminServiceImplTest {

    private KeycloakAdminProperties properties;
    private MockRestServiceServer server;
    private KeycloakAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = TestDataFactory.keycloakProperties();
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new KeycloakAdminServiceImpl(builder.build(), properties);
    }

    @Test
    void loginRefreshAndLogoutUseTokenEndpoints() {
        expectTokenExchange("password", tokenJson("access", "refresh"));
        expectTokenExchange("refresh_token", tokenJson("new-access", "new-refresh"));
        server.expect(once(), requestTo(properties.revokeEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withNoContent());

        TokenResponse login = service.login("user@example.com", "Password123");
        assertThat(login.accessToken()).isEqualTo("access");
        assertThat(login.refreshToken()).isEqualTo("refresh");

        assertThat(service.refresh("refresh").refreshToken()).isEqualTo("new-refresh");

        service.logout("new-refresh");

        server.verify();
    }

    @Test
    void createUserCachesAdminTokenAssignsRoleAndReusesTokenForUpdate() {
        UUID createdUserId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUsersEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withCreatedEntity(URI.create(properties.adminUserEndpoint(createdUserId.toString()))));
        server.expect(once(), requestTo(properties.adminRoleEndpoint("COURIER")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withSuccess("{\"id\":\"role-id\",\"name\":\"COURIER\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(properties.adminUserRealmRoleMappingsEndpoint(createdUserId.toString())))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withNoContent());
        server.expect(once(), requestTo(properties.adminUserEndpoint(createdUserId.toString())))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withNoContent());

        assertThat(service.createUser("user@example.com", "Jane", "Driver", "Password123", true, "COURIER"))
                .isEqualTo(createdUserId);

        service.updateUser(createdUserId, "new@example.com", "New", "Name", false);

        server.verify();
    }

    @Test
    void setEnabledFetchesRepresentationAndWritesUpdatedEnabledFlag() {
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUserEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"%s\",\"enabled\":false}".formatted(TestDataFactory.KEYCLOAK_ID), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(properties.adminUserEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withNoContent());

        service.setEnabled(TestDataFactory.KEYCLOAK_ID, true);

        server.verify();
    }

    @Test
    void resetPasswordAndDeleteUserUseAdminEndpoints() {
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUserResetPasswordEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withNoContent());
        server.expect(once(), requestTo(properties.adminUserEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withNoContent());

        service.resetPassword(TestDataFactory.KEYCLOAK_ID, "Password123", false);
        service.deleteUser(TestDataFactory.KEYCLOAK_ID);

        server.verify();
    }

    @Test
    void authenticationErrorsAndEmptyResponsesBecomeBadRequests() {
        server.expect(once(), requestTo(properties.tokenEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest().body("{\"error\":\"unauthorized_client\"}").contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.login("user@example.com", "Password123"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("grant type");

        server.reset();
        server.expect(once(), requestTo(properties.tokenEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.refresh("refresh"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("refresh response");
    }

    @Test
    void createUserRejectsMissingOrInvalidLocation() {
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUsersEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withCreatedEntity(null));

        assertThatThrownBy(() -> service.createUser("user@example.com", "Jane", "Driver", "Password123", true, "COURIER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("location");
    }

    @Test
    void createUserCleansUpWhenRoleAssignmentFails() {
        UUID createdUserId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUsersEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withCreatedEntity(URI.create(properties.adminUserEndpoint(createdUserId.toString()))));
        server.expect(once(), requestTo(properties.adminRoleEndpoint("COURIER")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());
        server.expect(once(), requestTo(properties.adminUserEndpoint(createdUserId.toString())))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        assertThatThrownBy(() -> service.createUser("user@example.com", "Jane", "Driver", "Password123", true, "COURIER"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createUserLogsWhenCleanupAfterRoleFailureAlsoFails() {
        UUID createdUserId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUsersEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withCreatedEntity(URI.create(properties.adminUserEndpoint(createdUserId.toString()))));
        server.expect(once(), requestTo(properties.adminRoleEndpoint("COURIER")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());
        server.expect(once(), requestTo(properties.adminUserEndpoint(createdUserId.toString())))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.createUser("user@example.com", "Jane", "Driver", "Password123", true, "COURIER"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createUserMapsPostFailureAndRejectsInvalidLocations() {
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUsersEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT));

        assertThatThrownBy(() -> service.createUser("user@example.com", "Jane", "Driver", "Password123", true, "COURIER"))
                .isInstanceOf(ConflictException.class);

        setUp();
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUsersEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withCreatedEntity(URI.create("http://keycloak.test/admin/realms/autopulse/users/")));
        assertThatThrownBy(() -> service.createUser("user@example.com", "Jane", "Driver", "Password123", true, "COURIER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("extract");

        setUp();
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUsersEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withCreatedEntity(URI.create("http://keycloak.test/admin/realms/autopulse/users/not-a-uuid")));
        assertThatThrownBy(() -> service.createUser("user@example.com", "Jane", "Driver", "Password123", true, "COURIER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid user identifier");
    }

    @Test
    void emptyRoleAndUserRepresentationsAreRejected() {
        UUID createdUserId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUsersEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withCreatedEntity(URI.create(properties.adminUserEndpoint(createdUserId.toString()))));
        server.expect(once(), requestTo(properties.adminRoleEndpoint("COURIER")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(properties.adminUserEndpoint(createdUserId.toString())))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        assertThatThrownBy(() -> service.createUser("user@example.com", "Jane", "Driver", "Password123", true, "COURIER"))
                .isInstanceOf(NotFoundException.class);

        setUp();
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUserEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.setEnabled(TestDataFactory.KEYCLOAK_ID, true))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateResetDeleteSetEnabledAndLogoutMapRemoteFailures() {
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUserEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andRespond(withBadRequest());
        assertThatThrownBy(() -> service.updateUser(TestDataFactory.KEYCLOAK_ID, "user@example.com", "Jane", "Driver", true))
                .isInstanceOf(BadRequestException.class);

        setUp();
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUserResetPasswordEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andRespond(withBadRequest());
        assertThatThrownBy(() -> service.resetPassword(TestDataFactory.KEYCLOAK_ID, "Password123", false))
                .isInstanceOf(BadRequestException.class);

        setUp();
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUserEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andRespond(withBadRequest());
        assertThatThrownBy(() -> service.deleteUser(TestDataFactory.KEYCLOAK_ID))
                .isInstanceOf(BadRequestException.class);

        setUp();
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUserEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andRespond(withSuccess("{\"id\":\"%s\",\"enabled\":false}".formatted(TestDataFactory.KEYCLOAK_ID), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(properties.adminUserEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andRespond(withBadRequest());
        assertThatThrownBy(() -> service.setEnabled(TestDataFactory.KEYCLOAK_ID, true))
                .isInstanceOf(BadRequestException.class);

        setUp();
        server.expect(once(), requestTo(properties.revokeEndpoint()))
                .andRespond(withServerError());
        assertThatThrownBy(() -> service.logout("refresh-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("500");
    }

    @Test
    void authenticationFailureWithoutUnauthorizedClientUsesGenericMessage() {
        server.expect(once(), requestTo(properties.tokenEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"invalid_grant\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.login("user@example.com", "bad-password"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void keycloakStatusesMapToDomainExceptions() {
        expectAdminToken();
        server.expect(once(), requestTo(properties.adminUserEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.updateUser(TestDataFactory.KEYCLOAK_ID, "user@example.com", "Jane", "Driver", true))
                .isInstanceOf(ConflictException.class);

        server.reset();
        server.expect(once(), requestTo(properties.adminUserEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andRespond(withServerError());
        assertThatThrownBy(() -> service.updateUser(TestDataFactory.KEYCLOAK_ID, "user@example.com", "Jane", "Driver", true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("500");
    }

    @Test
    void missingAdminTokenIsRejected() {
        server.expect(once(), requestTo(properties.tokenEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": null,
                          "expires_in": 60,
                          "refresh_expires_in": 0
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.deleteUser(TestDataFactory.KEYCLOAK_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("admin access token");
    }

    @Test
    void expiredAdminTokenIsRefreshed() throws Exception {
        Class<?> cachedTokenClass = Class.forName("com.autopulse.userservice.service.impl.KeycloakAdminServiceImpl$CachedToken");
        Constructor<?> constructor = cachedTokenClass.getDeclaredConstructor(String.class, Instant.class);
        constructor.setAccessible(true);
        ReflectionTestUtils.setField(service, "cachedAdminToken", constructor.newInstance("expired-token", Instant.now().minusSeconds(1)));

        server.expect(once(), requestTo(properties.tokenEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenJsonWithExpiry("fresh-admin-token", "admin-refresh", 10), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(properties.adminUserEndpoint(TestDataFactory.KEYCLOAK_ID.toString())))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fresh-admin-token"))
                .andRespond(withNoContent());

        service.deleteUser(TestDataFactory.KEYCLOAK_ID);
    }

    private void expectAdminToken() {
        server.expect(once(), requestTo(properties.tokenEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenJson("admin-token", "admin-refresh"), MediaType.APPLICATION_JSON));
    }

    private void expectTokenExchange(String grantType, String responseJson) {
        server.expect(once(), requestTo(properties.tokenEndpoint()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private String tokenJson(String accessToken, String refreshToken) {
        return tokenJsonWithExpiry(accessToken, refreshToken, 3600);
    }

    private String tokenJsonWithExpiry(String accessToken, String refreshToken, long expiresIn) {
        return """
                {
                  "access_token": "%s",
                  "expires_in": %d,
                  "refresh_expires_in": 7200,
                  "refresh_token": "%s",
                  "token_type": "Bearer",
                  "scope": "openid profile",
                  "session_state": "session-1"
                }
                """.formatted(accessToken, expiresIn, refreshToken);
    }
}
