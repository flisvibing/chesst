package com.chesst.opening;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "openings")
public class Opening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5)
    private String eco;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pgn;

    @Column(columnDefinition = "TEXT")
    private String fen;

    @Column(name = "white_wins", nullable = false)
    private Integer whiteWins = 0;

    @Column(nullable = false)
    private Integer draws = 0;

    @Column(name = "black_wins", nullable = false)
    private Integer blackWins = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getEco() { return eco; }
    public void setEco(String eco) { this.eco = eco; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPgn() { return pgn; }
    public void setPgn(String pgn) { this.pgn = pgn; }
    public String getFen() { return fen; }
    public void setFen(String fen) { this.fen = fen; }
    public Integer getWhiteWins() { return whiteWins; }
    public void setWhiteWins(Integer whiteWins) { this.whiteWins = whiteWins; }
    public Integer getDraws() { return draws; }
    public void setDraws(Integer draws) { this.draws = draws; }
    public Integer getBlackWins() { return blackWins; }
    public void setBlackWins(Integer blackWins) { this.blackWins = blackWins; }
    public Instant getCreatedAt() { return createdAt; }
}
