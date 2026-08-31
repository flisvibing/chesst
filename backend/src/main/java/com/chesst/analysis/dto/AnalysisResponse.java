package com.chesst.analysis.dto;

import com.chesst.stockfish.dto.EngineResult;
import com.chesst.stockfish.dto.MoveEvaluation;

import java.util.List;

public record AnalysisResponse(
        Long gameId,
        Integer depth,
        List<MoveEvaluation> moves,
        Double accuracyW,
        Double accuracyB,
        int blundersW,
        int blundersB,
        int mistakesW,
        int mistakesB
) {
    public static AnalysisResponse positionOnly(EngineResult r) {
        return new AnalysisResponse(null, r.depth(), List.of(), null, null, 0, 0, 0, 0);
    }
}
