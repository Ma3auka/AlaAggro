package com.ma3auka.alaaggro.util;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;

/**
 * Classifies whether a mob is a water dweller, so the aggro injection can give it a
 * water-appropriate brain instead of the land-mob default.
 *
 * <p>Land mobs need {@code FloatGoal} (so they don't drown) but must not path over water
 * toward the player; water mobs need the opposite — no {@code FloatGoal} (otherwise they
 * surface and leap out of the water, which is the fish/axolotl half of the jitter bug) and
 * are free to chase through their native fluid navigation.
 *
 * <p>Detection combines two stable signals so it catches both the obvious cases and the odd
 * ones (e.g. squid, which keeps the default ground navigation but breathes underwater):
 * <ul>
 *   <li>{@code canBreatheUnderwater()} — true for fish, squid, dolphins, guardians, axolotls;</li>
 *   <li>navigation type — {@link WaterBoundPathNavigation} / {@link AmphibiousPathNavigation}.</li>
 * </ul>
 *
 * <p>Kept out of {@link FluidAggro} on purpose: that class must stay free of Minecraft
 * imports so it can be unit-tested on a bare JVM.
 */
public final class AquaticMobs {

    private AquaticMobs() {}

    public static boolean isAquatic(Mob mob) {
        if (mob.canBreatheUnderwater()) return true;
        return mob.getNavigation() instanceof WaterBoundPathNavigation
                || mob.getNavigation() instanceof AmphibiousPathNavigation;
    }
}
