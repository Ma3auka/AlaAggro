package com.ma3auka.alaaggro.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.ai.AggroMarkerGoal;
import com.ma3auka.alaaggro.core.AggroConfig;
import com.ma3auka.alaaggro.core.AggroEligibility;
import com.ma3auka.alaaggro.core.AggroSettings;
import com.ma3auka.alaaggro.core.AggroVerdict;
import com.ma3auka.alaaggro.core.ExemptRegistry;
import com.ma3auka.alaaggro.core.MobFacts;
import com.ma3auka.alaaggro.entity.AggroInjector;
import com.ma3auka.alaaggro.entity.MobFactsReader;
import com.ma3auka.alaaggro.world.ExemptStorage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Every game event the mod reacts to, written once. Each loader subscribes its own event API and
 * forwards to the methods here, so behaviour cannot drift between the Fabric and NeoForge builds.
 */
public final class AggroHandlers {

    /** Player positions from the previous scan, used to notice a teleport. */
    private static final Map<UUID, Vec3> LAST_POSITIONS = new HashMap<>();
    private static final double TELEPORT_DISTANCE_SQ = 128.0 * 128.0;
    private static final double MEMORY_CLEAR_RADIUS = 256.0;

    private static int tickCounter;

    private AggroHandlers() {}

    // ------------------------------------------------------------------ spawn

    /** A mob entered the world: give it a hostile brain if the rules allow. */
    public static void onEntityJoin(Entity entity, Level level) {
        if (level.isClientSide()) return;
        if (!(entity instanceof Mob mob)) return;

        AggroSettings settings = AggroConfig.get();
        MobFacts facts = MobFactsReader.read(mob, level);
        AggroVerdict verdict = AggroEligibility.evaluate(facts, settings);
        if (!verdict.allowed()) {
            if (AlaAggro.LOGGER.isDebugEnabled()) {
                AlaAggro.LOGGER.debug("AlaAggro: skipping {} ({})", facts.entityId(), verdict);
            }
            return;
        }
        AggroInjector.inject(mob, settings, AggroConfig.generation());
    }

    // ------------------------------------------------------------------- tick

    /**
     * The safety net. Goal-based aggro alone is fragile across modded mobs and cannot reach mobs
     * whose chunks loaded before the mod was ready, so once every {@code scanIntervalTicks} we look
     * at the mobs near each player and make sure reality matches the config.
     */
    public static void onServerTick(MinecraftServer server) {
        AggroSettings settings = AggroConfig.get();
        if (++tickCounter < settings.scanIntervalTicks()) return;
        tickCounter = 0;
        if (!settings.enabled()) return;

        int generation = AggroConfig.generation();
        for (ServerLevel level : server.getAllLevels()) {
            scanLevel(level, settings, generation);
        }
        noticeTeleports(server, settings);
    }

