# InteractiveHolograms 3.4.2 Release Notes

This release introduces a zero-steady-state-cost, high-scale performance architecture for production Minecraft servers with **100 to 500+ active players** and thousands of holograms.

### Performance Highlights
- **Zero-Cost Steady State**: Stationary players and unchanging holograms generate **$0$ packet sends**, **$0$ regex passes**, **$0$ location object allocations**, and **$0$ hitbox rebuilds** per tick.
- **Fast Text Scanner**: Replaced regex `String.replaceAll` / `Pattern.compile` in polling hot paths with a zero-allocation single-pass scanner (`TextLayoutScanner`).
- **Spatial Grid Indexing**: Implemented `WorldSpatialIndex` ($16 \times 16$ block grid cells) to query candidates without scanning all registered holograms.
- **Bidirectional Viewer Tracking**: Implemented `BidirectionalViewerIndex` for $O(1)$ visibility queries and targeted $O(\text{visible})$ player quit cleanups.
- **Per-Viewer Revision Tracking**: Replaced single global dirty booleans with monotonically increasing `contentRevision`, `layoutRevision`, etc.
- **Orientation Lookup & Hitbox Caching**: 24 precomputed sine/cosine orientation buckets eliminate `Math.sin`/`Math.cos`/`Math.atan2` in tick loops.
- **Netty Rate-Limiting**: Bounded incoming interaction packet rate-limiting on Netty threads to prevent main-thread Bukkit task scheduling floods.
- **JMH Suite & Regression Tests**: Added operation-count regression tests and a JMH benchmark module (`benchmarks/`).

### Verification & Artifact Integrity
- **Gradle Build**: Passed `./gradlew clean test check shadowJar --no-build-cache`
- **Artifact**: `InteractiveHolograms-3.4.2.jar` (7,963,088 bytes)
- **SHA-256**: `F3A4B82AA1EB063AEB15DCEBE47BC4BEBFA5EBA1F0AC1541334E6B4CEF9D1515`
