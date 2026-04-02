package com.vieterp.otb.e2e;

import com.vieterp.otb.BaseIntegrationTest;
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

/**
 * End-to-end auth flow test against a real Spring Boot instance.
 * Uses the external PostgreSQL at localhost:5432 (must be running).
 * Run with: mvn test -pl module-otb -Dtest=AuthFlowE2eTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowE2eTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    JwtDecoder jwtDecoder;

    private String testEmail = "e2e-test@vieterp.com";
    private String testPassword = "e2ePassword123";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = User.builder()
            .email(testEmail)
            .name("E2E Test User")
            .passwordHash(encoder.encode(testPassword))
            .isActive(true)
            .createdAt(Instant.now())
            .build();
        userRepository.save(user);
    }

    @Test
    void fullAuthFlow_loginToRefresh() {
        // 1. POST /auth/login → extract JWT
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
            "/auth/login", loginRequest, AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        String accessToken = loginResponse.getBody().accessToken();
        String refreshToken = loginResponse.getBody().refreshToken();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // 2. GET /auth/me with JWT → verify user data
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<AuthResponse.UserDto> meResponse = restTemplate.exchange(
            "/auth/me", HttpMethod.GET, entity, AuthResponse.UserDto.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        assertThat(meResponse.getBody().email()).isEqualTo(testEmail);

        // 3. POST /auth/refresh → verify new tokens
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(refreshToken);

        ResponseEntity<AuthResponse> refreshResponse = restTemplate.postForEntity(
            "/auth/refresh", refreshRequest, AuthResponse.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        assertThat(refreshResponse.getBody().accessToken()).isNotBlank();
        assertThat(refreshResponse.getBody().refreshToken()).isNotBlank();
        // New tokens should be different from old ones (rotation)
        assertThat(refreshResponse.getBody().accessToken()).isNotEqualTo(accessToken);
        assertThat(refreshResponse.getBody().refreshToken()).isNotEqualTo(refreshToken);

        // 4. Verify new token works for /auth/me
        HttpHeaders newHeaders = new HttpHeaders();
        newHeaders.setBearerAuth(refreshResponse.getBody().accessToken());
        HttpEntity<Void> newEntity = new HttpEntity<>(newHeaders);

        ResponseEntity<AuthResponse.UserDto> newMeResponse = restTemplate.exchange(
            "/auth/me", HttpMethod.GET, newEntity, AuthResponse.UserDto.class);
        assertThat(newMeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(newMeResponse.getBody().email()).isEqualTo(testEmail);
    }

    @Test
    void authFlow_invalidCredentials_returnsBadRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@vieterp.com");
        loginRequest.setPassword("wrongpassword");

        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/auth/login", loginRequest, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void authFlow_noToken_returnsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/auth/me", HttpMethod.GET, entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
