package com.vieterp.auth;

import lombok.Builder;
import java.util.Set;
import java.util.UUID;

/**
 * JWT claims extracted from Keycloak token.
 * Corresponds to TypeScript JwtPayload in packages/auth/src/jwt.ts
 * — must be kept in sync.
 */
@Builder
public record JwtClaims(
    UUID sub,
    String email,
    String name,
    Set<String> roles,
    String realm,
    long exp,
    long iat
) {}
