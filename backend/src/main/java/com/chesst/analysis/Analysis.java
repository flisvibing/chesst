package com.chesst.analysis;

import com.chesst.game.Game;
import com.chesst.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer depth;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "accuracy_w")
    private Double accuracyW;

    @Column(name = "accuracy_b")
    private Double accuracyB;

    @Column(name = "blunders_w", nullable = false)
    private Integer blundersW = 0;

    @Column(name = "blunders_b", nullable = false)
    private Integer blundersB = 0;

    @Column(name = "mistakes_w", nullable = false)
    private Integer mistakesW = 0;

    @Column(name = "mistakes_b", nullable = false)
    private Integer mistakesB = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Integer getDepth() { return depth; }
    public void setDepth(Integer depth) { this.depth = depth; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Double getAccuracyW() { return accuracyW; }
    public void setAccuracyW(Double accuracyW) { this.accuracyW = accuracyW; }
    public Double getAccuracyB() { return accuracyB; }
    public void setAccuracyB(Double accuracyB) { this.accuracyB = accuracyB; }
    public Integer getBlundersW() { return blundersW; }
    public void setBlundersW(Integer blundersW) { this.blundersW = blundersW; }
    public Integer getBlundersB() { return blundersB; }
    public void setBlundersB(Integer blundersB) { this.blundersB = blundersB; }
    public Integer getMistakesW() { return mistakesW; }
    public void setMistakesW(Integer mistakesW) { this.mistakesW = mistakesW; }
    public Integer getMistakesB() { return mistakesB; }
    public void setMistakesB(Integer mistakesB) { this.mistakesB = mistakesB; }
    public Instant getCreatedAt() { return createdAt; }
}
