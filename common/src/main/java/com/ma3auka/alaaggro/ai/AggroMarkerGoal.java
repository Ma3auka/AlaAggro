package com.ma3auka.alaaggro.ai;

import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/**
 * A goal that never runs. It exists to carry two pieces of per-mob bookkeeping in a place that
 * works on both loaders and disappears with the mob: which config generation the mob's brain was
 * built under, and which player it is holding a grudge against.
 *
 * <p>Previously "have we already processed this mob?" was answered by looking for the attack goal.
 * That answer was wrong for every mob that never receives one, so the periodic scan rebuilt those
 * mobs once a second forever — wiping a ghast's fireball AI twenty times a minute. A dedicated
 * marker answers the question honestly, and stamping the generation on it means a config change
 * rebuilds exactly the stale mobs instead of every mob in the world.
 *
 * <p>Attachments would be the natural home for this, but NeoForge and Fabric spell them
 * differently; a goal is plain vanilla and costs one object on a mob that already holds several.
 */
public final class AggroMarkerGoal extends Goal {

    private final int generation;
    private UUID grudge;

    public AggroMarkerGoal(int generation) {
        this.generation = generation;
        // Claims no control flags: it must never compete with movement, looking or targeting.
        setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        return false;
    }

    public int generation() {
        return generation;
    }

    /** The player this mob remembers fighting, or null. */
    public UUID grudge() {
        return grudge;
    }

    public void remember(UUID player) {
        this.grudge = player;
    }

    public void forget() {
        this.grudge = null;
    }

    /** @return the mob's marker, or null if the mod has never touched it */
    public static AggroMarkerGoal of(Mob mob) {
        try {
            for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
                if (wrapped.getGoal() instanceof AggroMarkerGoal marker) return marker;
            }
        } catch (Throwable ignored) {
            // A modded mob with an unusual goal selector: treat as "not marked" rather than crash.
        }
        return null;
    }
}
