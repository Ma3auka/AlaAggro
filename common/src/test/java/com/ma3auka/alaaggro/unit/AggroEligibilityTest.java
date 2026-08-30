package com.ma3auka.alaaggro.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import com.ma3auka.alaaggro.core.AggroEligibility;
import com.ma3auka.alaaggro.core.AggroSettings;
import com.ma3auka.alaaggro.core.AggroVerdict;
import com.ma3auka.alaaggro.core.ConfigOption;
import com.ma3auka.alaaggro.core.MobFacts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Why this matters: these rules decide, for every mob that spawns, whether the mod rewrites its
 * brain. A wrong answer is either a mod that does nothing or a mod that wrecks something it was
 * told to leave alone — a player's tamed wolf, a modpack's boss, a blacklisted mob.
 *
 * <p>The bug that motivated pulling this out of the event handlers: the checks were written by hand
 * in the join handler and again in the periodic scan, and {@code /alaaggro reload} had no checks at
 * all beyond a boss guard. A villager left alone at spawn came back hostile after any reload, and so
 * did every blacklisted mob. Rules in one testable place is what makes that class of drift
 * impossible; these tests pin the rules themselves.
 */
class AggroEligibilityTest {

    private static AggroSettings defaults() {
        return AggroSettings.defaults();
    }

    /** Ordinary cow in the overworld, nothing special about it. */
    private static MobFacts cow() {
        return new MobFacts("minecraft:cow", "minecraft:overworld",
                false, false, false, true, false, false, false);
    }

    @Test
    @DisplayName("a plain mob is fair game")
    void plainMobIsEligible() {
        assertEquals(AggroVerdict.ALLOW, AggroEligibility.evaluate(cow(), defaults()));
    }

    @Test
    @DisplayName("master switch off means nothing is touched")
    void disabledModTouchesNothing() {
        AggroSettings off = ConfigOption.read(new DefaultsExcept(ConfigOption.ENABLED, false));
        assertEquals(AggroVerdict.MOD_DISABLED, AggroEligibility.evaluate(cow(), off));
    }

    @Test
    @DisplayName("bosses are never touched")
    void bossIsProtected() {
        MobFacts boss = new MobFacts("minecraft:wither", "minecraft:overworld",
                true, false, false, true, false, false, false);
        assertEquals(AggroVerdict.BOSS, AggroEligibility.evaluate(boss, defaults()));
    }

    @Test
    @DisplayName("a mob without walking AI keeps its own brain")
    void mobWithoutWalkingAiIsLeftAlone() {
        // Ghasts, phantoms and (on older versions) slimes. We wipe goals before rebuilding, and for
        // these there is nothing to rebuild with — so the wipe would leave them mindless, and the
        // periodic scan would repeat it every second because they never get our attack goal.
        MobFacts ghast = new MobFacts("minecraft:ghast", "minecraft:the_nether",
                false, false, false, false, false, false, false);
        assertEquals(AggroVerdict.NO_WALKING_AI, AggroEligibility.evaluate(ghast, defaults()));
    }

    @Test
    @DisplayName("boss check wins over the missing-AI check")
    void bossOutranksWalkingAi() {
        MobFacts dragon = new MobFacts("minecraft:ender_dragon", "minecraft:the_end",
                true, false, false, false, false, false, false);
        assertEquals(AggroVerdict.BOSS, AggroEligibility.evaluate(dragon, defaults()));
    }

    @Test
    @DisplayName("villagers follow their own switch")
    void villagersFollowTheirOwnSwitch() {
        MobFacts villager = new MobFacts("minecraft:villager", "minecraft:overworld",
                false, true, false, true, false, false, false);
        assertEquals(AggroVerdict.VILLAGER, AggroEligibility.evaluate(villager, defaults()));

        AggroSettings hostile = ConfigOption.read(new DefaultsExcept(ConfigOption.HOSTILE_VILLAGERS, true));
        assertEquals(AggroVerdict.ALLOW, AggroEligibility.evaluate(villager, hostile));
    }

    @Test
    @DisplayName("blacklisted dimensions are skipped")
    void blacklistedDimensionIsSkipped() {
        MobFacts inNether = new MobFacts("minecraft:cow", "minecraft:the_nether",
                false, false, false, true, false, false, false);
        AggroSettings settings = withDimensionBlacklist(Set.of("minecraft:the_nether"));
        assertEquals(AggroVerdict.DIMENSION_BLACKLISTED, AggroEligibility.evaluate(inNether, settings));
        assertEquals(AggroVerdict.ALLOW, AggroEligibility.evaluate(cow(), settings));
    }

    @Test
    @DisplayName("the excluded tag is honoured")
    void taggedMobIsSkipped() {
        MobFacts tagged = new MobFacts("minecraft:slime", "minecraft:overworld",
                false, false, true, true, false, false, false);
        assertEquals(AggroVerdict.TAG_EXCLUDED, AggroEligibility.evaluate(tagged, defaults()));
    }

