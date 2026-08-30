package com.ma3auka.alaaggro.gametest.neoforge;

import net.neoforged.bus.api.IEventBus;

/**
 * Entry point for the game-test source set, called reflectively from the mod's main class so that a
 * shipped jar — which does not contain this source set — has nothing to load.
 */
public final class NeoForgeGameTestBootstrap {

    private NeoForgeGameTestBootstrap() {}

    public static void init(IEventBus modBus) {
        NeoForgeGameTests.INSTANCE_TYPES.register(modBus);
        modBus.addListener(NeoForgeGameTests::register);
    }
}
