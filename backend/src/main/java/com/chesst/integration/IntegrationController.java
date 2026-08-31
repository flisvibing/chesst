package com.chesst.integration;

import com.chesst.common.exception.BusinessException;
import com.chesst.game.GameService;
import com.chesst.integration.dto.ExternalGameDto;
import com.chesst.security.AuthContext;
import com.chesst.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private final LichessService lichess;
    private final ChesscomService chesscom;
    private final GameService gameService;
    private final UserRepository users;

    public IntegrationController(LichessService lichess, ChesscomService chesscom,
                                 GameService gameService, UserRepository users) {
        this.lichess = lichess;
        this.chesscom = chesscom;
        this.gameService = gameService;
        this.users = users;
    }

    @GetMapping("/lichess/{username}/games")
    public List<ExternalGameDto> lichessGames(@PathVariable String username,
                                              @RequestParam(defaultValue = "10") int max) {
        String blob = lichess.fetchRecentPgns(username, max);
        return lichess.parsePgns(blob, max);
    }

    @GetMapping("/lichess/{username}/profile")
    public Object lichessProfile(@PathVariable String username) {
        return lichess.profile(username);
    }

    @PostMapping("/lichess/{username}/import")
    public ResponseEntity<Map<String, Object>> importLichess(HttpServletRequest req,
                                                             @PathVariable String username,
                                                             @RequestParam(defaultValue = "10") int max) {
        Long uid = requireUser(req);
        String blob = lichess.fetchRecentPgns(username, max);
        List<ExternalGameDto> games = lichess.parsePgns(blob, max);
        int saved = 0;
        for (ExternalGameDto g : games) {
            try {
                gameService.importExternal(uid, ExternalGameImport.from(g), "lichess");
                saved++;
            } catch (Exception ignored) { }
        }
        users.findById(uid).ifPresent(u -> {
            u.setLichessUsername(username);
            users.save(u);
        });
        return ResponseEntity.ok(Map.of("imported", saved, "total", games.size()));
    }

    @GetMapping("/chesscom/{username}/games")
    public List<ExternalGameDto> chesscomGames(@PathVariable String username,
                                               @RequestParam(defaultValue = "10") int max) {
        return chesscom.fetchRecentGames(username, max);
    }

    @GetMapping("/chesscom/{username}/profile")
    public Object chesscomProfile(@PathVariable String username) {
        return chesscom.profile(username);
    }

    @PostMapping("/chesscom/{username}/import")
    public ResponseEntity<Map<String, Object>> importChesscom(HttpServletRequest req,
                                                              @PathVariable String username,
                                                              @RequestParam(defaultValue = "10") int max) {
        Long uid = requireUser(req);
        List<ExternalGameDto> games = chesscom.fetchRecentGames(username, max);
        int saved = 0;
        for (ExternalGameDto g : games) {
            try {
                gameService.importExternal(uid, ExternalGameImport.from(g), "chesscom");
                saved++;
            } catch (Exception ignored) { }
        }
        users.findById(uid).ifPresent(u -> {
            u.setChesscomUsername(username);
            users.save(u);
        });
        return ResponseEntity.ok(Map.of("imported", saved, "total", games.size()));
    }

    private Long requireUser(HttpServletRequest req) {
        Long uid = AuthContext.resolve(req.getAttribute("userId"));
        if (uid == null) throw new BusinessException("Unauthorized", 401);
        return uid;
    }
}
