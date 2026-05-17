package com.autopulse.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigTest {

    @Test
    void keycloakPropertiesBuildEndpointsAndTrimTrailingSlash() {
        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setBaseUrl("http://keycloak/");
        properties.setRealm("autopulse");

        assertThat(properties.realmBaseUrl()).isEqualTo("http://keycloak/realms/autopulse");
        assertThat(properties.adminRealmBaseUrl()).isEqualTo("http://keycloak/admin/realms/autopulse");
        assertThat(properties.tokenEndpoint()).endsWith("/protocol/openid-connect/token");
        assertThat(properties.revokeEndpoint()).endsWith("/protocol/openid-connect/revoke");
        assertThat(properties.adminUsersEndpoint()).isEqualTo("http://keycloak/admin/realms/autopulse/users");
        assertThat(properties.adminUserEndpoint("id")).endsWith("/users/id");
        assertThat(properties.adminUserResetPasswordEndpoint("id")).endsWith("/users/id/reset-password");
        assertThat(properties.adminUserRealmRoleMappingsEndpoint("id")).endsWith("/users/id/role-mappings/realm");
        assertThat(properties.adminRoleEndpoint("ADMIN")).endsWith("/roles/ADMIN");

        properties.setBaseUrl(" ");
        assertThat(properties.realmBaseUrl()).isEqualTo("/realms/autopulse");
    }

    @Test
    void restClientConfigCreatesClient() {
        assertThat(new RestClientConfig().keycloakRestClient(RestClient.builder())).isNotNull();
    }

    @Test
    void openApiConfigContainsSecuritySchemeAndServers() {
        var openApi = new OpenApiConfig().userServiceOpenApi();

        assertThat(openApi.getInfo().getTitle()).contains("User Service");
        assertThat(openApi.getComponents().getSecuritySchemes()).containsKey(OpenApiConfig.BEARER_SCHEME);
        assertThat(openApi.getServers()).hasSize(2);
    }

    @Test
    void keycloakJwtAuthenticationConverterExtractsScopesRolesAndPrincipalFallbacks() throws Exception {
        Object converter = newConverter();

        Jwt jwt = jwt(Map.of(
                "scope", "profile email",
                "preferred_username", "driver",
                "realm_access", Map.of("roles", List.of("ADMIN", " COURIER ", "", 42))
        ));
        JwtAuthenticationToken token = (JwtAuthenticationToken) ReflectionTestUtils.invokeMethod(converter, "convert", jwt);

        assertThat(token.getName()).isEqualTo("driver");
        assertThat(token.getAuthorities()).extracting("authority")
                .contains("SCOPE_profile", "SCOPE_email", "ROLE_ADMIN", "ROLE_COURIER");

        Jwt emailFallback = jwt(Map.of("email", "user@example.com"));
        assertThat(((JwtAuthenticationToken) ReflectionTestUtils.invokeMethod(converter, "convert", emailFallback)).getName())
                .isEqualTo("user@example.com");

        Jwt subjectFallback = jwt(Map.of("realm_access", Map.of("roles", "not-a-list")));
        assertThat(((JwtAuthenticationToken) ReflectionTestUtils.invokeMethod(converter, "convert", subjectFallback)).getName())
                .isEqualTo("subject");

        Jwt noRealmAccess = jwt(Map.of("realm_access", "not-a-map"));
        assertThat(((JwtAuthenticationToken) ReflectionTestUtils.invokeMethod(converter, "convert", noRealmAccess)).getAuthorities())
                .extracting("authority")
                .doesNotContain("ROLE_ADMIN");
    }

    @Test
    void securityFilterChainBeanCanBeDeclared() throws Exception {
        var config = new SecurityConfig();
        assertThat(SecurityConfig.class.getDeclaredMethod("securityFilterChain", HttpSecurity.class)).isNotNull();
        assertThat(HttpMethod.POST).isNotNull();
        assertThat(config).isNotNull();
    }

    private Object newConverter() throws Exception {
        Class<?> converterClass = Class.forName("com.autopulse.userservice.config.SecurityConfig$KeycloakJwtAuthenticationConverter");
        Constructor<?> constructor = converterClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        claims.forEach(builder::claim);
        return builder.build();
    }
}
