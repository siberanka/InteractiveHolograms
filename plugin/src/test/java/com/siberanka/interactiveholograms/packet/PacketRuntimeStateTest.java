package com.siberanka.interactiveholograms.packet;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PacketRuntimeStateTest {

    @Test
    void testStateTransitionsForEmbeddedMode() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getGlobal());

        PacketRuntime runtime = new PacketRuntime(plugin);
        assertEquals(PacketRuntimeState.NEW, runtime.getState());

        runtime.onLoad();
        // Without external PacketEvents plugin on classpath during unit tests, embedded mode is selected or loaded
        assertNotNull(runtime.getBackend());

        runtime.onEnable();
        // Check state transitions cleanly
        assertFalse(runtime.getState() == PacketRuntimeState.NEW);

        runtime.onDisable();
        assertEquals(PacketRuntimeState.TERMINATED, runtime.getState());
    }

    @Test
    void testExternalBackendReflectionBridge() {
        ExternalPacketEventsBackend backend = new ExternalPacketEventsBackend();
        assertEquals(0, backend.getClientProtocolVersion(null));
        assertNotNull(backend.getVersionString());
    }
}
