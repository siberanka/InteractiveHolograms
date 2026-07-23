package com.siberanka.interactiveholograms.display.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecentHologramsImporterTest {
    @TempDir Path root;

    @Test
    void importsFirstPageTextLocationAndActions() throws Exception {
        Path source = root.resolve("plugins/DecentHolograms/holograms");
        Files.createDirectories(source);
        Files.write(source.resolve("welcome.yml"), ("location: world:1.5:65:2.5:90:0\n"
                + "enabled: true\ndisplay-range: 32\nupdate-interval: 20\n"
                + "pages:\n  - lines:\n      - content: '<gold>Hello'\n        height: 0.3\n"
                + "    actions:\n      RIGHT:\n        - 'MESSAGE:clicked'\n").getBytes(StandardCharsets.UTF_8));
        Path data = root.resolve("plugins/InteractiveHolograms");
        DecentHologramsImporter.ImportResult result = new DecentHologramsImporter(root, data).importYaml(null, false);
        assertEquals(1, result.getImported());
        YamlConfiguration output = YamlConfiguration.loadConfiguration(data.resolve("holograms/welcome.yml").toFile());
        assertEquals("TEXT", output.getString("type"));
        assertEquals("world", output.getString("location.world"));
        assertEquals("<gold>Hello", line(output, 0, 0).get("content"));
        assertEquals(0.3d, ((Number) line(output, 0, 0).get("height")).doubleValue());
        assertEquals("MESSAGE:clicked", actions(output, 0, "RIGHT").get(0));
    }

    @Test
    void rejectsPathTraversal() {
        DecentHologramsImporter importer = new DecentHologramsImporter(root, root.resolve("data"));
        assertThrows(IOException.class, () -> importer.importYaml("../outside", false));
    }

    @Test
    void overwriteCreatesBackup() throws Exception {
        Path source = root.resolve("legacy"); Files.createDirectories(source);
        Files.write(source.resolve("one.yml"), ("location: world:0:64:0\npages:\n  - lines:\n      - content: hi\n").getBytes(StandardCharsets.UTF_8));
        Path data = root.resolve("data"); Files.createDirectories(data.resolve("holograms"));
        Files.write(data.resolve("holograms/one.yml"), "old: true\n".getBytes(StandardCharsets.UTF_8));
        new DecentHologramsImporter(root, data).importYaml("legacy", true);
        try (java.util.stream.Stream<Path> files = Files.list(data.resolve("holograms"))) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("one.import-backup-")));
        }
    }

    @Test
    void convertsLegacyIconToPacketItemDisplay() throws Exception {
        Path source = root.resolve("legacy"); Files.createDirectories(source);
        Files.write(source.resolve("icon.yml"), ("location: world:0:64:0\npages:\n  - lines:\n      - content: '#ICON: DIAMOND'\n").getBytes(StandardCharsets.UTF_8));
        Path data = root.resolve("data");
        new DecentHologramsImporter(root, data).importYaml("legacy", false);
        YamlConfiguration output = YamlConfiguration.loadConfiguration(data.resolve("holograms/icon.yml").toFile());
        assertEquals("ITEM", output.getString("type"));
        assertEquals("minecraft:diamond", output.getString("item"));
    }

    @Test
    void preservesEveryPageAndMixedVisualLine() throws Exception {
        Path source = root.resolve("legacy"); Files.createDirectories(source);
        Files.write(source.resolve("mixed.yml"), ("location: world:0:64:0\npages:\n"
                + "  - lines:\n      - content: text\n      - content: '#ICON: DIAMOND'\n"
                + "  - lines:\n      - content: second\n").getBytes(StandardCharsets.UTF_8));
        Path data = root.resolve("data");
        DecentHologramsImporter.ImportResult result = new DecentHologramsImporter(root, data).importYaml("legacy", false);
        assertEquals(1, result.getImported());
        assertTrue(Files.isRegularFile(data.resolve("holograms/mixed.yml")));
        YamlConfiguration output = YamlConfiguration.loadConfiguration(
                data.resolve("holograms/mixed.yml").toFile());
        assertEquals("text", line(output, 0, 0).get("content"));
        assertEquals("#ICON: DIAMOND", line(output, 0, 1).get("content"));
        assertEquals("second", line(output, 1, 0).get("content"));
        assertTrue(Files.notExists(data.resolve("holograms/mixed_line2.yml")));
        assertTrue(Files.notExists(data.resolve("holograms/mixed_page2.yml")));
    }

    @Test
    void importsAllPagesHeightsAndNavigationActionsIntoOneYaml() throws Exception {
        Path source = root.resolve("legacy"); Files.createDirectories(source);
        Files.write(source.resolve("leaderboard.yml"), ("location: world:0:64:0\npages:\n"
                + "- lines:\n"
                + "  - content: '&6All time'\n"
                + "    height: 0.5\n"
                + "  - content: '&fFirst'\n"
                + "    height: 0.2\n"
                + "  actions:\n"
                + "    LEFT:\n"
                + "    - NEXT_PAGE\n"
                + "- lines:\n"
                + "  - content: '&eWeekly'\n"
                + "    height: 0.4\n"
                + "  actions:\n"
                + "    LEFT:\n"
                + "    - PAGE:1\n").getBytes(StandardCharsets.UTF_8));
        Path data = root.resolve("data");

        DecentHologramsImporter.ImportResult result =
                new DecentHologramsImporter(root, data).importYaml("legacy", false);

        assertEquals(1, result.getImported());
        YamlConfiguration output = YamlConfiguration.loadConfiguration(
                data.resolve("holograms/leaderboard.yml").toFile());
        assertEquals(2, output.getMapList("pages").size());
        assertEquals("&6All time", line(output, 0, 0).get("content"));
        assertEquals(0.5d, ((Number) line(output, 0, 0).get("height")).doubleValue());
        assertEquals("NEXT_PAGE", actions(output, 0, "LEFT").get(0));
        assertEquals("&eWeekly", line(output, 1, 0).get("content"));
        assertEquals("PAGE:1", actions(output, 1, "LEFT").get(0));
        String serialized = new String(Files.readAllBytes(
                data.resolve("holograms/leaderboard.yml")), StandardCharsets.UTF_8);
        assertTrue(serialized.contains("pages:\n- lines:\n  - content: '&6All time'\n    height: 0.5"));
        assertTrue(serialized.contains("  actions:\n    LEFT:\n    - NEXT_PAGE"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> line(YamlConfiguration output, int pageIndex, int lineIndex) {
        Map<String, Object> page = (Map<String, Object>) (Map<?, ?>)
                output.getMapList("pages").get(pageIndex);
        return ((List<Map<String, Object>>) page.get("lines")).get(lineIndex);
    }

    @SuppressWarnings("unchecked")
    private List<String> actions(YamlConfiguration output, int pageIndex, String click) {
        Map<String, Object> page = (Map<String, Object>) (Map<?, ?>)
                output.getMapList("pages").get(pageIndex);
        return (List<String>) ((Map<String, Object>) page.get("actions")).get(click);
    }
}
