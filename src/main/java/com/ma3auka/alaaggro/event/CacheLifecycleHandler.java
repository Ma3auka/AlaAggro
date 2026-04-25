package com.ma3auka.alaaggro.event;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.util.AggroConfigCache;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

public final class CacheLifecycleHandler {
    private CacheLifecycleHandler() {}

    public static void onConfigLoaded(ModConfigEvent.Loading event) {
        AggroConfigCache.rebuild();
    }

    public static void onConfigReloaded(ModConfigEvent.Reloading event) {
        AlaAggro.LOGGER.info("AlaAggro config reloaded — refreshing cache");
        AggroConfigCache.rebuild();
    }

    public static void onServerStarting(ServerStartingEvent event) {
        AggroConfigCache.rebuild();
    }
}
