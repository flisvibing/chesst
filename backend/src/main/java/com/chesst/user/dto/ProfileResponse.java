package com.chesst.user.dto;

public record ProfileResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String bio,
        String avatarUrl,
        Integer rating,
        boolean emailVerified,
        String lichessUsername,
        String chesscomUsername,
        long gameCount
) {}
