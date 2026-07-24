package com.siberanka.interactiveholograms.packet;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class PacketRuntime {

    private final Plugin plugin;
    private PacketBackend backend;
    private PacketRuntimeState state = PacketRuntimeState.NEW;

    public PacketRuntime(Plugin plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        if (state != PacketRuntimeState.NEW) {
            return;
        }

        try {
            if (isExternalPacketEventsPresent()) {
                this.backend = new ExternalPacketEventsBackend();
                this.state = PacketRuntimeState.EXTERNAL_SELECTED;
                plugin.getLogger().info("Packet backend mode: EXTERNAL (PacketEvents " + backend.getVersionString() + ")");
            } else {
                this.backend = new EmbeddedPacketEventsBackend(plugin);
                backend.onLoad();
                this.state = PacketRuntimeState.EMBEDDED_LOADED;
                plugin.getLogger().info("Packet backend mode: EMBEDDED (PacketEvents " + backend.getVersionString() + ")");
            }
        } catch (Throwable t) {
            this.state = PacketRuntimeState.FAILED;
            plugin.getLogger().severe("Failed to load PacketEvents backend: " + t.getMessage());
        }
    }

    public void onEnable() {
        if (state == PacketRuntimeState.FAILED || state == PacketRuntimeState.TERMINATED) {
            plugin.getLogger().warning("PacketRuntime is in state " + state + ". Skipping enable.");
            return;
        }

        if (state == PacketRuntimeState.INITIALIZED) {
            return;
        }

        try {
            if (backend != null) {
                backend.onEnable();
                this.state = PacketRuntimeState.INITIALIZED;
            } else {
                this.state = PacketRuntimeState.FAILED;
            }
        } catch (Throwable t) {
            this.state = PacketRuntimeState.FAILED;
            plugin.getLogger().severe("Failed to enable PacketEvents backend: " + t.getMessage());
        }
    }

    public void onDisable() {
        if (state == PacketRuntimeState.TERMINATED) {
            return;
        }

        try {
            if (backend != null) {
                backend.onDisable();
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Error during PacketEvents backend disable: " + t.getMessage());
        } finally {
            this.state = PacketRuntimeState.TERMINATED;
        }
    }

    public PacketBackend getBackend() {
        return backend;
    }

    public PacketRuntimeState getState() {
        return state;
    }

    public boolean isInitialized() {
        return state == PacketRuntimeState.INITIALIZED;
    }

    public boolean isExternalPacketEventsPresent() {
        try {
            Plugin externalPlugin = Bukkit.getPluginManager().getPlugin("packetevents");
            if (externalPlugin == null) {
                externalPlugin = Bukkit.getPluginManager().getPlugin("PacketEvents");
            }
            if (externalPlugin != null) {
                Class<?> peClass = Class.forName("com.github.retrooper." + "packetevents.PacketEvents");
                Method getApiMethod = peClass.getMethod("getAPI");
                return getApiMethod.invoke(null) != null;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
