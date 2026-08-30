package com.ma3auka.alaaggro.fabric;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.command.AlaAggroCommand;
import com.ma3auka.alaaggro.core.AggroConfig;
import com.ma3auka.alaaggro.handler.AggroHandlers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric entry point — the mirror image of the NeoForge one. It supplies a config backend and wires
 * Fabric's events to the same {@link AggroHandlers} methods, so both jars behave identically
 * without either loader's API reaching into shared code.
 */
public final class AlaAggroFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        AlaAggro.init(
                new FabricConfigBackend(loader.getConfigDir().resolve(AlaAggro.MODID + ".json")),
                readVersion(loader));

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) ->
                AggroHandlers.onEntityJoin(entity, level));

        ServerTickEvents.END_SERVER_TICK.register(AggroHandlers::onServerTick);

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) ->
                AggroHandlers.onPlayerHurtMob(entity, source.getEntity()));

        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
                AggroHandlers.onPlayerChangedDimension(player));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            AggroConfig.reload();
            AggroHandlers.onServerStarted(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(AggroHandlers::onServerStopping);

        // Datapack reload is Fabric's moment to re-read the config file, matching NeoForge's
        // config-reload event, so editing the file and running /reload takes effect on both.
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) ->
                AggroConfig.reload());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                AlaAggroCommand.register(dispatcher));
    }

    private static String readVersion(FabricLoader loader) {
        return loader.getModContainer(AlaAggro.MODID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
    }
}
