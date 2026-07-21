package com.siberanka.interactiveholograms.api.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.siberanka.interactiveholograms.api.InteractiveHolograms;
import com.siberanka.interactiveholograms.api.InteractiveHologramsAPI;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

@UtilityClass
public class BungeeUtils {

    private static final InteractiveHolograms INTERACTIVE_HOLOGRAMS = InteractiveHologramsAPI.get();
    private static final String BUNGEE_CORD_CHANNEL = "BungeeCord";
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        Messenger messenger = Bukkit.getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(INTERACTIVE_HOLOGRAMS.getPlugin(), BUNGEE_CORD_CHANNEL);
        initialized = true;
    }

    public static void destroy() {
        if (!initialized) return;
        Messenger messenger = Bukkit.getServer().getMessenger();
        messenger.unregisterOutgoingPluginChannel(INTERACTIVE_HOLOGRAMS.getPlugin(), BUNGEE_CORD_CHANNEL);
        initialized = false;
    }

    public static void connect(Player player, String server) {
        if (!initialized) init();
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(INTERACTIVE_HOLOGRAMS.getPlugin(), BUNGEE_CORD_CHANNEL, out.toByteArray());
        } catch (Exception e) {
            Log.warn("Failed to connect player %s to server %s.", e, player.getName(), server);
        }
    }

}
