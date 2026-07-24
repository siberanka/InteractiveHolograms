package com.siberanka.interactiveholograms.spatial;

import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WorldSpatialIndex {

    private static final int CELL_SHIFT = 4; // 16x16 block cells

    // worldName -> (cellKey -> Set<displayName>)
    private final Map<String, Map<Long, Set<String>>> worldIndex = new ConcurrentHashMap<>();
    // displayName -> Set<cellKey> indexed for that display
    private final Map<String, Set<Long>> displayCells = new ConcurrentHashMap<>();
    // displayName -> worldName
    private final Map<String, String> displayWorlds = new ConcurrentHashMap<>();

    public void addDisplay(DisplayBase display) {
        if (display == null || display.getLocation() == null) {
            return;
        }
        removeDisplay(display.getName());

        DecentLocation loc = display.getLocation();
        String worldName = loc.getWorldName();
        if (worldName == null) {
            return;
        }

        double range = display.getSettings().getDisplayRange();
        Set<Long> cells = calculateCells(loc.getX(), loc.getZ(), range);

        Map<Long, Set<String>> cellsMap = worldIndex.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>());
        for (Long cellKey : cells) {
            cellsMap.computeIfAbsent(cellKey, k -> ConcurrentHashMap.newKeySet()).add(display.getName());
        }

        displayCells.put(display.getName(), cells);
        displayWorlds.put(display.getName(), worldName);
    }

    public void removeDisplay(String displayName) {
        if (displayName == null) {
            return;
        }
        String worldName = displayWorlds.remove(displayName);
        Set<Long> cells = displayCells.remove(displayName);

        if (worldName != null && cells != null) {
            Map<Long, Set<String>> cellsMap = worldIndex.get(worldName);
            if (cellsMap != null) {
                for (Long cellKey : cells) {
                    Set<String> set = cellsMap.get(cellKey);
                    if (set != null) {
                        set.remove(displayName);
                    }
                }
            }
        }
    }

    public void updateDisplayLocation(DisplayBase display, DecentLocation oldLoc, DecentLocation newLoc) {
        if (display == null) {
            return;
        }
        addDisplay(display);
    }

    public Set<String> queryCandidates(String worldName, double x, double z, double queryRadius) {
        if (worldName == null) {
            return Collections.emptySet();
        }
        Map<Long, Set<String>> cellsMap = worldIndex.get(worldName);
        if (cellsMap == null || cellsMap.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> queryCells = calculateCells(x, z, queryRadius);
        Set<String> candidates = new HashSet<>();

        for (Long cellKey : queryCells) {
            Set<String> set = cellsMap.get(cellKey);
            if (set != null && !set.isEmpty()) {
                candidates.addAll(set);
            }
        }
        return candidates;
    }

    public void clear() {
        worldIndex.clear();
        displayCells.clear();
        displayWorlds.clear();
    }

    private static Set<Long> calculateCells(double centerX, double centerZ, double radius) {
        int minCellX = ((int) Math.floor(centerX - radius)) >> CELL_SHIFT;
        int maxCellX = ((int) Math.floor(centerX + radius)) >> CELL_SHIFT;
        int minCellZ = ((int) Math.floor(centerZ - radius)) >> CELL_SHIFT;
        int maxCellZ = ((int) Math.floor(centerZ + radius)) >> CELL_SHIFT;

        Set<Long> cells = new HashSet<>();
        for (int cx = minCellX; cx <= maxCellX; cx++) {
            for (int cz = minCellZ; cz <= maxCellZ; cz++) {
                cells.add(toCellKey(cx, cz));
            }
        }
        return cells;
    }

    public static long toCellKey(int cellX, int cellZ) {
        return (((long) cellX) & 0xFFFFFFFFL) | ((((long) cellZ) & 0xFFFFFFFFL) << 32);
    }
}
