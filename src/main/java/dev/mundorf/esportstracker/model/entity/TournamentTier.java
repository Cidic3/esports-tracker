package dev.mundorf.esportstracker.model.entity;

import java.util.Set;

public enum TournamentTier {
    INTERNATIONAL,
    PRIMARY,
    SECONDARY;

    private static final String REGION_INTERNATIONAL = "INTERNATIONAL";
    private static final Set<String> PRIMARY_LEAGUE_SLUGS = Set.of("lec", "lck", "lpl", "lcs");

    /**
     * Tier classification for a league, by its region and slug. Riot models international
     * events (Worlds/MSI) as leagues with region "INTERNATIONAL"; the primary regional
     * leagues are a known, stable set; everything else is a national/secondary league.
     * Shared by tournament sync (which stores the tier) and the league API (which derives
     * it at read time) so the two can never disagree.
     */
    public static TournamentTier forLeague(String region, String leagueSlug) {
        if (REGION_INTERNATIONAL.equalsIgnoreCase(region)) {
            return INTERNATIONAL;
        }
        if (PRIMARY_LEAGUE_SLUGS.contains(leagueSlug)) {
            return PRIMARY;
        }
        return SECONDARY;
    }
}
