package com.ma3auka.alaaggro.neoforge;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.command.AlaAggroCommand;
import com.ma3auka.alaaggro.core.AggroConfig;
import com.ma3auka.alaaggro.handler.AggroHandlers;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NeoForge entry point. Its whole job is to give the shared code a config backend and to translate
 * NeoForge's events into calls on {@link AggroHandlers} — the behaviour itself lives in common/.
 */
@Mod(AlaAggro.MODID)
public final class AlaAggroNeoForge {

    public AlaAggroNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, NeoForgeConfigBackend.SPEC);
        AlaAggro.init(new NeoForgeConfigBackend(), readVersion());

        // Config file read or edited: rebuild the shared snapshot.
        modEventBus.addListener((ModConfigEvent.Loading event) -> AggroConfig.reload());
        modEventBus.addListener((ModConfigEvent.Reloading event) -> AggroConfig.reload());

        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener((EntityJoinLevelEvent event) ->
                AggroHandlers.onEntityJoin(event.getEntity(), event.getLevel()));
        gameBus.addListener((ServerTickEvent.Post event) ->
                AggroHandlers.onServerTick(event.getServer()));
        gameBus.addListener((LivingDamageEvent.Post event) ->
                AggroHandlers.onPlayerHurtMob(event.getEntity(), event.getSource().getEntity()));
        gameBus.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                AggroHandlers.onPlayerChangedDimension(player);
            }
        });
        gameBus.addListener((ServerStartedEvent event) -> {
            AggroConfig.reload();
            AggroHandlers.onServerStarted(event.getServer());
        });
        gameBus.addListener((ServerStoppingEvent event) ->
                AggroHandlers.onServerStopping(event.getServer()));
        gameBus.addListener((RegisterCommandsEvent event) ->
                AlaAggroCommand.register(event.getDispatcher()));

        bootstrapGameTests(modEventBus);
    }

    /**
     * Wires up the game tests when the test source set is on the classpath. Called reflectively so
     * the released jar, which does not ship those classes, simply finds nothing and moves on.
     */
    private static void bootstrapGameTests(IEventBus modEventBus) {
        try {
            Class.forName("com.ma3auka.alaaggro.gametest.neoforge.NeoForgeGameTestBootstrap")
                    .getMethod("init", IEventBus.class)
                    .invoke(null, modEventBus);
        } catch (ClassNotFoundException expectedInProduction) {
            // Shipped jar: no game tests packed.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("game-test bootstrap present but failed to initialise", e);
        }
    }

    private static String readVersion() {
        return ModList.get().getModContainerById(AlaAggro.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("?");
    }
}
