package com.ma3auka.alaaggro.core;

import java.util.List;
import java.util.Set;

/**
 * The single declaration of every config entry: its section, key, type, default and valid range.
 *
 * <p>Both loaders build their storage from this list — the NeoForge backend turns it into a
 * {@code ModConfigSpec} so the TOML file and the in-game Settings screen keep working, the Fabric
 * backend reads and writes the same keys in JSON. Adding an option therefore means editing this
 * file and {@link AggroSettings}, and both loaders pick it up; there is no third place to forget.
 *
 * <p>Ranges live here rather than in the backends so that a value typed straight into the file by
 * hand is clamped identically on both loaders.
 */
public final class ConfigOption {

    public enum Kind { BOOL, INT, DOUBLE, STRING_LIST }

    // ---------------------------------------------------------------- [general]

    public static final ConfigOption ENABLED = bool("general", "enabled", true,
            "Enable the AlaAggro mod globally");
    public static final ConfigOption HOSTILE_VILLAGERS = bool("general", "hostileVillagers", false,
            "Villagers attack the player");
    public static final ConfigOption REACTIVE_ONLY = bool("general", "reactiveOnly", false,
            "Mobs stay calm and only fight back after the player hits them first");
    public static final ConfigOption DIMENSION_BLACKLIST = stringList("general", "dimensionBlacklist",
            "Dimensions where mobs are left alone (empty = everywhere), e.g. [\"minecraft:the_nether\"]",
            "minecraft:the_nether");
    public static final ConfigOption EXCLUDE_TAMED = bool("general", "excludeTamed", true,
            "Leave tamed pets alone (your own wolf, cat, parrot, horse ...)");
    public static final ConfigOption EXCLUDE_BABIES = bool("general", "excludeBabies", false,
            "Leave baby animals alone");
    public static final ConfigOption EXCLUDE_NAMED = bool("general", "excludeNamed", false,
            "Leave mobs carrying a name tag alone");
    public static final ConfigOption PERSIST_EXEMPT = bool("general", "persistExempt", true,
            "Keep the /alaaggro exempt list across server restarts (saved with the world)");

    // ------------------------------------------------------------- [aggression]

    public static final ConfigOption DAMAGE_MULTIPLIER = number("aggression", "damageMultiplier", 1.0, 0.1, 10.0,
            "Damage multiplier (1.0 = vanilla)");
    public static final ConfigOption SPEED_MULTIPLIER = number("aggression", "speedMultiplier", 1.0, 0.1, 3.0,
            "Speed multiplier (1.0 = vanilla, max 3.0)");
    public static final ConfigOption PER_CATEGORY_SPEED_CAP = number("aggression", "perCategorySpeedCap", 0.5, 0.05, 2.0,
            "Absolute speed cap after the multiplier (blocks/tick)");
    public static final ConfigOption DEFAULT_ATTACK_DAMAGE = number("aggression", "defaultAttackDamage", 2.0, 0.0, 100.0,
            "Attack damage used by mobs that have no attack_damage attribute of their own");
    public static final ConfigOption MIN_MOVEMENT_SPEED = number("aggression", "minMovementSpeed", 0.30, 0.0, 2.0,
            "Movement-speed floor, so slow animals can still catch up with a walking player");
    public static final ConfigOption FOLLOW_RANGE = number("aggression", "followRange", 32.0, 8.0, 128.0,
            "Follow range given to aggroed mobs (how far they can notice the player)");

    // ------------------------------------------------------------ [callForHelp]

    public static final ConfigOption CALL_FOR_HELP = bool("callForHelp", "enabled", true,
            "Hurting a mob alerts nearby mobs of the same type");
    public static final ConfigOption CALL_FOR_HELP_RADIUS = integer("callForHelp", "radius", 16, 1, 128,
            "Notification radius in blocks");

    // ----------------------------------------------------------------- [memory]

    public static final ConfigOption LONG_TERM_MEMORY = bool("memory", "longTermMemory", true,
            "Mobs keep chasing a player they lost sight of, instead of forgetting after a few seconds");

    // ------------------------------------------------------------ [performance]

    public static final ConfigOption SCAN_INTERVAL_TICKS = integer("performance", "scanIntervalTicks", 20, 1, 200,
            "How often the safety-net scan runs, in ticks (20 = once per second). Higher = cheaper, less responsive");
    public static final ConfigOption SCAN_RADIUS = number("performance", "scanRadius", 32.0, 8.0, 128.0,
            "Radius around each player scanned for mobs that need aggro, in blocks");

    // ------------------------------------------------------------------ [lists]

    public static final ConfigOption ENTITY_BLACKLIST = stringList("lists", "entityBlacklist",
            "Entity ids that are always left alone, e.g. [\"minecraft:bat\"]",
            "minecraft:bat");
    public static final ConfigOption ENTITY_WHITELIST = stringList("lists", "entityWhitelist",
            "If non-empty, ONLY these entity ids are made aggressive",
            "minecraft:cow");

