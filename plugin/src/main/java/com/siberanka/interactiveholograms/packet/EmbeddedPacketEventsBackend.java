package com.siberanka.interactiveholograms.packet;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.Plugin;

public class EmbeddedPacketEventsBackend extends AbstractPacketEventsBackend {

    private final Plugin plugin;
    private boolean initialized = false;

    public EmbeddedPacketEventsBackend(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad() {
        try {
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin));
            PacketEvents.getAPI().getSettings()
                    .reEncodeByDefault(false)
                    .checkForUpdates(false)
                    .bStats(false);
            PacketEvents.getAPI().load();
        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to load embedded PacketEvents: " + t.getMessage());
        }
    }

    @Override
    public void onEnable() {
        try {
            if (!initialized) {
                PacketEvents.getAPI().init();
                initialized = true;
            }
        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to init embedded PacketEvents: " + t.getMessage());
        }
    }

    @Override
    public void onDisable() {
        try {
            if (initialized) {
                PacketEvents.getAPI().terminate();
                initialized = false;
            }
        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to terminate embedded PacketEvents: " + t.getMessage());
        }
    }

    @Override
    public boolean isExternal() {
        return false;
    }
}
