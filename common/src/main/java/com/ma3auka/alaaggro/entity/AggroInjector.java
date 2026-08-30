package com.ma3auka.alaaggro.entity;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.ai.AggroAttackGoal;
import com.ma3auka.alaaggro.ai.AggroMarkerGoal;
import com.ma3auka.alaaggro.core.AggroConfig;
import com.ma3auka.alaaggro.core.AggroSettings;
import com.ma3auka.alaaggro.core.ExemptRegistry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * Builds and unbuilds the hostile brain: wipes a mob's vanilla goals, gives it a zombie-shaped set
 * instead, and scales its attributes — or puts all of that back.
 *
 * <p>The wipe is load-bearing. Left in place, {@code TemptGoal}, {@code BreedGoal},
 * {@code FollowParentGoal} and friends keep winning the navigator and the attack goal never gets to
 * path, so the mob stares at the player instead of chasing.
 *
 * <p>Attributes are changed through named modifiers rather than by writing the base value. Writing
 * the base compounded: every reload multiplied the damage again, so at x2 a mob that survived three
 * reloads hit for eight times vanilla, and turning the mod off left the inflated numbers behind.
 * A modifier with a fixed id is replaced, not stacked, on re-injection and can be removed cleanly.
 */
public final class AggroInjector {

    private static final Identifier DAMAGE_MODIFIER =
            Identifier.fromNamespaceAndPath(AlaAggro.MODID, "aggro_damage");
    private static final Identifier SPEED_MODIFIER =
            Identifier.fromNamespaceAndPath(AlaAggro.MODID, "aggro_speed");
    private static final Identifier FOLLOW_MODIFIER =
            Identifier.fromNamespaceAndPath(AlaAggro.MODID, "aggro_follow_range");

    private AggroInjector() {}

    /**
     * Gives the mob a hostile brain. Callers must have cleared it through
     * {@link com.ma3auka.alaaggro.core.AggroEligibility} first — in particular the caller
     * guarantees walking AI, since a mob without it has nothing to rebuild with.
     */
    public static void inject(Mob mob, AggroSettings settings, int generation) {
        if (!(mob instanceof PathfinderMob pathfinder)) return;
        try {
            applyAttributes(mob, settings);

            mob.goalSelector.removeAllGoals(goal -> true);
            mob.targetSelector.removeAllGoals(goal -> true);

            mob.goalSelector.addGoal(0, new AggroMarkerGoal(generation));

            boolean aquatic = AquaticMobs.isAquatic(pathfinder);
            if (!aquatic) {
                pathfinder.goalSelector.addGoal(1, new FloatGoal(pathfinder));
            }
            pathfinder.goalSelector.addGoal(2, new AggroAttackGoal(pathfinder));
            pathfinder.goalSelector.addGoal(7, aquatic
                    ? new RandomStrollGoal(pathfinder, 1.0D)
                    : new WaterAvoidingRandomStrollGoal(pathfinder, 1.0D));
            pathfinder.goalSelector.addGoal(8, new LookAtPlayerGoal(pathfinder, Player.class, 8.0F));
            pathfinder.goalSelector.addGoal(8, new RandomLookAroundGoal(pathfinder));

            // Fighting back is always on: it is the whole of reactive-only mode, and harmless
            // otherwise since a proactive mob already has the player as its target.
            pathfinder.targetSelector.addGoal(1, new HurtByTargetGoal(pathfinder));

            // Hunting the player is what reactive-only mode leaves out.
            if (!settings.reactiveOnly()) {
                mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                        mob, Player.class, 10, false, false,
                        (entity, level) -> entity instanceof Player player
                                && !player.isSpectator() && !player.isCreative()
                                && !ExemptRegistry.isExempt(player.getUUID())));
            }
        } catch (Throwable t) {
            AlaAggro.LOGGER.warn("AlaAggro: failed to inject aggro into {}: {}", mob.getType(), t.toString());
        }
    }

    /**
     * Returns the mob to something close to normal: our goals and attribute modifiers come off and
     * it stops chasing at once. The idle goals we added stay until the entity next reloads, which is
     * harmless — they are the same wandering and looking goals a passive mob has anyway.
     */
    public static void pacify(Mob mob) {
        try {
            mob.goalSelector.removeAllGoals(goal ->
                    goal instanceof AggroAttackGoal || goal instanceof AggroMarkerGoal);
            mob.targetSelector.removeAllGoals(goal ->
                    goal instanceof NearestAttackableTargetGoal || goal instanceof HurtByTargetGoal);
            removeModifiers(mob);
            mob.setTarget(null);
        } catch (Throwable t) {
            AlaAggro.LOGGER.warn("AlaAggro: failed to pacify {}: {}", mob.getType(), t.toString());
        }
    }

    /** True when the mob has a brain built under the current config. */
    public static boolean isUpToDate(Mob mob, int generation) {
        AggroMarkerGoal marker = AggroMarkerGoal.of(mob);
        return marker != null && marker.generation() == generation;
    }

    private static void applyAttributes(Mob mob, AggroSettings settings) {
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            // A mob whose base damage is zero still needs to hurt: fall back to the configured
            // default, the same number AggroAttackGoal uses when the attribute is missing entirely.
            double base = attack.getBaseValue();
            double source = base > 0.0 ? base : settings.defaultAttackDamage();
            setTargetValue(attack, DAMAGE_MODIFIER, source * settings.damageMultiplier());
        }

        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            double scaled = speed.getBaseValue() * settings.speedMultiplier();
            double capped = Math.min(scaled, settings.perCategorySpeedCap());
            // Floor as well as cap: a mob slower than a walking player can never land a hit.
            setTargetValue(speed, SPEED_MODIFIER, Math.max(capped, settings.minMovementSpeed()));
        }

        AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) {
            setTargetValue(follow, FOLLOW_MODIFIER, Math.max(follow.getBaseValue(), settings.followRange()));
        }
    }

    /**
     * Pins the attribute to {@code target} with a named modifier. The amount is the difference from
     * the base value, so re-running it lands on the same number instead of compounding, and the mod
     * can undo it by removing one modifier.
     */
    private static void setTargetValue(AttributeInstance attribute, Identifier id, double target) {
        attribute.removeModifier(id);
        double delta = target - attribute.getBaseValue();
        if (delta == 0.0) return;
        attribute.addOrUpdateTransientModifier(
                new AttributeModifier(id, delta, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeModifiers(Mob mob) {
        removeModifier(mob, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER);
        removeModifier(mob, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        removeModifier(mob, Attributes.FOLLOW_RANGE, FOLLOW_MODIFIER);
    }

    private static void removeModifier(Mob mob,
                                       net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                       Identifier id) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    /** Config generation stamped on newly injected mobs. */
    public static int currentGeneration() {
        return AggroConfig.generation();
    }
}
