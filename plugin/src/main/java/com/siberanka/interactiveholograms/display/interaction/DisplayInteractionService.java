package com.siberanka.interactiveholograms.display.interaction;

import com.siberanka.interactiveholograms.api.Settings;
import com.siberanka.interactiveholograms.api.actions.Action;
import com.siberanka.interactiveholograms.api.actions.ActionType;
import com.siberanka.interactiveholograms.api.actions.ClickType;
import com.siberanka.interactiveholograms.api.utils.Log;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.display.TextDisplay;
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
import java.util.Collections;
import java.util.List;
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
    private final Map<Integer, HitboxHandle> hitboxesByEntityId = new ConcurrentHashMap<>();
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
        hitboxesByEntityId.clear();
        clickCooldowns.clear();
    }

    /** Fast Netty-thread lookup; execution itself is always scheduled safely. */
    public boolean acceptClick(Player player, int entityId, ClickType clickType) {
        HitboxHandle matched = hitboxesByEntityId.get(entityId);
        if (matched == null || !matched.viewers.contains(player.getUniqueId())) {
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
                    new HitboxHandle(name));
            handle.ensureRendererCount(HitboxLayout.forDisplay(display).size());
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
        HitboxLayout layout = HitboxLayout.forDisplay(display);
        Set<UUID> online = ConcurrentHashMap.newKeySet();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            online.add(playerId);
            boolean visible = visibilityService.shouldBeShownToPlayer(display, new BukkitPlayer(player));
            List<DecentPosition> positions = layout.positions(display.getLocation(), player);
            if (visible && handle.viewers.add(playerId)) {
                handle.display(player, positions);
            } else if (!visible && handle.viewers.remove(playerId)) {
                handle.hide(player);
            } else if (visible) {
                handle.move(player, positions);
            }
        }
        for (UUID viewer : new ArrayList<>(handle.viewers)) {
            if (!online.contains(viewer)) {
                handle.viewers.remove(viewer);
                handle.lastPositions.remove(viewer);
            }
        }
        if (display instanceof TextDisplay) {
            ((TextDisplay) display).retainViewerPages(online);
        }
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
        boolean pageChanged = executeActions(display, player, clickType);
        if (pageChanged) {
            displayService.updateDisplayContent(display, new BukkitPlayer(player));
        }
    }

    static boolean executeActions(DisplayBase display, Player player, ClickType clickType) {
        List<Action> actions = display instanceof TextDisplay
                ? ((TextDisplay) display).getActions(player.getUniqueId(), clickType)
                : display.getActions(clickType);
        boolean pageChanged = false;
        for (Action action : actions) {
            try {
                if (display instanceof TextDisplay && isPageAction(action)) {
                    pageChanged |= executePageAction((TextDisplay) display, player.getUniqueId(), action);
                } else if (!action.execute(player)) {
                    break;
                }
            } catch (RuntimeException exception) {
                Log.warn("Failed to execute action '%s' for hologram '%s'.", exception, action, display.getName());
                break;
            }
        }
        return pageChanged;
    }

    private static boolean isPageAction(Action action) {
        return action.getType() == ActionType.NEXT_PAGE
                || action.getType() == ActionType.PREV_PAGE
                || action.getType() == ActionType.PAGE;
    }

    private static boolean executePageAction(TextDisplay display, UUID viewerId, Action action) {
        if (action.getType() == ActionType.NEXT_PAGE) {
            return display.nextPage(viewerId);
        }
        if (action.getType() == ActionType.PREV_PAGE) {
            return display.previousPage(viewerId);
        }
        String data = action.getData();
        if (data == null || data.trim().isEmpty()) {
            return false;
        }
        String[] parts = data.split(":");
        try {
            return display.setViewerPage(viewerId, Integer.parseInt(parts[parts.length - 1]) - 1);
        } catch (NumberFormatException ignored) {
            return false;
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
            removed.unregisterEntityIds();
        }
    }

    private boolean positionEquals(DecentPosition first, DecentPosition second) {
        return second != null
                && Double.compare(first.getX(), second.getX()) == 0
                && Double.compare(first.getY(), second.getY()) == 0
                && Double.compare(first.getZ(), second.getZ()) == 0
                && Float.compare(first.getYaw(), second.getYaw()) == 0
                && Float.compare(first.getPitch(), second.getPitch()) == 0;
    }

    private final class HitboxHandle {
        private final String displayName;
        private final List<NmsClickableHologramRenderer> renderers = new ArrayList<>();
        private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();
        private final Map<UUID, List<DecentPosition>> lastPositions = new ConcurrentHashMap<>();

        private HitboxHandle(String displayName) {
            this.displayName = displayName;
        }

        private void ensureRendererCount(int count) {
            if (renderers.size() == count) {
                return;
            }
            hideAll();
            unregisterEntityIds();
            renderers.clear();
            for (int index = 0; index < count; index++) {
                NmsClickableHologramRenderer renderer = rendererFactory.createClickableRenderer();
                renderers.add(renderer);
                hitboxesByEntityId.put(renderer.getEntityId(), this);
            }
        }

        private void unregisterEntityIds() {
            renderers.forEach(renderer -> hitboxesByEntityId.remove(renderer.getEntityId(), this));
        }

        private void display(Player player, List<DecentPosition> positions) {
            for (int index = 0; index < renderers.size(); index++) {
                renderers.get(index).display(player, positions.get(index));
            }
            lastPositions.put(player.getUniqueId(), positions);
        }

        private void move(Player player, List<DecentPosition> positions) {
            List<DecentPosition> previous = lastPositions.getOrDefault(
                    player.getUniqueId(), Collections.emptyList());
            for (int index = 0; index < renderers.size(); index++) {
                if (index >= previous.size()
                        || !positionEquals(positions.get(index), previous.get(index))) {
                    renderers.get(index).move(player, positions.get(index));
                }
            }
            lastPositions.put(player.getUniqueId(), positions);
        }

        private void hide(Player player) {
            renderers.forEach(renderer -> renderer.hide(player));
            lastPositions.remove(player.getUniqueId());
        }

        private void hideAll() {
            for (UUID viewer : new ArrayList<>(viewers)) {
                Player player = Bukkit.getPlayer(viewer);
                if (player != null) {
                    hide(player);
                }
            }
            viewers.clear();
            lastPositions.clear();
        }
    }

    static final class HitboxLayout {
        private static final double ARMOR_STAND_WIDTH = 0.55d;
        private static final double ARMOR_STAND_HEIGHT = 1.9d;
        private static final double ARMOR_STAND_CENTER_Y = 0.9875d;
        private static final double ORIENTATION_STEP = Math.PI / 12.0d;
        private static final int MAX_PACKET_ENTITIES = 64;
        private final double width;
        private final double height;
        private final int columns;
        private final int rows;

        private HitboxLayout(double width, double height, int columns, int rows) {
            this.width = width;
            this.height = height;
            this.columns = columns;
            this.rows = rows;
        }

        static HitboxLayout forDisplay(DisplayBase display) {
            double width = display.getSettings().getHitboxWidth();
            double height = display.getSettings().getHitboxHeight();
            if (display instanceof TextDisplay) {
                TextDisplay text = (TextDisplay) display;
                width = Math.max(width, text.getMaximumVisibleLineLength() * 0.1d);
                height = Math.max(height, text.getMaximumPageHeight());
            }
            width = Math.max(0.1d, Math.min(16.0d, width));
            height = Math.max(0.1d, Math.min(16.0d, height));
            int columns = Math.max(1, Math.min(24, (int) Math.ceil(width / ARMOR_STAND_WIDTH)));
            int rows = Math.max(1, Math.min((int) Math.ceil(height / ARMOR_STAND_HEIGHT),
                    MAX_PACKET_ENTITIES / columns));
            return new HitboxLayout(width, height, columns, rows);
        }

        int size() {
            return columns * rows;
        }

        double getWidth() {
            return width;
        }

        double getHeight() {
            return height;
        }

        List<DecentPosition> positions(DecentLocation location, Player viewer) {
            org.bukkit.Location viewerLocation = viewer.getLocation();
            double dx = viewerLocation.getX() - location.getX();
            double dz = viewerLocation.getZ() - location.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            double angle = length < 0.001d ? Math.PI / 2.0d : Math.atan2(dz, dx);
            double quantizedAngle = Math.rint(angle / ORIENTATION_STEP) * ORIENTATION_STEP;
            double rightX = Math.sin(quantizedAngle);
            double rightZ = -Math.cos(quantizedAngle);
            List<DecentPosition> positions = new ArrayList<>(size());
            double columnSpacing = width / columns;
            double rowSpacing = height / rows;
            for (int row = 0; row < rows; row++) {
                double y = location.getY() - height / 2.0d
                        + (row + 0.5d) * rowSpacing - ARMOR_STAND_CENTER_Y;
                for (int column = 0; column < columns; column++) {
                    double horizontal = -width / 2.0d + (column + 0.5d) * columnSpacing;
                    positions.add(new DecentPosition(
                            location.getX() + rightX * horizontal,
                            y,
                            location.getZ() + rightZ * horizontal,
                            location.getYaw(),
                            location.getPitch()));
                }
            }
            return positions;
        }
    }
}
