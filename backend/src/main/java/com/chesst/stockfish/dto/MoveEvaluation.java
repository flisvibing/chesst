package com.chesst.stockfish.dto;

public record MoveEvaluation(
        int ply,
        String color,
        String san,
        String fen,
        Integer evalCp,
        Integer mate,
        String bestMoveUci,
        String classification,
        Integer delta
) {}
