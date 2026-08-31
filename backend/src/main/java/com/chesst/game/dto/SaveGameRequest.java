package com.chesst.game.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveGameRequest(
        @NotBlank String pgn,
        String white,
        String black,
        String result,
        String event,
        String site,
        String datePlayed,
        String eco,
        String openingName,
        String startFen
) {}
