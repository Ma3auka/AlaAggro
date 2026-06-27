package com.ma3auka.alaaggro;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only @Mod entry point. Registers IConfigScreenFactory so the in-game
 * Mods → AlaAggro → "Settings" button is enabled and opens NeoForge's built-in
 * ConfigurationScreen for our SERVER config (which includes the master `enabled` toggle).
 *
 * Without this registration the button is greyed-out even when the SERVER spec
 * is correctly registered — NeoForge requires the explicit extension point.
 */
@Mod(value = AlaAggro.MODID, dist = Dist.CLIENT)
public class AlaAggroClient {
    public AlaAggroClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
