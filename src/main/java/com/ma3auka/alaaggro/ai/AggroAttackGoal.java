package com.ma3auka.alaaggro.ai;

import com.ma3auka.alaaggro.util.AggroConfigCache;
import com.ma3auka.alaaggro.util.AquaticMobs;
import com.ma3auka.alaaggro.util.FluidAggro;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Melee attack goal that does not rely on the mob having an {@code ATTACK_DAMAGE}
 * attribute. Vanilla {@link MeleeAttackGoal#checkAndPerformAttack} calls
 * {@code Mob.doHurtTarget}, which reads {@code ATTACK_DAMAGE} from the attribute map
 * and crashes (or silently no-ops) for {@code Animal} subclasses that don't have it
 * (chicken, sheep, cow, pig). We override the attack with a manual damage call that
 * pulls the value from {@link AggroConfigCache} when the attribute is missing.
 */
public class AggroAttackGoal extends MeleeAttackGoal {

    public AggroAttackGoal(PathfinderMob mob) {
        super(mob, 1.0D, true);
    }

    /**
     * Run the normal melee logic (look, cooldown, strike), then cancel the chase path if the
     * mob is buoyant in a fluid. {@code super.tick()} has already issued any {@code moveTo} and
     * decremented the attack cooldown; stopping the navigation immediately afterwards drops the
     * path before the mob acts on it, so it floats in place (held up by FloatGoal) instead of
     * fighting the buoyancy and jittering across the surface (TASK-002 water / TASK-003 lava).
     * The strike above still lands if the player is within reach, and aquatic mobs are never
     * suspended — they swim after the player normally.
     */
    @Override
    public void tick() {
        super.tick();
        if (FluidAggro.shouldSuspendChase(this.mob.isInWater(), this.mob.isInLava(),
                this.mob.onGround(), AquaticMobs.isAquatic(this.mob))) {
            this.mob.getNavigation().stop();
        }
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (!canPerformAttack(target)) return;
        this.resetAttackCooldown();
        this.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        performAttack(target);
    }

    private void performAttack(LivingEntity target) {
        AttributeInstance atk = this.mob.getAttribute(Attributes.ATTACK_DAMAGE);
        double damage = (atk != null && atk.getValue() > 0.0)
                ? atk.getValue()
                : AggroConfigCache.get().defaultAttackDamage()
                        * AggroConfigCache.get().damageMultiplier();
        DamageSource src = this.mob.damageSources().mobAttack(this.mob);
        target.hurt(src, (float) damage);
    }
}
