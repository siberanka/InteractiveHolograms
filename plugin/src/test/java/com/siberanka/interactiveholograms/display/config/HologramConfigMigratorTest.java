package com.siberanka.interactiveholograms.display.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HologramConfigMigratorTest {

    @Test
    void canonicalizeAddsDefaultsNormalizesAndRemovesUnknownKeys() {
        ConfigurationNode root = BasicConfigurationNode.root();
        root.node("type").raw("text");
        root.node("billboard").raw("sideways");
        root.node("block_brightness").raw(99);
        root.node("text").raw(Arrays.asList("hello"));
        root.node("unsafe_unknown").raw("remove me");

        boolean changed = new HologramConfigMigrator().canonicalize(root);

        assertTrue(changed);
        assertEquals(4, root.node("schema-version").getInt());
        assertEquals("TEXT", root.node("type").getString());
        assertEquals("CENTER", root.node("billboard").getString());
        assertEquals(15, root.node("block_brightness").getInt());
        assertFalse(root.node("location", "world").virtual());
        assertTrue(root.node("unsafe_unknown").virtual());
    }

    @Test
    void convertsFancyFlatKeysToTypedInternalAttributes() {
        ConfigurationNode root = BasicConfigurationNode.root();
        root.node("type").raw("TEXT");
        root.node("text").raw(Arrays.asList("hello"));
        root.node("scale_x").raw(2.0d);
        root.node("scale_y").raw(3.0d);
        root.node("scale_z").raw(4.0d);
        root.node("text_shadow").raw(true);

        HologramConfigMigrator migrator = new HologramConfigMigrator();
        migrator.canonicalize(root);
        ConfigurationNode internal = migrator.toInternal(root);

        assertEquals(2.0f, internal.node("attributes", "scale", "value", "x").getFloat());
        assertTrue(internal.node("attributes", "text-shadow", "value").getBoolean());
        assertEquals("hello", internal.node("pages", 0, "lines", 0, "content").getString());
        assertEquals(0.3d, internal.node("pages", 0, "lines", 0, "height").getDouble());
    }

    @Test
    void preservesMultiplePagesSpacingAndActions() {
        ConfigurationNode root = BasicConfigurationNode.root();
        root.node("schema-version").raw(3);
        root.node("type").raw("TEXT");
        root.node("pages", 0, "lines", 0, "content").raw("first");
        root.node("pages", 0, "lines", 0, "height").raw(0.5d);
        root.node("pages", 0, "actions", "LEFT").raw(Arrays.asList("NEXT_PAGE"));
        root.node("pages", 1, "lines", 0).raw("second");
        root.node("pages", 1, "actions", "LEFT").raw(Arrays.asList("PAGE:1"));

        HologramConfigMigrator migrator = new HologramConfigMigrator();
        assertTrue(migrator.canonicalize(root));
        ConfigurationNode internal = migrator.toInternal(root);

        assertEquals(4, internal.node("schema-version").getInt());
        assertEquals("first", internal.node("pages", 0, "lines", 0, "content").getString());
        assertEquals(0.5d, internal.node("pages", 0, "lines", 0, "height").getDouble());
        assertEquals("NEXT_PAGE", internal.node("pages", 0, "actions", "LEFT", 0).getString());
        assertEquals("second", internal.node("pages", 1, "lines", 0, "content").getString());
        assertEquals(0.3d, internal.node("pages", 1, "lines", 0, "height").getDouble());
        assertTrue(internal.node("text").virtual());
        assertFalse(migrator.canonicalize(root), "Canonical schema must not create a backup on every reload");
    }

    @Test
    void convertsYamlBackedDecimalsWithoutWritingUnsupportedFloatObjects(@TempDir Path directory) throws Exception {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(directory.resolve("hologram.yml"))
                .build();
        ConfigurationNode root = loader.createNode();
        root.node("type").raw("TEXT");
        root.node("text").raw(Arrays.asList("hello"));
        root.node("scale_x").raw(2.25d);
        root.node("shadow_radius").raw(0.75d);

        HologramConfigMigrator migrator = new HologramConfigMigrator();
        migrator.canonicalize(root);
        ConfigurationNode internal = migrator.toInternal(root);
        loader.save(internal);

        assertTrue(internal.node("attributes", "scale", "value", "x").raw() instanceof Double);
        assertTrue(internal.node("attributes", "shadow-radius", "value").raw() instanceof Double);
        assertEquals(2.25f, internal.node("attributes", "scale", "value", "x").getFloat());
        assertEquals(0.75f, internal.node("attributes", "shadow-radius", "value").getFloat());
    }
}
