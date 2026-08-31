package com.chesst.integration.dto;

public record ExternalGameDto(
        String source,
        String sourceGameId,
        String white,
        String black,
        String result,
        String event,
        String site,
        String date,
        String eco,
        String openingName,
        String pgn
) {}
