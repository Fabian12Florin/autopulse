package com.autopulse.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProperties {

    private String baseUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String adminUsername;
    private String adminPassword;
    private Roles roles = new Roles();

    public String realmBaseUrl() {
        return trimTrailingSlash(baseUrl) + "/realms/" + realm;
    }

    public String adminRealmBaseUrl() {
        return trimTrailingSlash(baseUrl) + "/admin/realms/" + realm;
    }

    public String tokenEndpoint() {
        return realmBaseUrl() + "/protocol/openid-connect/token";
    }

    public String revokeEndpoint() {
        return realmBaseUrl() + "/protocol/openid-connect/revoke";
    }

    public String adminUsersEndpoint() {
        return adminRealmBaseUrl() + "/users";
    }

    public String adminUserEndpoint(String userId) {
        return adminUsersEndpoint() + "/" + userId;
    }

    public String adminUserResetPasswordEndpoint(String userId) {
        return adminUserEndpoint(userId) + "/reset-password";
    }

    public String adminUserRealmRoleMappingsEndpoint(String userId) {
        return adminUserEndpoint(userId) + "/role-mappings/realm";
    }

    public String adminRoleEndpoint(String roleName) {
        return adminRealmBaseUrl() + "/roles/" + roleName;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @Getter
    @Setter
    public static class Roles {
        private String admin = "ADMIN";
        private String dispatcher = "DISPATCHER";
        private String courier = "COURIER";
    }
}
