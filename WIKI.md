# InteractiveHolograms Wiki

This document covers InteractiveHolograms 3.x. It is the canonical reference for installation, packet behavior, YAML files, commands, imports and optional integrations.

## Contents

- [Requirements and installation](#requirements-and-installation)
- [Packet-only architecture](#packet-only-architecture)
- [Creating and editing holograms](#creating-and-editing-holograms)
- [Hologram YAML reference](#hologram-yaml-reference)
- [Click actions and hitboxes](#click-actions-and-hitboxes)
- [FancyHolograms import](#fancyholograms-import)
- [Optional integrations](#optional-integrations)
- [Configuration and permissions](#configuration-and-permissions)
- [Backups and troubleshooting](#backups-and-troubleshooting)

## Requirements and installation

Use a Bukkit-compatible server version supported by the packaged NMS modules. Display entities require Minecraft 1.19.4 or newer; older servers use the legacy packet renderer and its compatible command set. Java 21 is recommended for current Paper releases and is required to build the complete project.

1. Stop the server.
2. Put `InteractiveHolograms-<version>.jar` in `plugins/`.
3. Remove the old DecentHolograms jar when migrating; do not run both implementations together.
4. Start the server and review the console.
5. Use `/ih version` and `/ih holograms help`.

The main aliases are `/ih`, `/holograms`, `/hologram`, `/holo` and the migration alias `/dh`.

## Packet-only architecture

InteractiveHolograms does not call Bukkit world spawn methods for persistent holograms. Each viewer receives spawn, metadata, move and destroy packets based on world, distance and visibility rules. A clickable hologram receives a virtual hitbox entity ID automatically. Incoming interaction packets are matched quickly, then permission, world, distance and cooldown checks and all actions run on the main server thread.

BetterModel integration uses its location-based `DummyTracker`, which is not attached to a Bukkit entity. MythicMobs mobs are never spawned by InteractiveHolograms; use a BetterModel model name for a packet-only visual representation.

## Creating and editing holograms

Core commands:

| Command | Purpose |
|---|---|
| `/ih holograms create TEXT <name> [text]` | Create a text hologram |
| `/ih holograms create ITEM <name> <item>` | Create a vanilla or namespaced custom-item hologram |
| `/ih holograms create BLOCK <name> <block>` | Create a block hologram |
| `/ih holograms delete <name>` | Delete the hologram and its file |
| `/ih holograms clone <name> <new-name>` | Clone a hologram |
| `/ih holograms rename <name> <new-name>` | Rename a hologram |
| `/ih holograms movehere <name>` | Move it to the sender |
| `/ih holograms move <name> <x> <y> <z>` | Apply a relative movement |
| `/ih holograms teleport <name>` | Teleport to the hologram |
| `/ih holograms set-facing <name> <yaw> [pitch]` | Set fixed rotation |
| `/ih holograms enable\|disable <name>` | Change rendering state |
| `/ih holograms display-range <name> <blocks>` | Change visibility distance |
| `/ih holograms update-interval <name> <ticks>` | Change content update frequency |
| `/ih holograms attribute <name> <attribute> [value]` | Read or edit a display attribute |
| `/ih holograms list-attributes <name>` | List valid attributes for that type |
| `/ih holograms reset-attribute <name> <attribute>` | Restore an attribute default |
| `/ih holograms setting <name> <setting> [value]` | Edit visibility, permission, persistence or hitbox metadata |
| `/ih holograms action ...` | Manage click actions |
| `/ih holograms import-fancy ...` | Import FancyHolograms YAML |

Frequently used attributes include `billboard`, `scale`, `translation`, `yaw`, `pitch`, `brightness`, `shadow-radius`, `shadow-strength`, `glow-color`, `alignment`, `text-shadow`, `see-through`, `background-color`, `line-width`, `text-opacity`, `display-type`, `enchanted`, `leather-color` and `skull-texture`. Run `list-attributes` because applicable attributes depend on hologram type and server capability.

Examples:

```text
/ih holograms attribute welcome billboard FIXED
/ih holograms attribute welcome scale 1.5 1.5 1.5
/ih holograms attribute welcome translation 0 0.25 0
/ih holograms attribute welcome brightness 15 15
/ih holograms attribute welcome text-shadow true
/ih holograms setting welcome visibility PERMISSION_REQUIRED
/ih holograms setting welcome permission server.vip
```

## Hologram YAML reference

Each `holograms/<name>.yml` file represents one hologram. Names may contain letters, digits, `_` and `-`, up to 64 characters. `hologram-example.yml` contains every key and every enumerated choice as comments.

General keys:

| Key | Values | Meaning |
|---|---|---|
| `schema-version` | `3` | Managed file schema |
| `type` | `TEXT`, `ITEM`, `BLOCK` | Display packet type |
| `location` | world, x, y, z, yaw, pitch | Anchor and fixed rotation |
| `enabled` | `true`, `false` | Whether packets are sent |
| `visibility_distance` | `-1` or block distance | `-1` uses the configured default |
| `visibility` | `ALL`, `MANUAL`, `PERMISSION_REQUIRED` | Viewer selection policy |
| `permission` | permission node or omitted | Override for permission visibility |
| `persistent` | `true`, `false` | Whether future command changes remain on disk |
| `billboard` | `CENTER`, `FIXED`, `HORIZONTAL`, `VERTICAL` | Client facing behavior |
| `scale_x/y/z` | decimal | Per-axis display scale |
| `translation_x/y/z` | decimal | Client-side positional offset |
| `shadow_radius` | decimal | Display shadow radius |
| `shadow_strength` | decimal | Display shadow opacity/strength |
| `block_brightness` | `-1`, `0..15` | Block-light override; `-1` disables override |
| `sky_brightness` | `-1`, `0..15` | Sky-light override; `-1` disables override |
| `glowing_color` | `disabled`, named color/hex where supported | Glow color |
| `update_text_interval` | `-1` or ticks | `-1` resolves once; positive values refresh |

Text-only keys are `text`, `text_shadow`, `see_through`, `text_alignment` (`LEFT`, `CENTER`, `RIGHT`), `background`, `line_width` and `text_opacity`. Item-only keys are `item`, `item_provider` (`AUTO`, `VANILLA`, `CRAFTENGINE`) and `item_display` (`NONE`, `THIRD_PERSON_LEFT_HAND`, `THIRD_PERSON_RIGHT_HAND`, `FIRST_PERSON_LEFT_HAND`, `FIRST_PERSON_RIGHT_HAND`, `HEAD`, `GUI`, `GROUND`, `FIXED`). Block holograms use `block`.

`model_provider` accepts `NONE`, `BETTERMODEL` or `MYTHICMOBS`. Both model modes require BetterModel and use `model` as the BetterModel model ID; `MYTHICMOBS` labels the source relationship but intentionally does not spawn a MythicMob. `animation` is retained as model metadata for forward-compatible animation selection. For a model-only ITEM hologram, set its display `scale` to zero so only the BetterModel dummy is visible.

On load, the schema manager adds missing known entries, normalizes case and ranges, and removes unknown root keys. A backup is made before any repaired file is saved.

## Click actions and hitboxes

Adding the first action creates the packet hitbox; removing the last action removes it. No Bukkit entity is inserted into a world or chunk.

```text
/ih holograms action <name> list <click>
/ih holograms action <name> add <click> <TYPE:data>
/ih holograms action <name> remove <click> <1-based-index>
/ih holograms action <name> clear <click>
```

Click choices: `LEFT`, `RIGHT`, `SHIFT_LEFT`, `SHIFT_RIGHT`.

Common action types:

- `MESSAGE:<text>` sends a message to the player.
- `COMMAND:/<command>` runs a command as the player.
- `CONSOLE:<command>` runs a console command.
- `SOUND:<sound>[:volume:pitch]` plays a sound.

Use `{player}` for player-name replacement. Packet input is revalidated against world and an eight-block interaction limit. `click-cooldown` in `config.yml` controls repeated activation.

## FancyHolograms import

The importer supports FancyHolograms' legacy `holograms.yml` storage:

```text
/ih holograms import-fancy
/ih holograms import-fancy plugins/FancyHolograms/holograms.yml
/ih holograms import-fancy plugins/FancyHolograms/holograms.yml --overwrite
```

Without `--overwrite`, existing names are skipped. With it, every replaced InteractiveHolograms file receives an `.import-backup-<timestamp>.yml` copy. Import paths are restricted to the server directory. Fancy ItemStacks are converted to vanilla namespaced IDs; when CraftEngine is loaded, its custom ID is retained.

Current FancyHolograms releases may migrate YAML into JSON. Import the preserved `holograms-old.yml`, or export/migrate to the legacy YAML layout first.

## Optional integrations

- **PlaceholderAPI:** placeholders are resolved for viewers in text and supported attributes.
- **HeadDatabase:** skull identifiers remain available through item/skull content support.
- **CraftEngine:** use a namespaced ID such as `default:ruby`; `AUTO` resolves non-`minecraft` IDs through CraftEngine's public API.
- **BetterModel:** set `model_provider: BETTERMODEL` and `model: demon_knight`. A location-only dummy tracker is created and moved with the hologram.
- **MythicMobs + BetterModel:** set `model_provider: MYTHICMOBS` and supply the corresponding BetterModel model ID. InteractiveHolograms never calls MythicMobs' mob-spawn API because that would create a real entity.

All integrations are soft dependencies. Missing providers do not prevent the plugin from enabling.

## Configuration and permissions

`config.yml` documents global defaults, performance limits, click cooldown, placeholder behavior and optional temporary damage/healing displays. Reload changes with `/ih reload` unless a setting explicitly requires restart.

The root administrative permission is `ih.admin`. Modern hologram commands inherit from `ih.command.displays`; fine-grained nodes include:

```text
ih.command.displays.create
ih.command.displays.delete
ih.command.displays.attribute
ih.command.displays.setting
ih.command.displays.action
ih.command.displays.import
```

`PERMISSION_REQUIRED` visibility defaults to `interactiveholograms.hologram.<name>.view` when `permission` is empty.

## Backups and troubleshooting

- Automatic repairs: `<name>.backup-YYYYMMDD-HHMMSS.yml`
- Fancy overwrite import: `<name>.import-backup-YYYYMMDD-HHMMSS.yml`
- Backups are ignored by the loader and may be restored while the server is stopped.
- YAML indentation uses spaces. Avoid tabs.
- Quote text containing YAML control characters such as `:`, `#`, `{` or `}`.
- If a hologram does not load, inspect the complete exception in the server console and compare the repaired file with its backup.
- If a custom item shows the fallback material, confirm CraftEngine is loaded and the namespaced item ID exists.
- If a BetterModel visual is absent, confirm its model ID and resource pack, then check the warning emitted during tracker creation.
