package com.ma3auka.alaaggro.util;

/**
 * Pure decision logic for suspending a land mob's chase while it is buoyant in a fluid.
 *
 * <p>This class intentionally has NO Minecraft imports so it can be unit-tested on a plain
 * JVM. The MC-facing callers ({@code AggroAttackGoal}, {@code TickAggroHandler}) read the
 * four primitives off the live entity and delegate the verdict here.
 *
 * <p>The bug it addresses: a land mob (cow, zombie, sheep…) that is forced to chase the
 * player ends up pathing into water/lava while {@code FloatGoal} pushes it back up. The two
 * forces fight every tick and the mob jitters on the surface. The fix is to stop the chase
 * pathing while the mob is actually floating, letting {@code FloatGoal} hold it at the
 * surface unopposed.
 */
public final class FluidAggro {

    private FluidAggro() {}

    /**
     * @param inWater  {@code entity.isInWater()}
     * @param inLava   {@code entity.isInLava()}
     * @param onGround {@code entity.onGround()} — true while the mob still has footing
     * @param aquatic  true for mobs that live in water (fish, squid, axolotl, dolphin…);
     *                 they navigate fluids natively and must NEVER be suspended.
     * @return true when the chase navigation should be cancelled this tick.
     */
    public static boolean shouldSuspendChase(boolean inWater, boolean inLava, boolean onGround, boolean aquatic) {
        if (aquatic) return false;            // water is their element — let them swim after the player
        return (inWater || inLava) && !onGround; // suspend only while genuinely buoyant, not when wading with footing
    }
}
