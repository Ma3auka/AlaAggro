package com.ma3auka.alaaggro.neoforge;

import com.ma3auka.alaaggro.AlaAggro;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only companion. Registering the config-screen factory is what enables the
 * Mods → AlaAggro → Settings button; without this explicit extension point NeoForge greys it out
 * even when the config spec itself is registered correctly.
 */
@Mod(value = AlaAggro.MODID, dist = Dist.CLIENT)
public final class AlaAggroNeoForgeClient {

    public AlaAggroNeoForgeClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
