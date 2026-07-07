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

@Entity
@Table(name = "leagues")
@EntityListeners(AuditingEntityListener.class)
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(length = 100)
    private String region;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected League() {
        // required by JPA
    }

    public League(String name, String slug, String region, Game game, String externalId) {
        this.name = name;
        this.slug = slug;
        this.region = region;
        this.game = game;
        this.externalId = externalId;
    }

    /** Refresh mutable fields from an external sync. Identity fields (game, externalId) are never changed. */
    public void update(String name, String slug, String region) {
        this.name = name;
        this.slug = slug;
        this.region = region;
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

    public String getRegion() {
        return region;
    }

    public Game getGame() {
        return game;
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
