package com.vieterp.otb;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    JwtDecoder jwtDecoder;

    private String testEmail = "integration-test@example.com";
    private String testPassword = "testPassword123";
    private String testName = "Integration Test User";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void login_withValidCredentials_returnsTokensAndUser() {
        createTestUser();

        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/auth/login", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().refreshToken()).isNotBlank();
        assertThat(response.getBody().user()).isNotNull();
        assertThat(response.getBody().user().email()).isEqualTo(testEmail);
        assertThat(response.getBody().user().name()).isEqualTo(testName);
    }

    @Test
    void login_withInvalidPassword_returnsBadRequest() {
        createTestUser();

        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("wrongpassword");

        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/auth/login", request, Void.class);

        // GlobalExceptionHandler maps IllegalArgumentException -> 400
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_withInactiveAccount_returnsBadRequest() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = User.builder()
            .email(testEmail)
            .name(testName)
            .passwordHash(encoder.encode(testPassword))
            .isActive(false)
            .createdAt(Instant.now())
            .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/auth/login", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getMe_withValidToken_returnsUserProfile() {
        createTestUser();

        AuthResponse loginResponse = login();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginResponse.accessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<AuthResponse.UserDto> response = restTemplate.exchange(
            "/auth/me", HttpMethod.GET, entity, AuthResponse.UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo(testEmail);
        assertThat(response.getBody().name()).isEqualTo(testName);
    }

    @Test
    void getMe_withoutToken_returnsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/auth/me", HttpMethod.GET, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_withValidRefreshToken_returnsNewTokens() {
        createTestUser();

        AuthResponse loginResponse = login();

        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(loginResponse.refreshToken());

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/auth/refresh", refreshRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
        assertThat(response.getBody().refreshToken()).isNotBlank();
    }

    @Test
    void refresh_withInvalidToken_returnsBadRequest() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("invalid-refresh-token");

        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/auth/refresh", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void logout_isSuccessful() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/auth/logout", null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void login_withNonExistentUser_returnsBadRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("anypassword");

        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/auth/login", request, Void.class);

        // GlobalExceptionHandler maps IllegalArgumentException -> 400
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private User createTestUser() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = User.builder()
            .email(testEmail)
            .name(testName)
            .passwordHash(encoder.encode(testPassword))
            .isActive(true)
            .createdAt(Instant.now())
            .build();
        return userRepository.save(user);
    }

    private AuthResponse login() {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/auth/login", request, AuthResponse.class);
        return response.getBody();
    }
}
