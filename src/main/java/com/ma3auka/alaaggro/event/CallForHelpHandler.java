package com.ma3auka.alaaggro.event;

import java.util.List;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.util.AggroConfigCache;
import com.ma3auka.alaaggro.util.ExemptRegistry;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * When a mob takes damage from a player, neighbouring mobs of the same EntityType
 * within `callForHelpRadius` blocks switch their target to that player.
 */
@EventBusSubscriber(modid = AlaAggro.MODID)
public final class CallForHelpHandler {
    private CallForHelpHandler() {}

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        AggroConfigCache.Snapshot s = AggroConfigCache.get();
        if (!s.enabled() || !s.callForHelp()) return;

        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        if (!(victim instanceof Mob hurtMob)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (ExemptRegistry.isExempt(player.getUUID())) return;

        double r = s.callForHelpRadius();
        AABB area = hurtMob.getBoundingBox().inflate(r, Math.min(r, 8.0), r);
        List<Mob> neighbours = hurtMob.level().getEntitiesOfClass(Mob.class, area,
                m -> m != hurtMob && m.getType() == hurtMob.getType() && m.isAlive());

        // Aggro the hit mob itself first — without this, hitting a passive mob
        // (chicken, cow, …) would only alert its neighbours but leave the actual
        // victim staring at the player.
        if (hurtMob.getTarget() == null || !hurtMob.getTarget().isAlive()) {
            hurtMob.setTarget(player);
        }

        for (Mob m : neighbours) {
            if (m.getTarget() == null || !m.getTarget().isAlive()) {
                m.setTarget(player);
            }
        }
    }
}