    private static void scanLevel(ServerLevel level, AggroSettings settings, int generation) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        for (ServerPlayer player : players) {
            if (!isAggroTarget(player)) continue;
            AABB area = player.getBoundingBox().inflate(settings.scanRadius());
            for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
                if (!mob.isAlive()) continue;

                MobFacts facts = MobFactsReader.read(mob, level);
                if (!AggroEligibility.isEligible(facts, settings)) {
                    // The rules changed under a mob we had already converted (a fresh blacklist
                    // entry, a pet that has just been tamed): give it its life back.
                    if (AggroMarkerGoal.of(mob) != null) AggroInjector.pacify(mob);
                    continue;
                }

                if (!AggroInjector.isUpToDate(mob, generation)) {
                    AggroInjector.inject(mob, settings, generation);
                }

                if (settings.reactiveOnly()) {
                    restoreGrudge(mob, player, settings);
                    continue;
                }
                chase(mob, player);
            }
        }
    }

    /** Point the mob at the player and start it moving, unless it already has a valid quarry. */
    private static void chase(Mob mob, ServerPlayer player) {
        LivingEntity target = mob.getTarget();
        boolean targetUsable = target != null && target.isAlive()
                && target instanceof Player other && !ExemptRegistry.isExempt(other.getUUID());
        if (!targetUsable) {
            mob.setTarget(player);
        }
        remember(mob, player.getUUID());

        // Kick off pathing now rather than waiting for the attack goal's next tick. The navigator
        // may float (land mobs keep FloatGoal), so this path is allowed to cross water and the mob
        // swims toward the player instead of stalling at the shore.
        if (mob instanceof PathfinderMob pathfinder) {
            pathfinder.getNavigation().moveTo(player, 1.0D);
        }
    }

    /**
     * Long-term memory, the half that matters. In reactive-only mode nothing hands mobs a target,
     * so once vanilla drops the player for being out of sight the fight is simply over. Here a mob
     * that was already fighting this player picks the grudge back up.
     */
    private static void restoreGrudge(Mob mob, ServerPlayer player, AggroSettings settings) {
        if (!settings.longTermMemory()) return;
        AggroMarkerGoal marker = AggroMarkerGoal.of(mob);
        if (marker == null || !player.getUUID().equals(marker.grudge())) return;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            mob.setTarget(player);
        }
    }

    private static void remember(Mob mob, UUID player) {
        AggroMarkerGoal marker = AggroMarkerGoal.of(mob);
        if (marker != null) marker.remember(player);
    }

    // ----------------------------------------------------------------- damage

    /**
     * Call for help: hurting one mob turns its neighbours of the same kind on the attacker, and the
     * victim itself — without that last part, punching a chicken alerted the whole coop while the
     * chicken you hit went on pecking the ground.
     */
    public static void onPlayerHurtMob(LivingEntity victim, Entity attacker) {
        AggroSettings settings = AggroConfig.get();
        if (!settings.enabled() || !settings.callForHelp()) return;
        if (victim.level().isClientSide()) return;
        if (!(victim instanceof Mob hurt)) return;
        if (!(attacker instanceof Player player)) return;
        if (!isAggroTarget(player)) return;

        MobFacts facts = MobFactsReader.read(hurt, hurt.level());
        if (!AggroEligibility.isEligible(facts, settings)) return;

        aggroOn(hurt, player);

        double radius = settings.callForHelpRadius();
        AABB area = hurt.getBoundingBox().inflate(radius, Math.min(radius, 8.0), radius);
        List<Mob> neighbours = hurt.level().getEntitiesOfClass(Mob.class, area,
                other -> other != hurt && other.getType() == hurt.getType() && other.isAlive());
        for (Mob neighbour : neighbours) {
            if (!AggroEligibility.isEligible(MobFactsReader.read(neighbour, neighbour.level()), settings)) continue;
            aggroOn(neighbour, player);
        }
    }

    private static void aggroOn(Mob mob, Player player) {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            mob.setTarget(player);
        }
        remember(mob, player.getUUID());
    }

    // ------------------------------------------------------------- dimensions

    /** Crossing to another dimension breaks pursuit: nothing should still be waiting on return. */
    public static void onPlayerChangedDimension(ServerPlayer player) {
        forgetAround(player);
        LAST_POSITIONS.remove(player.getUUID());
    }

    /**
     * A long teleport counts as escaping. Detected by comparing positions between scans rather than
     * through a teleport event, because only one of the two loaders offers one — and the position
     * check also catches teleports made by other mods' own means.
     */
    private static void noticeTeleports(MinecraftServer server, AggroSettings settings) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            Vec3 now = player.position();
            Vec3 before = LAST_POSITIONS.put(id, now);
            if (before == null) continue;
            if (!settings.longTermMemory()) continue;
            if (before.distanceToSqr(now) >= TELEPORT_DISTANCE_SQ) {
                forgetAround(player);
            }
        }
    }

    private static void forgetAround(ServerPlayer player) {
        try {
            ServerLevel level = player.level() instanceof ServerLevel serverLevel ? serverLevel : null;
            if (level == null) return;
            AABB area = player.getBoundingBox().inflate(MEMORY_CLEAR_RADIUS);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, area, other -> other.getTarget() == player)) {
                mob.setTarget(null);
                AggroMarkerGoal marker = AggroMarkerGoal.of(mob);
                if (marker != null) marker.forget();
            }
        } catch (Throwable t) {
            AlaAggro.LOGGER.debug("AlaAggro: could not clear mob memory: {}", t.toString());
        }
    }

    // ----------------------------------------------------------- server state

    public static void onServerStarted(MinecraftServer server) {
        ExemptStorage.load(server);
    }

    /**
     * Wipe per-session state on shutdown. Without this a single-player exemption survives into the
     * next world opened in the same game session, where nobody ever granted it.
     */
    public static void onServerStopping(MinecraftServer server) {
        ExemptStorage.detach();
        ExemptRegistry.clear();
        LAST_POSITIONS.clear();
        tickCounter = 0;
    }

    // ---------------------------------------------------------------- helpers

    /** Players mobs are allowed to hunt: alive, playing, and not exempt. */
    public static boolean isAggroTarget(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative()
                && !ExemptRegistry.isExempt(player.getUUID());
    }

    /**
     * Re-applies the current config to every loaded mob — used by {@code /alaaggro reload} and by
     * switching the mod on. Runs the same eligibility rules as spawning, so a reload can no longer
     * make villagers, blacklisted mobs or mobs in blacklisted dimensions hostile against the config.
     *
     * @return how many mobs were changed
     */
    public static int applyToLoadedMobs(MinecraftServer server) {
        if (server == null) return 0;
        AggroSettings settings = AggroConfig.get();
        int generation = AggroConfig.generation();
        int touched = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob)) continue;
                MobFacts facts = MobFactsReader.read(mob, level);
                if (AggroEligibility.isEligible(facts, settings)) {
                    AggroInjector.inject(mob, settings, generation);
                    touched++;
                } else if (AggroMarkerGoal.of(mob) != null) {
                    AggroInjector.pacify(mob);
                    touched++;
                }
            }
        }
        return touched;
    }

    /** Calms every mob the mod has touched — used when the mod is switched off. */
    public static int pacifyLoadedMobs(MinecraftServer server) {
        if (server == null) return 0;
        int touched = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob)) continue;
                if (AggroMarkerGoal.of(mob) == null) continue;
                AggroInjector.pacify(mob);
                touched++;
            }
        }
        return touched;
    }

    /** Test hook. */
    public static void resetTickCounter() {
        tickCounter = 0;
        LAST_POSITIONS.clear();
    }
}
