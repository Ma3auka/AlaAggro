# Changelog

All notable changes to AlaAggro will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.5] — 2026-06-27

### Fixed
- Mobs no longer jitter or bounce on the surface of water and lava. Land mobs (cows, sheep, zombies …) are kept afloat but no longer try to path across the water toward the player, and their chase navigation is paused while they float, so they settle at the surface instead of fighting the buoyancy.
- Water creatures (fish, axolotls, squid, dolphins …) no longer leap out of the water — they keep their native swimming and now pursue the player through the water instead of trying to surface.
- The fix is universal — it applies to modded mobs too, not just vanilla ones, since the buoyancy handling now keys off how each mob actually moves rather than a hardcoded list.

## [1.0.4] — 2026-06-27

### Fixed
- Stop the constant console spam. The diagnostic line that printed for every mob spawn (and for every loaded mob on `/alaaggro reload`) now logs at DEBUG instead of INFO, so a normal server no longer shows an endless stream of `AlaAggro: inject …` messages. Enable DEBUG logging if you need it for troubleshooting.

## [1.0.3] — 2026-04-26

### Added
- Config screen translations (en_us + ru_ru) — section names and entry labels now render as readable text instead of raw `alaaggro.configuration.*` keys.

## [1.0.2] — 2026-04-26

### Fixed
- Fix the in-game Mods → AlaAggro → Settings button being greyed-out — register `IConfigScreenFactory` via a new client-only `AlaAggroClient` so NeoForge's built-in `ConfigurationScreen` opens for the SERVER spec. Lets users toggle the master `enabled` flag from the UI instead of editing TOML by hand.

## [1.0.1] — 2026-04-26

### Changed
- Local rebuild — confirms the existing master `enabled` toggle (server config, `[general].enabled`) covers all runtime handlers (MobAggro, TickAggro, CallForHelp, Memory). No source changes.

## [1.0.0] — 2026-04-25

### Added
- Aggression injected into every mob on spawn — passive farm animals (chicken, sheep, cow, pig), aquatic creatures, ambient mobs, modded mobs.
- Wipe-and-rebuild mob brain so vanilla goals (TemptGoal, BreedGoal, EatBlockGoal, FollowParentGoal, AvoidEntityGoal) don't keep the navigator busy.
- `AggroAttackGoal` deals damage independently of the `ATTACK_DAMAGE` attribute, so `Animal` subclasses without the attribute still attack correctly.
- Defensive tick-based aggro fallback — every second, mobs near the player are scanned and force-targeted, with retroactive injection for mobs whose chunks loaded before the mod could reach them.
- Hardcoded boss guard for Wither, Ender Dragon and Elder Guardian (their custom AI phases are left untouched).
- Server config (`alaaggro-server.toml`) with five sections: `general`, `aggression`, `callForHelp`, `memory`, `lists`.
- Datapack-friendly entity tag `alaaggro:excluded` for opting mobs out without touching the config.
- Per-player exempt list with `/alaaggro exempt` and `/alaaggro unexempt`.
- Brigadier command `/alaaggro` with `reload`, `status`, `info`, `toggle`, `set`, `exempt`, `unexempt`.
- Call-for-help: hurting one mob alerts neighbours of the same type within configurable radius, and aggros the hit mob itself.
- Long-term memory: mobs do not forget the player when out of sight; memory resets on dimension change or long-distance teleport.
- Reactive-only mode: mobs stay calm until hit by the player.
- Damage and speed multipliers with an absolute speed cap.
- Dimension blacklist and entity blacklist/whitelist.
- Per-instance follow-range bump and movement-speed floor so passive mobs can actually catch and reach the player.
- Localisation files for ten languages: English, Русский, 简体中文, Español, Português (Brasil), Deutsch, Français, العربية, हिन्दी, 日本語.
