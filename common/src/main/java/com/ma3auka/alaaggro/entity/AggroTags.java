package com.ma3auka.alaaggro.entity;

import com.ma3auka.alaaggro.AlaAggro;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/** Entity tags the mod reads. Datapacks can add to either without touching the config. */
public final class AggroTags {

    /** Mobs to leave alone. Ships with the cube mobs, which hop in a fixed direction, not at you. */
    public static final TagKey<EntityType<?>> EXCLUDED = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(AlaAggro.MODID, "excluded"));

    /**
     * The cross-mod convention for bosses. Reading it means a modded boss is protected by the same
     * rule as the Wither — its author already tags it, and we no longer have to name every boss in
     * the game to avoid wiping a custom fight's AI.
     */
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("c", "bosses"));

    private AggroTags() {}
}
