package com.siberanka.interactiveholograms.display.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Imports DecentHolograms' per-hologram YAML files into the packet display schema. */
public final class DecentHologramsImporter {
    private static final int MAX_FILES = 10_000;
    private static final long MAX_FILE_BYTES = 16L * 1024L * 1024L;
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final Path serverRoot;
    private final Path hologramsDirectory;

    public DecentHologramsImporter(Path serverRoot, Path dataFolder) {
        this.serverRoot = serverRoot.toAbsolutePath().normalize();
        this.hologramsDirectory = dataFolder.resolve("holograms").toAbsolutePath().normalize();
    }

    public ImportResult importYaml(String configuredPath, boolean overwrite) throws IOException {
        String pathValue = configuredPath == null || configuredPath.trim().isEmpty()
                ? "plugins/DecentHolograms/holograms" : configuredPath.trim();
        Path candidate = serverRoot.resolve(pathValue).normalize().toAbsolutePath();
        if (!candidate.startsWith(serverRoot) || !Files.exists(candidate)) {
            throw new IOException("DecentHolograms path was not found inside the server directory: " + candidate);
        }
        Path source = candidate.toRealPath();
        Path realRoot = serverRoot.toRealPath();
        if (!source.startsWith(realRoot)) throw new IOException("Import path must stay inside the server directory.");

        List<Path> files;
        if (Files.isRegularFile(source)) {
            files = Collections.singletonList(source);
        } else if (Files.isDirectory(source)) {
            try (Stream<Path> stream = Files.list(source)) {
                files = stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
                        .sorted(Comparator.comparing(Path::toString)).limit(MAX_FILES + 1L).collect(Collectors.toList());
            }
            if (files.size() > MAX_FILES) throw new IOException("Import contains more than " + MAX_FILES + " YAML files.");
        } else {
            throw new IOException("Import source must be a YAML file or directory.");
        }

        Files.createDirectories(hologramsDirectory);
        int imported = 0, skipped = 0, warnings = 0;
        for (Path file : files) {
            if (Files.size(file) > MAX_FILE_BYTES) { skipped++; warnings++; continue; }
            String filename = file.getFileName().toString();
            String name = sanitizeName(filename.substring(0, filename.length() - 4));
            if (name == null) { skipped++; continue; }
            YamlConfiguration input = YamlConfiguration.loadConfiguration(file.toFile());
            YamlConfiguration output = convert(input);
            if (output == null) { skipped++; warnings++; continue; }
            Path target = hologramsDirectory.resolve(name + ".yml").normalize();
            if (!target.startsWith(hologramsDirectory)) { skipped++; continue; }
            if (Files.exists(target) && !overwrite) { skipped++; continue; }
            if (Files.exists(target)) backup(target, name);
            saveAtomically(output, target);
            imported++;
            if (input.getList("pages", Collections.emptyList()).size() > 1) warnings++;
        }
        return new ImportResult(imported, skipped, warnings, source);
    }

    private YamlConfiguration convert(YamlConfiguration input) {
        String[] location = parseLocation(input.getString("location"));
        if (location == null) return null;
        List<?> pages = input.getList("pages", Collections.emptyList());
        if (pages.isEmpty() || !(pages.get(0) instanceof Map)) return null;
        Map<?, ?> page = (Map<?, ?>) pages.get(0);
        Object rawLines = page.get("lines");
        if (!(rawLines instanceof List)) return null;
        List<String> text = new ArrayList<>();
        for (Object rawLine : (List<?>) rawLines) {
            Object content = rawLine instanceof Map ? ((Map<?, ?>) rawLine).get("content") : rawLine;
            if (content != null) text.add(String.valueOf(content));
        }
        if (text.isEmpty()) return null;

        YamlConfiguration output = new YamlConfiguration();
        output.set("schema-version", HologramConfigMigrator.SCHEMA_VERSION);
        output.set("location.world", location[0]);
        output.set("location.x", number(location[1])); output.set("location.y", number(location[2])); output.set("location.z", number(location[3]));
        output.set("location.yaw", location.length > 4 ? number(location[4]) : 0.0d);
        output.set("location.pitch", location.length > 5 ? number(location[5]) : 0.0d);
        output.set("enabled", input.getBoolean("enabled", true));
        output.set("visibility_distance", input.getDouble("display-range", -1.0d));
        output.set("update_text_interval", input.getInt("update-interval", -1));
        output.set("permission", input.getString("permission", ""));
        output.set("billboard", facing(input.getString("facing", "CENTER")));
        configureContent(output, text);
        Object actions = page.get("actions");
        output.set("actions", actions instanceof Map ? actions : Collections.emptyMap());
        return output;
    }

    private void configureContent(YamlConfiguration output, List<String> lines) {
        String single = lines.size() == 1 ? lines.get(0).trim() : null;
        if (single != null && single.regionMatches(true, 0, "#ICON:", 0, 6)) {
            output.set("type", "ITEM");
            output.set("item", legacyMaterial(single.substring(6)));
            output.set("item_provider", "AUTO");
            return;
        }
        if (single != null && (single.regionMatches(true, 0, "#HEAD:", 0, 6)
                || single.regionMatches(true, 0, "#SMALLHEAD:", 0, 11))) {
            output.set("type", "ITEM"); output.set("item", "minecraft:player_head"); output.set("item_provider", "VANILLA");
            return;
        }
        if (single != null && single.regionMatches(true, 0, "#ENTITY:", 0, 8)) {
            String model = safeIdentifier(single.substring(8));
            output.set("type", "ITEM"); output.set("item", "minecraft:barrier"); output.set("item_provider", "VANILLA");
            output.set("scale_x", 0.0d); output.set("scale_y", 0.0d); output.set("scale_z", 0.0d);
            output.set("model_provider", "MYTHICMOBS"); output.set("model", model);
            return;
        }
        output.set("type", "TEXT"); output.set("text", lines);
    }

    private String legacyMaterial(String input) {
        String value = safeIdentifier(input).toLowerCase(Locale.ROOT);
        return value.contains(":") ? value : "minecraft:" + value;
    }

    private String safeIdentifier(String input) {
        String value = input == null ? "" : input.trim().split("[ {]", 2)[0];
        return value.matches("[A-Za-z0-9_.:-]{1,128}") ? value : "barrier";
    }

    private String[] parseLocation(String input) {
        if (input == null) return null;
        String[] parts = input.replace(',', '.').split(":");
        if (parts.length < 4 || parts[0].trim().isEmpty()) return null;
        try { for (int i = 1; i < Math.min(parts.length, 6); i++) Double.parseDouble(parts[i]); }
        catch (NumberFormatException ignored) { return null; }
        return parts;
    }

    private double number(String value) { return Double.parseDouble(value); }

    private String facing(String value) {
        String normalized = value == null ? "CENTER" : value.toUpperCase(Locale.ROOT);
        return "FIXED".equals(normalized) ? "FIXED" : "CENTER";
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

    private void backup(Path target, String name) throws IOException {
        Path backup = target.resolveSibling(name + ".import-backup-" + LocalDateTime.now().format(BACKUP_TIME) + ".yml");
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private String sanitizeName(String name) { return name.matches("[A-Za-z0-9_-]{1,64}") ? name : null; }

    public static final class ImportResult {
        private final int imported, skipped, warnings; private final Path source;
        private ImportResult(int imported, int skipped, int warnings, Path source) {
            this.imported = imported; this.skipped = skipped; this.warnings = warnings; this.source = source;
        }
        public int getImported() { return imported; }
        public int getSkipped() { return skipped; }
        public int getWarnings() { return warnings; }
        public Path getSource() { return source; }
    }
}
