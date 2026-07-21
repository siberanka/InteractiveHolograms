package com.siberanka.interactiveholograms.display.config;

import org.junit.jupiter.api.Test;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;

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
        assertEquals(3, root.node("schema-version").getInt());
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
        assertEquals("hello", internal.node("pages", 0, "lines", 0).getString());
    }
}
