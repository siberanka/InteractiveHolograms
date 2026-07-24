package com.siberanka.interactiveholograms.display.config;

import com.siberanka.interactiveholograms.display.config.dto.ConfigDisplay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DisplayRepositoryTest {

    @Test
    void saveThrowsDisplayConfigExceptionWhenWriteFails(@TempDir Path tempDir) throws IOException {
        YamlConfigurationLoaderFactory loaderFactory = new YamlConfigurationLoaderFactory(TypeSerializerCollection.defaults());
        DisplayRepository repository = new DisplayRepository(tempDir, loaderFactory);

        // Create directory named 'test.yml' to cause a write/move failure
        Path hologramsDir = tempDir.resolve("holograms");
        Files.createDirectories(hologramsDir);
        Path blockingDir = hologramsDir.resolve("test.yml.tmp");
        Files.createDirectories(blockingDir);

        ConfigDisplay config = new ConfigDisplay();
        config.setName("test");

        assertThrows(DisplayConfigException.class, () -> repository.save("test", config));
    }
}
