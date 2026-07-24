package com.siberanka.interactiveholograms.packet;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PacketRuntime {

    private final Plugin plugin;
    private PacketBackend backend;

    public PacketRuntime(Plugin plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        if (isExternalPacketEventsPresent()) {
            this.backend = new ExternalPacketEventsBackend();
            plugin.getLogger().info("Packet backend: external PacketEvents " + backend.getVersionString());
        } else {
            this.backend = new EmbeddedPacketEventsBackend(plugin);
            backend.onLoad();
            plugin.getLogger().info("Packet backend: embedded PacketEvents " + backend.getVersionString());
        }
    }

    public void onEnable() {
        if (backend != null) {
            backend.onEnable();
        }
    }

    public void onDisable() {
        if (backend != null) {
            backend.onDisable();
        }
    }

    public PacketBackend getBackend() {
        return backend;
    }

    private boolean isExternalPacketEventsPresent() {
        try {
            Plugin externalPlugin = Bukkit.getPluginManager().getPlugin("packetevents");
            if (externalPlugin == null) {
                externalPlugin = Bukkit.getPluginManager().getPlugin("PacketEvents");
            }
            if (externalPlugin != null && externalPlugin.isEnabled()) {
                return PacketEvents.getAPI() != null && PacketEvents.getAPI().isLoaded();
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
