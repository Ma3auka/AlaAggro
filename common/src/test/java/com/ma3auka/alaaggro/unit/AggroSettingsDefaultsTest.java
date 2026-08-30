package com.ma3auka.alaaggro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import com.ma3auka.alaaggro.core.AggroConfig;
import com.ma3auka.alaaggro.core.AggroSettings;
import com.ma3auka.alaaggro.core.ConfigOption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Why this matters: these are the values every handler sees before a config file has been read —
 * during mod construction, and any time a loader reports its config as not yet loaded. They are
 * also the values a fresh install writes to disk, so a wrong default here is what a new player
 * actually plays with.
 *
 * <p>The failure mode this catches: someone adds a config option, wires it into one loader's
 * backend, and the shared defaults quietly disagree with the declared ones — the mod then behaves
 * one way for the first second of every server start and another way afterwards. Pinning every
 * value makes that a build failure with the changed field named in the diff.
 */
class AggroSettingsDefaultsTest {

    @Test
    @DisplayName("every default is pinned")
    void defaults_pinAllFields() {
        AggroSettings defaults = AggroSettings.defaults();

        assertTrue(defaults.enabled(), "the mod ships switched on");
        assertEquals(false, defaults.hostileVillagers());
        assertEquals(false, defaults.reactiveOnly());
        assertEquals(Set.of(), defaults.dimensionBlacklist());
        assertTrue(defaults.excludeTamed(), "a player's own pet must not turn on them out of the box");
        assertEquals(false, defaults.excludeBabies());
        assertEquals(false, defaults.excludeNamed());
        assertTrue(defaults.persistExempt());

        assertEquals(1.0, defaults.damageMultiplier());
        assertEquals(1.0, defaults.speedMultiplier());
        assertEquals(0.5, defaults.perCategorySpeedCap());
        assertEquals(2.0, defaults.defaultAttackDamage());
        assertEquals(0.30, defaults.minMovementSpeed());
        assertEquals(32.0, defaults.followRange());

        assertTrue(defaults.callForHelp());
        assertEquals(16, defaults.callForHelpRadius());

        assertTrue(defaults.longTermMemory());

        assertEquals(20, defaults.scanIntervalTicks(), "once per second");
        assertEquals(32.0, defaults.scanRadius());

        assertEquals(Set.of(), defaults.entityBlacklist());
        assertEquals(Set.of(), defaults.entityWhitelist());
    }

    @Test
    @DisplayName("the live settings are never null, even before a config exists")
    void get_neverNullBeforeLoad() {
        AggroConfig.resetForTesting();
        assertNotNull(AggroConfig.get(), "handlers read this on the hot path and never null-check");
        assertEquals(AggroSettings.defaults(), AggroConfig.get());
    }

    @Test
    @DisplayName("settings compare by value")
    void settings_areAValueType() {
        // Two independently built snapshots with the same contents must be equal, otherwise
        // "did the config actually change?" checks silently answer yes every time.
        assertEquals(AggroSettings.defaults(), AggroSettings.defaults());
        assertEquals(AggroSettings.defaults().hashCode(), AggroSettings.defaults().hashCode());
    }

    @Test
    @DisplayName("reloading bumps the generation counter")
    void reload_bumpsGeneration() {
        // Injected mobs carry the generation they were built under, and the periodic scan rebuilds
        // the ones that are behind. If the counter stopped moving, a config change would never
        // reach mobs already in the world — the symptom being "reload does nothing".
        AggroConfig.resetForTesting();
        int before = AggroConfig.generation();
        AggroConfig.reload();
        assertTrue(AggroConfig.generation() > before, "a reload must invalidate already-injected mobs");
    }

    @Test
    @DisplayName("every declared option is unique and lands in a settings field")
    void options_areDeclaredConsistently() {
        // ConfigOption.ALL feeds both loaders' storage and the order of ConfigOption.read().
        // A duplicate path would make one option silently shadow another in the Fabric JSON file.
        Set<String> paths = new HashSet<>();
        for (ConfigOption option : ConfigOption.ALL) {
            assertTrue(paths.add(option.path()), "duplicate config path: " + option.path());
            assertNotNull(option.comment, option.path() + " needs a comment for the config file");
            assertNotNull(option.defaultValue, option.path() + " needs a default");
        }
        assertEquals(AggroSettings.class.getRecordComponents().length, ConfigOption.ALL.size(),
                "each declared option must map to exactly one settings field, and vice versa");
    }

    @Test
    @DisplayName("out-of-range values from a hand-edited file are clamped")
    void read_clampsOutOfRangeValues() {
        // Both loaders read through ConfigOption.read, so a player typing damageMultiplier = 999
        // into the file gets the same, survivable behaviour on Fabric and NeoForge.
        AggroSettings absurd = ConfigOption.read(new ConfigOption.Source() {
            @Override
            public boolean bool(ConfigOption option) {
                return (Boolean) option.defaultValue;
            }

            @Override
            public int integer(ConfigOption option) {
                return 100000;
            }

            @Override
            public double number(ConfigOption option) {
                return 100000.0;
            }

            @Override
            public Set<String> strings(ConfigOption option) {
                return Set.of();
            }
        });

        assertEquals(ConfigOption.DAMAGE_MULTIPLIER.max, absurd.damageMultiplier());
        assertEquals(ConfigOption.SPEED_MULTIPLIER.max, absurd.speedMultiplier());
        assertEquals((int) ConfigOption.CALL_FOR_HELP_RADIUS.max, absurd.callForHelpRadius());
        assertEquals((int) ConfigOption.SCAN_INTERVAL_TICKS.max, absurd.scanIntervalTicks());
    }

    @Test
    @DisplayName("defaults() returns the same values as the declared option defaults")
    void defaults_matchDeclaredOptions() {
        assertSame(Boolean.class, ConfigOption.ENABLED.defaultValue.getClass());
        assertEquals(ConfigOption.ENABLED.defaultValue, AggroSettings.defaults().enabled());
        assertEquals(ConfigOption.CALL_FOR_HELP_RADIUS.defaultValue, AggroSettings.defaults().callForHelpRadius());
        assertEquals(ConfigOption.DEFAULT_ATTACK_DAMAGE.defaultValue, AggroSettings.defaults().defaultAttackDamage());
    }
}
