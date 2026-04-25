package com.ma3auka.alaaggro.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;

/**
 * Hardcoded boss list — bosses have custom AI phases that we must never touch.
 */
public final class BossGuard {
    private BossGuard() {}

    public static boolean isBoss(Entity e) {
        return e instanceof WitherBoss
                || e instanceof EnderDragon
                || e instanceof ElderGuardian;
    }
}
