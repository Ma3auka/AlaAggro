package com.ma3auka.alaaggro;

import com.ma3auka.alaaggro.core.AggroConfig;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

/**
 * Shared entry point. Both loaders construct their own thin entry class, which hands the mod its
 * config storage and then subscribes the loader's events to {@link com.ma3auka.alaaggro.handler}
 * methods; everything below this line is loader-neutral.
 */
public final class AlaAggro {

    public static final String MODID = "alaaggro";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static volatile String version = "?";

    private AlaAggro() {}

    /**
     * Called by each loader's entry point once its config backend is ready.
     *
     * @param modVersion the version string the loader reports, so {@code /alaaggro info} can print
     *                   it without either loader's mod-list API leaking into shared code
     */
    public static void init(AggroConfig.Backend configBackend, String modVersion) {
        version = modVersion == null || modVersion.isBlank() ? "?" : modVersion;
        AggroConfig.bind(configBackend);
        LOGGER.info("AlaAggro {} initialized", version);
    }

    public static String version() {
        return version;
    }
}
