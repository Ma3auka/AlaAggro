package com.ma3auka.alaaggro.world;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.core.AggroConfig;
import com.ma3auka.alaaggro.core.ExemptRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Keeps the {@code /alaaggro exempt} list in the world save.
 *
 * <p>Until now the list lived only in memory, so every restart silently un-exempted everyone and an
 * operator had to retype the commands — on a server that reboots nightly, the setting effectively
 * did not exist. The data rides along with the world rather than in a config file, which is what
 * makes it per-world and keeps it consistent with a backup or a rollback of that world.
 */
public final class ExemptStorage extends SavedData {

    public static final Codec<ExemptStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC_SET.optionalFieldOf("players", Set.of()).forGetter(storage -> storage.players)
    ).apply(instance, ExemptStorage::new));

    private static final SavedDataType<ExemptStorage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(AlaAggro.MODID, "exempt"),
            ExemptStorage::new,
            CODEC,
            DataFixTypes.LEVEL);

    private static ExemptStorage attached;

    private final Set<UUID> players;

    public ExemptStorage() {
        this(Set.of());
    }

    private ExemptStorage(Set<UUID> players) {
        this.players = new LinkedHashSet<>(players);
    }

    /**
     * Reads the saved list into the live registry and starts writing changes back. Called once the
     * server's worlds exist; when persistence is switched off it does nothing, leaving the registry
     * empty and in-memory as before.
     */
    public static void load(MinecraftServer server) {
        detach();
        if (!AggroConfig.get().persistExempt()) return;
        try {
            ServerLevel overworld = server.overworld();
            ExemptStorage storage = overworld.getDataStorage().computeIfAbsent(TYPE);
            attached = storage;
            ExemptRegistry.replaceAll(storage.players);
            ExemptRegistry.setChangeListener(ExemptStorage::persist);
        } catch (Throwable t) {
            AlaAggro.LOGGER.warn("AlaAggro: could not load the exempt list, continuing in memory only: {}",
                    t.toString());
            attached = null;
        }
    }

    /** Stops writing to the world save (server shutdown). */
    public static void detach() {
        ExemptRegistry.setChangeListener(null);
        attached = null;
    }

    private static void persist() {
        ExemptStorage storage = attached;
        if (storage == null) return;
        storage.players.clear();
        storage.players.addAll(ExemptRegistry.copy());
        storage.setDirty();
    }
}
