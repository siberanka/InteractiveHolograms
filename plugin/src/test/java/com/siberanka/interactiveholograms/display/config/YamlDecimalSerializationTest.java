package com.siberanka.interactiveholograms.display.config;

import com.siberanka.interactiveholograms.display.attribute.value.primitives.FloatValue;
import com.siberanka.interactiveholograms.display.attribute.value.primitives.FloatValueType;
import com.siberanka.interactiveholograms.display.attribute.value.primitives.Vector3fValue;
import com.siberanka.interactiveholograms.display.attribute.value.primitives.Vector3fValueType;
import com.siberanka.interactiveholograms.display.config.serializer.DecentLocationSerializer;
import com.siberanka.interactiveholograms.display.config.serializer.DisplayVector3fSerializer;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;
import com.siberanka.interactiveholograms.platform.api.data.DecentVector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlDecimalSerializationTest {

    @Test
    void everyFloatBackedSerializerWritesYamlSupportedDoubleScalars(@TempDir Path directory) throws Exception {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(directory.resolve("serialized.yml"))
                .build();
        ConfigurationNode root = loader.createNode();

        new FloatValueType().serialize(new FloatValue(1.25f), root.node("float"));
        new Vector3fValueType().serialize(new Vector3fValue(1.0f, 2.0f, 3.0f), root.node("attribute-vector"));
        new DisplayVector3fSerializer().serialize(DecentVector3f.class,
                new DecentVector3f(4.0f, 5.0f, 6.0f), root.node("display-vector"));
        new DecentLocationSerializer().serialize(DecentLocation.class,
                new DecentLocation("world", 1.0d, 2.0d, 3.0d, 90.0f, 10.0f), root.node("location"));
        loader.save(root);

        assertTrue(root.node("float").raw() instanceof Double);
        assertTrue(root.node("attribute-vector", "x").raw() instanceof Double);
        assertTrue(root.node("display-vector", "x").raw() instanceof Double);
        assertTrue(root.node("location", "yaw").raw() instanceof Double);
        assertEquals(90.0f, root.node("location", "yaw").getFloat());
    }
}
