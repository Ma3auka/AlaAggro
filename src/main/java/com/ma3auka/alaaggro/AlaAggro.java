package com.ma3auka.alaaggro;

import com.ma3auka.alaaggro.config.AlaAggroConfig;
import com.ma3auka.alaaggro.event.AttributeHandler;
import com.ma3auka.alaaggro.event.CacheLifecycleHandler;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(AlaAggro.MODID)
public class AlaAggro {
    public static final String MODID = "alaaggro";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AlaAggro(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, AlaAggroConfig.SPEC);

        modEventBus.addListener(CacheLifecycleHandler::onConfigLoaded);
        modEventBus.addListener(CacheLifecycleHandler::onConfigReloaded);
        modEventBus.addListener(AttributeHandler::onAttributeModify);
        NeoForge.EVENT_BUS.addListener(CacheLifecycleHandler::onServerStarting);

        LOGGER.info("AlaAggro initialized");
    }
}
