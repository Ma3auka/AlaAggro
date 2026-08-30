# Testing — AlaAggro

This document tracks what is covered by automated tests in this mod and **why** each test exists. It is the answer to *"what bug class does this catch?"* — not a vanity counter of test cases.

Two tiers, and each test has to answer one question before it is written: *what class of bug does
this catch?* A test that cannot answer it is not added.

---

## Quick reference

| Tier | Tool | Run with |
|---|---|---|
| 1 — Unit | JUnit 5 | `./gradlew :common:test` |
| 2 — GameTest | shared scenarios, both loaders | `./gradlew :fabric:runGameTest` · `./gradlew :neoforge:runGameTests` |

**Status: both tiers implemented.** 31 unit cases and 10 in-game scenarios, the latter running on Fabric *and* NeoForge from one set of bodies.

The Fabric game-test lane is part of `:fabric:build`. The NeoForge lane must be invoked explicitly — it is not wired into `build`.

---

## Tier 1 — Unit tests

**Location:** `common/src/test/java/com/ma3auka/alaaggro/unit/`
**Cases:** 31 (`AggroEligibilityTest` × 15, `ExemptRegistryTest` × 9, `AggroSettingsDefaultsTest` × 7)

Only `common/src/main/java/com/ma3auka/alaaggro/core/` is reachable from here — it is the part of the mod written without a single Minecraft import. That is not an accident: the eligibility rules were deliberately moved there so they could be tested at all.

### `AggroEligibilityTest` — who gets made hostile

The rules decide, for every mob that spawns, whether the mod rewrites its brain. A wrong answer is either a mod that does nothing, or a mod that wrecks something it was told to leave alone.

| # | Test | Bug class caught |
|---|---|---|
| 1 | `plainMobIsEligible` | The baseline works at all. |
| 2 | `disabledModTouchesNothing` | Master switch is honoured before anything else. |
| 3 | `bossIsProtected` | A boss fight's scripted AI is never wiped. |
| 4 | `mobWithoutWalkingAiIsLeftAlone` | Wiping a brain we have nothing to rebuild with. |
| 5 | `bossOutranksWalkingAi` | Rule precedence — a boss stays reported as a boss. |
| 6 | `villagersFollowTheirOwnSwitch` | Both directions of the villager option. |
| 7 | `blacklistedDimensionIsSkipped` | Dimension blacklist applies, and only there. |
| 8 | `taggedMobIsSkipped` | Datapack exclusion tag is honoured. |
| 9 | `listsBehaveAsDocumented` | Blacklist skips; a non-empty whitelist restricts. |
| 10 | `blacklistWinsOverWhitelist` | Pins the precedence rule an ordering change would flip. |
| 11–14 | `petProtections` (parameterised) | Each protection fires on its own trait and nothing else. |
| 15 | `petProtectionsAreOptional` | Protections can actually be switched off. |

**Real bug this catches:** the checks used to be hand-written in the join handler, written again in the periodic scan, and skipped entirely by `/alaaggro reload`. A villager left alone at spawn came back hostile after any reload. With one rule set behind one function, that drift is impossible — and these tests pin the rules themselves.

### `AggroSettingsDefaultsTest` — the config contract

| # | Test | Bug class caught |
|---|---|---|
| 16 | `defaults_pinAllFields` | Every default value pinned; drift breaks the build with the field named. |
| 17 | `get_neverNullBeforeLoad` | NPE on the hot path before a config file exists. |
| 18 | `settings_areAValueType` | Record contract — "did the config change?" checks stay meaningful. |
| 19 | `reload_bumpsGeneration` | If the counter froze, reloads would never reach mobs already in the world. |
| 20 | `options_areDeclaredConsistently` | Duplicate config paths, and options that map to no settings field. |
| 21 | `read_clampsOutOfRangeValues` | A hand-edited file behaves identically on both loaders. |
| 22 | `defaults_matchDeclaredOptions` | The declared defaults and the shared snapshot cannot disagree. |

**Real bug this catches:** a developer adds an option, wires it into one loader's backend, and forgets the shared defaults. The mod then behaves one way for the first second of every server start and another way afterwards.

### `ExemptRegistryTest` — the exempt-player set

Read from the server tick, written from command handlers. Bugs here are silent: a player just keeps getting hit.

| # | Test | Bug class caught |
|---|---|---|
| 23–27 | add/remove/isExempt/clear/view contracts | Command semantics and state leaks between worlds. |
| 28 | `view_rejectsMutation` | A caller cannot corrupt the backing set through the returned view. |
| 29 | `add_isThreadSafe` | Regressing to a plain `HashSet` loses entries under concurrency. |
| 30 | `changeListener_firesOnRealChangesOnly` | Persistence hook stops firing → list is empty after restart. |
| 31 | `replaceAll_doesNotNotify` | Loading is not a change; notifying there re-saves on every world load. |

