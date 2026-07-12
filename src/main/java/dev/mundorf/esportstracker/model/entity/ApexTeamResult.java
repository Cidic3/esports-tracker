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
 * One team's cumulative result across all games of an {@link ApexMatchDay} (rank + total points).
 * Like Standing, this is a derived aggregate with no stable external id of its own - upserts key
 * off UNIQUE(match_day_id, team_id) instead.
 */
@Entity
@Table(name = "apex_team_results")
@EntityListeners(AuditingEntityListener.class)
public class ApexTeamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "match_day_id", nullable = false)
    private ApexMatchDay matchDay;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    // "rank" is a reserved keyword in some SQL dialects (H2 included), same workaround as Standing.
    @Column(name = "rank_position", nullable = false)
    private int rank;

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApexTeamResult() {
        // required by JPA
    }

    public ApexTeamResult(ApexMatchDay matchDay, Team team, int rank, int totalPoints) {
        this.matchDay = matchDay;
        this.team = team;
        this.rank = rank;
        this.totalPoints = totalPoints;
    }

    /** Refresh mutable fields from an external sync. Identity fields (matchDay, team) never change. */
    public void update(int rank, int totalPoints) {
        this.rank = rank;
        this.totalPoints = totalPoints;
    }

    public UUID getId() {
        return id;
    }

    public ApexMatchDay getMatchDay() {
        return matchDay;
    }

    public Team getTeam() {
        return team;
    }

    public int getRank() {
        return rank;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
