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
 * A roster entry for a {@link Team}. Sourced from Riot's getTeams endpoint, which is the only
 * source of player identity/role data available (no coach field exists anywhere in that response -
 * confirmed by direct probing, not just absent from the DTOs mapped here).
 */
@Entity
@Table(name = "players")
@EntityListeners(AuditingEntityListener.class)
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "summoner_name", nullable = false, length = 100)
    private String summonerName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayerRole role;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Player() {
        // required by JPA
    }

    public Player(Team team, String summonerName, String firstName, String lastName,
                  String imageUrl, PlayerRole role, String externalId) {
        this.team = team;
        this.summonerName = summonerName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.imageUrl = imageUrl;
        this.role = role;
        this.externalId = externalId;
    }

    /** Refresh mutable fields from an external sync. Identity fields (team, externalId) never change. */
    public void update(String summonerName, String firstName, String lastName, String imageUrl, PlayerRole role) {
        this.summonerName = summonerName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.imageUrl = imageUrl;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }

    public String getSummonerName() {
        return summonerName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public PlayerRole getRole() {
        return role;
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
