package com.ma3auka.alaaggro.event;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.util.AggroConfigCache;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Long-term memory: cancels target reset when the mob loses sight of the player.
 * Resets memory on dimension change or long-distance teleport.
 */
@EventBusSubscriber(modid = AlaAggro.MODID)
public final class MemoryHandler {
    private static final double TELEPORT_RESET_DISTANCE_SQ = 128.0 * 128.0;

    private MemoryHandler() {}

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        AggroConfigCache.Snapshot s = AggroConfigCache.get();
        if (!s.enabled() || !s.longTermMemory()) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof Mob mob)) return;

        // If vanilla logic wants to drop a player target (newTarget == null), keep it.
        LivingEntity old = mob.getTarget();
        if (old instanceof Player && event.getNewAboutToBeSetTarget() == null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearMemoryAround(event.getEntity(), 256.0);
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        double dx = event.getTargetX() - event.getPrevX();
        double dy = event.getTargetY() - event.getPrevY();
        double dz = event.getTargetZ() - event.getPrevZ();
        if (dx * dx + dy * dy + dz * dz < TELEPORT_RESET_DISTANCE_SQ) return;
        clearMemoryAround(player, 256.0);
    }

    private static void clearMemoryAround(Player player, double radius) {
        try {
            if (!(player.level() instanceof ServerLevel level)) return;
            AABB area = player.getBoundingBox().inflate(radius);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, area, m -> m.getTarget() == player)) {
                mob.setTarget(null);
            }
        } catch (Throwable t) {
            AlaAggro.LOGGER.debug("AlaAggro: memory clear failed: {}", t.toString());
        }
    }
}
