package com.chesst.game;

import com.chesst.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 120)
    private String white = "White";

    @Column(nullable = false, length = 120)
    private String black = "Black";

    @Column(nullable = false, length = 16)
    private String result = "*";

    @Column(length = 200)
    private String event;

    @Column(length = 200)
    private String site;

    @Column(name = "date_played", length = 20)
    private String datePlayed;

    @Column(length = 5)
    private String eco;

    @Column(name = "opening_name", length = 200)
    private String openingName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pgn;

    @Column(name = "start_fen", columnDefinition = "TEXT")
    private String startFen;

    @Column(name = "move_count", nullable = false)
    private Integer moveCount = 0;

    @Column(nullable = false, length = 20)
    private String source = "manual";

    @Column(name = "source_game_id", length = 64)
    private String sourceGameId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getWhite() { return white; }
    public void setWhite(String white) { this.white = white; }
    public String getBlack() { return black; }
    public void setBlack(String black) { this.black = black; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public String getDatePlayed() { return datePlayed; }
    public void setDatePlayed(String datePlayed) { this.datePlayed = datePlayed; }
    public String getEco() { return eco; }
    public void setEco(String eco) { this.eco = eco; }
    public String getOpeningName() { return openingName; }
    public void setOpeningName(String openingName) { this.openingName = openingName; }
    public String getPgn() { return pgn; }
    public void setPgn(String pgn) { this.pgn = pgn; }
    public String getStartFen() { return startFen; }
    public void setStartFen(String startFen) { this.startFen = startFen; }
    public Integer getMoveCount() { return moveCount; }
    public void setMoveCount(Integer moveCount) { this.moveCount = moveCount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceGameId() { return sourceGameId; }
    public void setSourceGameId(String sourceGameId) { this.sourceGameId = sourceGameId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
