package com.chesst.analysis.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AnalyzeGameRequest(
        String startFen,
        @NotEmpty List<PlyInput> plies
) {
    public record PlyInput(
            int ply,
            String color,
            String san,
            String fenBefore
    ) {}
}
