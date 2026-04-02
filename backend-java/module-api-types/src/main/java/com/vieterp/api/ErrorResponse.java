package com.vieterp.api;

import java.time.Instant;
import lombok.Builder;

/**
 * Standard error response conforming to RFC 7807 Problem Details.
 * Must match TypeScript ErrorResponse in packages/api-types/src/error.ts
 * — must be kept in sync.
 */
@Builder
public record ErrorResponse(
    String type,
    String title,
    int status,
    String detail,
    Instant timestamp
) {
    /** Record body: ensures timestamp defaults to now if not explicitly set. */
    public ErrorResponse {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    }
