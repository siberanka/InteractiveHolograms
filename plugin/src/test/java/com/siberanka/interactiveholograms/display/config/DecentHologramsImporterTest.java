package com.siberanka.interactiveholograms.display.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
        assertEquals("<gold>Hello", output.getStringList("text").get(0));
        assertEquals("MESSAGE:clicked", output.getStringList("actions.RIGHT").get(0));
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
        assertEquals(3, result.getImported());
        assertTrue(Files.isRegularFile(data.resolve("holograms/mixed.yml")));
        assertTrue(Files.isRegularFile(data.resolve("holograms/mixed_line2.yml")));
        assertTrue(Files.isRegularFile(data.resolve("holograms/mixed_page2.yml")));
    }
}
