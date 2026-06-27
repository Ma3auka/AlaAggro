# Testing — AlaAggro

This document tracks what is covered by automated tests in this mod and **why** each test exists. It is the answer to *"what bug class does this catch?"* — not a vanity counter of test cases.

For studio-level testing strategy and rationale, see `_docs/14_TESTING_STRATEGY.md` in the workspace.

---

## Quick reference

| Tier | Tool | Run with |
|---|---|---|
| 1 — Unit | JUnit 5 | `./gradlew test` |
| 2 — GameTest | NeoForge `@GameTest` | `./gradlew runGameTestServer` (Phase 2) |

---

## Tier 1 — Unit tests

**Location:** `src/test/java/com/ma3auka/alaaggro/unit/`
**Status:** ✅ Phase 1 implemented — all 21 cases passing (`./gradlew test`)
**Cases:** 21 (`ExemptRegistryTest` × 7, `AggroSnapshotDefaultsTest` × 3, `FluidAggroTest` × 11)

### `ExemptRegistryTest` — global exempt-player set

`ExemptRegistry` is the only thing standing between an exempt player and the entire mob population aggroing on them. It is read every server tick from many threads and mutated from command handlers. Bugs are silent — a player just keeps getting hit.

| # | Test | Bug class caught |
|---|---|---|
| 1 | `add_returnTrueOnceThenFalse` | Idempotency contract for `/aggro exempt add`. |
| 2 | `isExempt_tracksMembership` | Read after add/remove returns correct value. |
| 3 | `remove_signalContract` | Distinguishes "removed" from "wasn't there". |
| 4 | `clear_removesEverything` | Server stop / reload doesn't leak entries. |
| 5 | `view_isLiveReflection` | Inspection commands see the real state. |
| 6 | `view_rejectsMutation` | View leak — caller can't corrupt internal set via returned reference. |
| 7 | `add_isThreadSafe` | Backing set must be `ConcurrentHashMap.newKeySet()`; regressing to plain `HashSet` is caught immediately. |

**Real bug this catches:** if someone "simplifies" the backing collection from `ConcurrentHashMap.newKeySet()` to `HashSet` thinking "it's just a few UUIDs", server tick + command thread can race and lose entries — exempt players un-exempt themselves randomly. Test #7 (16 threads × 250 adds) reproduces this in milliseconds.

### `AggroSnapshotDefaultsTest` — fallback snapshot when config not loaded

`Snapshot.defaults()` is what every event handler sees between mod construction and config-load (and when `SPEC.isLoaded() == false`). Drift between config defaults and snapshot defaults = mod misbehaves at startup before any user-visible config exists.

| # | Test | Bug class caught |
|---|---|---|
| 8 | `defaults_pinAllFields` | Pin every default value. Any drift = build breaks with diff showing the changed field. |
| 9 | `get_neverNullEvenBeforeLoad` | NPE in hot path before config loads. |
| 10 | `snapshot_isValueType` | Record contract — equals/hashCode by content, two `defaults()` calls are equal. |

**Real bug this catches:** developer adds new config field `enableScreaming`, wires it into `AggroConfigCache.rebuild()`, but forgets to add it to `Snapshot.defaults()`. Compiles fine. Server starts → handlers see snapshot from before reload, screaming is "false" instead of intended "true" for first 1-2 seconds. Test #8 fails on the missing record component or wrong value.

### `FluidAggroTest` — water/lava jitter suspend rule

`FluidAggro.shouldSuspendChase(inWater, inLava, onGround, aquatic)` is the gate that fixes the
"mobs bounce all over on water and lava surfaces" bug (TASK-002 / TASK-003). The fix is a tiny
boolean rule, but it is exactly the kind of logic that breaks silently in both directions, so it
is the one piece extracted into a pure (MC-free) helper and pinned by a truth table.

| # | Test | Bug class caught |
|---|---|---|
| 11–19 | `shouldSuspendChase_truthTable` (9 rows) | Full truth table over the four inputs. |
| 20 | `aquatic_isNeverSuspended` | Aquatic short-circuit — a submerged fish must keep chasing, never freeze. |
| 21 | `dryLandMob_chases` | A mob on dry ground is never suspended (no false positive that would kill aggro). |

**Real bug this catches:** if the rule regresses to "suspend whenever `inWater || inLava`" (dropping
the `onGround` term), mobs wading through 1-deep water with their feet on the ground would freeze and
stop attacking — silently breaking the mod's whole point. If it regresses the other way (dropping the
fluid check), the original surface jitter returns. Row #6/#7 and the aquatic rows lock both directions.

**Why this is a pure helper:** the four inputs are read off the live entity in
`AggroAttackGoal.tick()` and `TickAggroHandler`, but the verdict itself has no Minecraft types, so it
runs on a bare JVM. The `aquatic` flag is computed by `AquaticMobs.isAquatic(Mob)` (MC `instanceof` +
`canBreatheUnderwater()`), which is GameTest-only (Tier 2).

