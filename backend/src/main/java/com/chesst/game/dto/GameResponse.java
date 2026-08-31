package com.chesst.game.dto;

import java.time.Instant;

public record GameResponse(
        Long id,
        Long ownerId,
        String white,
        String black,
        String result,
        String event,
        String eco,
        String openingName,
        String pgn,
        String startFen,
        Integer moveCount,
        String source,
        Instant createdAt
) {}
