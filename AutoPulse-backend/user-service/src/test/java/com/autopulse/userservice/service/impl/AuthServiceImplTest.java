package com.autopulse.userservice.service.impl;

import com.autopulse.userservice.service.KeycloakAdminService;
import com.autopulse.userservice.service.UserService;
import com.autopulse.userservice.testutil.TestDataFactory;
import com.autopulse.userservice.web.dto.PasswordResetAcceptedResponse;
import com.autopulse.userservice.web.dto.TokenResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final KeycloakAdminService keycloakAdminService = mock(KeycloakAdminService.class);
    private final UserService userService = mock(UserService.class);
    private final AuthServiceImpl service = new AuthServiceImpl(keycloakAdminService, userService);

    @Test
    void loginNormalizesEmailBeforeDelegating() {
        TokenResponse token = TestDataFactory.tokenResponse("refresh");
        when(keycloakAdminService.login("user@example.com", "Password123")).thenReturn(token);

        assertThat(service.login(TestDataFactory.loginRequest())).isSameAs(token);
    }

    @Test
    void refreshLogoutAndPasswordResetDelegate() {
        TokenResponse token = TestDataFactory.tokenResponse("new-refresh");
        PasswordResetAcceptedResponse response = new PasswordResetAcceptedResponse("accepted");
        when(keycloakAdminService.refresh("refresh-token")).thenReturn(token);
        when(userService.requestPasswordReset(TestDataFactory.passwordResetRequest())).thenReturn(response);

        assertThat(service.refresh(TestDataFactory.refreshTokenRequest())).isSameAs(token);
        service.logout(TestDataFactory.logoutRequest());
        assertThat(service.requestPasswordReset(TestDataFactory.passwordResetRequest())).isSameAs(response);

        verify(keycloakAdminService).logout("refresh-token");
    }
}
