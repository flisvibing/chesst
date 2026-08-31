package com.chesst.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Jwt jwt,
        Email email,
        Stockfish stockfish,
        Lichess lichess,
        Chesscom chesscom
) {
    public record Cors(List<String> allowedOrigins) {}
    public record Jwt(String secret, int accessTokenTtlMinutes, int refreshTokenTtlDays, String issuer) {}
    public record Email(String from, boolean verificationRequired, boolean logCodesWhenNoSmtp) {}
    public record Stockfish(String binaryPath, int depth, int movetimeMs, int maxConcurrent) {}
    public record Lichess(String baseUrl) {}
    public record Chesscom(String baseUrl) {}
}
