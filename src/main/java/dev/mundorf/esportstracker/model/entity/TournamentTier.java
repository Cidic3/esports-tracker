package dev.mundorf.esportstracker.model.entity;

import java.util.Set;

public enum TournamentTier {
    INTERNATIONAL,
    PRIMARY,
    SECONDARY;

    private static final String REGION_INTERNATIONAL = "INTERNATIONAL";
    // LoL's four major regional leagues plus the four ALGS Pro League regions - ALGS regional
    // Pro League is Apex's top level of competition, the same standing LEC/LCK have in LoL.
    private static final Set<String> PRIMARY_LEAGUE_SLUGS = Set.of(
            "lec", "lck", "lpl", "lcs",
            "algs-americas", "algs-emea", "algs-apac-north", "algs-apac-south");

    /**
     * Tier classification for a league, by its region and slug. Riot models international
     * events (Worlds/MSI) as leagues with region "INTERNATIONAL"; the primary regional
     * leagues are a known, stable set; everything else is a national/secondary league.
     * ALGS follows the same shape: regional Pro Leagues are PRIMARY, cross-region events
     * (Playoffs/EWC, region "Global" mapped to INTERNATIONAL) are INTERNATIONAL.
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
