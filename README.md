# AlaAggro

A NeoForge mod for Minecraft Java Edition. Every mob in the world becomes aggressive toward players the moment it spawns — passive farm animals, ambient bats, fish in the ocean, axolotls, all of them. Vanilla hostile mobs (zombies, skeletons, etc.) are left alone — they are already hostile.

- **Minecraft:** 1.21.10 (26.1.2)
- **Loader:** NeoForge 26.1.2.28-beta
- **Side:** server-only (vanilla clients can join)
- **License:** MIT
- **Languages:** English, Русский, 简体中文, Español, Português (Brasil), Deutsch, Français, العربية, हिन्दी, 日本語

## Features

- **Universal aggression** — cows, sheep, fish, bats, axolotls and modded mobs all attack on sight.
- **Boss guard** — Wither, Ender Dragon and Elder Guardian are never touched (they have custom AI phases).
- **Datapack-friendly exclusion** — add mobs to the entity tag `alaaggro:excluded` to opt them out without editing the config.
- **Per-player exemption** — `/alaaggro exempt <player>` temporarily removes a player from the aggro list.
- **Reactive-only mode** — flip a switch and mobs only attack after being hit first.
- **Call for help** — hurting one mob alerts neighbours of the same type within a configurable radius.
- **Long-term memory** — mobs do not forget the player when out of sight; resets on dimension change or long-distance teleport.
- **Damage and speed multipliers** with an absolute speed cap to keep fast mobs sane.
- **Dimension blacklist** — disable injection in The Nether, The End or any modded dimension.

## Commands

All commands require operator (permission level 2).

| Command | Action |
|---|---|
| `/alaaggro reload` | Reload the config and reapply to all loaded mobs. |
| `/alaaggro status` | Show current settings + active aggro mob counts by category. |
| `/alaaggro info` | Mod version + headline flags. |
| `/alaaggro toggle` | Enable / disable the mod globally. |
| `/alaaggro set damage <value>` | Set damage multiplier (0.1 – 10.0). |
| `/alaaggro set speed <value>` | Set speed multiplier (0.1 – 3.0). |
| `/alaaggro set callforhelp <true\|false>` | Toggle call-for-help. |
| `/alaaggro set memory <true\|false>` | Toggle long-term memory. |
| `/alaaggro set villagers <true\|false>` | Toggle hostile villagers. |
| `/alaaggro set reactive <true\|false>` | Toggle reactive-only mode. |
| `/alaaggro exempt <player>` | Exempt a player from aggro (in-memory, not persisted). |
| `/alaaggro unexempt <player>` | Remove a player from the exempt list. |

## Configuration

Server config: `config/alaaggro-server.toml`. Five sections:

- `[general]` — enabled, hostileVillagers, reactiveOnly, dimensionBlacklist
- `[aggression]` — damageMultiplier, speedMultiplier, perCategorySpeedCap, defaultAttackDamage, addMeleeGoalToPassive, removePanicGoal
- `[callForHelp]` — enabled, radius
- `[memory]` — longTermMemory
- `[lists]` — entityBlacklist, entityWhitelist

Run `/alaaggro reload` after editing the file — no server restart required.

## Installation

1. Install [NeoForge 26.1.2.28-beta](https://neoforged.net) for Minecraft 1.21.10.
2. Download the latest `alaaggro-26.1.2-<version>.jar` from the [Releases](../../releases) page or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/alaaggro).
3. Drop the jar into `.minecraft/mods/` (client) or `<server>/mods/` (server).
4. Launch.

## Building from source

Requires JDK 25 (bundled with the Minecraft 1.21.10 launcher).

```bash
git clone https://github.com/Ma3auka/AlaAggro.git
cd AlaAggro
./gradlew build
# Output: build/libs/alaaggro-26.1.2-<version>.jar
```

## Issues / contributing

Bug reports and feature requests: [GitHub Issues](../../issues). Pull requests welcome — please target `main` and describe the change.

## License

[MIT](LICENSE) © Artem Pavelko (Ma3auka).
