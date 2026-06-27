package com.ma3auka.alaaggro.event;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.ai.AggroAttackGoal;
import com.ma3auka.alaaggro.util.AggroConfigCache;
import com.ma3auka.alaaggro.util.AquaticMobs;
import com.ma3auka.alaaggro.util.BossGuard;
import com.ma3auka.alaaggro.util.ExemptRegistry;
import com.ma3auka.alaaggro.util.FluidAggro;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Defensive layer: every second, scan loaded mobs and assign the nearest eligible
 * player as their target if they don't already have one. Goal-based aggro can be
 * fragile across modded mobs; this guarantees a baseline of actually-aggressive
 * behaviour even if a per-entity goal injection misbehaves.
 */
@EventBusSubscriber(modid = AlaAggro.MODID)
public final class TickAggroHandler {
    private static final int INTERVAL_TICKS = 20;
    private static final double SEARCH_RADIUS = 32.0D;

    private static int counter = 0;

    private TickAggroHandler() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        counter++;
        if (counter < INTERVAL_TICKS) return;
        counter = 0;

        AggroConfigCache.Snapshot s = AggroConfigCache.get();
        if (!s.enabled()) return;
        if (s.reactiveOnly()) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            tickLevel(level, s);
        }
    }

    private static void tickLevel(ServerLevel level, AggroConfigCache.Snapshot s) {
        if (level.players().isEmpty()) return;
        if (isBlacklistedDim(level, s)) return;

        // Scan only mobs near players — cheap, scales with players, not loaded chunks.
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) continue;
            if (ExemptRegistry.isExempt(player.getUUID())) continue;
            net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(SEARCH_RADIUS);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
                if (!mob.isAlive()) continue;
                if (!isEligible(mob, s)) continue;

                // Retroactive injection: if the mob's chunk loaded before the mod was active
                // (or before the latest version with wipe-and-rebuild), EntityJoinLevelEvent
                // never fired for it. Detect un-injected mobs by checking for our marker goal
                // and run full injectAggro on them once.
                if (!hasAggroAttackGoal(mob)) {
                    MobAggroEventHandler.injectAggro(mob, s);
                }

                LivingEntity current = mob.getTarget();
                if (current != null && current.isAlive() && current instanceof Player p && !ExemptRegistry.isExempt(p.getUUID())) {
                    continue; // already has a valid target
                }
                mob.setTarget(player);

                // Force-start pathing in case MeleeAttackGoal hasn't ticked yet this second.
                // Skip it while a land mob is buoyant in a fluid — forcing a fresh path there is
                // exactly what fights FloatGoal and makes the mob jitter (TASK-002/003). The target
                // stays set, so the chase resumes the instant the mob regains footing.
                if (mob instanceof PathfinderMob pf
                        && !FluidAggro.shouldSuspendChase(mob.isInWater(), mob.isInLava(),
                                mob.onGround(), AquaticMobs.isAquatic(mob))) {
                    pf.getNavigation().moveTo(player, 1.0D);
                }
            }
        }
    }

    private static boolean isEligible(Mob mob, AggroConfigCache.Snapshot s) {
        if (BossGuard.isBoss(mob)) return false;
        if (mob instanceof Villager && !s.hostileVillagers()) return false;
        if (mob.getType().builtInRegistryHolder().is(MobAggroEventHandler.EXCLUDED_TAG)) return false;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        String key = id != null ? id.toString() : "";
        if (s.entityBlacklist().contains(key)) return false;
        if (!s.entityWhitelist().isEmpty() && !s.entityWhitelist().contains(key)) return false;
        return true;
    }

    private static boolean hasAggroAttackGoal(Mob mob) {
        try {
            for (WrappedGoal w : mob.goalSelector.getAvailableGoals()) {
                if (w.getGoal() instanceof AggroAttackGoal) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isBlacklistedDim(ServerLevel level, AggroConfigCache.Snapshot s) {
        if (s.dimensionBlacklist().isEmpty()) return false;
        return s.dimensionBlacklist().contains(level.dimension().identifier().toString());
    }
}
