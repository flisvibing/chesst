package com.chesst.game;

import com.chesst.common.exception.BusinessException;
import com.chesst.game.dto.GameResponse;
import com.chesst.game.dto.SaveGameRequest;
import com.chesst.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<GameResponse> save(HttpServletRequest req, @Valid @RequestBody SaveGameRequest body) {
        Long uid = AuthContext.resolve(req.getAttribute("userId"));
        if (uid == null) throw new BusinessException("Unauthorized", 401);
        return ResponseEntity.status(HttpStatus.CREATED).body(gameService.save(uid, body));
    }

    @GetMapping
    public List<GameResponse> list(HttpServletRequest req) {
        Long uid = AuthContext.resolve(req.getAttribute("userId"));
        if (uid == null) throw new BusinessException("Unauthorized", 401);
        return gameService.listForUser(uid);
    }

    @GetMapping("/{id}")
    public GameResponse get(HttpServletRequest req, @PathVariable Long id) {
        Long uid = AuthContext.resolve(req.getAttribute("userId"));
        if (uid == null) throw new BusinessException("Unauthorized", 401);
        return gameService.get(id, uid);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(HttpServletRequest req, @PathVariable Long id) {
        Long uid = AuthContext.resolve(req.getAttribute("userId"));
        if (uid == null) throw new BusinessException("Unauthorized", 401);
        gameService.delete(id, uid);
        return ResponseEntity.noContent().build();
    }
}
