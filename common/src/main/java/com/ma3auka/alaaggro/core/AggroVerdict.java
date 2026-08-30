package com.ma3auka.alaaggro.core;

/**
 * Outcome of the eligibility rules: either the mob is ours to make aggressive, or the exact reason
 * it was left alone.
 *
 * <p>Naming the reason rather than returning a bare {@code false} pays off twice: the debug log says
 * <em>why</em> a mob stayed calm (the most common support question), and the unit tests assert on a
 * specific rule instead of on "somehow rejected", so a filter that rejects for the wrong reason
 * still fails the build.
 */
public enum AggroVerdict {
    /** Make this mob aggressive. */
    ALLOW,
    /** Master switch is off. */
    MOD_DISABLED,
    /** Wither, Ender Dragon, Elder Guardian, or a mob tagged as a boss by another mod. */
    BOSS,
    /**
     * No ordinary walking AI (slimes, ghasts, phantoms …). We would wipe their brain and have
     * nothing to rebuild it with, so we never touch them.
     */
    NO_WALKING_AI,
    /** A villager, and hostile villagers are switched off. */
    VILLAGER,
    /** The mob's dimension is on the blacklist. */
    DIMENSION_BLACKLISTED,
    /** Carries the {@code alaaggro:excluded} tag (datapacks and our built-in cube-mob list). */
    TAG_EXCLUDED,
    /** Entity id is on the config blacklist. */
    BLACKLISTED,
    /** A whitelist is active and this entity id is not on it. */
    NOT_WHITELISTED,
    /** A tamed pet, and pets are protected. */
    TAMED,
    /** A baby animal, and babies are protected. */
    BABY,
    /** Carries a name tag, and named mobs are protected. */
    NAMED;

    public boolean allowed() {
        return this == ALLOW;
    }
}
