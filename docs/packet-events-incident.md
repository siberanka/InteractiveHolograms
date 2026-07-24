# PacketEvents 1.21.11 Decoder Incident & Architecture Resolution Report

## 1. Symptom Summary
On servers running Minecraft 1.21.11 (e.g. Leaf 1.21.11), players attempting to connect were immediately disconnected with errors such as:
- Client-side: `Internal Exception: io.netty.handler.codec.DecoderException: Failed to decode packet 'clientbound/minecraft:debug/event'`
- Server-side: `java.lang.IllegalStateException: NBT fog_color does not exist` (along with missing keys for `template_item`, `ingredient`, `wild_texture`, `effects`, `saddle` during registry synchronization)
- Server-side packet process exception: `Failed to map the Packet ID 131 to a PacketType constant. Bound: CLIENT. Connection state: PLAY. Server version: 1.21.4` followed by `PacketEvents 2.0 failed to inject`.

## 2. Root Cause Analysis

### Root Cause A: Obsolete Embedded PacketEvents Version (2.7.0)
In `gradle/libs.versions.toml`, the embedded PacketEvents dependency was pinned to `2.7.0`. PacketEvents 2.7.0 predates Minecraft 1.21.11 protocol updates. When connecting on 1.21.11, the 2.7.0 codec attempted to decode 1.21.11 configuration registries using outdated 1.21.4 registry schemas and packet maps. Packet ID 131 (`clientbound/minecraft:debug/event` in 1.21.11) was unknown to 2.7.0, causing decoding failure and disconnects.

### Root Cause B: External Detection Failure During `onLoad()`
`PacketRuntime#onLoad()` checked external plugin presence using `externalPlugin.isEnabled()`. During Bukkit's `onLoad()` phase, soft-depend plugins are loaded but NOT yet enabled (`isEnabled()` returns `false`). Consequently, `PacketRuntime` falsely determined that no external PacketEvents plugin existed and forced initialization of the embedded PacketEvents instance even when an external PacketEvents plugin was installed on the server.

### Root Cause C: ShadowRelocation Masking External API Calls
Gradle ShadowJar relocated `com.github.retrooper.packetevents` and `io.github.retrooper.packetevents` in `InteractiveHolograms` classes to `com.siberanka.interactiveholograms.shadow.packetevents`. Calling `PacketEvents.getAPI()` directly in Java source code resulted in bytecode calls to the relocated embedded PacketEvents singleton, preventing `ExternalPacketEventsBackend` from interacting with the external unrelocated PacketEvents plugin singleton.

---

## 3. Technical Solution

1. **Dependency Upgrade**: Upgraded PacketEvents version catalog entry to `2.12.1` (which fully supports 1.21.11 registry synchronization and packet maps).
2. **External Detection Fix**: `PacketRuntime` checks `Bukkit.getPluginManager().getPlugin("packetevents") != null || Bukkit.getPluginManager().getPlugin("PacketEvents") != null` and verifies external API class existence without checking `isEnabled()` during `onLoad()`.
3. **Reflection Bridge for External Backend**: `ExternalPacketEventsBackend` accesses the external unrelocated `com.github.retrooper.packetevents.PacketEvents` singleton via reflection (`Class.forName("com.github.retrooper." + "packetevents.PacketEvents")`), bypassing ShadowJar relocation completely.
4. **State Machine & Fail-Closed Lifecycle**:
   - `NEW` $\rightarrow$ `EXTERNAL_SELECTED` / `EMBEDDED_LOADED` $\rightarrow$ `INITIALIZED` $\rightarrow$ `TERMINATED` / `FAILED`.
   - If backend initialization fails, `PacketRuntime` transitions to `FAILED` and prevents registration of invalid packet listeners.
5. **Listener Unregistration**: `InteractiveHolograms` retains a reference to `PacketInteractionListener` and unregisters it cleanly on `onDisable()`.

---

## 4. Verification Matrix

| Scenario | Mode Selected | Pipeline Encoders | Protocol Detected | Result |
|---|---|---|---|---|
| Leaf 1.21.11 + External PacketEvents 2.12.1+ | EXTERNAL | 1 (External only) | V_1_21_11 | Pass (No disconnects, click actions work) |
| Leaf 1.21.11 (No External Plugin) | EMBEDDED | 1 (Embedded only) | V_1_21_11 | Pass (No disconnects, click actions work) |
| Paper 1.21.11 + ProtocolLib / Triton | EXTERNAL / EMBEDDED | Single IH Backend | V_1_21_11 | Pass (No duplicate encoder injection) |
