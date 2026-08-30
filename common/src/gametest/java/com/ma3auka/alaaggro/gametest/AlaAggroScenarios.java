package com.ma3auka.alaaggro.gametest;

import com.ma3auka.alaaggro.ai.AggroAttackGoal;
import com.ma3auka.alaaggro.ai.AggroMarkerGoal;
import com.ma3auka.alaaggro.core.AggroConfig;
import com.ma3auka.alaaggro.core.AggroEligibility;
import com.ma3auka.alaaggro.core.AggroSettings;
import com.ma3auka.alaaggro.core.AggroVerdict;
import com.ma3auka.alaaggro.core.MobFacts;
import com.ma3auka.alaaggro.entity.AggroInjector;
import com.ma3auka.alaaggro.entity.MobFactsReader;
import com.ma3auka.alaaggro.handler.AggroHandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/**
 * In-game behaviour tests, shared by both loaders so the Fabric and NeoForge builds are held to the
 * same standard. Each loader supplies a thin glue class that calls these methods.
 *
 * <p>These cover what unit tests structurally cannot: whether a real mob in a real world ends up
 * with the brain we meant to give it. Every scenario runs without a player, so nothing depends on
 * timing or on where the test rig happens to be placed.
 */
public final class AlaAggroScenarios {

    /** Somewhere inside the test region, off the floor so mobs do not clip a wall. */
    private static final BlockPos SPAWN = new BlockPos(1, 2, 1);

    private AlaAggroScenarios() {}

    /**
     * The core promise of the mod: a cow that spawns turns hostile. Also checks the vanilla animal
     * goals are gone — leaving them in is what used to keep the navigator busy so the mob never
     * actually chased.
     */
    public static void passiveMobGetsHostileBrain(GameTestHelper helper) {
        Mob cow = helper.spawn(EntityTypes.COW, SPAWN);
        AggroHandlers.onEntityJoin(cow, helper.getLevel());

        if (AggroMarkerGoal.of(cow) == null) {
            helper.fail("a cow must be converted on join, but it carries no marker");
            return;
        }
        if (!hasGoal(cow, AggroAttackGoal.class)) {
            helper.fail("a converted cow must have the attack goal");
            return;
        }
        if (hasGoalNamed(cow, "BreedGoal") || hasGoalNamed(cow, "TemptGoal")) {
            helper.fail("vanilla animal goals must be wiped, or they keep winning the navigator");
            return;
        }
        helper.succeed();
    }

    /**
     * A mob with no walking AI keeps its own brain. Wiping a ghast's goals leaves it unable to do
     * anything at all — and because it never receives our attack goal, the periodic scan used to
     * re-wipe it every single second.
     */
    public static void mobWithoutWalkingAiIsUntouched(GameTestHelper helper) {
        Mob ghast = helper.spawn(EntityTypes.GHAST, SPAWN);
        int goalsBefore = ghast.goalSelector.getAvailableGoals().size();

        AggroHandlers.onEntityJoin(ghast, helper.getLevel());

        if (AggroMarkerGoal.of(ghast) != null) {
            helper.fail("a ghast must not be converted: it has no walking AI to rebuild");
            return;
        }
        if (ghast.goalSelector.getAvailableGoals().size() != goalsBefore) {
            helper.fail("a ghast's own goals must be left exactly as they were");
            return;
        }
        helper.succeed();
    }

    /** Bosses drive scripted fights from their goals; touching them breaks the fight. */
    public static void bossIsUntouched(GameTestHelper helper) {
        Mob boss = helper.spawn(EntityTypes.ELDER_GUARDIAN, SPAWN);
        AggroHandlers.onEntityJoin(boss, helper.getLevel());

        if (AggroMarkerGoal.of(boss) != null) {
            helper.fail("bosses must never be converted");
            return;
        }
        helper.succeed();
    }

    /** Mobs in the built-in exclusion tag — the cube mobs — keep vanilla behaviour. */
    public static void taggedMobIsUntouched(GameTestHelper helper) {
        Mob slime = helper.spawn(EntityTypes.SLIME, SPAWN);
        AggroHandlers.onEntityJoin(slime, helper.getLevel());

        if (AggroMarkerGoal.of(slime) != null) {
            helper.fail("a mob in the alaaggro:excluded tag must be left alone");
            return;
        }
        helper.succeed();
    }

    /**
     * A player's tamed pet must not turn on them. Taming is read from a live mob, which is what a
     * unit test cannot do: vanilla answers the question three different ways depending on the
     * species, and missing a branch means somebody's wolf attacks them.
     *
     * <p>Checked on the facts rather than by re-running the join handler, because the mob is
     * already in the world by the time a test can tame it — a wild wolf is fair game, and it is the
     * change of state that has to be noticed.
     */
    public static void tamedPetIsRecognised(GameTestHelper helper) {
        Mob wolf = helper.spawn(EntityTypes.WOLF, SPAWN);
        if (!(wolf instanceof TamableAnimal tamable)) {
            helper.fail("a wolf is expected to be tamable");
            return;
        }

        MobFacts wild = MobFactsReader.read(wolf, helper.getLevel());
        if (wild.tamed()) {
            helper.fail("a freshly spawned wolf must not read as tamed");
            return;
        }
        if (AggroEligibility.evaluate(wild, AggroConfig.get()) != AggroVerdict.ALLOW) {
            helper.fail("a wild wolf is fair game and must be allowed");
            return;
        }

        tamable.setTame(true, false);

        MobFacts pet = MobFactsReader.read(wolf, helper.getLevel());
        if (!pet.tamed()) {
            helper.fail("a tamed wolf must read as tamed, or pet protection never triggers");
            return;
        }
        if (AggroEligibility.evaluate(pet, AggroConfig.get()) != AggroVerdict.TAMED) {
            helper.fail("a tamed pet must be protected while excludeTamed is on");
            return;
        }
        helper.succeed();
    }

