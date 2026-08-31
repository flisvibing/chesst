package com.chesst.game;

import com.chesst.common.exception.BusinessException;
import com.chesst.game.dto.GameResponse;
import com.chesst.game.dto.SaveGameRequest;
import com.chesst.integration.ExternalGameImport;
import com.chesst.opening.Opening;
import com.chesst.opening.OpeningRepository;
import com.chesst.user.User;
import com.chesst.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GameService {

    private static final Pattern ECO_HEADER = Pattern.compile("\\[ECO\\s+\"([^\"]*)\"\\]");
    private static final Pattern OPENING_HEADER = Pattern.compile("\\[Opening\\s+\"([^\"]*)\"\\]");
    private static final Pattern WHITE_HEADER = Pattern.compile("\\[White\\s+\"([^\"]*)\"\\]");
    private static final Pattern BLACK_HEADER = Pattern.compile("\\[Black\\s+\"([^\"]*)\"\\]");
    private static final Pattern RESULT_HEADER = Pattern.compile("\\[Result\\s+\"([^\"]*)\"\\]");
    private static final Pattern FEN_HEADER = Pattern.compile("\\[FEN\\s+\"([^\"]*)\"\\]");
    private static final Pattern EVENT_HEADER = Pattern.compile("\\[Event\\s+\"([^\"]*)\"\\]");
    private static final Pattern DATE_HEADER = Pattern.compile("\\[Date\\s+\"([^\"]*)\"\\]");
    private static final Pattern SITE_HEADER = Pattern.compile("\\[Site\\s+\"([^\"]*)\"\\]");
    private static final Pattern MOVE_COUNT = Pattern.compile("\\d+\\.\\s");

    private final GameRepository games;
    private final UserRepository users;
    private final OpeningRepository openings;

    public GameService(GameRepository games, UserRepository users, OpeningRepository openings) {
        this.games = games;
        this.users = users;
        this.openings = openings;
    }

    @Transactional
    public GameResponse save(Long ownerId, SaveGameRequest req) {
        User owner = users.findById(ownerId)
                .orElseThrow(() -> new BusinessException("User not found", 404));
        Game g = new Game();
        g.setOwner(owner);
        g.setPgn(req.pgn());
        g.setWhite(orHeader(req.white(), WHITE_HEADER, req.pgn(), "White"));
        g.setBlack(orHeader(req.black(), BLACK_HEADER, req.pgn(), "Black"));
        g.setResult(orHeader(req.result(), RESULT_HEADER, req.pgn(), "*"));
        g.setEvent(orHeader(req.event(), EVENT_HEADER, req.pgn(), null));
        g.setSite(orHeader(req.site(), SITE_HEADER, req.pgn(), null));
        g.setDatePlayed(orHeader(req.datePlayed(), DATE_HEADER, req.pgn(), null));
        g.setEco(orHeader(req.eco(), ECO_HEADER, req.pgn(), null));
        g.setOpeningName(orHeader(req.openingName(), OPENING_HEADER, req.pgn(), null));
        g.setStartFen(orHeader(req.startFen(), FEN_HEADER, req.pgn(), null));
        g.setMoveCount(countPlies(req.pgn()));
        g.setSource("manual");
        games.save(g);
        return toResponse(g);
    }

    @Transactional
    public GameResponse importExternal(Long ownerId, ExternalGameImport imported, String source) {
        User owner = users.findById(ownerId)
                .orElseThrow(() -> new BusinessException("User not found", 404));
        Game g = new Game();
        g.setOwner(owner);
        g.setPgn(imported.pgn());
        g.setWhite(imported.white());
        g.setBlack(imported.black());
        g.setResult(imported.result());
        g.setEvent(imported.event());
        g.setSite(imported.site());
        g.setDatePlayed(imported.date());
        g.setEco(imported.eco());
        g.setOpeningName(imported.openingName());
        g.setStartFen(null);
        g.setMoveCount(countPlies(imported.pgn()));
        g.setSource(source);
        g.setSourceGameId(imported.sourceGameId());
        games.save(g);
        return toResponse(g);
    }

    @Transactional(readOnly = true)
    public List<GameResponse> listForUser(Long ownerId) {
        return games.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GameResponse get(Long id, Long requesterId) {
        Game g = games.findById(id)
                .orElseThrow(() -> new BusinessException("Game not found", 404));
        if (!g.getOwner().getId().equals(requesterId)) {
            throw new BusinessException("Forbidden", 403);
        }
        return toResponse(g);
    }

    @Transactional
    public void delete(Long id, Long requesterId) {
        Game g = games.findById(id)
                .orElseThrow(() -> new BusinessException("Game not found", 404));
        if (!g.getOwner().getId().equals(requesterId)) {
            throw new BusinessException("Forbidden", 403);
        }
        games.delete(g);
    }

    @Transactional(readOnly = true)
    public Opening detectOpeningFromPgn(String pgn) {
        if (pgn == null || pgn.isBlank()) return null;
        String sans = pgnToSans(pgn);
        Opening best = null;
        int bestLen = 0;
        for (Opening o : openings.findAll()) {
            String oSans = pgnToSans(o.getPgn());
            if (oSans.length() > sans.length()) continue;
            if (sans.startsWith(oSans) && oSans.length() > bestLen) {
                best = o;
                bestLen = oSans.length();
            }
        }
        return best;
    }

    private String orHeader(String explicit, Pattern header, String pgn, String fallback) {
        if (explicit != null && !explicit.isBlank()) return explicit;
        Matcher m = header.matcher(pgn);
        if (m.find()) return m.group(1);
        return fallback;
    }

    private int countPlies(String pgn) {
        if (pgn == null) return 0;
        String body = pgn.replaceAll("\\[[^]]*]", "").replaceAll("\\{[^}]*}", "");
        String[] tokens = body.replaceAll("\\d+\\.", " ").split("\\s+");
        int plies = 0;
        for (String t : tokens) {
            if (t.isBlank()) continue;
            if (t.equals("1-0") || t.equals("0-1") || t.equals("1/2-1/2") || t.equals("*")) continue;
            plies++;
        }
        return plies;
    }

    public static String pgnToSans(String pgn) {
        if (pgn == null) return "";
        String body = pgn.replaceAll("\\[[^]]*]", "").replaceAll("\\{[^}]*}", "");
        body = body.replaceAll("\\d+\\.", " ");
        StringBuilder sb = new StringBuilder();
        for (String t : body.split("\\s+")) {
            if (t.isBlank()) continue;
            if (t.equals("1-0") || t.equals("0-1") || t.equals("1/2-1/2") || t.equals("*")) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(t);
        }
        return sb.toString();
    }

    private GameResponse toResponse(Game g) {
        return new GameResponse(
                g.getId(),
                g.getOwner().getId(),
                g.getWhite(),
                g.getBlack(),
                g.getResult(),
                g.getEvent(),
                g.getEco(),
                g.getOpeningName(),
                g.getPgn(),
                g.getStartFen(),
                g.getMoveCount(),
                g.getSource(),
                g.getCreatedAt()
        );
    }
}
