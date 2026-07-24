package com.siberanka.interactiveholograms.packet;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collection;

public class ExternalPacketEventsBackend implements PacketBackend {

    private static final String PE_PKG = "com.github.retrooper." + "packetevents";

    private Object peApi;

    public ExternalPacketEventsBackend() {
        try {
            Class<?> peClass = Class.forName(PE_PKG + ".PacketEvents");
            Method getApiMethod = peClass.getMethod("getAPI");
            this.peApi = getApiMethod.invoke(null);
        } catch (Throwable t) {
            this.peApi = null;
        }
    }

    @Override
    public void onLoad() {
        // External mode: lifecycle is owned by external PacketEvents plugin.
    }

    @Override
    public void onEnable() {
        // External mode: lifecycle is owned by external PacketEvents plugin.
    }

    @Override
    public void onDisable() {
        // External mode: lifecycle is owned by external PacketEvents plugin.
    }

    @Override
    public void sendPacket(Player player, Object packetWrapper) {
        if (player == null || !player.isOnline() || packetWrapper == null || peApi == null) {
            return;
        }
        try {
            Object playerManager = peApi.getClass().getMethod("getPlayerManager").invoke(peApi);
            Object user = playerManager.getClass().getMethod("getUser", Object.class).invoke(playerManager, player);
            if (user != null) {
                Method sendMethod = user.getClass().getMethod("sendPacket", Class.forName(PE_PKG + ".wrapper.PacketWrapper"));
                sendMethod.invoke(user, packetWrapper);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void sendPackets(Player player, Collection<?> packetWrappers) {
        if (packetWrappers == null) {
            return;
        }
        for (Object pw : packetWrappers) {
            sendPacket(player, pw);
        }
    }

    @Override
    public void registerListener(Object packetListener) {
        if (packetListener == null || peApi == null) {
            return;
        }
        try {
            Object eventManager = peApi.getClass().getMethod("getEventManager").invoke(peApi);
            Class<?> listenerClass = Class.forName(PE_PKG + ".event.PacketListenerCommon");
            Method regMethod = eventManager.getClass().getMethod("registerListener", listenerClass);
            regMethod.invoke(eventManager, packetListener);
        } catch (Throwable t) {
            // Log or fallback
        }
    }

    @Override
    public void unregisterListener(Object packetListener) {
        if (packetListener == null || peApi == null) {
            return;
        }
        try {
            Object eventManager = peApi.getClass().getMethod("getEventManager").invoke(peApi);
            Class<?> listenerClass = Class.forName(PE_PKG + ".event.PacketListenerCommon");
            Method unregMethod = eventManager.getClass().getMethod("unregisterListener", listenerClass);
            unregMethod.invoke(eventManager, packetListener);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public int getClientProtocolVersion(Player player) {
        if (player == null || peApi == null) {
            return 0;
        }
        try {
            Object playerManager = peApi.getClass().getMethod("getPlayerManager").invoke(peApi);
            Object user = playerManager.getClass().getMethod("getUser", Object.class).invoke(playerManager, player);
            if (user != null) {
                Object clientVersion = user.getClass().getMethod("getClientVersion").invoke(user);
                if (clientVersion != null) {
                    Object versionInt = clientVersion.getClass().getMethod("getProtocolVersion").invoke(clientVersion);
                    if (versionInt instanceof Integer) {
                        return (Integer) versionInt;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    @Override
    public boolean isExternal() {
        return true;
    }

    @Override
    public String getVersionString() {
        if (peApi != null) {
            try {
                Object versionObj = peApi.getClass().getMethod("getVersion").invoke(peApi);
                if (versionObj != null) {
                    return versionObj.toString();
                }
            } catch (Throwable ignored) {
            }
        }
        return "external-unknown";
    }
}
