package com.chesst.integration;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LichessService {

    private static final Logger log = LoggerFactory.getLogger(LichessService.class);
    private static final Pattern HDR = Pattern.compile("\\[(\\w+)\\s+\"([^\"]*)\"]");

    private final WebClient client;
    private final ObjectMapper json = new ObjectMapper();

    public LichessService(com.chesst.config.AppProperties props) {
        this.client = WebClient.builder().baseUrl(props.lichess().baseUrl()).build();
    }

    public String fetchRecentPgns(String username, int max) {
        return client.get()
                .uri(uri -> uri.path("/games/user/{u}")
                        .queryParam("max", Math.min(50, max))
                        .queryParam("perfType", "blitz,rapid,classical")
                        .build(username))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(20))
                .block();
    }

    public JsonNode profile(String username) {
        return client.get()
                .uri("/user/{u}", username)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(15))
                .block();
    }

    public List<ExternalGameDto> parsePgns(String blob, int limit) {
        List<ExternalGameDto> out = new ArrayList<>();
        if (blob == null || blob.isBlank()) return out;
        String[] parts = blob.split("(?m)^\\[Event ");
        List<String> games = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            games.add("[Event " + parts[i]);
            if (games.size() >= limit) break;
        }
        for (String g : games) {
            out.add(parseOne(g, "lichess"));
        }
        return out;
    }

    private ExternalGameDto parseOne(String pgn, String source) {
        String white = "", black = "", result = "*", event = "", site = "", date = "", eco = "", opening = "";
        String sourceGameId = "";
        Matcher m = HDR.matcher(pgn);
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