    /** Every option, in file order. Backends iterate this to build their storage. */
    public static final List<ConfigOption> ALL = List.of(
            ENABLED, HOSTILE_VILLAGERS, REACTIVE_ONLY, DIMENSION_BLACKLIST,
            EXCLUDE_TAMED, EXCLUDE_BABIES, EXCLUDE_NAMED, PERSIST_EXEMPT,
            DAMAGE_MULTIPLIER, SPEED_MULTIPLIER, PER_CATEGORY_SPEED_CAP, DEFAULT_ATTACK_DAMAGE,
            MIN_MOVEMENT_SPEED, FOLLOW_RANGE,
            CALL_FOR_HELP, CALL_FOR_HELP_RADIUS,
            LONG_TERM_MEMORY,
            SCAN_INTERVAL_TICKS, SCAN_RADIUS,
            ENTITY_BLACKLIST, ENTITY_WHITELIST);

    /**
     * How a backend hands values back. Implementations return the stored value; clamping to the
     * declared range is done here so a hand-edited file behaves the same on both loaders.
     */
    public interface Source {
        boolean bool(ConfigOption option);

        int integer(ConfigOption option);

        double number(ConfigOption option);

        Set<String> strings(ConfigOption option);
    }

    /** Assembles a settings record from a backend. The one place field order is decided. */
    public static AggroSettings read(Source src) {
        return new AggroSettings(
                src.bool(ENABLED),
                src.bool(HOSTILE_VILLAGERS),
                src.bool(REACTIVE_ONLY),
                src.strings(DIMENSION_BLACKLIST),
                src.bool(EXCLUDE_TAMED),
                src.bool(EXCLUDE_BABIES),
                src.bool(EXCLUDE_NAMED),
                src.bool(PERSIST_EXEMPT),
                clamp(src.number(DAMAGE_MULTIPLIER), DAMAGE_MULTIPLIER),
                clamp(src.number(SPEED_MULTIPLIER), SPEED_MULTIPLIER),
                clamp(src.number(PER_CATEGORY_SPEED_CAP), PER_CATEGORY_SPEED_CAP),
                clamp(src.number(DEFAULT_ATTACK_DAMAGE), DEFAULT_ATTACK_DAMAGE),
                clamp(src.number(MIN_MOVEMENT_SPEED), MIN_MOVEMENT_SPEED),
                clamp(src.number(FOLLOW_RANGE), FOLLOW_RANGE),
                src.bool(CALL_FOR_HELP),
                (int) clamp(src.integer(CALL_FOR_HELP_RADIUS), CALL_FOR_HELP_RADIUS),
                src.bool(LONG_TERM_MEMORY),
                (int) clamp(src.integer(SCAN_INTERVAL_TICKS), SCAN_INTERVAL_TICKS),
                clamp(src.number(SCAN_RADIUS), SCAN_RADIUS),
                src.strings(ENTITY_BLACKLIST),
                src.strings(ENTITY_WHITELIST));
    }

    /** Settings built purely from the declared defaults — used before any file is read. */
    public static AggroSettings defaults() {
        return read(DEFAULTS);
    }

    private static final Source DEFAULTS = new Source() {
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
            return Set.of();
        }
    };

    private static double clamp(double value, ConfigOption o) {
        if (value < o.min) return o.min;
        if (value > o.max) return o.max;
        return value;
    }

    // ---------------------------------------------------------------- instance

    public final String section;
    public final String key;
    public final Kind kind;
    public final Object defaultValue;
    public final double min;
    public final double max;
    public final String comment;
    /** Sample entry a list option shows in the generated file, so the format is obvious. */
    public final String listExample;

    private ConfigOption(String section, String key, Kind kind, Object defaultValue,
                         double min, double max, String comment, String listExample) {
        this.section = section;
        this.key = key;
        this.kind = kind;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.comment = comment;
        this.listExample = listExample;
    }

    /** {@code general.enabled} — the flat path used as the JSON key on Fabric. */
    public String path() {
        return section + "." + key;
    }

    private static ConfigOption bool(String section, String key, boolean def, String comment) {
        return new ConfigOption(section, key, Kind.BOOL, def, 0, 0, comment, null);
    }

    private static ConfigOption integer(String section, String key, int def, int min, int max, String comment) {
        return new ConfigOption(section, key, Kind.INT, def, min, max, comment, null);
    }

    private static ConfigOption number(String section, String key, double def, double min, double max, String comment) {
        return new ConfigOption(section, key, Kind.DOUBLE, def, min, max, comment, null);
    }

    private static ConfigOption stringList(String section, String key, String comment, String example) {
        return new ConfigOption(section, key, Kind.STRING_LIST, List.of(), 0, 0, comment, example);
    }
}
