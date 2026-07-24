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
import java.util.concurrent.atomic.AtomicInteger;

/** Owns client-side hitboxes for modern packet holograms. */
public final class DisplayInteractionService {

    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 64.0d;
    private static final int MAX_CLICKS_PER_SECOND = 20;

    private final JavaPlugin plugin;
    private final DisplayService displayService;
    private final DisplayVisibilityService visibilityService;
    private final NmsHologramRendererFactory rendererFactory;
    private final Map<String, HitboxHandle> hitboxes = new ConcurrentHashMap<>();
    private final Map<Integer, HitboxHandle> hitboxesByEntityId = new ConcurrentHashMap<>();
    private final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> clickRateLimiters = new ConcurrentHashMap<>();
    private final Map<UUID, Long> clickRateTimestamps = new ConcurrentHashMap<>();
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
        clickRateLimiters.clear();
        clickRateTimestamps.clear();
    }

    public void onPlayerQuit(UUID playerId) {
        if (playerId != null) {
            clickCooldowns.remove(playerId);
            clickRateLimiters.remove(playerId);
            clickRateTimestamps.remove(playerId);
            for (HitboxHandle handle : hitboxes.values()) {
                handle.removeViewer(playerId);
            }
        }
    }

    /** Fast Netty-thread lookup; rate-limited before scheduling tasks on main thread. */
    public boolean acceptClick(Player player, int entityId, ClickType clickType) {
        HitboxHandle matched = hitboxesByEntityId.get(entityId);
        if (matched == null || !matched.viewers.contains(player.getUniqueId())) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Netty thread rate limiting
        Long lastTime = clickRateTimestamps.putIfAbsent(playerId, now);
        AtomicInteger tokens = clickRateLimiters.computeIfAbsent(playerId, k -> new AtomicInteger(MAX_CLICKS_PER_SECOND));

        if (lastTime != null && now - lastTime >= 1000L) {
            tokens.set(MAX_CLICKS_PER_SECOND);
            clickRateTimestamps.put(playerId, now);
        }

        if (tokens.decrementAndGet() < 0) {
            return true; // Suppress packet processing without task scheduling
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
            HitboxHandle handle = hitboxes.computeIfAbsent(display.getName(), HitboxHandle::new);

            HitboxLayout layout = handle.getOrBuildLayout(display);
            handle.ensureRendererCount(layout.size());
            synchronizeViewers(display, handle, layout);
        }

        for (String name : new ArrayList<>(hitboxes.keySet())) {
            if (!liveNames.contains(name)) {
                removeHitbox(name);
            }
        }
    }

    /** Click hitboxes depend on actions, not the visual kind (text/item/block/model/mob). */
    public static boolean requiresPacketHitbox(DisplayBase display) {
        return display != null && display.hasActions() && display.getSettings().isEnabled();
    }

    private void synchronizeViewers(DisplayBase display, HitboxHandle handle, HitboxLayout layout) {
        Set<UUID> visibleViewers = visibilityService.getVisibleViewers(display);

        for (UUID playerId : visibleViewers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            boolean newlyAdded = handle.viewers.add(playerId);
            int bucket = layout.getOrientationBucket(display.getLocation(), player);

            if (newlyAdded) {
                List<DecentPosition> positions = layout.positions(display.getLocation(), bucket);
                handle.display(player, positions, bucket);
            } else if (handle.shouldUpdateMove(playerId, bucket)) {
                List<DecentPosition> positions = layout.positions(display.getLocation(), bucket);
                handle.move(player, positions, bucket);
            }
        }

        for (UUID viewer : new ArrayList<>(handle.viewers)) {
            if (!visibleViewers.contains(viewer) || Bukkit.getPlayer(viewer) == null) {
                handle.removeViewer(viewer);
            }
        }

        if (display instanceof TextDisplay) {
            ((TextDisplay) display).retainViewerPages(visibleViewers);
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
        private final Map<UUID, Integer> lastBuckets = new ConcurrentHashMap<>();
        private volatile HitboxLayout cachedLayout;
        private volatile long cachedLayoutRevision = -1;

        private HitboxHandle(String displayName) {
            this.displayName = displayName;
        }

        private HitboxLayout getOrBuildLayout(DisplayBase display) {
            long currentRev = display.getLayoutRevision();
            HitboxLayout layout = cachedLayout;
            if (layout == null || cachedLayoutRevision != currentRev) {
                layout = HitboxLayout.forDisplay(display);
                cachedLayout = layout;
                cachedLayoutRevision = currentRev;
            }
            return layout;
        }

        private boolean shouldUpdateMove(UUID viewerId, int currentBucket) {
            Integer previousBucket = lastBuckets.get(viewerId);
            return previousBucket == null || previousBucket != currentBucket;
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

        private void display(Player player, List<DecentPosition> positions, int bucket) {
            for (int index = 0; index < renderers.size(); index++) {
                renderers.get(index).display(player, positions.get(index));
            }
            lastPositions.put(player.getUniqueId(), positions);
            lastBuckets.put(player.getUniqueId(), bucket);
        }

        private void move(Player player, List<DecentPosition> positions, int bucket) {
            List<DecentPosition> previous = lastPositions.getOrDefault(
                    player.getUniqueId(), Collections.emptyList());
            for (int index = 0; index < renderers.size(); index++) {
                if (index >= previous.size()
                        || !positionEquals(positions.get(index), previous.get(index))) {
                    renderers.get(index).move(player, positions.get(index));
                }
            }
            lastPositions.put(player.getUniqueId(), positions);
            lastBuckets.put(player.getUniqueId(), bucket);
        }

        private void removeViewer(UUID viewerId) {
            if (viewers.remove(viewerId)) {
                Player player = Bukkit.getPlayer(viewerId);
                if (player != null) {
                    renderers.forEach(renderer -> renderer.hide(player));
                }
                lastPositions.remove(viewerId);
                lastBuckets.remove(viewerId);
            }
        }

        private void hideAll() {
            for (UUID viewer : new ArrayList<>(viewers)) {
                removeViewer(viewer);
            }
            viewers.clear();
            lastPositions.clear();
            lastBuckets.clear();
        }
    }

    static final class HitboxLayout {
        private static final double ARMOR_STAND_WIDTH = 0.55d;
        private static final double ARMOR_STAND_HEIGHT = 1.9d;
        private static final double ARMOR_STAND_CENTER_Y = 0.9875d;
        private static final double ORIENTATION_STEP = Math.PI / 12.0d;
        private static final int BUCKET_COUNT = 24;
        private static final int MAX_PACKET_ENTITIES = 64;

        private static final double[] SIN_LOOKUP = new double[BUCKET_COUNT];
        private static final double[] COS_LOOKUP = new double[BUCKET_COUNT];

        static {
            for (int i = 0; i < BUCKET_COUNT; i++) {
                double angle = i * ORIENTATION_STEP;
                SIN_LOOKUP[i] = Math.sin(angle);
                COS_LOOKUP[i] = Math.cos(angle);
            }
        }

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

        int getOrientationBucket(DecentLocation location, Player viewer) {
            org.bukkit.Location viewerLocation = viewer.getLocation();
            double dx = viewerLocation.getX() - location.getX();
            double dz = viewerLocation.getZ() - location.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            double angle = length < 0.001d ? Math.PI / 2.0d : Math.atan2(dz, dx);
            int bucket = (int) Math.round(angle / ORIENTATION_STEP);
            return (bucket % BUCKET_COUNT + BUCKET_COUNT) % BUCKET_COUNT;
        }

        List<DecentPosition> positions(DecentLocation location, int bucket) {
            double rightX = SIN_LOOKUP[bucket];
            double rightZ = -COS_LOOKUP[bucket];
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
