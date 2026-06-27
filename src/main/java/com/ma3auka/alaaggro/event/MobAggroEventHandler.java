package com.ma3auka.alaaggro.event;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.ai.AggroAttackGoal;
import com.ma3auka.alaaggro.util.AggroConfigCache;
import com.ma3auka.alaaggro.util.AquaticMobs;
import com.ma3auka.alaaggro.util.BossGuard;
import com.ma3auka.alaaggro.util.ExemptRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = AlaAggro.MODID)
public final class MobAggroEventHandler {

    public static final TagKey<EntityType<?>> EXCLUDED_TAG =
            TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(AlaAggro.MODID, "excluded"));

    private MobAggroEventHandler() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        String key = id != null ? id.toString() : "";

        AggroConfigCache.Snapshot s = AggroConfigCache.get();
        if (!s.enabled()) return;
        if (BossGuard.isBoss(mob)) return;
        if (mob instanceof Villager && !s.hostileVillagers()) return;
        if (isInBlacklistedDimension(level, s)) return;
        if (mob.getType().builtInRegistryHolder().is(EXCLUDED_TAG)) return;
        if (s.entityBlacklist().contains(key)) return;
        if (!s.entityWhitelist().isEmpty() && !s.entityWhitelist().contains(key)) return;

        injectAggro(mob, s);
    }

    /**
     * Wipe the mob's vanilla goal/target selectors and rebuild a hostile-mob brain.
     * Without the wipe, competing goals (TemptGoal, BreedGoal, EatBlockGoal, AvoidEntityGoal,
     * FollowParentGoal …) keep the navigator busy and the MeleeAttackGoal never gets to path.
     */
    public static void injectAggro(Mob mob, AggroConfigCache.Snapshot s) {
        try {
            AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (AlaAggro.LOGGER.isDebugEnabled()) {
                AlaAggro.LOGGER.debug("AlaAggro: inject {} (hasATTACK_DAMAGE={}, isPathfinder={})",
                        BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
                        atk != null, mob instanceof PathfinderMob);
            }

            // 1. Attack damage (per instance, only if the attribute exists). For mobs without
            //    it (chicken, sheep, cow, pig) AggroAttackGoal falls back to defaultAttackDamage
            //    from the config, so we don't need to fail injection here.
            if (atk != null) {
                double base = atk.getBaseValue();
                if (base <= 0.0) base = s.defaultAttackDamage();
                atk.setBaseValue(base * s.damageMultiplier());
            }

            // 2. Movement speed.
            AttributeInstance spd = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spd != null) {
                double scaled = spd.getBaseValue() * s.speedMultiplier();
                if (scaled > s.perCategorySpeedCap()) scaled = s.perCategorySpeedCap();
                if (scaled < 0.30D) scaled = 0.30D; // never slower than a player walk
                spd.setBaseValue(scaled);
            }

            // 3. Follow range — needed by NearestAttackableTargetGoal to even consider the player.
            AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (follow != null && follow.getBaseValue() < 32.0D) {
                follow.setBaseValue(32.0D);
            }

            // 4. WIPE existing goals — this is the load-bearing step. Without it, vanilla
            //    Animal goals (TemptGoal, BreedGoal, FollowParentGoal, EatBlockGoal …) keep
            //    winning the navigator and our attack goal never gets to run.
            mob.goalSelector.removeAllGoals(g -> true);
            mob.targetSelector.removeAllGoals(g -> true);

            // 5. Rebuild as a Zombie-like hostile. PathfinderMob required for HurtByTargetGoal
            //    and the random-stroll/look goals. ATTACK_DAMAGE attribute is NOT required —
            //    AggroAttackGoal falls back to config defaultAttackDamage if it's missing.
            //    Land and water mobs get different brains:
            //      - Land mobs keep FloatGoal (so they don't drown) but their navigator is told
            //        NOT to route over water (setCanFloat(false)); pathing into water toward the
            //        player is the root of the surface jitter (TASK-002/003).
            //      - Water mobs (fish, axolotl, squid, dolphin…) get NO FloatGoal — otherwise they
            //        surface and leap out of the water — and chase the player through their native
            //        water navigation instead of avoiding it.
            if (mob instanceof PathfinderMob pf) {
                boolean aquatic = AquaticMobs.isAquatic(pf);
                if (!aquatic) {
                    pf.goalSelector.addGoal(1, new FloatGoal(pf));
                    // FloatGoal's constructor flips the navigator to canFloat(true); undo it so the
                    // mob walks up to the water's edge to attack instead of bobbing across the surface.
                    pf.getNavigation().setCanFloat(false);
                }
                pf.goalSelector.addGoal(2, new AggroAttackGoal(pf));
                pf.goalSelector.addGoal(7, aquatic
                        ? new RandomStrollGoal(pf, 1.0D)
                        : new WaterAvoidingRandomStrollGoal(pf, 1.0D));
                pf.goalSelector.addGoal(8, new LookAtPlayerGoal(pf, Player.class, 8.0F));
                pf.goalSelector.addGoal(8, new RandomLookAroundGoal(pf));
                pf.targetSelector.addGoal(1, new HurtByTargetGoal(pf));
            }

            // 6. Targeting. NearestAttackableTargetGoal with mustSee=false so passive mobs
            //    aggro through tall grass and leaves.
            mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                    mob, Player.class, 10, false, false,
                    (entity, lvl) -> entity instanceof Player p
                            && !p.isSpectator() && !p.isCreative()
                            && !ExemptRegistry.isExempt(p.getUUID())));
        } catch (Throwable t) {
            AlaAggro.LOGGER.warn("AlaAggro: failed to inject aggro into {}: {}",
                    mob.getType(), t.toString());
        }
    }

    private static boolean isInBlacklistedDimension(Level level, AggroConfigCache.Snapshot s) {
        if (s.dimensionBlacklist().isEmpty()) return false;
        Identifier dim = level.dimension().identifier();
        return s.dimensionBlacklist().contains(dim.toString());
    }
}
