# InteractiveHolograms Wiki

This document covers InteractiveHolograms 3.x. It is the canonical reference for installation, packet behavior, YAML files, commands, imports and optional integrations.

## Contents

- [Requirements and installation](#requirements-and-installation)
- [Packet-only architecture](#packet-only-architecture)
- [Creating and editing holograms](#creating-and-editing-holograms)
- [Hologram YAML reference](#hologram-yaml-reference)
- [Click actions and hitboxes](#click-actions-and-hitboxes)
- [Importing other hologram plugins](#importing-other-hologram-plugins)
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

BetterModel integration uses its location-based `DummyTracker`. ModelEngine uses its non-Bukkit `Dummy`. MythicMobs mobs are never spawned by InteractiveHolograms; their registered IDs are resolved through an installed packet model backend.

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
| `/ih import <source> [path] [--overwrite]` | Convert any supported provider into modern IH YAML |
| `/ih convert ...`, `/ih migrate ...` | Aliases of the same modern import command |
| `/ih holograms model <name> <provider> [model] [animation]` | Select an installed model/mob; arguments tab-complete dynamically |

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

`model_provider` accepts `NONE`, `BETTERMODEL`, `MYTHICMOBS` or `MODELENGINE`. `BETTERMODEL` uses a location-only DummyTracker; `MODELENGINE` uses a non-Bukkit Dummy. `MYTHICMOBS` validates a registered Mythic mob ID, then resolves the same visual ID through BetterModel or ModelEngine without invoking a mob-spawn API. The `/ih holograms model` command lists installed providers, model/mob IDs and discoverable animations in tab completion. For a model-only base display, set its display scale to zero so only the external packet model is visible.

On load, the schema manager adds missing known entries, normalizes case and ranges, and removes unknown root keys. A backup is made before any repaired file is saved.

### Text formatting and PlaceholderAPI

Text holograms and `MESSAGE` click actions accept mixed formatting. PlaceholderAPI is resolved first, so a placeholder expansion may safely return any supported color syntax:

| Syntax | Examples |
|---|---|
| MiniMessage colors | `<aqua>Text</aqua>`, `<#12AB34>RGB</#12AB34>` |
| MiniMessage effects | `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>`, `<gradient:#ff0000:#0000ff>`, `<rainbow>`, `<transition:#f00:#00f:0.5>`, `<reset>`, `<newline>`, `<br>` |
| Legacy | `&a`, `§a`, `&l`, `&x&1&2&A&B&3&4` |
| Hex aliases | `#12AB34`, `&#12AB34`, `§#12AB34`, `{#12AB34}` |
| Historical IH/Iridium | `<#ff0000>Gradient</#0000ff>`, `<RAINBOW100>Rainbow</RAINBOW>` |
| ANSI/console | 16-color SGR, 256-color `ESC[38;5;196m`, true-color `ESC[38;2;18;171;52m` |

Supported PlaceholderAPI forms are standard `%identifier_parameter%`, bracket `{identifier_parameter}`, and relational `%rel_identifier_parameter%`. Relational placeholders use the current viewer as both relational contexts because a packet hologram has one viewer context and no server-side target entity. Placeholder output is resolved for up to three passes, allowing bounded nested expansions without infinite recursion. Results and formatter input are length-bounded, malformed formatting remains visible instead of interrupting rendering, and recurring output is kept in a bounded thread-safe LRU cache.

```yaml
text:
  - '<gradient:#00ffff:#5555ff><bold>Skyblock</bold></gradient>'
  - '<gray>Welcome, <aqua>%player_name%</aqua>!'
  - '&eBalance: &#55FF55%vault_eco_balance_formatted%'
  - '{#AAAAAA}Server: {server_name}'
```

Always quote YAML text containing `#`, `&`, `<` or `>`. Interactive MiniMessage tags such as `<click>` and `<hover>` are deliberately not executed inside hologram text; use the packet hitbox `actions` section for authenticated, distance-checked clicks. MiniMessage syntax follows the [Kyori format reference](https://docs.advntr.dev/minimessage/format.html), and expansion syntax follows the [PlaceholderAPI developer reference](https://wiki.placeholderapi.com/developers/using-placeholderapi/).

### Complete hologram YAML example

The following file is valid as `plugins/InteractiveHolograms/holograms/complete-example.yml`. It deliberately includes every public entry; only the content entries matching `type` are rendered.

```yaml
schema-version: 3
type: TEXT # TEXT, ITEM, BLOCK
location:
  world: world
  x: 0.0
  y: 64.0
  z: 0.0
  yaw: 0.0 # -180..180
  pitch: 0.0 # -90..90
enabled: true
visibility_distance: -1 # -1 or a non-negative block distance
visibility: ALL # ALL, MANUAL, PERMISSION_REQUIRED
permission: ''
persistent: true
billboard: CENTER # CENTER, FIXED, HORIZONTAL, VERTICAL
scale_x: 1.0
scale_y: 1.0
scale_z: 1.0
translation_x: 0.0
translation_y: 0.0
translation_z: 0.0
shadow_radius: 0.0
shadow_strength: 1.0
block_brightness: -1 # -1 or 0..15
sky_brightness: -1 # -1 or 0..15
glowing_color: disabled # disabled, named color, or supported hex
update_text_interval: -1 # -1 or positive ticks

# TEXT content
text:
  - '<gold><bold>InteractiveHolograms</bold>'
  - '<gray>Packet-only and clickable'
text_shadow: false
see_through: false
text_alignment: CENTER # LEFT, CENTER, RIGHT
background: '#40000000'
line_width: 300 # 1..4096
text_opacity: 255 # 0..255

# ITEM content
item: minecraft:apple # or a CraftEngine ID such as default:ruby
item_provider: AUTO # AUTO, VANILLA, CRAFTENGINE
item_display: NONE # NONE, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND,
                   # FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, HEAD,
                   # GUI, GROUND, FIXED

# BLOCK content
block: minecraft:grass_block

# External packet model
model_provider: NONE # NONE, BETTERMODEL, MYTHICMOBS, MODELENGINE
model: '' # provider model/mob ID
animation: '' # provider animation ID or empty

# LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT. A non-empty action map creates the
# per-viewer packet hitbox automatically for text, item, block, model and mob.
actions:
  RIGHT:
    - 'MESSAGE:<green>You clicked the hologram!'
  SHIFT_RIGHT:
    - 'CONSOLE:say {player} used the hologram'
```

The generated `hologram-example.yml` contains the same keys with expanded inline explanations and can be copied as a starting point.

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

## Importing other hologram plugins

The main migration command is:

```text
/ih import <source> [relative-path] [--overwrite]
```

`source` and its default path are tab-completed. The supported formats are:

| Source | Default input |
|---|---|
| `DecentHolograms` | `plugins/DecentHolograms/holograms/` |
| `FancyHolograms` | `plugins/FancyHolograms/holograms.yml` |
| `HolographicDisplays` | `plugins/HolographicDisplays/database.yml` |
| `CMI` | `plugins/CMI/Saves/holograms.yml`, then the older CMI path |
| `FutureHolograms` | `plugins/FutureHolograms/holograms.yml` |
| `GHolo` | `plugins/GHolo/data/h.data` |
| `Holograms` | `plugins/Holograms/holograms.yml` |

Examples:

```text
/ih import DecentHolograms
/ih import FancyHolograms --overwrite
/ih import HolographicDisplays plugins/HolographicDisplays/database.yml
/ih import CMI
```

All formats are written to `plugins/InteractiveHolograms/holograms/` using the current modern schema. Multi-page sources become `<name>_page2`, `<name>_page3`, and so on. Mixed text/item/entity line stacks are split into positioned `<name>_line2` files so visual lines are not serialized as literal text. `ICON`, `ITEM`, `HEAD`, `SMALLHEAD` and `ENTITY` markers are converted to packet display equivalents; entity markers become zero-scale anchors resolved through an installed packet model backend.

Imports execute off the server thread and only the final display reload returns to the main thread. Only one import may run at a time. Input paths are confined to the server directory, source size/output counts are bounded, writes are atomic and malformed entries fail closed.

DecentHolograms stores one YAML file per hologram. The importer reads a directory or a single file, converts every page into the modern packet schema, recognizes text plus `#ICON`, `#HEAD`, `#SMALLHEAD` and `#ENTITY` visuals, and carries location, visibility range, update interval, permission and page click actions. Imported `#ENTITY` lines become zero-scale ITEM anchors with a `MYTHICMOBS` model ID; they remain entity-free and need a matching BetterModel or ModelEngine visual:

```text
/ih holograms import-decent
/ih holograms import-decent plugins/DecentHolograms/holograms
/ih holograms import-decent plugins/DecentHolograms/holograms/example.yml --overwrite
```

The importer supports FancyHolograms' legacy `holograms.yml` storage:

```text
/ih holograms import-fancy
/ih holograms import-fancy plugins/FancyHolograms/holograms.yml
/ih holograms import-fancy plugins/FancyHolograms/holograms.yml --overwrite
```

Without `--overwrite`, existing names are skipped. With it, every replaced InteractiveHolograms file receives an `.import-backup-<timestamp>.yml` copy. Import paths are restricted to the server directory. Fancy ItemStacks are converted to vanilla namespaced IDs; when CraftEngine is loaded, its custom ID is retained.

Current FancyHolograms releases may migrate YAML into JSON. Import the preserved `holograms-old.yml`, or export/migrate to the legacy YAML layout first.

## Optional integrations

- **PlaceholderAPI:** standard, bracket and relational placeholders are resolved per viewer before formatting; bounded nested output is supported.
- **HeadDatabase:** skull identifiers remain available through item/skull content support.
- **CraftEngine:** use a namespaced ID such as `default:ruby`; `AUTO` resolves non-`minecraft` IDs through CraftEngine's public API.
- **BetterModel:** `model_provider: BETTERMODEL` creates a location-only DummyTracker.
- **ModelEngine:** `model_provider: MODELENGINE` creates a ModelEngine Dummy and active packet model.
- **MythicMobs:** `model_provider: MYTHICMOBS` selects a registered mob ID and resolves its model through BetterModel or ModelEngine. InteractiveHolograms never calls the mob-spawn API.

Use the command instead of guessing IDs: `/ih holograms model <hologram> <provider> <model-or-mob> [animation]`. Every selectable provider, model/mob and discoverable animation is offered by live tab completion and validated before it is saved.

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
ih.command.displays.model
ih.command.import
```

`check-for-updates` controls the asynchronous startup check against this repository's GitHub latest-release endpoint. When a newer semantic version is available, administrators receive a validated, clickable GitHub Release link. No update file is downloaded or executed automatically.

`PERMISSION_REQUIRED` visibility defaults to `interactiveholograms.hologram.<name>.view` when `permission` is empty.

## Backups and troubleshooting

- Automatic repairs: `<name>.backup-YYYYMMDD-HHMMSS.yml`
- Decent/Fancy overwrite import: `<name>.import-backup-YYYYMMDD-HHMMSS.yml`
- Backups are ignored by the loader and may be restored while the server is stopped.
- YAML indentation uses spaces. Avoid tabs.
- Quote text containing YAML control characters such as `:`, `#`, `{` or `}`.
- If a hologram does not load, inspect the complete exception in the server console and compare the repaired file with its backup.
- If a custom item shows the fallback material, confirm CraftEngine is loaded and the namespaced item ID exists.
- If a BetterModel visual is absent, confirm its model ID and resource pack, then check the warning emitted during tracker creation.
