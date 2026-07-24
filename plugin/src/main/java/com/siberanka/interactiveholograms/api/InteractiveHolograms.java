package com.siberanka.interactiveholograms.api;

import com.siberanka.interactiveholograms.api.animations.AnimationManager;
import com.siberanka.interactiveholograms.api.commands.CommandManager;
import com.siberanka.interactiveholograms.api.features.FeatureManager;
import com.siberanka.interactiveholograms.api.holograms.Hologram;
import com.siberanka.interactiveholograms.api.holograms.HologramManager;
import com.siberanka.interactiveholograms.api.listeners.PlayerListener;
import com.siberanka.interactiveholograms.api.listeners.WorldListener;
import com.siberanka.interactiveholograms.api.utils.BungeeUtils;
import com.siberanka.interactiveholograms.api.utils.Log;
import com.siberanka.interactiveholograms.api.utils.Common;
import com.siberanka.interactiveholograms.api.utils.UpdateChecker;
import com.siberanka.interactiveholograms.api.utils.event.EventFactory;
import com.siberanka.interactiveholograms.api.utils.reflect.Version;
import com.siberanka.interactiveholograms.api.utils.tick.Ticker;
import com.siberanka.interactiveholograms.display.DisplayModule;
import com.siberanka.interactiveholograms.event.InteractiveHologramsReloadEvent;
import com.siberanka.interactiveholograms.integration.IntegrationAvailabilityService;
import com.siberanka.interactiveholograms.nms.InteractiveHologramsNmsPacketListener;
import com.siberanka.interactiveholograms.nms.NmsAdapterFactory;
import com.siberanka.interactiveholograms.nms.NmsPacketListenerService;
import com.siberanka.interactiveholograms.nms.api.InteractiveHologramsNmsException;
import com.siberanka.interactiveholograms.nms.api.NmsAdapter;
import com.siberanka.interactiveholograms.platform.api.capability.MinecraftFeature;
import com.siberanka.interactiveholograms.platform.bukkit.BukkitPlatformAdapter;
import com.siberanka.interactiveholograms.platform.bukkit.player.BukkitPlayerListener;
import com.siberanka.interactiveholograms.platform.bukkit.player.BukkitPlayerService;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.util.logging.Logger;

/**
 * This is the main class of InteractiveHolograms. It contains all the methods
 * and fields that are used to manage InteractiveHolograms. You can get the instance
 * of this class by using {@link InteractiveHologramsAPI#get()}.
 *
 * @author d0by
 * @see InteractiveHologramsAPI
 */
@Getter
public final class InteractiveHolograms {

    private final JavaPlugin plugin;
    private NmsAdapter nmsAdapter;
    private IntegrationAvailabilityService integrationAvailabilityService;
    private NmsPacketListenerService nmsPacketListenerService;
    private HologramManager hologramManager;
    private CommandManager commandManager;
    private FeatureManager featureManager;
    private AnimationManager animationManager;
    private Ticker ticker;
    private DisplayModule displayModule;
    private volatile boolean updateAvailable;
    private volatile String latestReleaseUrl = UpdateChecker.LATEST_RELEASE_URL;

