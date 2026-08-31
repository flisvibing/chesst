package com.chesst.integration;

import com.chesst.config.AppProperties;
import com.chesst.integration.dto.ExternalGameDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChesscomService {

    private static final Logger log = LoggerFactory.getLogger(ChesscomService.class);

    private final WebClient client;
    private final ObjectMapper json = new ObjectMapper();

    public ChesscomService(AppProperties props) {
        this.client = WebClient.builder().baseUrl(props.chesscom().baseUrl()).build();
    }

    public JsonNode profile(String username) {
        return client.get()
                .uri("/player/{u}", username)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .block();
    }

    public List<ExternalGameDto> fetchRecentGames(String username, int max) {
        List<ExternalGameDto> out = new ArrayList<>();
        try {
            JsonNode archives = client.get()
                    .uri("/player/{u}/games/archives", username)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            if (archives == null || !archives.has("archives")) return out;
            JsonNode arr = archives.get("archives");
            int total = arr.size();
            for (int i = total - 1; i >= 0 && out.size() < max; i--) {
                String url = arr.get(i).asText();
                String monthPgn = client.get()
                        .uri(url.replace("https://api.chess.com", ""))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(30))
                        .block();
                if (monthPgn == null) continue;
                String[] parts = monthPgn.split("(?m)^\\[Event ");
                for (int j = 1; j < parts.length && out.size() < max; j++) {
                    String pgn = "[Event " + parts[j];
                    out.add(parseOne(pgn, "chesscom"));
                }
            }
        } catch (Exception e) {
            log.warn("Chess.com fetch failed for {}: {}", username, e.getMessage());
        }
        return out;
    }

    private ExternalGameDto parseOne(String pgn, String source) {
        String white = "", black = "", result = "*", event = "", site = "", date = "", eco = "", opening = "";
        String sourceGameId = "";
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\[(\\w+)\\s+\"([^\"]*)\"]").matcher(pgn);
        while (m.find()) {
            String key = m.group(1);
            String val = m.group(2);
            switch (key) {
                case "White" -> white = val;
                case "Black" -> black = val;
                case "Result" -> result = val;
                case "Event" -> event = val;
                case "Site" -> { site = val; int slash = val.lastIndexOf('/'); if (slash >= 0) sourceGameId = val.substring(slash + 1); }
                case "Date" -> date = val;
                case "ECO" -> eco = val;
                case "Opening" -> opening = val;
                default -> { }
            }
        }
        return new ExternalGameDto(source, sourceGameId, white, black, result, event, site, date,
                eco, opening, pgn.trim());
    }
}
