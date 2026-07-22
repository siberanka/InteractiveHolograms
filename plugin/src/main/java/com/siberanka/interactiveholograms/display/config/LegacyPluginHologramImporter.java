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
import java.util.List;
import java.util.Locale;

/** Converts the legacy formats previously handled by /ih convert into modern IH YAML. */
final class LegacyPluginHologramImporter {
    private static final long MAX_FILE_BYTES = 32L * 1024L * 1024L;
    private static final int MAX_OUTPUTS = 10_000;
    private static final int MAX_LINES = 1_000;
    private static final int MAX_LINE_LENGTH = 8_192;
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final Path serverRoot;
    private final Path hologramsDirectory;

    LegacyPluginHologramImporter(Path serverRoot, Path dataFolder) {
        this.serverRoot = serverRoot.toAbsolutePath().normalize();
        this.hologramsDirectory = dataFolder.resolve("holograms").toAbsolutePath().normalize();
    }

    HologramImportService.ImportResult importYaml(HologramImportSource source, String configuredPath,
                                                   boolean overwrite) throws IOException {
        Path input = resolveSource(source, configuredPath);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(input.toFile());
        List<Imported> imported = read(source, yaml);
        if (imported.size() > MAX_OUTPUTS) throw new IOException("Import would create more than " + MAX_OUTPUTS + " holograms.");
        Files.createDirectories(hologramsDirectory);
        int success = 0, skipped = 0, warnings = 0;
        for (Imported entry : imported) {
            String name = sanitizeName(entry.name);
            if (name == null || entry.location == null || entry.lines.isEmpty()) { skipped++; warnings++; continue; }
            Path target = hologramsDirectory.resolve(name + ".yml").normalize();
            if (!target.startsWith(hologramsDirectory)) { skipped++; warnings++; continue; }
            if (Files.exists(target) && !overwrite) { skipped++; continue; }
            if (Files.exists(target)) backup(target, name);
            saveAtomically(toYaml(entry), target);
            success++;
        }
        return new HologramImportService.ImportResult(success, skipped, warnings, input);
    }

    private Path resolveSource(HologramImportSource source, String configuredPath) throws IOException {
        String pathValue = configuredPath == null || configuredPath.trim().isEmpty() ? source.getDefaultPath() : configuredPath.trim();
        Path candidate = serverRoot.resolve(pathValue).normalize().toAbsolutePath();
        if (!candidate.startsWith(serverRoot) || !Files.isRegularFile(candidate)) {
            if (source == HologramImportSource.CMI && configuredPath == null) {
                candidate = serverRoot.resolve("plugins/CMI/holograms.yml").normalize().toAbsolutePath();
            }
        }
        if (!candidate.startsWith(serverRoot) || !Files.isRegularFile(candidate)) {
            throw new IOException(source.getDisplayName() + " file was not found inside the server directory: " + candidate);
        }
        Path real = candidate.toRealPath();
        if (!real.startsWith(serverRoot.toRealPath())) throw new IOException("Import path must stay inside the server directory.");
        if (Files.size(real) > MAX_FILE_BYTES) throw new IOException("Import file exceeds the 32 MiB limit.");
        return real;
    }

    private List<Imported> read(HologramImportSource source, YamlConfiguration yaml) throws IOException {
        List<Imported> result;
        switch (source) {
            case CMI: result = readCmi(yaml); break;
            case FUTURE_HOLOGRAMS: result = readFuture(yaml); break;
            case GHOLO: result = readGholo(yaml); break;
            case HOLOGRAPHIC_DISPLAYS: result = readHolographicDisplays(yaml); break;
            case HOLOGRAMS: result = readHolograms(yaml); break;
            default: throw new IOException("Unsupported legacy source: " + source);
        }
        if (result.size() > MAX_OUTPUTS) throw new IOException("Import contains too many generated holograms.");
        return result;
    }

    private List<Imported> readCmi(YamlConfiguration yaml) {
        List<Imported> result = new ArrayList<>();
        for (String name : sortedKeys(yaml)) {
            if (name.endsWith("#>") || name.endsWith("#<")) continue;
            LocationData location = parseLocation(yaml.getString(name + ".Loc"));
            List<List<String>> pages = splitPages(yaml.getStringList(name + ".Lines"), "!nextpage!");
            addPages(result, name, location, pages);
        }
        return result;
    }