---

## Tier 2 — NeoForge GameTests

**Location:** `src/main/java/com/ma3auka/alaaggro/gametest/` (to be created)
**Status:** 🟡 Phase 2 — not implemented yet

### Planned scenarios (priority order)

1. **`mobGetsAggroOnJoin`** — spawn a Cow, after 1 tick its `goalSelector` contains `AggroAttackGoal`.
2. **`bossNotInjected`** — spawn an EnderDragon, no `AggroAttackGoal` injected (we never touch boss AI).
3. **`exemptPlayerNotTargeted`** — add player to `ExemptRegistry`, spawn mob nearby, after 60 ticks mob still has `target == null`.
4. **`disabledModSkipsInjection`** — set `enabled=false`, spawn cow, no goal injection.
5. **`blacklistedEntitySkipped`** — entity in `ENTITY_BLACKLIST`, no injection.
6. **`whitelistOnlyAllowedInjected`** — non-empty whitelist excluding cow → cow not injected; including cow → cow injected.
7. **`landMobNoJitterInWater`** — spawn a Cow over deep water, after 60 ticks its vertical velocity stays near zero (no FloatGoal-vs-navigation bounce). Verifies the TASK-002/003 fix end-to-end.
8. **`waterMobHasNoFloatGoal`** — spawn a Cod, its `goalSelector` contains no `FloatGoal` (so it can't leap out of water) but does contain `AggroAttackGoal`.

### Test isolation requirements

All AlaAggro GameTests **must** use:
- `@BeforeBatch` calling `ExemptRegistry.clear()` and `AggroConfigCache.rebuild()` to reset global state.
- `succeedWhen()` (not `succeedIf()`) — `TickAggroHandler` runs every 20 ticks, immediate assertion races.
- `timeoutTicks = 60` minimum for tick-driven assertions.

**Why deferred:** GameTest API on NeoForge 26.1 needs first-run verification before we commit to specific imports / helper signatures. See `_docs/14_TESTING_STRATEGY.md` §5.

---

## Coverage map (what is and isn't tested)

| Production class / method | Tier 1 | Tier 2 | Why not / Notes |
|---|---|---|---|
| `ExemptRegistry` (full API) | ✅ 100% | — | Pure Java + ConcurrentHashMap, fully covered. |
| `FluidAggro.shouldSuspendChase` | ✅ 100% | — | Pure boolean rule, full truth table pinned. |
| `AquaticMobs.isAquatic(Mob)` | ❌ | 🟡 planned | `instanceof` + `canBreatheUnderwater()` on MC entities — GameTest only. |
| `AggroConfigCache.Snapshot.defaults()` | ✅ | — | All fields pinned. |
| `AggroConfigCache.get()` | ✅ partial | — | Null-safety only; full state is integration. |
| `AggroConfigCache.rebuild()` | ❌ | 🟡 planned | Reads `ModConfigSpec` — needs MC bootstrap. |
| `BossGuard.isBoss(Entity)` | ❌ | 🟡 planned | `instanceof` on MC entity classes — GameTest only. |
| `MobAggroEventHandler.injectAggro` | ❌ | 🟡 planned | High priority for Tier 2 — load-bearing AI logic; now branches land vs aquatic brain + `setCanFloat(false)`. |
| `AggroAttackGoal.tick` (fluid suspend) | ⚠️ partial | 🟡 planned | Decision covered via `FluidAggro`; the `navigation.stop()` side-effect is Tier 2. |
| `MobAggroEventHandler.onEntityJoin` | ❌ | 🟡 planned | Event handler — GameTest only. |
| `TickAggroHandler.onServerTick` | ❌ | 🟡 planned | Defensive layer — Tier 2 must verify. |
| `AggroAttackGoal` | ❌ | 🟡 planned | Goal AI — GameTest only. |
| `CallForHelpHandler` | ❌ | 🟡 planned | Event handler. |
| `MemoryHandler` | ❌ | 🟡 planned | Event handler. |
| `AttributeHandler` | ❌ | 🟡 planned | Event handler. |
| `AlaAggroCommand` | ❌ | 🟡 planned | Command — GameTest only. |
| `AlaAggroConfig.SPEC` | ❌ | ❌ | NeoForge handles config parsing; not our code. |

---

## Adding a new test

1. Find / extract the pure logic. If a method takes MC types, look for math/branching that could move into a primitive-arg helper.
2. Add the test under `src/test/java/com/ma3auka/alaaggro/unit/<Name>Test.java`.
3. Open the test class with a `Why this matters:` javadoc block — what bug class does it catch?
4. Run `./gradlew test` and confirm green.
5. Add a row to the table above with the exact bug class.

If the bug class can only be expressed as in-game behaviour, defer to Tier 2 (Phase 2 backlog).

---

*Last updated: 2026-06-27 — added `FluidAggroTest` (water/lava jitter fix, TASK-002/003); 21 unit cases. Phase 2 (GameTest) still deferred.*
