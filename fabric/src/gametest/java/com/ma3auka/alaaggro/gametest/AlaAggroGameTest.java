package com.ma3auka.alaaggro.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric glue: one wrapper per shared scenario. The bodies live in {@link AlaAggroScenarios} so the
 * NeoForge lane runs exactly the same checks.
 */
public class AlaAggroGameTest {

    @GameTest
    public void passiveMobGetsHostileBrain(GameTestHelper helper) {
        AlaAggroScenarios.passiveMobGetsHostileBrain(helper);
    }

    @GameTest
    public void mobWithoutWalkingAiIsUntouched(GameTestHelper helper) {
        AlaAggroScenarios.mobWithoutWalkingAiIsUntouched(helper);
    }

    @GameTest
    public void bossIsUntouched(GameTestHelper helper) {
        AlaAggroScenarios.bossIsUntouched(helper);
    }

    @GameTest
    public void taggedMobIsUntouched(GameTestHelper helper) {
        AlaAggroScenarios.taggedMobIsUntouched(helper);
    }

    @GameTest
    public void tamedPetIsRecognised(GameTestHelper helper) {
        AlaAggroScenarios.tamedPetIsRecognised(helper);
    }

    @GameTest
    public void landMobKeepsFloatGoal(GameTestHelper helper) {
        AlaAggroScenarios.landMobKeepsFloatGoal(helper);
    }

    @GameTest
    public void waterMobHasNoFloatGoal(GameTestHelper helper) {
        AlaAggroScenarios.waterMobHasNoFloatGoal(helper);
    }

    @GameTest
    public void repeatedInjectionDoesNotCompound(GameTestHelper helper) {
        AlaAggroScenarios.repeatedInjectionDoesNotCompound(helper);
    }

    @GameTest
    public void pacifyRestoresTheMob(GameTestHelper helper) {
        AlaAggroScenarios.pacifyRestoresTheMob(helper);
    }

    @GameTest
    public void staleMobIsRebuilt(GameTestHelper helper) {
        AlaAggroScenarios.staleMobIsRebuilt(helper);
    }
}
