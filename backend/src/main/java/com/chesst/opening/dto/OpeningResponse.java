package com.chesst.opening.dto;

public record OpeningResponse(
        Long id,
        String eco,
        String name,
        String pgn,
        String fen,
        Integer whiteWins,
        Integer draws,
        Integer blackWins
) {}
