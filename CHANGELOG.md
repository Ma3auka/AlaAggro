# Changelog

All notable changes to AlaAggro will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
