package com.ma3auka.alaaggro.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;

/**
 * Bosses are never touched: they drive scripted fight phases from their own goals, and wiping those
 * would break the fight rather than make it harder.
 *
 * <p>Two layers. The three vanilla bosses are named outright, because that must hold even with no
 * datapacks loaded. Everything else is recognised through the conventional {@code c:bosses} tag,
 * which mod authors already apply to their own bosses — so a modpack's custom boss is protected
 * without us maintaining a list of every boss ever written.
 */
public final class BossGuard {

    private BossGuard() {}

    public static boolean isBoss(Entity entity) {
        if (entity instanceof WitherBoss || entity instanceof EnderDragon || entity instanceof ElderGuardian) {
            return true;
        }
        return entity.getType().builtInRegistryHolder().is(AggroTags.BOSSES);
    }
}
