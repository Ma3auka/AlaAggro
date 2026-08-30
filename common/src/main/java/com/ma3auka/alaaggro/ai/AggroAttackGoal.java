package com.ma3auka.alaaggro.ai;

import com.ma3auka.alaaggro.core.AggroConfig;
import com.ma3auka.alaaggro.core.AggroSettings;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Melee attack that works on animals which have no attack damage of their own.
 *
 * <p>Vanilla's {@code MeleeAttackGoal} ends in {@code Mob.doHurtTarget}, which reads the
 * {@code ATTACK_DAMAGE} attribute — chickens, sheep, cows and pigs simply do not have one, so an
 * unmodified goal makes them charge and then do nothing. This override deals the damage itself,
 * falling back to the configured default when the attribute is missing, which is what lets a cow
 * actually hurt the player.
 */
public class AggroAttackGoal extends MeleeAttackGoal {

    public AggroAttackGoal(PathfinderMob mob) {
        super(mob, 1.0D, true);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (!canPerformAttack(target)) return;
        this.resetAttackCooldown();
        this.mob.swing(InteractionHand.MAIN_HAND);
        performAttack(target);
    }

    private void performAttack(LivingEntity target) {
        AttributeInstance attack = this.mob.getAttribute(Attributes.ATTACK_DAMAGE);
        double damage;
        if (attack != null && attack.getValue() > 0.0) {
            damage = attack.getValue();
        } else {
            AggroSettings settings = AggroConfig.get();
            damage = settings.defaultAttackDamage() * settings.damageMultiplier();
        }
        DamageSource source = this.mob.damageSources().mobAttack(this.mob);
        target.hurt(source, (float) damage);
    }
}
