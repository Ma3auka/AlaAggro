package com.ma3auka.alaaggro.gametest.neoforge;

import java.util.function.Consumer;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.gametest.AlaAggroScenarios;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge glue: registers the shared scenarios as game tests. */
public final class NeoForgeGameTests {

    public static final DeferredRegister<MapCodec<? extends GameTestInstance>> INSTANCE_TYPES =
            DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, AlaAggro.MODID);

    public static final DeferredHolder<MapCodec<? extends GameTestInstance>, MapCodec<CodeGameTestInstance>> CODE_TYPE =
            INSTANCE_TYPES.register("code", () -> CodeGameTestInstance.CODEC);

    /**
     * An 8x8x8 box of air, not {@code minecraft:empty}. The engine sizes chunk force-loading and
     * entity ticking from the structure's bounds, and the vanilla empty template is 1x1x1 — small
     * enough that a rig near a chunk edge misbehaves depending on where the server happens to place
     * it, which reads as a random failure.
     */
    private static final Identifier RIG = Identifier.fromNamespaceAndPath(AlaAggro.MODID, "gametest_rig");
    private static final int RIG_PADDING = 1;

    private static Holder<TestEnvironmentDefinition<?>> environment;

    private NeoForgeGameTests() {}

    public static void register(RegisterGameTestsEvent event) {
        environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(AlaAggro.MODID, "empty_env"),
                new TestEnvironmentDefinition.AllOf());

        add(event, "passive_mob_gets_hostile_brain", AlaAggroScenarios::passiveMobGetsHostileBrain);
        add(event, "mob_without_walking_ai_is_untouched", AlaAggroScenarios::mobWithoutWalkingAiIsUntouched);
        add(event, "boss_is_untouched", AlaAggroScenarios::bossIsUntouched);
        add(event, "tagged_mob_is_untouched", AlaAggroScenarios::taggedMobIsUntouched);
        add(event, "tamed_pet_is_recognised", AlaAggroScenarios::tamedPetIsRecognised);
        add(event, "land_mob_keeps_float_goal", AlaAggroScenarios::landMobKeepsFloatGoal);
        add(event, "water_mob_has_no_float_goal", AlaAggroScenarios::waterMobHasNoFloatGoal);
        add(event, "repeated_injection_does_not_compound", AlaAggroScenarios::repeatedInjectionDoesNotCompound);
        add(event, "pacify_restores_the_mob", AlaAggroScenarios::pacifyRestoresTheMob);
        add(event, "stale_mob_is_rebuilt", AlaAggroScenarios::staleMobIsRebuilt);
    }

    private static void add(RegisterGameTestsEvent event, String name, Consumer<GameTestHelper> body) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                RIG,
                200,          // maxTicks
                0,            // setupTicks
                true,         // required
                Rotation.NONE,
                false,        // manualOnly
                1,            // maxAttempts — a retry would only hide a real defect
                1,            // requiredSuccesses
                false,        // skyAccess
                RIG_PADDING);
        event.registerTest(Identifier.fromNamespaceAndPath(AlaAggro.MODID, name),
                new CodeGameTestInstance(body, data));
    }
}
