package com.ma3auka.alaaggro.core;

import java.util.Set;

/**
 * Immutable value of every setting the mod reads at runtime.
 *
 * <p>This is the only shape the rest of the mod knows about. Each loader stores the values its own
 * way — NeoForge keeps the existing {@code alaaggro-server.toml} through {@code ModConfigSpec},
 * Fabric writes a JSON file — but both hand the mod one of these records, so no handler, goal or
 * command ever talks to a loader-specific config API.
 *
 * <p>Read the live value through {@link AggroConfig#get()}; never cache a reference to a snapshot
 * across ticks, because a reload replaces it wholesale.
 */
public record AggroSettings(
        // [general]
        boolean enabled,
        boolean hostileVillagers,
        boolean reactiveOnly,
        Set<String> dimensionBlacklist,
        boolean excludeTamed,
        boolean excludeBabies,
        boolean excludeNamed,
        boolean persistExempt,

        // [aggression]
        double damageMultiplier,
        double speedMultiplier,
        double perCategorySpeedCap,
        double defaultAttackDamage,
        double minMovementSpeed,
        double followRange,

        // [callForHelp]
        boolean callForHelp,
        int callForHelpRadius,

        // [memory]
        boolean longTermMemory,

        // [performance]
        int scanIntervalTicks,
        double scanRadius,

        // [lists]
        Set<String> entityBlacklist,
        Set<String> entityWhitelist
) {

    /**
     * Values used before any config file has been read — during mod construction, and whenever a
     * loader reports its config as not loaded yet. Must stay in step with the defaults declared in
     * {@link ConfigOption}; {@code AggroSettingsDefaultsTest} fails the build if the two drift.
     */
    public static AggroSettings defaults() {
        return ConfigOption.defaults();
    }

    /** Whitelist mode: when the whitelist has entries, only those entity ids are touched. */
    public boolean whitelistActive() {
        return !entityWhitelist.isEmpty();
    }
}
