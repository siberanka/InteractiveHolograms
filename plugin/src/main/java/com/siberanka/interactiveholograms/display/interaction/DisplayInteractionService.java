package com.siberanka.interactiveholograms.display.interaction;

import com.siberanka.interactiveholograms.api.Settings;
import com.siberanka.interactiveholograms.api.actions.Action;
import com.siberanka.interactiveholograms.api.actions.ClickType;
import com.siberanka.interactiveholograms.api.utils.Log;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.display.render.DisplayVisibilityService;
import com.siberanka.interactiveholograms.nms.api.renderer.NmsClickableHologramRenderer;
import com.siberanka.interactiveholograms.nms.api.renderer.NmsHologramRendererFactory;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;
import com.siberanka.interactiveholograms.platform.bukkit.player.BukkitPlayer;
import com.siberanka.interactiveholograms.shared.DecentPosition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Owns client-side hitboxes for modern packet holograms. */
public final class DisplayInteractionService {

    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 64.0d;

    private final JavaPlugin plugin;
    private final DisplayService displayService;
    private final DisplayVisibilityService visibilityService;
    private final NmsHologramRendererFactory rendererFactory;
    private final Map<String, HitboxHandle> hitboxes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();
    private BukkitTask task;

    public DisplayInteractionService(JavaPlugin plugin,
                                     DisplayService displayService,
                                     DisplayVisibilityService visibilityService,
                                     NmsHologramRendererFactory rendererFactory) {
        this.plugin = plugin;
        this.displayService = displayService;
        this.visibilityService = visibilityService;
        this.rendererFactory = rendererFactory;
    }

    public void start() {
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::synchronize, 1L, 10L);
        }
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (HitboxHandle handle : hitboxes.values()) {
            handle.hideAll();
        }
        hitboxes.clear();
        clickCooldowns.clear();
    }

    /** Fast Netty-thread lookup; execution itself is always scheduled safely. */
    public boolean acceptClick(Player player, int entityId, ClickType clickType) {
        HitboxHandle matched = null;
        for (HitboxHandle handle : hitboxes.values()) {
            if (handle.renderer.getEntityId() == entityId && handle.viewers.contains(player.getUniqueId())) {
                matched = handle;
                break;
            }
        }
        if (matched == null) {
            return false;
        }
        String displayName = matched.displayName;
        Bukkit.getScheduler().runTask(plugin, () -> executeValidated(player, displayName, clickType));
        return true;
    }

    private void synchronize() {
        Collection<DisplayBase> displays = displayService.getRegisteredDisplays();
        Set<String> liveNames = ConcurrentHashMap.newKeySet();
        for (DisplayBase display : displays) {
            if (!requiresPacketHitbox(display)) {
                removeHitbox(display.getName());
                continue;
            }
            liveNames.add(display.getName());
            HitboxHandle handle = hitboxes.computeIfAbsent(display.getName(), name ->
                    new HitboxHandle(name, rendererFactory.createClickableRenderer()));
            synchronizeViewers(display, handle);
        }
        for (String name : new ArrayList<>(hitboxes.keySet())) {
            if (!liveNames.contains(name)) {
                removeHitbox(name);
            }
        }
        clickCooldowns.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
    }

    /** Click hitboxes depend on actions, not the visual kind (text/item/block/model/mob). */
    public static boolean requiresPacketHitbox(DisplayBase display) {
        return display != null && display.hasActions() && display.getSettings().isEnabled();
    }

    private void synchronizeViewers(DisplayBase display, HitboxHandle handle) {
        DecentPosition position = toPosition(display.getLocation());
        Set<UUID> online = ConcurrentHashMap.newKeySet();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            online.add(playerId);
            boolean visible = visibilityService.shouldBeShownToPlayer(display, new BukkitPlayer(player));
            if (visible && handle.viewers.add(playerId)) {
                handle.renderer.display(player, position);
            } else if (!visible && handle.viewers.remove(playerId)) {
                handle.renderer.hide(player);
            } else if (visible && !positionEquals(position, handle.lastPosition)) {
                handle.renderer.move(player, position);
            }
        }
        for (UUID viewer : new ArrayList<>(handle.viewers)) {
            if (!online.contains(viewer)) {
                handle.viewers.remove(viewer);
            }
        }
        handle.lastPosition = position;
    }

    private void executeValidated(Player player, String displayName, ClickType clickType) {
        if (!player.isOnline()) {
            return;
        }
        DisplayBase display = displayService.getDisplay(displayName);
        if (display == null || !display.hasActions()
                || !visibilityService.shouldBeShownToPlayer(display, new BukkitPlayer(player))) {
            return;
        }
        if (!isWithinInteractionDistance(player, display.getLocation())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldownMillis = Math.max(1, Settings.CLICK_COOLDOWN) * 50L;
        Long previous = clickCooldowns.putIfAbsent(playerId, now);
        if (previous != null && now - previous < cooldownMillis) {
            return;
        }
        clickCooldowns.put(playerId, now);
        executeActions(display, player, clickType);
    }

    static void executeActions(DisplayBase display, Player player, ClickType clickType) {
        for (Action action : display.getActions(clickType)) {
            try {
                if (!action.execute(player)) {
                    break;
                }
            } catch (RuntimeException exception) {
                Log.warn("Failed to execute action '%s' for hologram '%s'.", exception, action, display.getName());
                break;
            }
        }
    }

    private boolean isWithinInteractionDistance(Player player, DecentLocation target) {
        if (player.getWorld() == null || !player.getWorld().getName().equals(target.getWorldName())) {
            return false;
        }
        double dx = player.getLocation().getX() - target.getX();
        double dy = player.getEyeLocation().getY() - target.getY();
        double dz = player.getLocation().getZ() - target.getZ();
        return dx * dx + dy * dy + dz * dz <= MAX_INTERACTION_DISTANCE_SQUARED;
    }

    private void removeHitbox(String name) {
        HitboxHandle removed = hitboxes.remove(name);
        if (removed != null) {
            removed.hideAll();
        }
    }

    private DecentPosition toPosition(DecentLocation location) {
        return new DecentPosition(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    private boolean positionEquals(DecentPosition first, DecentPosition second) {
        return second != null
                && Double.compare(first.getX(), second.getX()) == 0
                && Double.compare(first.getY(), second.getY()) == 0
                && Double.compare(first.getZ(), second.getZ()) == 0
                && Float.compare(first.getYaw(), second.getYaw()) == 0
                && Float.compare(first.getPitch(), second.getPitch()) == 0;
    }

    private static final class HitboxHandle {
        private final String displayName;
        private final NmsClickableHologramRenderer renderer;
        private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
        private DecentPosition lastPosition;

        private HitboxHandle(String displayName, NmsClickableHologramRenderer renderer) {
            this.displayName = displayName;
            this.renderer = renderer;
        }

        private void hideAll() {
            for (UUID viewer : new ArrayList<>(viewers)) {
                Player player = Bukkit.getPlayer(viewer);
                if (player != null) {
                    renderer.hide(player);
                }
            }
            viewers.clear();
        }
    }
}
