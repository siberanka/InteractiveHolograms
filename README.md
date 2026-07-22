# InteractiveHolograms

[![CI](https://github.com/siberanka/InteractiveHolograms/actions/workflows/ci.yml/badge.svg)](https://github.com/siberanka/InteractiveHolograms/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/siberanka/InteractiveHolograms)](https://github.com/siberanka/InteractiveHolograms/releases/latest)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)

InteractiveHolograms is a packet-only hologram engine for Bukkit-compatible Minecraft servers. Text, item, block, model and interaction data are sent directly to clients; holograms and their automatic click hitboxes are not added to the server world as persistent entities.

## Highlights

- Packet-only rendering and automatic packet hitboxes for clickable holograms
- Text, item and block display controls: billboard, rotation, scale, translation, light, shadow, background, opacity and alignment
- Per-hologram visibility, permission, distance, persistence and update settings
- LEFT, RIGHT, SHIFT_LEFT and SHIFT_RIGHT actions with server-side distance and cooldown validation
- Safe YAML schema migration: missing values are supplied and repaired files are backed up before invalid values are removed
- One `/ih import` workflow for DecentHolograms, FancyHolograms, HolographicDisplays, CMI, FutureHolograms, GHolo and Holograms
- Bounded, asynchronous import with path confinement, atomic writes and collision-safe overwrite backups
- PlaceholderAPI and HeadDatabase compatibility
- CraftEngine custom-item IDs plus entity-free BetterModel and ModelEngine models
- Dynamic command completion for installed BetterModel models, MythicMobs mob types, ModelEngine models and their discoverable animations
- Full in-game editing through `/ih holograms ...`
- GitHub Releases update checks with an in-game clickable download link

## Documentation

The complete installation guide, YAML reference, commands, actions, migration procedure and integration examples are in the **[InteractiveHolograms Wiki](WIKI.md)**.

The generated `hologram-example.yml`, `config.yml` and `attribute-defaults.yml` files also document every available value inline.

## Quick start

1. Download the latest jar from [GitHub Releases](https://github.com/siberanka/InteractiveHolograms/releases/latest).
2. Put it in the server's `plugins` directory and restart the server.
3. Create a hologram: `/ih holograms create TEXT welcome Welcome to the server!`
4. Add an interaction: `/ih holograms action welcome add RIGHT MESSAGE:<green>Hello!`
5. Edit display properties: `/ih holograms attribute welcome billboard FIXED`
6. Select an installed custom model: `/ih holograms model welcome BETTERMODEL <tab>`

Import an existing installation with `/ih import <source> [relative-path] [--overwrite]`. Source names and their default paths are available through tab completion.

Holograms are stored as individual files in `plugins/InteractiveHolograms/holograms/`.

## Building

JDK 21 is required to build the complete multi-version NMS matrix.

```bash
./gradlew clean test shadowJar
```

The release artifact is written to `plugin/build/libs/InteractiveHolograms-<version>.jar`.

## API

The Java package is `com.siberanka.interactiveholograms`. JitPack coordinates:

```groovy
repositories {
    maven { url = 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.siberanka:InteractiveHolograms:VERSION'
}
```

Replace `VERSION` with a published tag such as `v3.2.1`.

## Contributing and security

Issues and pull requests are welcome. For security-sensitive reports, avoid publishing exploit details until a maintainer has acknowledged the report. Changes should preserve the packet-only invariant, validate packet-originated input on the main server thread, and include tests where practical.

InteractiveHolograms is licensed under [GPL-3.0](LICENSE). Authors: d0by and siberanka.
