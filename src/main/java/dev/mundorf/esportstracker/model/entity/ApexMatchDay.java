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

/**
 * A Battle Royale "match day" (e.g. ALGS "Group A vs B" on a given date): the Apex analog of a
 * {@link Match}, but with ~20 teams competing simultaneously across several games instead of a
 * head-to-head series. Deliberately a separate entity rather than a Match with nullable teamB -
 * forcing BR results into the two-team schema would corrupt the meaning of those columns.
 * Source: Cito API (aggregating official ALGS data), synced by CitoSyncService.
 */
@Entity
@Table(name = "apex_match_days")
@EntityListeners(AuditingEntityListener.class)
public class ApexMatchDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApexMatchDay() {
        // required by JPA
    }

    public ApexMatchDay(Tournament tournament, Game game, String name, Instant startsAt,
                        EventStatus status, String externalId) {
        this.tournament = tournament;
        this.game = game;
        this.name = name;
        this.startsAt = startsAt;
        this.status = status;
        this.externalId = externalId;
    }

    /** Refresh mutable fields from an external sync. Identity fields (game, externalId) never change. */
    public void update(Tournament tournament, String name, Instant startsAt, EventStatus status) {
        this.tournament = tournament;
        this.name = name;
        this.startsAt = startsAt;
        this.status = status;
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

    public String getName() {
        return name;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public EventStatus getStatus() {
        return status;
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