    @Test
    @DisplayName("blacklist skips, whitelist restricts")
    void listsBehaveAsDocumented() {
        AggroSettings blacklisted = withLists(Set.of("minecraft:cow"), Set.of());
        assertEquals(AggroVerdict.BLACKLISTED, AggroEligibility.evaluate(cow(), blacklisted));

        AggroSettings whitelistWithoutCow = withLists(Set.of(), Set.of("minecraft:sheep"));
        assertEquals(AggroVerdict.NOT_WHITELISTED, AggroEligibility.evaluate(cow(), whitelistWithoutCow));

        AggroSettings whitelistWithCow = withLists(Set.of(), Set.of("minecraft:cow"));
        assertEquals(AggroVerdict.ALLOW, AggroEligibility.evaluate(cow(), whitelistWithCow));
    }

    @Test
    @DisplayName("an entry on both lists stays blocked")
    void blacklistWinsOverWhitelist() {
        // A deliberate rule, pinned because it is the kind of thing a refactor flips by accident:
        // an explicit "never touch this" outranks a whitelist that happens to include it. Safer to
        // leave a mob alone than to make one hostile that the operator had listed as off-limits.
        AggroSettings both = withLists(Set.of("minecraft:cow"), Set.of("minecraft:cow"));
        assertEquals(AggroVerdict.BLACKLISTED, AggroEligibility.evaluate(cow(), both));
    }

    @ParameterizedTest(name = "tamed={0} baby={1} named={2} with protection on -> {3}")
    @CsvSource({
            "true,  false, false, TAMED",
            "false, true,  false, BABY",
            "false, false, true,  NAMED",
            "false, false, false, ALLOW"
    })
    @DisplayName("pet protections apply only to the matching trait")
    void petProtections(boolean tamed, boolean baby, boolean named, String expected) {
        MobFacts mob = new MobFacts("minecraft:wolf", "minecraft:overworld",
                false, false, false, true, tamed, baby, named);
        AggroSettings allProtections = protective();
        assertEquals(AggroVerdict.valueOf(expected), AggroEligibility.evaluate(mob, allProtections));
    }

    @Test
    @DisplayName("pet protections can be switched off")
    void petProtectionsAreOptional() {
        MobFacts tamedWolf = new MobFacts("minecraft:wolf", "minecraft:overworld",
                false, false, false, true, true, false, false);
        AggroSettings noProtection = ConfigOption.read(new DefaultsExcept(ConfigOption.EXCLUDE_TAMED, false));
        assertEquals(AggroVerdict.ALLOW, AggroEligibility.evaluate(tamedWolf, noProtection));
    }

    // ------------------------------------------------------------- fixtures

    /** Everything at its declared default except one option. */
    private record DefaultsExcept(ConfigOption option, Object value) implements ConfigOption.Source {
        @Override
        public boolean bool(ConfigOption o) {
            return o == option ? (Boolean) value : (Boolean) o.defaultValue;
        }

        @Override
        public int integer(ConfigOption o) {
            return o == option ? (Integer) value : (Integer) o.defaultValue;
        }

        @Override
        public double number(ConfigOption o) {
            return o == option ? (Double) value : (Double) o.defaultValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<String> strings(ConfigOption o) {
            return o == option ? (Set<String>) value : Set.of();
        }
    }

    private static AggroSettings protective() {
        return ConfigOption.read(new ConfigOption.Source() {
            @Override
            public boolean bool(ConfigOption o) {
                if (o == ConfigOption.EXCLUDE_TAMED || o == ConfigOption.EXCLUDE_BABIES
                        || o == ConfigOption.EXCLUDE_NAMED) {
                    return true;
                }
                return (Boolean) o.defaultValue;
            }

            @Override
            public int integer(ConfigOption o) {
                return (Integer) o.defaultValue;
            }

            @Override
            public double number(ConfigOption o) {
                return (Double) o.defaultValue;
            }

            @Override
            public Set<String> strings(ConfigOption o) {
                return Set.of();
            }
        });
    }

    private static AggroSettings withDimensionBlacklist(Set<String> dimensions) {
        return ConfigOption.read(new DefaultsExcept(ConfigOption.DIMENSION_BLACKLIST, dimensions));
    }

    private static AggroSettings withLists(Set<String> blacklist, Set<String> whitelist) {
        return ConfigOption.read(new ConfigOption.Source() {
            @Override
            public boolean bool(ConfigOption o) {
                return (Boolean) o.defaultValue;
            }

            @Override
            public int integer(ConfigOption o) {
                return (Integer) o.defaultValue;
            }

            @Override
            public double number(ConfigOption o) {
                return (Double) o.defaultValue;
            }

            @Override
            public Set<String> strings(ConfigOption o) {
                if (o == ConfigOption.ENTITY_BLACKLIST) return blacklist;
                if (o == ConfigOption.ENTITY_WHITELIST) return whitelist;
                return Set.of();
            }
        });
    }
}
