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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Imports DecentHolograms' per-hologram YAML files into the packet display schema. */
public final class DecentHologramsImporter {
    private static final int MAX_FILES = 10_000;
    private static final long MAX_FILE_BYTES = 16L * 1024L * 1024L;
    private static final int MAX_PAGES = 128;
    private static final int MAX_LINES_PER_PAGE = 512;
    private static final int MAX_CONTENT_LENGTH = 32_767;
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
            List<Converted> outputs = convert(input);
            if (outputs.isEmpty()) { skipped++; warnings++; continue; }
            if (imported + outputs.size() > MAX_FILES) throw new IOException("Import would create more than " + MAX_FILES + " holograms.");
            for (Converted converted : outputs) {
                String outputName = sanitizeName(name + converted.suffix);
                if (outputName == null) { skipped++; warnings++; continue; }
                Path target = hologramsDirectory.resolve(outputName + ".yml").normalize();
                if (!target.startsWith(hologramsDirectory)) { skipped++; continue; }
                if (Files.exists(target) && !overwrite) { skipped++; continue; }
                if (Files.exists(target)) backup(target, outputName);
                saveAtomically(converted.yaml, target);
                imported++;
            }
        }
        return new ImportResult(imported, skipped, warnings, source);
    }

    private List<Converted> convert(YamlConfiguration input) {
        String[] location = parseLocation(input.getString("location"));
        if (location == null) return Collections.emptyList();
        List<?> pages = input.getList("pages", Collections.emptyList());
        if (pages.isEmpty()) return Collections.emptyList();
        List<PageData> convertedPages = new ArrayList<>();
        for (Object rawPage : pages.subList(0, Math.min(pages.size(), MAX_PAGES))) {
            if (!(rawPage instanceof Map)) continue;
            Map<?, ?> page = (Map<?, ?>) rawPage;
            Object rawLines = page.get("lines");
            if (!(rawLines instanceof List)) continue;
            List<LineData> lines = new ArrayList<>();
            List<?> sourceLines = (List<?>) rawLines;
            for (Object rawLine : sourceLines.subList(0, Math.min(sourceLines.size(), MAX_LINES_PER_PAGE))) {
                Object content = rawLine instanceof Map ? ((Map<?, ?>) rawLine).get("content") : rawLine;
                if (content == null) continue;
                String value = String.valueOf(content);
                if (value.length() > MAX_CONTENT_LENGTH) {
                    value = value.substring(0, MAX_CONTENT_LENGTH);
                }
                double height = rawLine instanceof Map
                        ? normalizedHeight(((Map<?, ?>) rawLine).get("height")) : 0.3d;
                lines.add(new LineData(value, height));
            }
            if (!lines.isEmpty()) {
                convertedPages.add(new PageData(lines, normalizeActions(page.get("actions"))));
            }
        }
        if (convertedPages.isEmpty()) return Collections.emptyList();
        return Collections.singletonList(new Converted("", createOutput(input, location, convertedPages)));
    }

    private YamlConfiguration createOutput(YamlConfiguration input, String[] location, List<PageData> pages) {
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
        if (pages.size() == 1 && pages.get(0).lines.size() == 1
                && isVisual(pages.get(0).lines.get(0).content)) {
            configureVisualContent(output, pages.get(0).lines.get(0).content);
            output.set("actions", pages.get(0).actions);
        } else {
            output.set("type", "TEXT");
            output.set("pages", toYamlPages(pages));
            output.set("actions", Collections.emptyMap());
        }
        return output;
    }

    private boolean isVisual(String line) {
        String value = line == null ? "" : line.trim().toUpperCase(Locale.ROOT);
        return value.startsWith("#ICON:") || value.startsWith("#HEAD:")
                || value.startsWith("#SMALLHEAD:") || value.startsWith("#ENTITY:");
    }

    private void configureVisualContent(YamlConfiguration output, String content) {
        String single = content.trim();
        if (single.regionMatches(true, 0, "#ICON:", 0, 6)) {
            output.set("type", "ITEM");
            output.set("item", legacyMaterial(single.substring(6)));
            output.set("item_provider", "AUTO");
            return;
        }
        if (single.regionMatches(true, 0, "#HEAD:", 0, 6)
                || single.regionMatches(true, 0, "#SMALLHEAD:", 0, 11)) {
            output.set("type", "ITEM"); output.set("item", "minecraft:player_head"); output.set("item_provider", "VANILLA");
            return;
        }
        if (single.regionMatches(true, 0, "#ENTITY:", 0, 8)) {
            String model = safeIdentifier(single.substring(8));
            output.set("type", "ITEM"); output.set("item", "minecraft:barrier"); output.set("item_provider", "VANILLA");
            output.set("scale_x", 0.0d); output.set("scale_y", 0.0d); output.set("scale_z", 0.0d);
            output.set("model_provider", "MYTHICMOBS"); output.set("model", model);
        }
    }

    private List<Map<String, Object>> toYamlPages(List<PageData> pages) {
        List<Map<String, Object>> result = new ArrayList<>(pages.size());
        for (PageData page : pages) {
            Map<String, Object> pageMap = new LinkedHashMap<>();
            List<Map<String, Object>> lines = new ArrayList<>(page.lines.size());
            for (LineData line : page.lines) {
                Map<String, Object> lineMap = new LinkedHashMap<>();
                lineMap.put("content", line.content);
                lineMap.put("height", line.height);
                lines.add(lineMap);
            }
            pageMap.put("lines", lines);
            pageMap.put("actions", page.actions);
            result.add(pageMap);
        }
        return result;
    }

    private Map<String, List<String>> normalizeActions(Object raw) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (!(raw instanceof Map)) return result;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
            if (entry.getKey() == null || !(entry.getValue() instanceof List)) continue;
            List<String> actions = ((List<?>) entry.getValue()).stream()
                    .filter(java.util.Objects::nonNull)
                    .limit(128)
                    .map(String::valueOf)
                    .filter(value -> value.length() <= 4096)
                    .collect(Collectors.toList());
            if (!actions.isEmpty()) {
                result.put(String.valueOf(entry.getKey()).toUpperCase(Locale.ROOT), actions);
            }
        }
        return result;
    }

    private double normalizedHeight(Object raw) {
        double value = raw instanceof Number ? ((Number) raw).doubleValue() : 0.3d;
        return Double.isFinite(value) ? Math.max(0.01d, Math.min(16.0d, value)) : 0.3d;
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

    private static final class Converted {
        private final String suffix; private final YamlConfiguration yaml;
        private Converted(String suffix, YamlConfiguration yaml) { this.suffix = suffix; this.yaml = yaml; }
    }

    private static final class PageData {
        private final List<LineData> lines;
        private final Map<String, List<String>> actions;
        private PageData(List<LineData> lines, Map<String, List<String>> actions) {
            this.lines = lines;
            this.actions = actions;
        }
    }

    private static final class LineData {
        private final String content;
        private final double height;
        private LineData(String content, double height) {
            this.content = content;
            this.height = height;
        }
    }

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
