package com.chesst.analysis;

import com.chesst.analysis.dto.AnalyzeGameRequest;
import com.chesst.analysis.dto.AnalyzePositionRequest;
import com.chesst.analysis.dto.AnalysisResponse;
import com.chesst.common.exception.BusinessException;
import com.chesst.security.AuthContext;
import com.chesst.stockfish.dto.EngineResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/position")
    public EngineResult analyzePosition(HttpServletRequest req, @Valid @RequestBody AnalyzePositionRequest body) {
        requireUser(req);
        return analysisService.analyzePosition(body);
    }

    @PostMapping("/games/{gameId}")
    public AnalysisResponse analyzeGame(HttpServletRequest req,
                                        @PathVariable Long gameId,
                                        @Valid @RequestBody AnalyzeGameRequest body) {
        Long uid = requireUser(req);
        return analysisService.analyzeGame(uid, gameId, body);
    }

    @GetMapping("/games/{gameId}")
    public List<Analysis> history(HttpServletRequest req, @PathVariable Long gameId) {
        requireUser(req);
        return analysisService.historyForGame(gameId);
    }

    private Long requireUser(HttpServletRequest req) {
        Long uid = AuthContext.resolve(req.getAttribute("userId"));
        if (uid == null) throw new BusinessException("Unauthorized", 401);
        return uid;
    }
}
