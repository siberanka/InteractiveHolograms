package com.siberanka.interactiveholograms.api;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

/**
 * This class is used to access InteractiveHologramsAPI. You can use this class
 * to get the instance of running InteractiveHolograms.
 *
 * @author d0by
 * @see InteractiveHolograms
 */
@UtilityClass
public final class InteractiveHologramsAPI {

    private static InteractiveHolograms implementation;
    private static boolean enabled = false;

    /**
     * <b>This is an internal method. Do not use it.</b>
     * <p>
     * Load InteractiveHologramsAPI. This method will be called by InteractiveHolograms
     * plugin when it is being loaded.
     *
     * @param plugin The instance of the InteractiveHolograms plugin.
     */
    @ApiStatus.Internal
    public static void onLoad(@NonNull JavaPlugin plugin) {
        if (implementation != null) {
            return;
        }
        implementation = new InteractiveHolograms(plugin);
    }

    /**
     * <b>This is an internal method. Do not use it.</b>
     * <p>
     * Enable InteractiveHologramsAPI. This method will be called by InteractiveHolograms
     * plugin when it is being enabled.
     */
    @ApiStatus.Internal
    public static void onEnable() {
        if (implementation == null) {
            return;
        }
        enabled = true;
        implementation.enable();
    }

    /**
     * <b>This is an internal method. Do not use it.</b>
     * <p>
     * Disable InteractiveHologramsAPI. This method will be called by InteractiveHolograms
     * plugin when it is being disabled.
     */
    @ApiStatus.Internal
    public static void onDisable() {
        if (implementation == null) {
            return;
        }
        implementation.disable();
        implementation = null;
        enabled = false;
    }

    /**
     * Check whether InteractiveHologramsAPI is currently running and ready for use.
     *
     * @return True if InteractiveHologramsAPI is running, false otherwise.
     */
    public static boolean isRunning() {
        return implementation != null && enabled;
    }

    /**
     * Get the instance of running InteractiveHolograms. This method will throw
     * an exception if InteractiveHologramsAPI is not running. You can check whether
     * InteractiveHologramsAPI is running by using {@link #isRunning()}.
     * <p>
     * You might need to wait until InteractiveHologramsAPI is fully enabled before
     * using this method. You can check whether InteractiveHologramsAPI is enabled
     * by using {@link #isRunning()}. Alternatively, you can use the
     * {@link PluginEnableEvent} event to detect when InteractiveHologramsAPI
     * is enabled.
     *
     * @return The instance of running InteractiveHolograms (if running).
     * @throws IllegalStateException If InteractiveHologramsAPI is not running.
     * @see #isRunning()
     * @see PluginEnableEvent
     */
    public static InteractiveHolograms get() {
        if (implementation == null || !enabled) {
            throw new IllegalStateException("InteractiveHolograms is not running (yet). Do you have InteractiveHolograms plugin installed?");
        }
        return implementation;
    }

}