    InteractiveHolograms(@NonNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private Object packetInteractionListener;

    void enable() {
        Log.setLogger(plugin.getLogger());
        initializeNmsAdapter();
        Settings.reload();
        Lang.reload();

        PluginManager pluginManager = Bukkit.getPluginManager();
        this.integrationAvailabilityService = new IntegrationAvailabilityService(plugin, pluginManager);
        this.integrationAvailabilityService.initialize();
        this.ticker = new Ticker();
        this.hologramManager = new HologramManager(this);
        this.commandManager = new CommandManager();
        this.featureManager = new FeatureManager();
        this.animationManager = new AnimationManager(this);
        InteractiveHologramsNmsPacketListener nmsPacketListener = new InteractiveHologramsNmsPacketListener(hologramManager);
        this.nmsPacketListenerService = new NmsPacketListenerService(plugin, nmsAdapter, nmsPacketListener);
        BukkitPlatformAdapter platformAdapter = new BukkitPlatformAdapter(plugin, nmsAdapter.getDisplayRendererFactory());
        if (platformAdapter.getCapabilities().supports(MinecraftFeature.DISPLAY_ENTITIES)) {
            this.displayModule = new DisplayModule(plugin, animationManager, platformAdapter);
            this.displayModule.initialize();
            nmsPacketListener.setDisplayInteractionService(this.displayModule.getInteractionService());

            if (plugin instanceof com.siberanka.interactiveholograms.plugin.InteractiveHologramsPlugin) {
                com.siberanka.interactiveholograms.packet.PacketRuntime runtime =
                        ((com.siberanka.interactiveholograms.plugin.InteractiveHologramsPlugin) plugin).getPacketRuntime();
                if (runtime != null && runtime.getBackend() != null) {
                    this.packetInteractionListener = new com.siberanka.interactiveholograms.packet.PacketInteractionListener(this.displayModule.getInteractionService());
                    runtime.getBackend().registerListener(this.packetInteractionListener);
                }
            }
        } else {
            // Legacy protocol versions do not have display entities. They keep
            // using the packet-only armor-stand renderer and Decent YAML model.
            com.siberanka.interactiveholograms.api.utils.scheduler.S.async(hologramManager::reload);
        }

        pluginManager.registerEvents(new PlayerListener(this), this.plugin);
        pluginManager.registerEvents(new WorldListener(hologramManager), this.plugin);
        BukkitPlayerService playerService = (BukkitPlayerService) platformAdapter.getPlayerService();
        pluginManager.registerEvents(new BukkitPlayerListener(playerService), this.plugin);

        BungeeUtils.init();
        checkForUpdates();
    }

    private void checkForUpdates() {
        if (!Settings.CHECK_FOR_UPDATES) return;
        String currentVersion = plugin.getDescription().getVersion();
        new UpdateChecker(plugin).getLatestRelease(release -> {
            latestReleaseUrl = release.getUrl();
            updateAvailable = Common.isVersionHigher(currentVersion, release.getVersion());
            if (updateAvailable) {
                Log.info("InteractiveHolograms %s is available: %s", release.getVersion(), release.getUrl());
            }
        });
    }

    void disable() {
        if (plugin instanceof com.siberanka.interactiveholograms.plugin.InteractiveHologramsPlugin) {
            com.siberanka.interactiveholograms.packet.PacketRuntime runtime =
                    ((com.siberanka.interactiveholograms.plugin.InteractiveHologramsPlugin) plugin).getPacketRuntime();
            if (runtime != null && runtime.getBackend() != null && this.packetInteractionListener != null) {
                runtime.getBackend().unregisterListener(this.packetInteractionListener);
                this.packetInteractionListener = null;
            }
        }

        if (this.displayModule != null) {
            this.displayModule.shutdown();
        }
        this.nmsPacketListenerService.shutdown();
        this.featureManager.destroy();
        this.hologramManager.destroy();
        this.animationManager.destroy();
        this.ticker.destroy();

        for (Hologram hologram : Hologram.getCachedHolograms()) {
            hologram.destroy();
        }

        this.integrationAvailabilityService.shutdown();
        BungeeUtils.destroy();
    }

    /**
     * Reload the plugin, this method also calls the reload event.
     *
     * @see InteractiveHologramsReloadEvent
     */
    public void reload() {
        Settings.reload();
        Lang.reload();

        this.animationManager.reload();
        this.featureManager.reload();
        if (this.displayModule != null) {
            this.displayModule.reload();
        } else {
            this.hologramManager.reload();
        }

        EventFactory.fireReloadEvent();
    }

    private void initializeNmsAdapter() {
        try {
            nmsAdapter = new NmsAdapterFactory().createNmsAdapter(Version.CURRENT);
            Log.info("Initialized NMS adapter for %s (%s).", Version.CURRENT.name(), Version.CURRENT_MINECRAFT_VERSION);
            return;
        } catch (InteractiveHologramsNmsException e) {
            Log.error("Error loading an NMS adapter for " + Version.CURRENT + ": " + e.getMessage(), e);
        } catch (Exception e) {
            Log.error("Unknown error loading an NMS adapter for " + Version.CURRENT, e);
        }
        Log.error("The plugin will now be disabled.");
        Bukkit.getPluginManager().disablePlugin(plugin);
    }

    @Contract(pure = true)
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Contract(pure = true)
    public Logger getLogger() {
        return plugin.getLogger();
    }

}
