package com.ma3auka.alaaggro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ma3auka.alaaggro.util.AggroConfigCache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the fallback Snapshot used before the config TOML is loaded.
 *
 * Why this matters:
 *   AggroConfigCache.Snapshot.defaults() is what every event handler sees
 *   between mod construction and config-load (and also when SPEC.isLoaded()
 *   returns false). If a developer adds a new field to the snapshot record
 *   but forgets to wire a sensible default, mobs can spawn with multiplier=0,
 *   nobody can hurt them, and the bug is invisible until a player rage-quits.
 *   These assertions are deliberately exhaustive: every defaulted value is
 *   pinned so that any drift triggers a build failure with a clear diff.
 */
final class AggroSnapshotDefaultsTest {

    @Test
    @DisplayName("defaults() — all fields match documented config defaults")
    void defaults_pinAllFields() {
        AggroConfigCache.Snapshot d = AggroConfigCache.Snapshot.class
                .cast(invokeDefaults());

        // [general]
        assertEquals(true,  d.enabled(),           "enabled");
        assertEquals(false, d.hostileVillagers(),  "hostileVillagers");
        assertEquals(false, d.reactiveOnly(),      "reactiveOnly");
        assertTrue(d.dimensionBlacklist().isEmpty(), "dimensionBlacklist must default empty");

        // [aggression]
        assertEquals(1.0, d.damageMultiplier(),    "damageMultiplier");
        assertEquals(1.0, d.speedMultiplier(),     "speedMultiplier");
        assertEquals(0.5, d.perCategorySpeedCap(), "perCategorySpeedCap");
        assertEquals(2.0, d.defaultAttackDamage(), "defaultAttackDamage");
        assertEquals(true, d.addMeleeGoalToPassive(), "addMeleeGoalToPassive");
        assertEquals(true, d.removePanicGoal(),    "removePanicGoal");

        // [callForHelp]
        assertEquals(true, d.callForHelp(),        "callForHelp");
        assertEquals(16,   d.callForHelpRadius(),  "callForHelpRadius");

        // [memory]
        assertEquals(true, d.longTermMemory(),     "longTermMemory");

        // [lists]
        assertTrue(d.entityBlacklist().isEmpty(), "entityBlacklist must default empty");
        assertTrue(d.entityWhitelist().isEmpty(), "entityWhitelist must default empty");
    }

    @Test
    @DisplayName("get() returns a non-null snapshot before config loads")
    void get_neverNullEvenBeforeLoad() {
        // Hot-path handlers call AggroConfigCache.get() unconditionally. A null
        // here would NPE every server tick — this guards against accidental
        // initialisation regressions.
        AggroConfigCache.Snapshot s = AggroConfigCache.get();
        assertEquals(false, s == null);
    }

    @Test
    @DisplayName("Snapshot is a value record — equals/hashCode by content")
    void snapshot_isValueType() {
        // Records give us free equals/hashCode by all components. If anyone
        // ever rewrites Snapshot as a class without overriding equals, this
        // test fails — and so does the cache invalidation logic that compares
        // snapshots in future revisions.
        AggroConfigCache.Snapshot a = AggroConfigCache.Snapshot.class.cast(invokeDefaults());
        AggroConfigCache.Snapshot b = AggroConfigCache.Snapshot.class.cast(invokeDefaults());
        assertEquals(a, b, "two defaults() calls must be equal by value");
        assertEquals(a.hashCode(), b.hashCode());
        assertNotSame(a, b, "but they must be distinct instances");
    }

    /**
     * Snapshot.defaults() is package-private. Tests live in a sibling package
     * (com.ma3auka.alaaggro.unit) so we reach it via reflection. This keeps
     * the production API surface clean without resorting to @VisibleForTesting.
     */
    private static Object invokeDefaults() {
        try {
            var m = AggroConfigCache.Snapshot.class.getDeclaredMethod("defaults");
            m.setAccessible(true);
            return m.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Snapshot.defaults() must remain available to tests", e);
        }
    }
}
