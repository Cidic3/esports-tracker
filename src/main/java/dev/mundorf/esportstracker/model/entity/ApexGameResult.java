package dev.mundorf.esportstracker.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

/**
 * One team's result in a single game of an {@link ApexMatchDay} (placement + kills + points).
 * Child of {@link ApexTeamResult}; upserts key off UNIQUE(team_result_id, game_number).
 */
@Entity
@Table(name = "apex_game_results")
@EntityListeners(AuditingEntityListener.class)
public class ApexGameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_result_id", nullable = false)
    private ApexTeamResult teamResult;

    @Column(name = "game_number", nullable = false)
    private int gameNumber;

    @Column(nullable = false)
    private int placement;

    @Column(nullable = false)
    private int kills;

    @Column(nullable = false)
    private int points;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApexGameResult() {
        // required by JPA
    }

    public ApexGameResult(ApexTeamResult teamResult, int gameNumber, int placement, int kills, int points) {
        this.teamResult = teamResult;
        this.gameNumber = gameNumber;
        this.placement = placement;
        this.kills = kills;
        this.points = points;
    }

    /** Refresh mutable fields from an external sync. Identity fields (teamResult, gameNumber) never change. */
    public void update(int placement, int kills, int points) {
        this.placement = placement;
        this.kills = kills;
        this.points = points;
    }

    public UUID getId() {
        return id;
    }

    public ApexTeamResult getTeamResult() {
        return teamResult;
    }

    public int getGameNumber() {
        return gameNumber;
    }

    public int getPlacement() {
        return placement;
    }

    public int getKills() {
        return kills;
    }

    public int getPoints() {
        return points;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
