package com.siberanka.interactiveholograms.display.render;

import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplayVisibility;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;
import com.siberanka.interactiveholograms.platform.api.player.PlatformPlayer;
import com.siberanka.interactiveholograms.spatial.BidirectionalViewerIndex;
import com.siberanka.interactiveholograms.spatial.WorldSpatialIndex;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class DisplayVisibilityService {

    private final WorldSpatialIndex spatialIndex = new WorldSpatialIndex();
    private final BidirectionalViewerIndex viewerIndex = new BidirectionalViewerIndex();

    public WorldSpatialIndex getSpatialIndex() {
        return spatialIndex;
    }

    public BidirectionalViewerIndex getViewerIndex() {
        return viewerIndex;
    }

    public void registerDisplay(DisplayBase display) {
        if (display != null) {
            spatialIndex.addDisplay(display);
        }
    }

    public void unregisterDisplay(DisplayBase display) {
        if (display != null) {
            spatialIndex.removeDisplay(display.getName());
            viewerIndex.removeDisplay(display.getName());
        }
    }

    public void onPlayerQuit(UUID playerId) {
        if (playerId != null) {
            viewerIndex.removeViewer(playerId);
        }
    }

    public Set<UUID> getVisibleViewers(DisplayBase display) {
        if (display == null) {
            return java.util.Collections.emptySet();
        }
        return viewerIndex.getVisibleViewersForDisplay(display.getName());
    }

    public Set<String> getVisibleDisplays(UUID viewerId) {
        return viewerIndex.getVisibleDisplaysForViewer(viewerId);
    }

    public boolean updateVisibilityState(DisplayBase display, PlatformPlayer player) {
        boolean shouldShow = shouldBeShownToPlayer(display, player);
        UUID viewerId = player.getUniqueId();
        String displayName = display.getName();

        if (shouldShow) {
            viewerIndex.addVisible(viewerId, displayName);
        } else {
            viewerIndex.removeVisible(viewerId, displayName);
        }
        return shouldShow;
    }

    public boolean shouldBeShownToPlayer(DisplayBase display, PlatformPlayer player) {
        return isDisplayEnabled(display)
                && isVisibleToPlayer(display, player)
                && isPlayerWithinDisplayRange(display, player);
    }

    private boolean isVisibleToPlayer(DisplayBase display, PlatformPlayer player) {
        DisplayVisibility visibility = display.getSettings().getVisibility();
        if (visibility == DisplayVisibility.MANUAL) {
            return display.isManualViewer(player.getUniqueId());
        }
        if (visibility == DisplayVisibility.PERMISSION_REQUIRED) {
            String permission = display.getSettings().getPermission();
            if (permission == null) {
                permission = "interactiveholograms.hologram." + display.getName() + ".view";
            }
            return player.hasPermission(permission);
        }
        return true;
    }

    private boolean isDisplayEnabled(DisplayBase display) {
        return display != null && display.getSettings().isEnabled();
    }

    private boolean isPlayerWithinDisplayRange(DisplayBase display, PlatformPlayer player) {
        double displayRange = display.getSettings().getDisplayRange();
        DecentLocation displayLocation = display.getLocation();
        DecentLocation playerLocation = player.getLocation();
        return displayLocation.isSameWorld(playerLocation)
                && displayLocation.distanceSquared(playerLocation) <= displayRange * displayRange;
    }
}