    private List<Imported> readFuture(YamlConfiguration yaml) {
        List<Imported> result = new ArrayList<>();
        for (String name : sortedKeys(yaml)) {
            ConfigurationSection section = yaml.getConfigurationSection(name);
            if (section == null) continue;
            LocationData location = parseLocation(section.getString("location"));
            List<List<String>> pages = new ArrayList<>();
            for (String page : sortedKeys(section)) {
                if (page.matches("(?i)default|refresh|cooldown|refreshRate|location")) continue;
                List<String> lines = section.getStringList(page + ".lines");
                if (!lines.isEmpty()) pages.add(lines);
            }
            addPages(result, name, location, pages);
        }
        return result;
    }

    private List<Imported> readGholo(YamlConfiguration yaml) {
        List<Imported> result = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("H");
        if (section == null) return result;
        for (String name : sortedKeys(section)) {
            addPages(result, name, parseLocation(section.getString(name + ".l")),
                    Collections.singletonList(section.getStringList(name + ".c")));
        }
        return result;
    }

    private List<Imported> readHolographicDisplays(YamlConfiguration yaml) {
        List<Imported> result = new ArrayList<>();
        for (String name : sortedKeys(yaml)) {
            LocationData location = parseLocation(yaml.getString(name + ".location"));
            if (location == null) {
                String world = yaml.getString(name + ".position.world");
                if (world != null) location = new LocationData(world, yaml.getDouble(name + ".position.x"),
                        yaml.getDouble(name + ".position.y"), yaml.getDouble(name + ".position.z"), 0, 0);
            }
            List<String> lines = new ArrayList<>();
            for (String line : yaml.getStringList(name + ".lines")) {
                lines.add(line.trim().equalsIgnoreCase("{empty}") ? "" : line.replaceAll("\\{papi: ([^}]+)}", "%$1%"));
            }
            addPages(result, name, location, Collections.singletonList(lines));
        }
        return result;
    }

    private List<Imported> readHolograms(YamlConfiguration yaml) {
        List<Imported> result = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("holograms");
        if (section == null) return result;
        for (String name : sortedKeys(section)) {
            addPages(result, name, parseLocation(section.getString(name + ".location")),
                    Collections.singletonList(section.getStringList(name + ".lines")));
        }
        return result;
    }

    private void addPages(List<Imported> result, String baseName, LocationData location, List<List<String>> pages) {
        for (int pageIndex = 0; pageIndex < pages.size() && result.size() < MAX_OUTPUTS; pageIndex++) {
            List<String> lines = boundedLines(pages.get(pageIndex));
            if (lines.isEmpty()) continue;
            String pageName = pageIndex == 0 ? baseName : baseName + "_page" + (pageIndex + 1);
            boolean containsVisual = lines.stream().anyMatch(this::isVisual);
            if (containsVisual && lines.size() > 1) {
                for (int lineIndex = 0; lineIndex < lines.size() && result.size() < MAX_OUTPUTS; lineIndex++) {
                    String lineName = lineIndex == 0 ? pageName : pageName + "_line" + (lineIndex + 1);
                    LocationData adjusted = location == null ? null : location.withY(location.y - lineIndex * 0.3d);
                    result.add(new Imported(lineName, adjusted, Collections.singletonList(lines.get(lineIndex))));
                }
            } else result.add(new Imported(pageName, location, lines));
        }
    }

    private List<String> boundedLines(List<String> source) {
        if (source == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String line : source) {
            if (result.size() >= MAX_LINES) break;
            String value = line == null ? "" : line;
            result.add(value.length() > MAX_LINE_LENGTH ? value.substring(0, MAX_LINE_LENGTH) : value);
        }
        return result;
    }

    private List<List<String>> splitPages(List<String> lines, String separator) {
        List<List<String>> pages = new ArrayList<>(); List<String> current = new ArrayList<>();
        for (String line : lines) {
            if (separator.equalsIgnoreCase(line)) { if (!current.isEmpty()) pages.add(current); current = new ArrayList<>(); }
            else current.add(line);
        }
        if (!current.isEmpty()) pages.add(current);
        return pages;
    }

    private YamlConfiguration toYaml(Imported entry) {
        YamlConfiguration output = new YamlConfiguration();
        output.set("schema-version", HologramConfigMigrator.SCHEMA_VERSION);
        output.set("location.world", entry.location.world); output.set("location.x", entry.location.x);
        output.set("location.y", entry.location.y); output.set("location.z", entry.location.z);
        output.set("location.yaw", entry.location.yaw); output.set("location.pitch", entry.location.pitch);
        output.set("enabled", true); output.set("visibility_distance", -1); output.set("visibility", "ALL");
        output.set("permission", ""); output.set("persistent", true); output.set("billboard", "CENTER");
        output.set("scale_x", 1.0); output.set("scale_y", 1.0); output.set("scale_z", 1.0);
        output.set("translation_x", 0.0); output.set("translation_y", 0.0); output.set("translation_z", 0.0);
        output.set("shadow_radius", 0.0); output.set("shadow_strength", 1.0);
        output.set("block_brightness", -1); output.set("sky_brightness", -1); output.set("glowing_color", "disabled");
        output.set("update_text_interval", -1); output.set("model_provider", "NONE");
        output.set("model", ""); output.set("animation", ""); output.set("actions", Collections.emptyMap());
        configureContent(output, entry.lines);
        return output;
    }