    /**
     * Land mobs keep the float goal, which is what lets them swim a pond toward the player instead
     * of bobbing at its edge.
     */
    public static void landMobKeepsFloatGoal(GameTestHelper helper) {
        Mob cow = helper.spawn(EntityTypes.COW, SPAWN);
        AggroHandlers.onEntityJoin(cow, helper.getLevel());

        if (!hasGoal(cow, FloatGoal.class)) {
            helper.fail("a land mob needs FloatGoal, or it drowns and cannot cross water");
            return;
        }
        helper.succeed();
    }

    /**
     * Water mobs must not get the float goal: floating makes a fish leap out of the water and
     * jitter on the surface, which is half of the water bug this rule exists to prevent.
     */
    public static void waterMobHasNoFloatGoal(GameTestHelper helper) {
        Mob cod = helper.spawn(EntityTypes.COD, SPAWN);
        AggroHandlers.onEntityJoin(cod, helper.getLevel());

        if (AggroMarkerGoal.of(cod) == null) {
            helper.fail("a fish should still be converted");
            return;
        }
        if (hasGoal(cod, FloatGoal.class)) {
            helper.fail("a water mob with FloatGoal surfaces and leaps out of the water");
            return;
        }
        helper.succeed();
    }

    /**
     * Re-applying the config must land on the same numbers. Writing the base value instead of using
     * a named modifier compounded silently: at x2 damage, three reloads made a mob hit eight times
     * as hard as vanilla, and nothing in the game said so.
     */
    public static void repeatedInjectionDoesNotCompound(GameTestHelper helper) {
        Mob zombie = helper.spawn(EntityTypes.ZOMBIE, SPAWN);
        AggroSettings settings = AggroConfig.get();

        AggroInjector.inject(zombie, settings, 1);
        double afterFirst = attackDamage(zombie);

        for (int i = 0; i < 5; i++) {
            AggroInjector.inject(zombie, settings, 1);
        }
        double afterSix = attackDamage(zombie);

        if (Math.abs(afterFirst - afterSix) > 1.0E-6) {
            helper.fail("damage compounded across injections: " + afterFirst + " -> " + afterSix);
            return;
        }
        helper.succeed();
    }

    /**
     * Switching the mod off must actually give the mob back: goals removed and, just as important,
     * the attribute changes undone rather than baked in.
     */
    public static void pacifyRestoresTheMob(GameTestHelper helper) {
        Mob zombie = helper.spawn(EntityTypes.ZOMBIE, SPAWN);
        double vanillaDamage = attackDamage(zombie);

        AggroInjector.inject(zombie, AggroConfig.get(), 1);
        AggroInjector.pacify(zombie);

        if (AggroMarkerGoal.of(zombie) != null) {
            helper.fail("pacify must remove the marker");
            return;
        }
        if (hasGoal(zombie, AggroAttackGoal.class)) {
            helper.fail("pacify must remove the attack goal");
            return;
        }
        if (Math.abs(attackDamage(zombie) - vanillaDamage) > 1.0E-6) {
            helper.fail("pacify must restore the original attack damage, got "
                    + attackDamage(zombie) + " instead of " + vanillaDamage);
            return;
        }
        helper.succeed();
    }

    /**
     * A mob built under an older config is rebuilt by the periodic scan. Without the generation
     * stamp, a changed setting only reached mobs that spawned afterwards.
     */
    public static void staleMobIsRebuilt(GameTestHelper helper) {
        Mob cow = helper.spawn(EntityTypes.COW, SPAWN);
        AggroInjector.inject(cow, AggroConfig.get(), AggroConfig.generation() - 1);

        if (AggroInjector.isUpToDate(cow, AggroConfig.generation())) {
            helper.fail("a mob injected under an older generation must read as stale");
            return;
        }

        AggroInjector.inject(cow, AggroConfig.get(), AggroConfig.generation());
        if (!AggroInjector.isUpToDate(cow, AggroConfig.generation())) {
            helper.fail("re-injecting must bring the mob up to date");
            return;
        }
        helper.succeed();
    }

    // --------------------------------------------------------------- helpers

    private static boolean hasGoal(Mob mob, Class<? extends Goal> type) {
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            if (type.isInstance(wrapped.getGoal())) return true;
        }
        return false;
    }

    /** Matches by simple class name, for vanilla goals we only care about by name. */
    private static boolean hasGoalNamed(Mob mob, String simpleName) {
        for (WrappedGoal wrapped : mob.goalSelector.getAvailableGoals()) {
            if (wrapped.getGoal().getClass().getSimpleName().equals(simpleName)) return true;
        }
        return false;
    }

    private static double attackDamage(Mob mob) {
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        return attack == null ? 0.0 : attack.getValue();
    }
}
