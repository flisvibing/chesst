package com.chesst.analysis.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalyzePositionRequest(
        @NotBlank String fen,
        Integer depth,
        Integer movetimeMs
) {}
