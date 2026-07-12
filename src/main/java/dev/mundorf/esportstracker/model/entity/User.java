package dev.mundorf.esportstracker.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Backs API-level optimistic concurrency for the follow-update endpoints, not just JPA's own
     * flush-time check: two requests computed from a stale cached profile (two tabs, or a toggle
     * fired before an earlier one's response lands) would otherwise silently overwrite each other,
     * since each PUT is a fresh full-replace load-modify-save with no memory of what the client
     * last saw. Callers must submit the version they last read; UserService compares it against
     * this column before applying a follow change and rejects a mismatch as a 409 rather than
     * quietly discarding one of the two updates.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_followed_games",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "game_id"))
    private Set<Game> followedGames = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_followed_teams",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "team_id"))
    private Set<Team> followedTeams = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_followed_leagues",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "league_id"))
    private Set<League> followedLeagues = new HashSet<>();

    protected User() {
        // required by JPA
    }

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<Game> getFollowedGames() {
        return followedGames;
    }

    public Set<Team> getFollowedTeams() {
        return followedTeams;
    }

    public Set<League> getFollowedLeagues() {
        return followedLeagues;
    }

    /** Full-replace semantics, matching the PUT /api/users/me/games contract. */
    public void replaceFollowedGames(Set<Game> games) {
        followedGames.clear();
        followedGames.addAll(games);
    }

    /** Full-replace semantics, matching the PUT /api/users/me/teams contract. */
    public void replaceFollowedTeams(Set<Team> teams) {
        followedTeams.clear();
        followedTeams.addAll(teams);
    }

    /** Full-replace semantics, matching the PUT /api/users/me/leagues contract. */
    public void replaceFollowedLeagues(Set<League> leagues) {
        followedLeagues.clear();
        followedLeagues.addAll(leagues);
    }
}
