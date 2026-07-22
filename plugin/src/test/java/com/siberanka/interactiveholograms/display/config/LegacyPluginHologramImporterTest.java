package com.siberanka.interactiveholograms.display.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyPluginHologramImporterTest {
    @TempDir Path root;

    static Stream<Arguments> legacyFormats() {
        return Stream.of(
                Arguments.of(HologramImportSource.CMI,
                        "test:\n  Loc: world;1;64;2;90;0\n  Lines:\n    - Hello\n"),
                Arguments.of(HologramImportSource.FUTURE_HOLOGRAMS,
                        "test:\n  location: world,1,64,2\n  page1:\n    lines:\n      - Hello\n"),
                Arguments.of(HologramImportSource.GHOLO,
                        "H:\n  test:\n    l: world:1:64:2\n    c:\n      - Hello\n"),
                Arguments.of(HologramImportSource.HOLOGRAPHIC_DISPLAYS,
                        "test:\n  position:\n    world: world\n    x: 1\n    y: 64\n    z: 2\n  lines:\n    - Hello\n"),
                Arguments.of(HologramImportSource.HOLOGRAMS,
                        "holograms:\n  test:\n    location: world;1;64;2\n    lines:\n      - Hello\n")
        );
    }

    @ParameterizedTest
    @MethodSource("legacyFormats")
    void convertsEveryPreviouslySupportedFormatToModernYaml(HologramImportSource source, String yaml) throws Exception {
        Path input = root.resolve(source.getDisplayName() + ".yml");
        Files.write(input, yaml.getBytes(StandardCharsets.UTF_8));
        Path data = root.resolve("plugins/InteractiveHolograms");
        HologramImportService.ImportResult result = new HologramImportService(root, data)
                .importFrom(source, input.getFileName().toString(), false);
        assertEquals(1, result.getImported());
        YamlConfiguration output = YamlConfiguration.loadConfiguration(data.resolve("holograms/test.yml").toFile());
        assertEquals("TEXT", output.getString("type"));
        assertEquals("Hello", output.getStringList("text").get(0));
        assertEquals("world", output.getString("location.world"));
    }

    @Test
    void preservesCmiPagesAndMixedVisualLinesAsSeparateModernHolograms() throws Exception {
        Path input = root.resolve("cmi.yml");
        Files.write(input, ("shop:\n  Loc: world;0;64;0\n  Lines:\n    - Welcome\n    - 'ICON: DIAMOND'\n"
                + "    - '!nextpage!'\n    - Second page\n").getBytes(StandardCharsets.UTF_8));
        Path data = root.resolve("data");
        HologramImportService.ImportResult result = new HologramImportService(root, data)
                .importFrom(HologramImportSource.CMI, "cmi.yml", false);
        assertEquals(3, result.getImported());
        assertTrue(Files.isRegularFile(data.resolve("holograms/shop.yml")));
        assertTrue(Files.isRegularFile(data.resolve("holograms/shop_line2.yml")));
        assertTrue(Files.isRegularFile(data.resolve("holograms/shop_page2.yml")));
    }

    @Test
    void rejectsTraversalBeforeReading() {
        HologramImportService importer = new HologramImportService(root, root.resolve("data"));
        assertThrows(IOException.class, () -> importer.importFrom(HologramImportSource.CMI, "../outside.yml", false));
    }
}