    private void configureContent(YamlConfiguration output, List<String> lines) {
        String line = lines.size() == 1 ? lines.get(0).trim() : null;
        String normalized = line == null ? "" : line.replaceFirst("^#", "");
        if (normalized.regionMatches(true, 0, "ICON:", 0, 5) || normalized.regionMatches(true, 0, "ITEM:", 0, 5)) {
            output.set("type", "ITEM"); output.set("item", legacyMaterial(normalized.substring(5)));
            output.set("item_provider", "AUTO"); output.set("item_display", "NONE"); return;
        }
        if (normalized.regionMatches(true, 0, "HEAD:", 0, 5) || normalized.regionMatches(true, 0, "SMALLHEAD:", 0, 10)) {
            output.set("type", "ITEM"); output.set("item", "minecraft:player_head");
            output.set("item_provider", "VANILLA"); output.set("item_display", "NONE"); return;
        }
        if (normalized.regionMatches(true, 0, "ENTITY:", 0, 7)) {
            output.set("type", "ITEM"); output.set("item", "minecraft:barrier"); output.set("item_provider", "VANILLA");
            output.set("item_display", "NONE"); output.set("scale_x", 0.0); output.set("scale_y", 0.0); output.set("scale_z", 0.0);
            output.set("model_provider", "MYTHICMOBS"); output.set("model", safeIdentifier(normalized.substring(7))); return;
        }
        output.set("type", "TEXT"); output.set("text", lines); output.set("text_shadow", false);
        output.set("see_through", false); output.set("text_alignment", "CENTER"); output.set("background", "#40000000");
        output.set("line_width", 300); output.set("text_opacity", 255);
    }

    private boolean isVisual(String line) {
        String value = line == null ? "" : line.trim().replaceFirst("^#", "").toUpperCase(Locale.ROOT);
        return value.startsWith("ICON:") || value.startsWith("ITEM:") || value.startsWith("HEAD:")
                || value.startsWith("SMALLHEAD:") || value.startsWith("ENTITY:");
    }

    private String legacyMaterial(String input) {
        String value = safeIdentifier(input).toLowerCase(Locale.ROOT);
        if (value.matches("[a-z0-9_]+:[0-9]+")) value = value.substring(0, value.indexOf(':'));
        return value.contains(":") ? value : "minecraft:" + value;
    }

    private String safeIdentifier(String input) {
        String value = input == null ? "" : input.trim().split("[ {]", 2)[0];
        return value.matches("[A-Za-z0-9_.:-]{1,128}") ? value : "barrier";
    }

    private LocationData parseLocation(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String[] parts = input.trim().split("\\s*[:;,]\\s*");
        if (parts.length < 4 || parts[0].isEmpty()) return null;
        try {
            return new LocationData(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]), parts.length > 4 ? Float.parseFloat(parts[4]) : 0,
                    parts.length > 5 ? Float.parseFloat(parts[5]) : 0);
        } catch (NumberFormatException ignored) { return null; }
    }

    private List<String> sortedKeys(ConfigurationSection section) {
        List<String> keys = new ArrayList<>(section.getKeys(false)); keys.sort(String.CASE_INSENSITIVE_ORDER); return keys;
    }

    private String sanitizeName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]{1,64}") ? name : null;
    }

    private void backup(Path target, String name) throws IOException {
        Path backup = target.resolveSibling(name + ".import-backup-" + LocalDateTime.now().format(BACKUP_TIME) + ".yml");
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void saveAtomically(YamlConfiguration output, Path target) throws IOException {
        Path temporary = Files.createTempFile(hologramsDirectory, ".import-", ".yml");
        try {
            output.save(temporary.toFile());
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private static final class Imported {
        private final String name; private final LocationData location; private final List<String> lines;
        private Imported(String name, LocationData location, List<String> lines) { this.name = name; this.location = location; this.lines = lines; }
    }
    private static final class LocationData {
        private final String world; private final double x, y, z; private final float yaw, pitch;
        private LocationData(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world; this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
        private LocationData withY(double value) { return new LocationData(world, x, value, z, yaw, pitch); }
    }
}
