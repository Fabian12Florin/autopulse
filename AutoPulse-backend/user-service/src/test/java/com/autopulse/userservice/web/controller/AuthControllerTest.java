package com.autopulse.userservice.web.controller;

import com.autopulse.userservice.service.AuthService;
import com.autopulse.userservice.testutil.TestDataFactory;
import com.autopulse.userservice.web.dto.PasswordResetAcceptedResponse;
import com.autopulse.userservice.web.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController controller = new AuthController(authService);

    @Test
    void delegatesAuthEndpointsAndUsesNoContentForLogout() {
        TokenResponse token = TestDataFactory.tokenResponse("refresh");
        PasswordResetAcceptedResponse reset = new PasswordResetAcceptedResponse("accepted");
        when(authService.login(TestDataFactory.loginRequest())).thenReturn(token);
        when(authService.refresh(TestDataFactory.refreshTokenRequest())).thenReturn(token);
        when(authService.requestPasswordReset(TestDataFactory.passwordResetRequest())).thenReturn(reset);

        assertThat(controller.login(TestDataFactory.loginRequest()).getBody()).isSameAs(token);
        assertThat(controller.refresh(TestDataFactory.refreshTokenRequest()).getBody()).isSameAs(token);
        assertThat(controller.logout(TestDataFactory.logoutRequest()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.requestPasswordReset(TestDataFactory.passwordResetRequest()).getBody()).isSameAs(reset);
        verify(authService).logout(TestDataFactory.logoutRequest());
    }
}
