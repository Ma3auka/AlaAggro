# AlaAggro

A Minecraft mod for **Fabric and NeoForge**. Every mob in the world becomes aggressive toward players the moment it spawns — passive farm animals, ambient bats, fish in the ocean, axolotls, all of them. Vanilla hostile mobs (zombies, skeletons, etc.) are left alone; they are already hostile.

- **Minecraft:** 26.2
- **Loaders:** NeoForge 26.2.0.67 · Fabric Loader 0.19.3 (requires Fabric API)
- **Side:** server-side logic (vanilla clients can join)
- **License:** MIT
- **Languages:** English, Русский, 简体中文, Español, Português (Brasil), Deutsch, Français, العربية, हिन्दी, 日本語

## Features

- **Universal aggression** — cows, sheep, fish, bats, axolotls and modded mobs all attack on sight.
- **Your pets stay yours** — tamed wolves, cats, parrots and horses are left alone by default. Baby animals and mobs wearing a name tag can be protected too.
- **Boss guard** — the Wither, Ender Dragon and Elder Guardian are never touched, and neither is any mob another mod tags as a boss (`c:bosses`), so custom boss fights keep working.
- **Mobs that hop or fly stay themselves** — slimes, ghasts and phantoms move in ways a chase brain cannot drive, so the mod leaves their AI untouched instead of breaking it.
- **Datapack-friendly exclusion** — add mobs to the entity tag `alaaggro:excluded` to opt them out without editing the config.
- **Per-player exemption** — `/alaaggro exempt <player>` removes a player from the aggro list, and the list now survives a server restart.
- **Reactive-only mode** — flip a switch and mobs stay calm until something hits them first.
- **Call for help** — hurting one mob alerts neighbours of the same type within a configurable radius.
- **Long-term memory** — a mob that was fighting you picks the fight back up instead of forgetting; it lets go when you change dimension or teleport far away.
- **Damage and speed multipliers** with an absolute speed cap and a speed floor, so slow animals can still reach you and fast ones do not teleport.
- **Dimension blacklist** — disable the mod in The Nether, The End or any modded dimension.
- **Tunable performance** — the scan interval and radius are config values, so a busy server can trade responsiveness for CPU.

## Commands

All commands require operator (permission level 2).

| Command | Action |
|---|---|
| `/alaaggro reload` | Reload the config and reapply it to all loaded mobs. |
| `/alaaggro status` | Show current settings + active aggro mob counts by category. |
| `/alaaggro info` | Mod version + headline flags. |
| `/alaaggro toggle` | Enable / disable the mod globally, taking effect immediately. |
| `/alaaggro set damage <value>` | Set damage multiplier (0.1 – 10.0). |
| `/alaaggro set speed <value>` | Set speed multiplier (0.1 – 3.0). |
| `/alaaggro set callforhelp <true\|false>` | Toggle call-for-help. |
| `/alaaggro set memory <true\|false>` | Toggle long-term memory. |
| `/alaaggro set villagers <true\|false>` | Toggle hostile villagers. |
| `/alaaggro set reactive <true\|false>` | Toggle reactive-only mode. |
| `/alaaggro set tamed <true\|false>` | Toggle protection for tamed pets. |
| `/alaaggro set babies <true\|false>` | Toggle protection for baby animals. |
| `/alaaggro set named <true\|false>` | Toggle protection for mobs with a name tag. |
| `/alaaggro exempt <player>` | Exempt a player from aggro. |
| `/alaaggro unexempt <player>` | Remove a player from the exempt list. |

Changes made with `set` and `toggle` are written to the config file, so they survive a restart.

## Configuration

- **NeoForge:** `config/alaaggro-server.toml` (also editable in-game through Mods → AlaAggro → Settings)
- **Fabric:** `config/alaaggro.json`

Both files carry the same keys in the same sections:

- `general` — enabled, hostileVillagers, reactiveOnly, dimensionBlacklist, excludeTamed, excludeBabies, excludeNamed, persistExempt
- `aggression` — damageMultiplier, speedMultiplier, perCategorySpeedCap, defaultAttackDamage, minMovementSpeed, followRange
- `callForHelp` — enabled, radius
- `memory` — longTermMemory
- `performance` — scanIntervalTicks, scanRadius
- `lists` — entityBlacklist, entityWhitelist

Run `/alaaggro reload` after editing the file — no server restart required.

### Lists

`entityBlacklist` mobs are always left alone. If `entityWhitelist` is non-empty, only mobs on it are made aggressive. An entry on both lists stays protected: an explicit "never touch this" wins.

## Installation

1. Install [NeoForge](https://neoforged.net) or [Fabric Loader](https://fabricmc.net) for Minecraft 26.2. On Fabric, also install [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download the jar matching your loader — `alaaggro-neoforge-26.2-<version>.jar` or `alaaggro-fabric-26.2-<version>.jar` — from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/alaaggro).
3. Drop it into `.minecraft/mods/` (client) or `<server>/mods/` (server).
4. Launch.

## Building from source

Requires JDK 25.

```bash
git clone https://github.com/Ma3auka/AlaAggro.git
cd AlaAggro
./gradlew build
# Output: fabric/build/libs/alaaggro-fabric-26.2-<version>.jar
#         neoforge/build/libs/alaaggro-neoforge-26.2-<version>.jar
```

The project is split into `common/` (all the behaviour, compiled against plain Minecraft) plus a thin `fabric/` and `neoforge/` adapter each. A fix therefore lands in both jars at once.

Tests:

```bash
./gradlew :common:test          # unit tests
./gradlew :fabric:runGameTest   # in-game tests on Fabric
./gradlew :neoforge:runGameTests # the same scenarios on NeoForge
```

## Issues / contributing

Bug reports and feature requests: [GitHub Issues](../../issues). Pull requests welcome — please target `main` and describe the change.

## License

[MIT](LICENSE) © Artem Pavelko (Ma3auka).
