package com.siberanka.interactiveholograms.plugin;

import com.siberanka.interactiveholograms.api.InteractiveHolograms;
import com.siberanka.interactiveholograms.api.InteractiveHologramsAPI;
import com.siberanka.interactiveholograms.api.commands.CommandManager;
import com.siberanka.interactiveholograms.api.commands.DecentCommand;
import com.siberanka.interactiveholograms.api.utils.reflect.Version;
import com.siberanka.interactiveholograms.display.DisplayModule;
import com.siberanka.interactiveholograms.hook.NbtApiHook;
import com.siberanka.interactiveholograms.packet.PacketRuntime;
import com.siberanka.interactiveholograms.plugin.commands.HologramsCommand;
import com.siberanka.interactiveholograms.plugin.features.DamageDisplayFeature;
import com.siberanka.interactiveholograms.plugin.features.HealingDisplayFeature;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class InteractiveHologramsPlugin extends JavaPlugin {

    private PacketRuntime packetRuntime;
    private boolean unsupportedServerVersion = false;

    @Override
    public void onLoad() {
        if (Version.CURRENT == null) {
            unsupportedServerVersion = true;
            return;
        }

        packetRuntime = new PacketRuntime(this);
        packetRuntime.onLoad();
        InteractiveHologramsAPI.onLoad(this);
    }

    @Override
    public void onEnable() {
        if (unsupportedServerVersion) {
            getLogger().severe("Unsupported server version detected: " + Bukkit.getServer().getVersion());
            getLogger().severe("Plugin will now be disabled.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (packetRuntime != null) {
            packetRuntime.onEnable();
        }
        InteractiveHologramsAPI.onEnable();

        InteractiveHolograms interactiveHolograms = InteractiveHologramsAPI.get();
        interactiveHolograms.getFeatureManager().registerFeature(new DamageDisplayFeature());
        interactiveHolograms.getFeatureManager().registerFeature(new HealingDisplayFeature());

        CommandManager commandManager = interactiveHolograms.getCommandManager();
        DisplayModule displayModule = interactiveHolograms.getDisplayModule();
        DecentCommand mainCommand = new HologramsCommand(
                displayModule == null ? null : displayModule.getDisplaysCommand(),
                displayModule == null ? null : displayModule.getHologramImportService());
        commandManager.setMainCommand(mainCommand);
        commandManager.registerCommand(mainCommand);

        // Enable NBT API to avoid lag spikes when parsing NBT for the first time.
        NbtApiHook.initialize();
    }

    @Override
    public void onDisable() {
        if (unsupportedServerVersion) {
            return;
        }

        InteractiveHologramsAPI.onDisable();
        if (packetRuntime != null) {
            packetRuntime.onDisable();
        }
    }

    public PacketRuntime getPacketRuntime() {
        return packetRuntime;
    }

}
