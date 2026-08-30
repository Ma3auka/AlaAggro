package com.ma3auka.alaaggro.entity;

import com.ma3auka.alaaggro.core.MobFacts;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

/**
 * Turns a live mob into the plain values the rules work on. The only place that knows how to ask
 * Minecraft each question, so {@link com.ma3auka.alaaggro.core.AggroEligibility} can stay free of
 * Minecraft types and be tested without the game.
 */
public final class MobFactsReader {

    private MobFactsReader() {}

    public static MobFacts read(Mob mob, Level level) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return new MobFacts(
                id != null ? id.toString() : "",
                level.dimension().identifier().toString(),
                BossGuard.isBoss(mob),
                mob instanceof Villager,
                mob.getType().builtInRegistryHolder().is(AggroTags.EXCLUDED),
                mob instanceof PathfinderMob,
                isTamed(mob),
                mob.isBaby(),
                mob.hasCustomName());
    }

    /**
     * Three questions, because vanilla answers taming in three different ways: wolves and cats
     * through {@link TamableAnimal}, horses and their relatives through their own flag, and modded
     * companions usually through {@link OwnableEntity} alone. Missing any of them would let a
     * player's own pet turn on them, which is exactly what the option exists to prevent.
     */
    private static boolean isTamed(Mob mob) {
        if (mob instanceof TamableAnimal tamable) return tamable.isTame();
        if (mob instanceof AbstractHorse horse) return horse.isTamed();
        return mob instanceof OwnableEntity ownable && ownable.getOwnerReference() != null;
    }
}
