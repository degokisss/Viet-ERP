package com.vieterp.otb;

import com.vieterp.otb.auth.AuthService;
import com.vieterp.otb.auth.dto.AuthResponse;
import com.vieterp.otb.auth.dto.LoginRequest;
import com.vieterp.otb.auth.dto.RefreshRequest;
import com.vieterp.otb.domain.User;
import com.vieterp.otb.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthServiceDirectTest extends BaseIntegrationTest {

    @MockBean
    JwtDecoder jwtDecoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private String testEmail = "direct-test@example.com";
    private String testPassword = "directTestPassword123";
    private String testName = "Direct Test User";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void login_withValidCredentials_returnsTokens() {
        User user = User.builder()
            .email(testEmail)
            .name(testName)
            .isActive(true)
            .createdAt(Instant.now())
            .build();
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
            .encode(testPassword));
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo(testEmail);
        assertThat(response.user().name()).isEqualTo(testName);
    }

    @Test
    void login_withInvalidPassword_throws() {
        User user = User.builder()
            .email(testEmail)
            .name(testName)
            .isActive(true)
            .createdAt(Instant.now())
            .build();
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
            .encode(testPassword));
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("wrongpassword");

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_withInactiveAccount_throws() {
        User user = User.builder()
            .email(testEmail)
            .name(testName)
            .isActive(false)
            .createdAt(Instant.now())
            .build();
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
            .encode(testPassword));
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("deactivated");
    }

    @Test
    void login_withNonExistentUser_throws() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("anypassword");

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid email or password");
    }

    @Test
    void getProfile_returnsUserData() {
        User user = User.builder()
            .email(testEmail)
            .name(testName)
            .passwordHash("$2a$10$dummy")
            .isActive(true)
            .createdAt(Instant.now())
            .build();
        user = userRepository.save(user);

        AuthResponse.UserDto profile = authService.getProfile(user.getId());

        assertThat(profile.email()).isEqualTo(testEmail);
        assertThat(profile.name()).isEqualTo(testName);
    }

    @Test
    void updateProfile_updatesName() {
        User user = User.builder()
            .email(testEmail)
            .name(testName)
            .passwordHash("$2a$10$dummy")
            .isActive(true)
            .createdAt(Instant.now())
            .build();
        user = userRepository.save(user);

        com.vieterp.otb.auth.dto.UpdateProfileRequest updateRequest =
            new com.vieterp.otb.auth.dto.UpdateProfileRequest();
        updateRequest.setName("Updated Name");

        AuthResponse.UserDto updated = authService.updateProfile(user.getId(), updateRequest);

        assertThat(updated.name()).isEqualTo("Updated Name");
        assertThat(updated.email()).isEqualTo(testEmail);
    }
}
