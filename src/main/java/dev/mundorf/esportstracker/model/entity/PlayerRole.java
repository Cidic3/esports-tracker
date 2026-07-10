package dev.mundorf.esportstracker.model.entity;

import java.util.Locale;

/**
 * Declaration order doubles as display order (top -> jungle -> mid -> bottom -> support), the
 * conventional way a LoL roster is presented. NONE covers substitutes/analysts and anything Riot's
 * getTeams endpoint doesn't tag with one of the five lane roles.
 */
public enum PlayerRole {
    TOP,
    JUNGLE,
    MID,
    BOTTOM,
    SUPPORT,
    NONE;

    public static PlayerRole fromRiot(String riotRole) {
        if (riotRole == null) {
            return NONE;
        }
        try {
            return valueOf(riotRole.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }
}
