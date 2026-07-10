package dev.mundorf.esportstracker.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * An esports org, grouping {@link Team} rows that represent the same brand across different games
 * (e.g. a future Dota 2 "T1" and the existing LoL "T1" would both point here). Slug is globally
 * unique (unlike Team's, which is only unique per game) since an org is meant to span games. Only
 * League of Legends is synced today, so this is currently 1:1 with Team - the grouping exists so
 * linking logic is already in place whenever a second game's sync is built, rather than needing a
 * backfill migration later.
 */
@Entity
@Table(name = "organizations")
@EntityListeners(AuditingEntityListener.class)
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Organization() {
        // required by JPA
    }

    public Organization(String name, String slug, String logoUrl) {
        this.name = name;
        this.slug = slug;
        this.logoUrl = logoUrl;
    }

    /** Refresh mutable fields from an external sync. Identity field (slug) never changes. */
    public void update(String name, String logoUrl) {
        this.name = name;
        this.logoUrl = logoUrl;
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

    public String getLogoUrl() {
        return logoUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
