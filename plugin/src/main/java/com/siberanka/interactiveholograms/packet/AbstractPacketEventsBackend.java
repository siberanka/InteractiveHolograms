package com.siberanka.interactiveholograms.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.entity.Player;

import java.util.Collection;

public abstract class AbstractPacketEventsBackend implements PacketBackend {

    @Override
    public void sendPacket(Player player, Object packetWrapper) {
        if (player == null || !player.isOnline() || packetWrapper == null) {
            return;
        }
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user != null && packetWrapper instanceof PacketWrapper) {
            user.sendPacket((PacketWrapper<?>) packetWrapper);
        }
    }

    @Override
    public void sendPackets(Player player, Collection<?> packetWrappers) {
        if (player == null || !player.isOnline() || packetWrappers == null || packetWrappers.isEmpty()) {
            return;
        }
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user != null) {
            for (Object pw : packetWrappers) {
                if (pw instanceof PacketWrapper) {
                    user.sendPacket((PacketWrapper<?>) pw);
                }
            }
        }
    }

    @Override
    public void registerListener(Object packetListener) {
        if (packetListener instanceof PacketListenerCommon) {
            PacketEvents.getAPI().getEventManager().registerListener((PacketListenerCommon) packetListener);
        }
    }

    @Override
    public void unregisterListener(Object packetListener) {
        if (packetListener instanceof PacketListenerCommon) {
            PacketEvents.getAPI().getEventManager().unregisterListener((PacketListenerCommon) packetListener);
        }
    }

    @Override
    public int getClientProtocolVersion(Player player) {
        if (player == null) {
            return 0;
        }
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user != null && user.getClientVersion() != null) {
            return user.getClientVersion().getProtocolVersion();
        }
        return 0;
    }

    @Override
    public String getVersionString() {
        try {
            return String.valueOf(PacketEvents.getAPI().getVersion());
        } catch (Throwable ignored) {
            return "unknown";
        }
    }
}
