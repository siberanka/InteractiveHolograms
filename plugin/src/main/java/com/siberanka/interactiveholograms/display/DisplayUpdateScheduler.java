package com.siberanka.interactiveholograms.display;

import com.siberanka.interactiveholograms.display.render.DisplayRenderCoordinator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

/**
 * Manages scheduled tasks for display updates with work budget enforcement.
 */
public class DisplayUpdateScheduler {

    private static final long LOGICAL_STATE_BASE_INTERVAL_MS = 50;
    private static final long VISIBILITY_BASE_INTERVAL_MS = 1000;
    private static final long POST_PROCESSING_BASE_INTERVAL_MS = 50;
    private static final int MAX_WORK_ITEMS_PER_TICK = 5000;

    private long lastLogicalStateTick = 0;
    private long lastVisibilityTick = 0;
    private long lastPostProcessingTick = 0;

    private final JavaPlugin plugin;
    private BukkitTask task;
    private final DisplayService displayService;
    private final DisplayRenderCoordinator renderCoordinator;

    public DisplayUpdateScheduler(JavaPlugin plugin, DisplayService displayService, DisplayRenderCoordinator renderCoordinator) {
        this.plugin = plugin;
        this.displayService = displayService;
        this.renderCoordinator = renderCoordinator;
    }

    public void start() {
        if (task == null) {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateDisplays, 1L, 1L);
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void updateDisplays() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastLogicalStateTick >= LOGICAL_STATE_BASE_INTERVAL_MS) {
            tickLogicalStates(currentTime);
            lastLogicalStateTick = currentTime;
        }

        if (currentTime - lastVisibilityTick >= VISIBILITY_BASE_INTERVAL_MS) {
            tickVisibility();
            lastVisibilityTick = currentTime;
        }

        if (currentTime - lastPostProcessingTick >= POST_PROCESSING_BASE_INTERVAL_MS) {
            tickPostProcessing();
            lastPostProcessingTick = currentTime;
        }
    }

    private void tickLogicalStates(long currentTime) {
        int processed = 0;
        Collection<DisplayBase> registered = displayService.getRegisteredDisplays();
        for (DisplayBase display : registered) {
            if (shouldUpdateLogicalState(display, currentTime)) {
                renderCoordinator.update(display);
                display.setLastLogicalUpdateMs(System.currentTimeMillis());
                processed++;
                if (processed >= MAX_WORK_ITEMS_PER_TICK) {
                    break;
                }
            }
        }
    }

    private void tickVisibility() {
        int processed = 0;
        Collection<DisplayBase> registered = displayService.getRegisteredDisplays();
        for (DisplayBase display : registered) {
            renderCoordinator.updateVisibility(display);
            processed++;
            if (processed >= MAX_WORK_ITEMS_PER_TICK) {
                break;
            }
        }
    }

    private void tickPostProcessing() {
        int processed = 0;
        Collection<DisplayBase> registered = displayService.getRegisteredDisplays();
        for (DisplayBase display : registered) {
            renderCoordinator.postProcess(display);
            processed++;
            if (processed >= MAX_WORK_ITEMS_PER_TICK) {
                break;
            }
        }
    }

    private boolean shouldUpdateLogicalState(DisplayBase display, long currentTime) {
        long lastUpdate = display.getLastLogicalUpdateMs();
        long intervalInTicks = display.getSettings().getUpdateInterval();

        if (intervalInTicks < 0) {
            return lastUpdate == 0;
        }

        return (currentTime - lastUpdate) >= ticksToMs(intervalInTicks);
    }

    private long ticksToMs(long intervalInTicks) {
        return intervalInTicks * 50L;
    }
}
