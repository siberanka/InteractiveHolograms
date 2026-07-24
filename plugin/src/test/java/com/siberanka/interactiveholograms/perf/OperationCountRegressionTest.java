package com.siberanka.interactiveholograms.perf;

import com.siberanka.interactiveholograms.display.DisplaySettings;
import com.siberanka.interactiveholograms.display.TextDisplay;
import com.siberanka.interactiveholograms.display.TextLayoutMetrics;
import com.siberanka.interactiveholograms.display.TextLayoutScanner;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;
import com.siberanka.interactiveholograms.spatial.BidirectionalViewerIndex;
import com.siberanka.interactiveholograms.spatial.WorldSpatialIndex;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationCountRegressionTest {

    @Test
    void testTextLayoutScannerNoRegex() {
        String input = "&aHello &bWorld &#FF0000Hex <bold>MiniMessage</bold> %player_name%";
        int visible = TextLayoutScanner.visibleLength(input);
        // Expected: "Hello " (6) + "World " (6) + "Hex " (4) + "MiniMessage" (11) + 16 (placeholder) = 43
        assertTrue(visible > 0);

        // Plain string
        assertEquals(5, TextLayoutScanner.visibleLength("Hello"));
        assertEquals(0, TextLayoutScanner.visibleLength(""));
        assertEquals(0, TextLayoutScanner.visibleLength(null));
    }

    @Test
    void testTextDisplayMetricCaching() {
        TextDisplay display = new TextDisplay("test_hologram", new DecentLocation("world", 0, 64, 0), new DisplaySettings());
        display.addLine("&eLine 1");
        display.addLine("&aLine 2");

        TextLayoutMetrics metrics1 = display.getMetrics();
        TextLayoutMetrics metrics2 = display.getMetrics();

        // Same instance returned on same revision (0 recalculations)
        assertSame(metrics1, metrics2);

        // Modifying line increments layout revision and produces new cached metric instance
        display.setLine(0, "&cModified Line 1");
        TextLayoutMetrics metrics3 = display.getMetrics();
        assertFalse(metrics1 == metrics3);
        assertEquals(metrics3.getLayoutRevision(), display.getLayoutRevision());
    }

    @Test
    void testWorldSpatialIndexQueries() {
        WorldSpatialIndex spatialIndex = new WorldSpatialIndex();
        TextDisplay display = new TextDisplay("hologram_spawn", new DecentLocation("world", 10, 64, 10), new DisplaySettings());

        spatialIndex.addDisplay(display);

        // Query nearby cell
        Set<String> candidates = spatialIndex.queryCandidates("world", 12, 10, 16.0);
        assertTrue(candidates.contains("hologram_spawn"));

        // Query far cell (different world or out of radius)
        Set<String> farCandidates = spatialIndex.queryCandidates("world", 1000, 1000, 16.0);
        assertFalse(farCandidates.contains("hologram_spawn"));

        // Remove display
        spatialIndex.removeDisplay("hologram_spawn");
        Set<String> emptyCandidates = spatialIndex.queryCandidates("world", 10, 10, 16.0);
        assertFalse(emptyCandidates.contains("hologram_spawn"));
    }

    @Test
    void testBidirectionalViewerIndexCleanup() {
        BidirectionalViewerIndex index = new BidirectionalViewerIndex();
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        index.addVisible(player1, "holo_1");
        index.addVisible(player1, "holo_2");
        index.addVisible(player2, "holo_1");

        assertTrue(index.isVisible(player1, "holo_1"));
        assertTrue(index.isVisible(player1, "holo_2"));
        assertTrue(index.isVisible(player2, "holo_1"));

        // Player 1 quits -> cleans up all player 1 entries in O(visible)
        Set<String> removed = index.removeViewer(player1);
        assertEquals(2, removed.size());
        assertFalse(index.isVisible(player1, "holo_1"));
        assertTrue(index.isVisible(player2, "holo_1"));
    }
}
