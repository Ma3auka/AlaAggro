package com.ma3auka.alaaggro.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ma3auka.alaaggro.config.AlaAggroConfig;

/**
 * Volatile snapshot of server config values, refreshed on config load/reload.
 * Hot-path event handlers read from here instead of touching ModConfigSpec on every tick.
 */
public final class AggroConfigCache {
    private static volatile Snapshot CURRENT = Snapshot.defaults();

    private AggroConfigCache() {}

    public static Snapshot get() {
        return CURRENT;
    }

    public static void rebuild() {
        if (!AlaAggroConfig.SPEC.isLoaded()) {
            CURRENT = Snapshot.defaults();
            return;
        }
        CURRENT = new Snapshot(
                AlaAggroConfig.ENABLED.get(),
                AlaAggroConfig.HOSTILE_VILLAGERS.get(),
                AlaAggroConfig.REACTIVE_ONLY.get(),
                toStringSet(AlaAggroConfig.DIMENSION_BLACKLIST.get()),
                AlaAggroConfig.DAMAGE_MULTIPLIER.get(),
                AlaAggroConfig.SPEED_MULTIPLIER.get(),
                AlaAggroConfig.PER_CATEGORY_SPEED_CAP.get(),
                AlaAggroConfig.DEFAULT_ATTACK_DAMAGE.get(),
                AlaAggroConfig.ADD_MELEE_GOAL_TO_PASSIVE.get(),
                AlaAggroConfig.REMOVE_PANIC_GOAL.get(),
                AlaAggroConfig.CALL_FOR_HELP.get(),
                AlaAggroConfig.CALL_FOR_HELP_RADIUS.get(),
                AlaAggroConfig.LONG_TERM_MEMORY.get(),
                toStringSet(AlaAggroConfig.ENTITY_BLACKLIST.get()),
                toStringSet(AlaAggroConfig.ENTITY_WHITELIST.get())
        );
    }

    private static Set<String> toStringSet(List<? extends String> list) {
        Set<String> out = new HashSet<>(list.size());
        for (Object o : list) out.add(String.valueOf(o));
        return out;
    }

    public record Snapshot(
            boolean enabled,
            boolean hostileVillagers,
            boolean reactiveOnly,
            Set<String> dimensionBlacklist,
            double damageMultiplier,
            double speedMultiplier,
            double perCategorySpeedCap,
            double defaultAttackDamage,
            boolean addMeleeGoalToPassive,
            boolean removePanicGoal,
            boolean callForHelp,
            int callForHelpRadius,
            boolean longTermMemory,
            Set<String> entityBlacklist,
            Set<String> entityWhitelist
    ) {
        static Snapshot defaults() {
            return new Snapshot(true, false, false, Set.of(),
                    1.0, 1.0, 0.5, 2.0, true, true,
                    true, 16, true, Set.of(), Set.of());
        }
    }
}
