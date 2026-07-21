package com.siberanka.interactiveholograms.display.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Imports the legacy FancyHolograms YAML storage without loading its plugin. */
public final class FancyHologramsImporter {

    private static final long MAX_FILE_BYTES = 32L * 1024L * 1024L;
    private static final int MAX_HOLOGRAMS = 10_000;
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final Path serverRoot;
    private final Path hologramsDirectory;

    public FancyHologramsImporter(Path serverRoot, Path dataFolder) {
        this.serverRoot = serverRoot.toAbsolutePath().normalize();
        this.hologramsDirectory = dataFolder.resolve("holograms").toAbsolutePath().normalize();
    }

    public ImportResult importYaml(String configuredPath, boolean overwrite) throws IOException {
        String pathValue = configuredPath == null || configuredPath.trim().isEmpty()
                ? "plugins/FancyHolograms/holograms.yml" : configuredPath.trim();
        Path candidate = serverRoot.resolve(pathValue).normalize().toAbsolutePath();
        if (!candidate.startsWith(serverRoot) || !Files.isRegularFile(candidate)) {
            throw new IOException("FancyHolograms file was not found inside the server directory: " + candidate);
        }
        Path source = candidate.toRealPath();
        if (!source.startsWith(serverRoot.toRealPath())) {
            throw new IOException("Import path must stay inside the server directory.");
        }
        if (Files.size(source) > MAX_FILE_BYTES) throw new IOException("FancyHolograms YAML exceeds the 32 MiB import limit.");

        YamlConfiguration input = YamlConfiguration.loadConfiguration(source.toFile());
        ConfigurationSection holograms = input.getConfigurationSection("holograms");
        if (holograms == null) {
            holograms = input;
        }
        if (holograms.getKeys(false).size() > MAX_HOLOGRAMS) {
            throw new IOException("Import contains more than " + MAX_HOLOGRAMS + " holograms.");
        }
        Files.createDirectories(hologramsDirectory);

        int imported = 0;
        int skipped = 0;
        for (String rawName : holograms.getKeys(false)) {
            ConfigurationSection section = holograms.getConfigurationSection(rawName);
            String name = sanitizeName(rawName);
            if (section == null || name == null) {
                skipped++;
                continue;
            }
            Path target = hologramsDirectory.resolve(name + ".yml").normalize();
            if (!target.startsWith(hologramsDirectory)) {
                skipped++;
                continue;
            }
            if (Files.exists(target)) {
                if (!overwrite) {
                    skipped++;
                    continue;
                }
                Path backup = target.resolveSibling(name + ".import-backup-"
                        + LocalDateTime.now().format(BACKUP_TIME) + ".yml");
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }

            YamlConfiguration output = new YamlConfiguration();
            for (Map.Entry<String, Object> entry : section.getValues(true).entrySet()) {
                if (!(entry.getValue() instanceof ConfigurationSection)) {
                    output.set(entry.getKey(), entry.getValue());
                }
            }
            output.set("schema-version", HologramConfigMigrator.SCHEMA_VERSION);
            output.set("enabled", true);
            normalizeFancyItem(output);
            saveAtomically(output, target);
            imported++;
        }
        return new ImportResult(imported, skipped, source);
    }

    private void saveAtomically(YamlConfiguration output, Path target) throws IOException {
        Path temporary = Files.createTempFile(hologramsDirectory, ".import-", ".yml");
        try {
            output.save(temporary.toFile());
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void normalizeFancyItem(YamlConfiguration output) {
        ItemStack stack = output.getItemStack("item");
        if (stack == null) {
            return;
        }
        String customId = findCraftEngineId(stack);
        output.set("item", customId == null
                ? "minecraft:" + stack.getType().name().toLowerCase(java.util.Locale.ROOT)
                : customId);
        output.set("item_provider", customId == null ? "VANILLA" : "CRAFTENGINE");
    }

    private String findCraftEngineId(ItemStack stack) {
        try {
            Class<?> api = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            Method method = api.getMethod("getCustomItemId", ItemStack.class);
            Object key = method.invoke(null, stack);
            return key == null ? null : key.toString();
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private String sanitizeName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]{1,64}") ? name : null;
    }

    public static final class ImportResult {
        private final int imported;
        private final int skipped;
        private final Path source;

        private ImportResult(int imported, int skipped, Path source) {
            this.imported = imported;
            this.skipped = skipped;
            this.source = source;
        }

        public int getImported() { return imported; }
        public int getSkipped() { return skipped; }
        public Path getSource() { return source; }
    }
}
