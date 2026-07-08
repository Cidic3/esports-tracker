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
 * A team's position within one standings table of a tournament. Riot organizes standings by
 * section (e.g. "Regular Season", or "Group A"/"Group B" for group-stage formats) - {@code
 * groupName} carries that section name so a tournament with more than one ranked table doesn't
 * collide. Bracket/playoff stages have no meaningful win-loss table (Riot returns an empty
 * ranking list for them), so they're never represented here - see Match for bracket data instead.
 * No externalId: unlike League/Team/Tournament/Match, a Riot ranking is a derived aggregate with
 * no stable id of its own, so upserts key off (tournament, team, groupName) instead.
 */
@Entity
@Table(name = "standings")
@EntityListeners(AuditingEntityListener.class)
public class Standing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "rank_position", nullable = false)
    private int rank;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Standing() {
        // required by JPA
    }

    public Standing(Tournament tournament, Team team, String groupName, int rank, int wins, int losses) {
        this.tournament = tournament;
        this.team = team;
        this.groupName = groupName;
        this.rank = rank;
        this.wins = wins;
        this.losses = losses;
    }

    /** Refresh mutable fields from an external sync. Identity fields (tournament, team, groupName) never change. */
    public void update(int rank, int wins, int losses) {
        this.rank = rank;
        this.wins = wins;
        this.losses = losses;
    }

    public UUID getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public Team getTeam() {
        return team;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getRank() {
        return rank;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
