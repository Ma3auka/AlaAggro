package com.ma3auka.alaaggro.core;

/**
 * Everything the eligibility rules need to know about one mob, reduced to plain values.
 *
 * <p>The Minecraft-facing layer looks these up once ({@code MobFactsReader}); the rules themselves
 * then run on strings and booleans. That split is what makes {@link AggroEligibility} unit-testable
 * without booting the game, and it is why the same rules can serve the join event, the periodic
 * scan and the {@code /alaaggro reload} command instead of each re-implementing the checks.
 *
 * @param entityId    registry id, e.g. {@code "minecraft:cow"}
 * @param dimensionId dimension the mob is in, e.g. {@code "minecraft:the_nether"}
 * @param boss        a boss: hardcoded vanilla three, or carrying the conventional boss tag
 * @param villager    a villager (their own config switch)
 * @param tagExcluded carries the {@code alaaggro:excluded} entity tag
 * @param pathfinder  has ordinary walking AI; mobs without it keep their brain untouched
 * @param tamed       tamed by a player
 * @param baby        a baby animal
 * @param named       carries a name tag
 */
public record MobFacts(
        String entityId,
        String dimensionId,
        boolean boss,
        boolean villager,
        boolean tagExcluded,
        boolean pathfinder,
        boolean tamed,
        boolean baby,
        boolean named
) {
}
