package com.ma3auka.alaaggro.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;

/**
 * Tells a water dweller from a land mob, so each gets a brain that suits how it actually moves.
 *
 * <p>Land mobs keep {@code FloatGoal} — it stops them drowning, and the {@code canFloat(true)} it
 * sets lets the chase path run across water, so a cow swims a pond to reach the player instead of
 * bobbing at the edge. Water mobs must not get it: floating makes a fish leap out of the water and
 * jitter on the surface. That is the whole water/lava fix, keyed off movement rather than a
 * hardcoded species list, which is why it also covers modded mobs.
 */
public final class AquaticMobs {

    private AquaticMobs() {}

    public static boolean isAquatic(Mob mob) {
        if (mob.canBreatheUnderwater()) return true;
        return mob.getNavigation() instanceof WaterBoundPathNavigation
                || mob.getNavigation() instanceof AmphibiousPathNavigation;
    }
}
