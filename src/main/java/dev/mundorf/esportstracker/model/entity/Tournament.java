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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tournaments")
@EntityListeners(AuditingEntityListener.class)
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String slug;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TournamentTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "prize_pool", precision = 12, scale = 2)
    private BigDecimal prizePool;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Tournament() {
        // required by JPA
    }

    public Tournament(String name, String slug, League league, Game game, LocalDate startDate,
                       LocalDate endDate, TournamentTier tier, EventStatus status,
                       BigDecimal prizePool, String externalId) {
        this.name = name;
        this.slug = slug;
        this.league = league;
        this.game = game;
        this.startDate = startDate;
        this.endDate = endDate;
        this.tier = tier;
        this.status = status;
        this.prizePool = prizePool;
        this.externalId = externalId;
    }

    /** Refresh mutable fields from an external sync. Identity fields (game, externalId) are never changed. */
    public void update(String name, String slug, League league, LocalDate startDate, LocalDate endDate,
                        TournamentTier tier, EventStatus status, BigDecimal prizePool) {
        this.name = name;
        this.slug = slug;
        this.league = league;
        this.startDate = startDate;
        this.endDate = endDate;
        this.tier = tier;
        this.status = status;
        this.prizePool = prizePool;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public League getLeague() {
        return league;
    }

    public Game getGame() {
        return game;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public TournamentTier getTier() {
        return tier;
    }

    public EventStatus getStatus() {
        return status;
    }

    public BigDecimal getPrizePool() {
        return prizePool;
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
