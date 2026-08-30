package com.ma3auka.alaaggro.core;

/**
 * The one place that decides whether a mob may be made aggressive.
 *
 * <p>Before this class existed the same chain of checks was written out by hand in the join
 * handler and again in the per-second scan, while {@code /alaaggro reload} skipped it altogether
 * and re-aggroed everything except bosses — so a villager, a blacklisted mob or a mob in a
 * blacklisted dimension came back hostile after a reload, contradicting the config. Now all three
 * callers ask this method, and a new rule cannot be added to one path and forgotten in another.
 *
 * <p>Pure: no Minecraft types, no static state, no side effects. Facts in, verdict out.
 */
public final class AggroEligibility {

    private AggroEligibility() {}

    /**
     * Checks are ordered cheapest-first, and the order is also the priority of the reason reported
     * back: a boss in a blacklisted dimension reads as {@link AggroVerdict#BOSS}, because that is
     * the rule that would keep protecting it if the dimension list changed.
     */
    public static AggroVerdict evaluate(MobFacts mob, AggroSettings settings) {
        if (!settings.enabled()) return AggroVerdict.MOD_DISABLED;
        if (mob.boss()) return AggroVerdict.BOSS;
        if (!mob.pathfinder()) return AggroVerdict.NO_WALKING_AI;
        if (mob.villager() && !settings.hostileVillagers()) return AggroVerdict.VILLAGER;
        if (settings.dimensionBlacklist().contains(mob.dimensionId())) return AggroVerdict.DIMENSION_BLACKLISTED;
        if (mob.tagExcluded()) return AggroVerdict.TAG_EXCLUDED;
        if (settings.entityBlacklist().contains(mob.entityId())) return AggroVerdict.BLACKLISTED;
        if (settings.whitelistActive() && !settings.entityWhitelist().contains(mob.entityId())) {
            return AggroVerdict.NOT_WHITELISTED;
        }
        if (mob.tamed() && settings.excludeTamed()) return AggroVerdict.TAMED;
        if (mob.baby() && settings.excludeBabies()) return AggroVerdict.BABY;
        if (mob.named() && settings.excludeNamed()) return AggroVerdict.NAMED;
        return AggroVerdict.ALLOW;
    }

    /** Convenience for callers that only need yes/no. */
    public static boolean isEligible(MobFacts mob, AggroSettings settings) {
        return evaluate(mob, settings).allowed();
    }
}
