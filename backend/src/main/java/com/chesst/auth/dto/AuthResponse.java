package com.chesst.auth.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserProfile user
) {
    public record UserProfile(
            Long id,
            String username,
            String email,
            String displayName,
            Integer rating,
            boolean emailVerified,
            String lichessUsername,
            String chesscomUsername,
            Instant createdAt
    ) {}
}