---

## Tier 2 — GameTests

**Bodies:** `common/src/gametest/java/com/ma3auka/alaaggro/gametest/AlaAggroScenarios.java`
**Glue:** `fabric/src/gametest/java/…/AlaAggroGameTest.java` · `neoforge/src/gametest/java/…/NeoForgeGameTests.java`

One set of bodies, two lanes. Both loaders therefore prove the same behaviour, which is the point: the whole risk of a multi-loader mod is the two jars quietly diverging.

| Scenario | What it proves |
|---|---|
| `passive_mob_gets_hostile_brain` | A cow really turns hostile, and the vanilla animal goals are gone — leaving them in is what kept the navigator busy so the mob never chased. |
| `mob_without_walking_ai_is_untouched` | A ghast's goal count is byte-for-byte unchanged — it used to be wiped once a second. |
| `boss_is_untouched` | Boss AI is never rewritten. |
| `tagged_mob_is_untouched` | The `alaaggro:excluded` tag works end to end, tag loading included. |
| `tamed_pet_is_recognised` | Taming is read correctly from a live mob — vanilla answers it three different ways, and missing a branch means somebody's wolf attacks them. |
| `land_mob_keeps_float_goal` | Land mobs can still swim across water instead of bouncing in place. |
| `water_mob_has_no_float_goal` | Fish do not surface and leap out of the water. |
| `repeated_injection_does_not_compound` | Six injections land on the same damage as one. Catches the compounding-multiplier bug directly. |
| `pacify_restores_the_mob` | Switching off removes goals *and* the attribute changes, rather than baking them in. |
| `stale_mob_is_rebuilt` | A config change reaches mobs already in the world. |

### Notes for adding a scenario

- Put the body in `AlaAggroScenarios`, then add a wrapper in **both** glue classes. A scenario running on one loader only defeats the purpose.
- Do not call anything that walks the whole server (`applyToLoadedMobs`, `pacifyLoadedMobs`) — game tests share a world, and a global sweep will disturb whatever is running alongside.
- Spawning a mob already fires the join event, so a scenario cannot observe a mob "before" the mod sees it. Assert on the resulting state, or on the facts, instead.
- The NeoForge lane needs its own rig structure (`neoforge/src/gametest/resources/data/alaaggro/structure/gametest_rig.nbt`, 8×8×8 of air). `minecraft:empty` is 1×1×1 and makes tests fail depending on where the server places the rig.

---

## Coverage map

| Production class | Tier 1 | Tier 2 | Notes |
|---|---|---|---|
| `core/AggroEligibility` | ✅ full | ✅ indirect | The rules, tested directly. |
| `core/ExemptRegistry` | ✅ full | — | Pure Java + concurrency. |
| `core/AggroSettings` / `ConfigOption` | ✅ full | — | Defaults, clamping, declaration consistency. |
| `core/AggroConfig` | ✅ partial | ✅ generation | Backend binding is loader-specific. |
| `entity/AggroInjector` | ❌ | ✅ | Attribute modifiers, goal rebuild, pacify. |
| `entity/MobFactsReader` | ❌ | ✅ | Taming detection on a live mob. |
| `entity/BossGuard` | ❌ | ✅ | Vanilla bosses; the `c:bosses` tag path is untested. |
| `entity/AquaticMobs` | ❌ | ✅ | Via the two float-goal scenarios. |
| `ai/AggroMarkerGoal` | ❌ | ✅ | Generation staleness. |
| `ai/AggroAttackGoal` | ❌ | ❌ | Damage fallback needs a target taking damage — not covered. |
| `handler/AggroHandlers` (join) | ❌ | ✅ | Every scenario goes through it. |
| `handler/AggroHandlers` (tick, call-for-help, memory, teleport) | ❌ | ❌ | Needs a player in the world. **The largest gap.** |
| `world/ExemptStorage` | ❌ | ❌ | Needs a world save/reload cycle. |
| `command/AlaAggroCommand` | ❌ | ❌ | Needs a command source. |
| Both config backends | ❌ | ❌ | Loader-specific; the Fabric JSON round-trip is unverified. |

### Known gaps, in priority order

1. **Player-driven behaviour** — chasing, call for help, long-term memory, teleport reset. All of it runs only when a player is present, and none of it is covered. A scenario using a fake player would close most of this.
2. **`ExemptStorage` round-trip** — the list is written and read across a restart, which no current test exercises.
3. **Config backends** — a bad overload in the NeoForge backend crashed server start during this very rewrite, and only the game tests caught it. A round-trip test per backend would catch it earlier and cheaper.

---

*Last updated: 2026-08-30 — multi-loader rewrite: 10 → 31 unit cases, Tier 2 implemented (11 scenarios on both loaders).*
