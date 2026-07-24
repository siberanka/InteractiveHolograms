package com.siberanka.interactiveholograms.spatial;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BidirectionalViewerIndex {

    // viewer UUID -> Set<displayName>
    private final Map<UUID, Set<String>> viewerToDisplays = new ConcurrentHashMap<>();
    // displayName -> Set<viewer UUID>
    private final Map<String, Set<UUID>> displayToViewers = new ConcurrentHashMap<>();

    public void addVisible(UUID viewerId, String displayName) {
        if (viewerId == null || displayName == null) {
            return;
        }
        viewerToDisplays.computeIfAbsent(viewerId, k -> ConcurrentHashMap.newKeySet()).add(displayName);
        displayToViewers.computeIfAbsent(displayName, k -> ConcurrentHashMap.newKeySet()).add(viewerId);
    }

    public boolean removeVisible(UUID viewerId, String displayName) {
        if (viewerId == null || displayName == null) {
            return false;
        }
        Set<String> displays = viewerToDisplays.get(viewerId);
        boolean removed = false;
        if (displays != null) {
            removed = displays.remove(displayName);
            if (displays.isEmpty()) {
                viewerToDisplays.remove(viewerId);
            }
        }
        Set<UUID> viewers = displayToViewers.get(displayName);
        if (viewers != null) {
            viewers.remove(viewerId);
            if (viewers.isEmpty()) {
                displayToViewers.remove(displayName);
            }
        }
        return removed;
    }

    public boolean isVisible(UUID viewerId, String displayName) {
        if (viewerId == null || displayName == null) {
            return false;
        }
        Set<String> displays = viewerToDisplays.get(viewerId);
        return displays != null && displays.contains(displayName);
    }

    public Set<String> getVisibleDisplaysForViewer(UUID viewerId) {
        if (viewerId == null) {
            return Collections.emptySet();
        }
        Set<String> displays = viewerToDisplays.get(viewerId);
        return displays == null ? Collections.emptySet() : Collections.unmodifiableSet(displays);
    }

    public Set<UUID> getVisibleViewersForDisplay(String displayName) {
        if (displayName == null) {
            return Collections.emptySet();
        }
        Set<UUID> viewers = displayToViewers.get(displayName);
        return viewers == null ? Collections.emptySet() : Collections.unmodifiableSet(viewers);
    }

    public Set<String> removeViewer(UUID viewerId) {
        if (viewerId == null) {
            return Collections.emptySet();
        }
        Set<String> visibleDisplays = viewerToDisplays.remove(viewerId);
        if (visibleDisplays != null) {
            for (String displayName : visibleDisplays) {
                Set<UUID> viewers = displayToViewers.get(displayName);
                if (viewers != null) {
                    viewers.remove(viewerId);
                    if (viewers.isEmpty()) {
                        displayToViewers.remove(displayName);
                    }
                }
            }
            return visibleDisplays;
        }
        return Collections.emptySet();
    }

    public Set<UUID> removeDisplay(String displayName) {
        if (displayName == null) {
            return Collections.emptySet();
        }
        Set<UUID> visibleViewers = displayToViewers.remove(displayName);
        if (visibleViewers != null) {
            for (UUID viewerId : visibleViewers) {
                Set<String> displays = viewerToDisplays.get(viewerId);
                if (displays != null) {
                    displays.remove(displayName);
                    if (displays.isEmpty()) {
                        viewerToDisplays.remove(viewerId);
                    }
                }
            }
            return visibleViewers;
        }
        return Collections.emptySet();
    }

    public void clear() {
        viewerToDisplays.clear();
        displayToViewers.clear();
    }
}
