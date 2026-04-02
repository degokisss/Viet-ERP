package com.vieterp.otb.auth;

import com.vieterp.otb.auth.dto.*;
import com.vieterp.otb.domain.User;
import com.vieterp.otb.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private static final String MICROSOFT_GRAPH_URL = "https://graph.microsoft.com/v1.0/me";

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        // BCrypt check first, then plain-text fallback for legacy passwords
        String storedHash = user.getPasswordHash();
        boolean matches = storedHash != null && storedHash.startsWith("$2")
                ? passwordEncoder.matches(request.getPassword(), storedHash)
                : request.getPassword().equals(storedHash); // legacy plain-text fallback

        if (!matches) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Migrate legacy plain-text hash to BCrypt on successful login
        if (storedHash != null && !storedHash.startsWith("$2")) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
        }

        List<String> roles = getRolesForUser(user);
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getName(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                new AuthResponse.UserDto(user.getId(), user.getEmail(), user.getName())
        );
    }

    public AuthResponse loginWithMicrosoft(MicrosoftLoginRequest request) {
        // Validate Microsoft access token and get user info
        Map<String, Object> msUser = validateMicrosoftToken(request.getAccessToken());
        String email = (String) msUser.get("mail");
        if (email == null) {
            email = (String) msUser.get("userPrincipalName");
        }
        String name = (String) msUser.get("displayName");

        // Find or create user
        Optional<User> existingUser = userRepository.findByEmail(email.toLowerCase());
        User user;
        if (existingUser.isEmpty()) {
            // Generate a random encoded password — user will only use Microsoft SSO
            user = User.builder()
                    .email(email.toLowerCase())
                    .name(name)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .isActive(true)
                    .createdAt(java.time.Instant.now())
                    .build();
            user = userRepository.save(user);
        } else {
            user = existingUser.get();
            if (Boolean.TRUE.equals(user.getIsActive())) {
                // Update name if changed
                if (name != null && !name.equals(user.getName())) {
                    user.setName(name);
                    user = userRepository.save(user);
                }
            } else {
                throw new IllegalArgumentException("Account is deactivated");
            }
        }

        List<String> roles = getRolesForUser(user);
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getName(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                new AuthResponse.UserDto(user.getId(), user.getEmail(), user.getName())
        );
    }

    public AuthResponse refresh(RefreshRequest request) {
        String token = request.getRefreshToken();
        if (!jwtUtil.isRefreshToken(token)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        List<String> roles = getRolesForUser(user);
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getName(), roles);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                newRefreshToken,
                new AuthResponse.UserDto(user.getId(), user.getEmail(), user.getName())
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new AuthResponse.UserDto(user.getId(), user.getEmail(), user.getName());
    }

    public AuthResponse.UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        user = userRepository.save(user);
        return new AuthResponse.UserDto(user.getId(), user.getEmail(), user.getName());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> validateMicrosoftToken(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(accessToken);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                    MICROSOFT_GRAPH_URL,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("Invalid Microsoft access token");
            }
            return response.getBody();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to validate Microsoft token: " + e.getMessage());
        }
    }

    private List<String> getRolesForUser(User user) {
        // Default role for OTB users
        return List.of("user");
    }
}
