package com.ma3auka.alaaggro.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ma3auka.alaaggro.util.FluidAggro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for the fluid-suspend decision behind the water/lava jitter fix.
 *
 * Why this matters:
 *   This is the gate that stops a land mob's chase navigation from fighting FloatGoal
 *   on a fluid surface (TASK-002 water, TASK-003 lava). Get the boolean algebra wrong
 *   in either direction and the mod regresses: too eager and mobs freeze while wading
 *   through shallow water with their feet on the ground (no longer aggressive); too lax
 *   and the original "bounce all over" jitter comes straight back. The rule is exact —
 *   suspend ONLY a non-aquatic mob that is in a fluid AND has lost its footing.
 */
final class FluidAggroTest {

    @ParameterizedTest(name = "[{index}] water={0} lava={1} onGround={2} aquatic={3} -> suspend={4}")
    @CsvSource({
            // aquatic mobs are NEVER suspended — water is their element
            "true,  false, false, true,  false",
            "true,  false, true,  true,  false",
            "false, true,  false, true,  false",
            // land mob floating in water (no footing) -> suspend
            "true,  false, false, false, true",
            // land mob floating in lava (no footing) -> suspend
            "false, true,  false, false, true",
            // land mob wading with footing -> keep chasing (NOT suspended)
            "true,  false, true,  false, false",
            "false, true,  true,  false, false",
            // land mob on dry ground -> keep chasing
            "false, false, true,  false, false",
            "false, false, false, false, false",
    })
    void shouldSuspendChase_truthTable(boolean water, boolean lava, boolean onGround,
                                       boolean aquatic, boolean expected) {
        if (expected) {
            assertTrue(FluidAggro.shouldSuspendChase(water, lava, onGround, aquatic));
        } else {
            assertFalse(FluidAggro.shouldSuspendChase(water, lava, onGround, aquatic));
        }
    }

    @Test
    @DisplayName("aquatic flag dominates — submerged fish is never suspended")
    void aquatic_isNeverSuspended() {
        // A fish is always in water with no ground footing; if the aquatic short-circuit
        // ever regresses, fish would be frozen mid-water and stop attacking entirely.
        assertFalse(FluidAggro.shouldSuspendChase(true, false, false, true));
    }

    @Test
    @DisplayName("dry land mob keeps chasing")
    void dryLandMob_chases() {
        assertFalse(FluidAggro.shouldSuspendChase(false, false, true, false));
    }
}
