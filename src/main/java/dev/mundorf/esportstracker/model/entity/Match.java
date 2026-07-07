package dev.mundorf.esportstracker.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matches")
@EntityListeners(AuditingEntityListener.class)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_id", nullable = false)
    private Team teamA;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_id", nullable = false)
    private Team teamB;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "score_a")
    private Integer scoreA;

    @Column(name = "score_b")
    private Integer scoreB;

    @Column(name = "stream_url", length = 500)
    private String streamUrl;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Match() {
        // required by JPA
    }

    public Match(Tournament tournament, Game game, Team teamA, Team teamB, Instant scheduledAt,
                 EventStatus status, Integer scoreA, Integer scoreB, String streamUrl, String externalId) {
        this.tournament = tournament;
        this.game = game;
        this.teamA = teamA;
        this.teamB = teamB;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.streamUrl = streamUrl;
        this.externalId = externalId;
    }

    /** Refresh mutable fields from an external sync. Identity fields (game, externalId) are never changed. */
    public void update(Tournament tournament, Team teamA, Team teamB, Instant scheduledAt,
                       EventStatus status, Integer scoreA, Integer scoreB, String streamUrl) {
        this.tournament = tournament;
        this.teamA = teamA;
        this.teamB = teamB;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.streamUrl = streamUrl;
    }

    public UUID getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public Game getGame() {
        return game;
    }

    public Team getTeamA() {
        return teamA;
    }

    public Team getTeamB() {
        return teamB;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Integer getScoreA() {
        return scoreA;
    }

    public Integer getScoreB() {
        return scoreB;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getExternalId() {
        return externalId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
