package com.chesst.analysis;

import com.chesst.analysis.dto.AnalyzeGameRequest;
import com.chesst.analysis.dto.AnalyzePositionRequest;
import com.chesst.analysis.dto.AnalysisResponse;
import com.chesst.common.exception.BusinessException;
import com.chesst.game.Game;
import com.chesst.game.GameRepository;
import com.chesst.stockfish.StockfishService;
import com.chesst.stockfish.dto.EngineResult;
import com.chesst.stockfish.dto.MoveEvaluation;
import com.chesst.user.User;
import com.chesst.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final StockfishService stockfish;
    private final AnalysisRepository analyses;
    private final GameRepository games;
    private final UserRepository users;
    private final ObjectMapper json;

    public AnalysisService(StockfishService stockfish, AnalysisRepository analyses,
                           GameRepository games, UserRepository users, ObjectMapper json) {
        this.stockfish = stockfish;
        this.analyses = analyses;
        this.games = games;
        this.users = users;
        this.json = json;
    }

    public EngineResult analyzePosition(AnalyzePositionRequest req) {
        return stockfish.analyze(req.fen(), req.depth(), req.movetimeMs());
    }

    @Transactional
    public AnalysisResponse analyzeGame(Long userId, Long gameId, AnalyzeGameRequest req) {
        User u = users.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", 404));
        Game g = games.findById(gameId)
                .orElseThrow(() -> new BusinessException("Game not found", 404));
        if (!g.getOwner().getId().equals(userId)) {
            throw new BusinessException("Forbidden", 403);
        }

        int depth = 12;
        List<MoveEvaluation> evals = new ArrayList<>();
        int blundersW = 0, blundersB = 0, mistakesW = 0, mistakesB = 0;
        int[] lossesW = new int[req.plies().size()];
        int[] lossesB = new int[req.plies().size()];
        int wIdx = 0, bIdx = 0;

        for (AnalyzeGameRequest.PlyInput ply : req.plies()) {
            EngineResult before = stockfish.analyze(ply.fenBefore(), depth, 400);
            int evalBefore = toCp(before);
            String bestUci = before.ok() ? before.bestMoveUci() : null;
            boolean isBest = bestUci != null && !bestUci.equals("(none)");
            String classification;
            int delta;
            if (isBest) {
                classification = "best";
                delta = 0;
            } else {
                classification = "inaccuracy";
                delta = 40;
            }
            if ("w".equals(ply.color())) {
                lossesW[wIdx++] = delta;
                if ("blunder".equals(classification)) blundersW++;
                if ("mistake".equals(classification)) mistakesW++;
            } else {
                lossesB[bIdx++] = delta;
                if ("blunder".equals(classification)) blundersB++;
                if ("mistake".equals(classification)) mistakesB++;
            }
            evals.add(new MoveEvaluation(
                    ply.ply(),
                    ply.color(),
                    ply.san(),
                    ply.fenBefore(),
                    evalBefore,
                    before.mate(),
                    bestUci,
                    classification,
                    delta
            ));
        }

        Double accW = wIdx > 0 ? accuracy(lossesW, wIdx) : null;
        Double accB = bIdx > 0 ? accuracy(lossesB, bIdx) : null;

        Analysis a = new Analysis();
        a.setGame(g);
        a.setUser(u);
        a.setDepth(depth);
        try {
            a.setPayload(json.writeValueAsString(evals));
        } catch (JsonProcessingException e) {
            a.setPayload("[]");
        }
        a.setAccuracyW(accW);
        a.setAccuracyB(accB);
        a.setBlundersW(blundersW);
        a.setBlundersB(blundersB);
        a.setMistakesW(mistakesW);
        a.setMistakesB(mistakesB);
        analyses.save(a);

        return new AnalysisResponse(gameId, depth, evals, accW, accB,
                blundersW, blundersB, mistakesW, mistakesB);
    }

    @Transactional(readOnly = true)
    public List<Analysis> historyForGame(Long gameId) {
        Game g = games.findById(gameId)
                .orElseThrow(() -> new BusinessException("Game not found", 404));
        return analyses.findByGameOrderByCreatedAtDesc(g);
    }

    private int toCp(EngineResult r) {
        if (r.mate() != null) {
            return r.mate() > 0 ? 100000 - r.mate() * 100 : -100000 - r.mate() * 100;
        }
        return r.cp();
    }

    private double accuracy(int[] losses, int n) {
        double sum = 0;
        for (int i = 0; i < n; i++) sum += losses[i];
        double avgCpLoss = sum / n;
        double avgWpl = winPercentLoss(avgCpLoss);
        double acc = 103.1668 * Math.exp(-0.04354 * (avgWpl - 58.72)) + 50;
        return Math.max(0, Math.min(100, Math.round(acc * 10) / 10.0));
    }

    private double winPercentLoss(double cpLoss) {
        double p = 1.0 / (1.0 + Math.exp(-0.00368208 * cpLoss));
        return p * 100;
    }
}
