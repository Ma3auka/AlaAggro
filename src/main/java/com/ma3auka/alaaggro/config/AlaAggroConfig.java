package com.ma3auka.alaaggro.config;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AlaAggroConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // [general]
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue HOSTILE_VILLAGERS;
    public static final ModConfigSpec.BooleanValue REACTIVE_ONLY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSION_BLACKLIST;

    // [aggression]
    public static final ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SPEED_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue PER_CATEGORY_SPEED_CAP;
    public static final ModConfigSpec.DoubleValue DEFAULT_ATTACK_DAMAGE;
    public static final ModConfigSpec.BooleanValue ADD_MELEE_GOAL_TO_PASSIVE;
    public static final ModConfigSpec.BooleanValue REMOVE_PANIC_GOAL;

    // [callForHelp]
    public static final ModConfigSpec.BooleanValue CALL_FOR_HELP;
    public static final ModConfigSpec.IntValue CALL_FOR_HELP_RADIUS;

    // [memory]
    public static final ModConfigSpec.BooleanValue LONG_TERM_MEMORY;

    // [lists]
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_WHITELIST;

    static {
        B.push("general");
        ENABLED = B.comment("Enable the AlaAggro mod globally").define("enabled", true);
        HOSTILE_VILLAGERS = B.comment("Villagers attack the player").define("hostileVillagers", false);
        REACTIVE_ONLY = B.comment("Mobs attack only after being hit by the player").define("reactiveOnly", false);
        DIMENSION_BLACKLIST = B.comment("Dimensions where injection does not happen (empty = everywhere)")
                .defineListAllowEmpty("dimensionBlacklist", List.of(),
                        () -> "minecraft:the_nether", AlaAggroConfig::isString);
        B.pop();

        B.push("aggression");
        DAMAGE_MULTIPLIER = B.comment("Damage multiplier (1.0 = vanilla)")
                .defineInRange("damageMultiplier", 1.0, 0.1, 10.0);
        SPEED_MULTIPLIER = B.comment("Speed multiplier (1.0 = vanilla, max 3.0)")
                .defineInRange("speedMultiplier", 1.0, 0.1, 3.0);
        PER_CATEGORY_SPEED_CAP = B.comment("Absolute speed cap after multiplier (blocks/tick)")
                .defineInRange("perCategorySpeedCap", 0.5, 0.05, 2.0);
        DEFAULT_ATTACK_DAMAGE = B.comment("Default attack damage for mobs without the attack_damage attribute")
                .defineInRange("defaultAttackDamage", 2.0, 0.0, 100.0);
        ADD_MELEE_GOAL_TO_PASSIVE = B.comment("Add MeleeAttackGoal to passive mobs (CREATURE category)")
                .define("addMeleeGoalToPassive", true);
        REMOVE_PANIC_GOAL = B.comment("Remove PanicGoal from passive mobs on inject")
                .define("removePanicGoal", true);
        B.pop();

        B.push("callForHelp");
        CALL_FOR_HELP = B.comment("Call for help when hit by a player").define("enabled", true);
        CALL_FOR_HELP_RADIUS = B.comment("Notification radius in blocks")
                .defineInRange("radius", 16, 1, 128);
        B.pop();

        B.push("memory");
        LONG_TERM_MEMORY = B.comment("Mobs do not forget the player when out of sight")
                .define("longTermMemory", true);
        B.pop();

        B.push("lists");
        ENTITY_BLACKLIST = B.comment("Entity ids that are always skipped, e.g. [\"minecraft:bat\"]")
                .defineListAllowEmpty("entityBlacklist", List.of(),
                        () -> "minecraft:bat", AlaAggroConfig::isString);
        ENTITY_WHITELIST = B.comment("If non-empty, only these entity ids will be injected")
                .defineListAllowEmpty("entityWhitelist", List.of(),
                        () -> "minecraft:cow", AlaAggroConfig::isString);
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private AlaAggroConfig() {}

    private static boolean isString(Object o) {
        return o instanceof String;
    }
}
