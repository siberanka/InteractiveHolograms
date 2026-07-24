package com.siberanka.interactiveholograms.packet;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PacketRuntimeTest {

    @Test
    void initializesBackendForPlugin() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getGlobal());

        PacketRuntime runtime = new PacketRuntime(plugin);
        runtime.onLoad();
        runtime.onEnable();

        assertNotNull(runtime.getBackend());

        runtime.onDisable();
    }
}
