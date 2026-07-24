package com.siberanka.interactiveholograms.packet;

import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Interface abstracting packet interactions via PacketEvents.
 */
public interface PacketBackend {

    void onLoad();

    void onEnable();

    void onDisable();

    void sendPacket(Player player, Object packetWrapper);

    void sendPackets(Player player, Collection<?> packetWrappers);

    void registerListener(Object packetListener);

    void unregisterListener(Object packetListener);

    int getClientProtocolVersion(Player player);

    boolean isExternal();

    String getVersionString();
}
