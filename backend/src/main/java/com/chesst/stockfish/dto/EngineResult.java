package com.chesst.stockfish.dto;

public record EngineResult(
        String fen,
        String bestMoveUci,
        int cp,
        Integer mate,
        int depth,
        int nps,
        String pv,
        boolean ok,
        String error
) {
    public static EngineResult error(String fen, String error) {
        return new EngineResult(fen, "(none)", 0, null, 0, 0, "", false, error);
    }
}
