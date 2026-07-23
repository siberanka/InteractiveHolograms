package com.siberanka.interactiveholograms.display.config;

import com.siberanka.interactiveholograms.api.utils.Log;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts public, Fancy-compatible flat YAML keys to the internal typed
 * attribute tree. The original file is backed up before a repair is saved.
 */
public final class HologramConfigMigrator {

    public static final int SCHEMA_VERSION = 4;
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("TEXT", "ITEM", "BLOCK"));
    private static final Set<String> BILLBOARDS = new HashSet<>(Arrays.asList("CENTER", "FIXED", "HORIZONTAL", "VERTICAL"));
    private static final Set<String> ALIGNMENTS = new HashSet<>(Arrays.asList("LEFT", "CENTER", "RIGHT"));
    private static final Set<String> VISIBILITIES = new HashSet<>(Arrays.asList("ALL", "MANUAL", "PERMISSION_REQUIRED"));
    private static final Set<String> CLICKS = new HashSet<>(Arrays.asList("LEFT", "RIGHT", "SHIFT_LEFT", "SHIFT_RIGHT"));
    private static final Set<String> ITEM_PROVIDERS = new HashSet<>(Arrays.asList("AUTO", "VANILLA", "CRAFTENGINE"));
    private static final Set<String> MODEL_PROVIDERS = new HashSet<>(Arrays.asList("NONE", "BETTERMODEL", "MYTHICMOBS", "MODELENGINE"));
    private static final Set<String> ITEM_DISPLAYS = new HashSet<>(Arrays.asList(
            "NONE", "THIRD_PERSON_LEFT_HAND", "THIRD_PERSON_RIGHT_HAND", "FIRST_PERSON_LEFT_HAND",
            "FIRST_PERSON_RIGHT_HAND", "HEAD", "GUI", "GROUND", "FIXED"));
    private static final Set<String> PUBLIC_KEYS = new HashSet<>(Arrays.asList(
            "schema-version", "type", "location", "enabled", "visibility_distance", "visibility", "permission",
            "persistent", "billboard", "scale_x", "scale_y", "scale_z", "translation_x", "translation_y",
            "translation_z", "shadow_radius", "shadow_strength", "block_brightness", "sky_brightness",
            "glowing_color", "text", "text_shadow",
            "see_through", "text_alignment", "update_text_interval", "background", "line_width", "text_opacity",
            "item", "item_provider", "item_display", "block", "model_provider", "model", "animation", "actions",
            "hitbox_width", "hitbox_height", "settings", "attributes", "pages"));

    public boolean canonicalize(ConfigurationNode root) {
        boolean changed = flattenInternalTree(root);
        if (root.node("schema-version").getInt(-1) != SCHEMA_VERSION) {
            root.node("schema-version").raw(SCHEMA_VERSION);
            changed = true;
        }
        changed |= normalizeEnum(root.node("type"), "TEXT", TYPES);
        String type = root.node("type").getString("TEXT");

        changed |= putDefault(root.node("enabled"), true);
        changed |= normalizeSpecialInterval(root.node("visibility_distance"), -1, 4096);
        changed |= normalizeEnum(root.node("visibility"), "ALL", VISIBILITIES);
        changed |= putDefault(root.node("persistent"), true);
        changed |= normalizeDouble(root.node("hitbox_width"), 1.0d, 0.1d, 16.0d);
        changed |= normalizeDouble(root.node("hitbox_height"), 1.0d, 0.1d, 16.0d);
        changed |= normalizeLocation(root.node("location"));
        changed |= normalizeEnum(root.node("billboard"), "CENTER", BILLBOARDS);
        changed |= normalizeDouble(root.node("scale_x"), 1.0d, -1000.0d, 1000.0d);
        changed |= normalizeDouble(root.node("scale_y"), 1.0d, -1000.0d, 1000.0d);
        changed |= normalizeDouble(root.node("scale_z"), 1.0d, -1000.0d, 1000.0d);
        changed |= normalizeDouble(root.node("translation_x"), 0.0d, -1000.0d, 1000.0d);
        changed |= normalizeDouble(root.node("translation_y"), 0.0d, -1000.0d, 1000.0d);
        changed |= normalizeDouble(root.node("translation_z"), 0.0d, -1000.0d, 1000.0d);
        changed |= normalizeDouble(root.node("shadow_radius"), 0.0d, 0.0d, 1000.0d);
        changed |= normalizeDouble(root.node("shadow_strength"), 1.0d, 0.0d, 1000.0d);
        changed |= clampInteger(root.node("block_brightness"), -1, -1, 15);
        changed |= clampInteger(root.node("sky_brightness"), -1, -1, 15);
        changed |= putDefault(root.node("glowing_color"), "disabled");
        changed |= normalizeActionMap(root.node("actions"));
        changed |= normalizeEnum(root.node("model_provider"), "NONE", MODEL_PROVIDERS);
        changed |= putDefault(root.node("model"), "");
        changed |= putDefault(root.node("animation"), "");

        if ("TEXT".equals(type)) {
            changed |= normalizeTextPages(root);
            changed |= putDefault(root.node("text_shadow"), false);
            changed |= putDefault(root.node("see_through"), false);
            changed |= normalizeEnum(root.node("text_alignment"), "CENTER", ALIGNMENTS);
            changed |= normalizeSpecialInterval(root.node("update_text_interval"), -1, 1200);
            changed |= putDefault(root.node("background"), "#40000000");
            changed |= clampInteger(root.node("line_width"), 300, 1, 4096);
            changed |= clampInteger(root.node("text_opacity"), 255, 0, 255);
        } else if ("ITEM".equals(type)) {
            changed |= normalizeMaterial(root.node("item"), "minecraft:apple");
            changed |= normalizeEnum(root.node("item_provider"), "AUTO", ITEM_PROVIDERS);
            changed |= normalizeEnum(root.node("item_display"), "NONE", ITEM_DISPLAYS);
            changed |= normalizeSpecialInterval(root.node("update_text_interval"), -1, 1200);
        } else {
            changed |= normalizeMaterial(root.node("block"), "minecraft:grass_block");
            changed |= normalizeSpecialInterval(root.node("update_text_interval"), -1, 1200);
        }

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.childrenMap().entrySet()) {
            if (!PUBLIC_KEYS.contains(String.valueOf(entry.getKey()))) {
                entry.getValue().raw(null);
                changed = true;
            }
        }
        return changed;
    }

    public ConfigurationNode toInternal(ConfigurationNode publicRoot) {
        ConfigurationNode root = publicRoot.copy();
        copy(root.node("enabled"), root.node("settings", "enabled"));
        int visibilityDistance = root.node("visibility_distance").getInt(-1);
        root.node("settings", "display-range").raw(visibilityDistance < 0 ? 48 : visibilityDistance);
        copy(root.node("update_text_interval"), root.node("settings", "update-interval"));
        copy(root.node("visibility"), root.node("settings", "visibility"));
        copy(root.node("permission"), root.node("settings", "permission"));
        copy(root.node("persistent"), root.node("settings", "persistent"));
        copy(root.node("hitbox_width"), root.node("settings", "hitbox", "width"));
        copy(root.node("hitbox_height"), root.node("settings", "hitbox", "height"));

        attribute(root, "billboard", "billboard_constraints", root.node("billboard").getString("CENTER").toUpperCase(Locale.ROOT));
        vectorAttribute(root, "scale", "scale_x", "scale_y", "scale_z");
        vectorAttribute(root, "translation", "translation_x", "translation_y", "translation_z");
        attribute(root, "shadow-radius", "float", root.node("shadow_radius").getDouble(0.0d));
        attribute(root, "shadow-strength", "float", root.node("shadow_strength").getDouble(1.0d));

        int blockLight = root.node("block_brightness").getInt(-1);
        int skyLight = root.node("sky_brightness").getInt(-1);
        if (blockLight >= 0 || skyLight >= 0) {
            ConfigurationNode brightness = root.node("attributes", "brightness");
            brightness.node("value-type").raw("brightness");
            brightness.node("value", "block-light").raw(Math.max(0, blockLight));
            brightness.node("value", "sky-light").raw(Math.max(0, skyLight));
        }
        String glow = root.node("glowing_color").getString("disabled");
        if (!"disabled".equalsIgnoreCase(glow)) {
            colorAttribute(root, "glow-color", parseColor(glow, new int[]{255, 255, 255, 255}));
        }

        String type = root.node("type").getString("TEXT");
        if ("TEXT".equalsIgnoreCase(type)) {
            attribute(root, "alignment", "text_alignment", root.node("text_alignment").getString("CENTER").toUpperCase(Locale.ROOT));
            attribute(root, "text-shadow", "boolean", root.node("text_shadow").getBoolean(false));
            attribute(root, "see-through", "boolean", root.node("see_through").getBoolean(false));
            attribute(root, "line-width", "integer", root.node("line_width").getInt(300));
            attribute(root, "text-opacity", "integer", root.node("text_opacity").getInt(255));
            int[] rgba = parseColor(root.node("background").getString("#40000000"), new int[]{0, 0, 0, 64});
            colorAttribute(root, "background-color", rgba);
        } else if ("ITEM".equalsIgnoreCase(type)) {
            attribute(root, "display-type", "item_display_type", root.node("item_display").getString("NONE").toUpperCase(Locale.ROOT));
        }
        root.node("settings").childrenMap();
        return root;
    }

    public void backup(Path source) throws IOException {
        if (!Files.isRegularFile(source)) {
            return;
        }
        String filename = source.getFileName().toString();
        int extension = filename.lastIndexOf('.');
        String base = extension < 0 ? filename : filename.substring(0, extension);
        String suffix = extension < 0 ? ".yml" : filename.substring(extension);
        Path backup = source.resolveSibling(base + ".backup-" + LocalDateTime.now().format(BACKUP_TIME) + suffix);
        int collision = 1;
        while (Files.exists(backup)) {
            backup = source.resolveSibling(base + ".backup-" + LocalDateTime.now().format(BACKUP_TIME) + "-" + collision++ + suffix);
        }
        Files.copy(source, backup, StandardCopyOption.COPY_ATTRIBUTES);
        Log.warn("Backed up repaired hologram YAML to '%s'.", backup.getFileName());
    }

    private boolean flattenInternalTree(ConfigurationNode root) {
        boolean changed = false;
        ConfigurationNode settings = root.node("settings");
        if (!settings.virtual()) {
            changed |= copyPresent(settings.node("enabled"), root.node("enabled"));
            changed |= copyPresent(settings.node("display-range"), root.node("visibility_distance"));
            changed |= copyPresent(settings.node("update-interval"), root.node("update_text_interval"));
            changed |= copyPresent(settings.node("visibility"), root.node("visibility"));
            changed |= copyPresent(settings.node("permission"), root.node("permission"));
            changed |= copyPresent(settings.node("persistent"), root.node("persistent"));
            changed |= copyPresent(settings.node("hitbox", "width"), root.node("hitbox_width"));
            changed |= copyPresent(settings.node("hitbox", "height"), root.node("hitbox_height"));
            settings.raw(null);
        }
        ConfigurationNode pages = root.node("pages");
        if (!pages.virtual() && !pages.isList()) {
            pages.raw(null);
            changed = true;
        }
        ConfigurationNode attributes = root.node("attributes");
        if (!attributes.virtual()) {
            changed |= flattenAttribute(attributes, "billboard", root.node("billboard"));
            changed |= flattenVector(attributes, "scale", root, "scale_x", "scale_y", "scale_z");
            changed |= flattenVector(attributes, "translation", root, "translation_x", "translation_y", "translation_z");
            changed |= flattenAttribute(attributes, "shadow-radius", root.node("shadow_radius"));
            changed |= flattenAttribute(attributes, "shadow-strength", root.node("shadow_strength"));
            changed |= flattenAttribute(attributes, "alignment", root.node("text_alignment"));
            changed |= flattenAttribute(attributes, "text-shadow", root.node("text_shadow"));
            changed |= flattenAttribute(attributes, "see-through", root.node("see_through"));
            changed |= flattenAttribute(attributes, "line-width", root.node("line_width"));
            changed |= flattenAttribute(attributes, "text-opacity", root.node("text_opacity"));
            changed |= flattenAttribute(attributes, "display-type", root.node("item_display"));
            ConfigurationNode glow = attributes.node("glow-color", "value");
            if (!glow.virtual()) {
                int red = glow.node("red").getInt(255);
                int green = glow.node("green").getInt(255);
                int blue = glow.node("blue").getInt(255);
                int alpha = glow.node("alpha").getInt(255);
                root.node("glowing_color").raw(String.format("#%02X%02X%02X%02X", alpha, red, green, blue));
                changed = true;
            }
            ConfigurationNode brightness = attributes.node("brightness", "value");
            changed |= copyPresent(brightness.node("block-light"), root.node("block_brightness"));
            changed |= copyPresent(brightness.node("sky-light"), root.node("sky_brightness"));
            attributes.raw(null);
        }
        return changed;
    }

    private boolean normalizeTextPages(ConfigurationNode root) {
        boolean changed = false;
        ConfigurationNode pages = root.node("pages");
        if (!pages.isList() || pages.childrenList().isEmpty()) {
            Object rawText = root.node("text").raw();
            pages.raw(null);
            int index = 0;
            if (rawText instanceof Iterable) {
                for (Object entry : (Iterable<?>) rawText) {
                    addPageLine(pages, 0, index++, entry == null ? "" : String.valueOf(entry),
                            com.siberanka.interactiveholograms.display.TextDisplayLine.DEFAULT_HEIGHT);
                }
            }
            if (index == 0) {
                addPageLine(pages, 0, 0, "InteractiveHolograms",
                        com.siberanka.interactiveholograms.display.TextDisplayLine.DEFAULT_HEIGHT);
            }
            changed = true;
        }

        int validPages = 0;
        for (ConfigurationNode page : pages.childrenList()) {
            if (!page.isMap()) {
                page.raw(null);
                changed = true;
            }
            ConfigurationNode lines = page.node("lines");
            if (!lines.isList() || lines.childrenList().isEmpty()) {
                lines.raw(null);
                addPageLineNode(lines.node(0), "InteractiveHolograms",
                        com.siberanka.interactiveholograms.display.TextDisplayLine.DEFAULT_HEIGHT);
                changed = true;
            }
            for (ConfigurationNode line : lines.childrenList()) {
                if (!line.isMap()) {
                    String content = line.getString("");
                    line.raw(null);
                    addPageLineNode(line, content,
                            com.siberanka.interactiveholograms.display.TextDisplayLine.DEFAULT_HEIGHT);
                    changed = true;
                } else {
                    String content = line.node("content").getString();
                    if (content == null) {
                        line.node("content").raw("");
                        changed = true;
                    }
                    changed |= normalizeDouble(line.node("height"),
                            com.siberanka.interactiveholograms.display.TextDisplayLine.DEFAULT_HEIGHT,
                            0.01d, 16.0d);
                    for (Map.Entry<Object, ? extends ConfigurationNode> entry
                            : line.childrenMap().entrySet()) {
                        String key = String.valueOf(entry.getKey());
                        if (!"content".equals(key) && !"height".equals(key)) {
                            entry.getValue().raw(null);
                            changed = true;
                        }
                    }
                }
            }
            changed |= normalizeActionMap(page.node("actions"));
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : page.childrenMap().entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (!"lines".equals(key) && !"actions".equals(key)) {
                    entry.getValue().raw(null);
                    changed = true;
                }
            }
            validPages++;
        }
        if (validPages == 0) {
            addPageLine(pages, 0, 0, "InteractiveHolograms",
                    com.siberanka.interactiveholograms.display.TextDisplayLine.DEFAULT_HEIGHT);
            pages.node(0, "actions").raw(java.util.Collections.emptyMap());
            changed = true;
        }
        if (!root.node("text").virtual()) {
            root.node("text").raw(null);
            changed = true;
        }
        return changed;
    }

    private boolean normalizeActionMap(ConfigurationNode actions) {
        if (!actions.isMap()) {
            actions.raw(java.util.Collections.emptyMap());
            return true;
        }
        Map<String, java.util.List<String>> normalized = new java.util.LinkedHashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : actions.childrenMap().entrySet()) {
            String click = String.valueOf(entry.getKey()).toUpperCase(Locale.ROOT);
            ConfigurationNode values = entry.getValue();
            if (!CLICKS.contains(click) || !values.isList()) {
                continue;
            }
            java.util.List<String> validActions = new java.util.ArrayList<>();
            for (ConfigurationNode value : values.childrenList()) {
                String action = value.getString();
                if (action != null && !action.trim().isEmpty() && action.length() <= 4096) {
                    validActions.add(action);
                }
            }
            if (!validActions.isEmpty()) {
                normalized.put(click, validActions);
            }
        }
        if (!normalized.equals(actions.raw())) {
            actions.raw(normalized);
            return true;
        }
        return false;
    }

    private void addPageLine(ConfigurationNode pages, int pageIndex, int lineIndex,
                             String content, double height) {
        addPageLineNode(pages.node(pageIndex, "lines", lineIndex), content, height);
        if (!pages.node(pageIndex, "actions").isMap()) {
            pages.node(pageIndex, "actions").raw(java.util.Collections.emptyMap());
        }
    }

    private void addPageLineNode(ConfigurationNode line, String content, double height) {
        line.node("content").raw(content);
        line.node("height").raw(height);
    }

    private boolean flattenAttribute(ConfigurationNode attributes, String key, ConfigurationNode destination) {
        return copyPresent(attributes.node(key, "value"), destination);
    }

    private boolean flattenVector(ConfigurationNode attributes, String key, ConfigurationNode root, String x, String y, String z) {
        ConfigurationNode value = attributes.node(key, "value");
        boolean changed = copyPresent(value.node("x"), root.node(x));
        changed |= copyPresent(value.node("y"), root.node(y));
        changed |= copyPresent(value.node("z"), root.node(z));
        return changed;
    }

    private void vectorAttribute(ConfigurationNode root, String name, String x, String y, String z) {
        ConfigurationNode attribute = root.node("attributes", name);
        attribute.node("value-type").raw("vector3f");
        // Configurate YAML accepts Double as its decimal scalar representation,
        // but rejects Float objects before type serializers can coerce them.
        attribute.node("value", "x").raw(root.node(x).getDouble());
        attribute.node("value", "y").raw(root.node(y).getDouble());
        attribute.node("value", "z").raw(root.node(z).getDouble());
    }

    private void attribute(ConfigurationNode root, String name, String valueType, Object value) {
        ConfigurationNode attribute = root.node("attributes", name);
        attribute.node("value-type").raw(valueType);
        attribute.node("value").raw(value);
    }

    private void colorAttribute(ConfigurationNode root, String name, int[] rgba) {
        ConfigurationNode attribute = root.node("attributes", name);
        attribute.node("value-type").raw("rgba");
        attribute.node("value", "red").raw(rgba[0]);
        attribute.node("value", "green").raw(rgba[1]);
        attribute.node("value", "blue").raw(rgba[2]);
        attribute.node("value", "alpha").raw(rgba[3]);
    }

    private int[] parseColor(String value, int[] fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if ("transparent".equals(normalized) || "disabled".equals(normalized)) {
            return new int[]{0, 0, 0, 0};
        }
        int[] named = namedColor(normalized);
        if (named != null) {
            return named;
        }
        String hex = normalized.replace("#", "");
        try {
            long parsed = Long.parseLong(hex, 16);
            if (hex.length() == 6) {
                return new int[]{(int) (parsed >> 16) & 255, (int) (parsed >> 8) & 255, (int) parsed & 255, 255};
            }
            if (hex.length() == 8) {
                return new int[]{(int) (parsed >> 16) & 255, (int) (parsed >> 8) & 255, (int) parsed & 255, (int) (parsed >> 24) & 255};
            }
        } catch (NumberFormatException ignored) {
            // Replaced by a safe default below.
        }
        return fallback;
    }

    private int[] namedColor(String name) {
        switch (name) {
            case "black": return new int[]{0, 0, 0, 255};
            case "dark_blue": return new int[]{0, 0, 170, 255};
            case "dark_green": return new int[]{0, 170, 0, 255};
            case "dark_aqua": return new int[]{0, 170, 170, 255};
            case "dark_red": return new int[]{170, 0, 0, 255};
            case "dark_purple": return new int[]{170, 0, 170, 255};
            case "gold": return new int[]{255, 170, 0, 255};
            case "gray": return new int[]{170, 170, 170, 255};
            case "dark_gray": return new int[]{85, 85, 85, 255};
            case "blue": return new int[]{85, 85, 255, 255};
            case "green": return new int[]{85, 255, 85, 255};
            case "aqua": return new int[]{85, 255, 255, 255};
            case "red": return new int[]{255, 85, 85, 255};
            case "light_purple": return new int[]{255, 85, 255, 255};
            case "yellow": return new int[]{255, 255, 85, 255};
            case "white": return new int[]{255, 255, 255, 255};
            default: return null;
        }
    }

    private boolean normalizeLocation(ConfigurationNode node) {
        boolean changed = false;
        if (!node.isMap()) {
            node.raw(null);
            changed = true;
        }
        String world = node.node("world").getString();
        if (world == null || world.trim().isEmpty()) {
            node.node("world").raw("world");
            changed = true;
        }
        changed |= normalizeDouble(node.node("x"), 0.0d, -30000000.0d, 30000000.0d);
        changed |= normalizeDouble(node.node("y"), 0.0d, -2048.0d, 2048.0d);
        changed |= normalizeDouble(node.node("z"), 0.0d, -30000000.0d, 30000000.0d);
        changed |= normalizeDouble(node.node("yaw"), 0.0d, -180.0d, 180.0d);
        changed |= normalizeDouble(node.node("pitch"), 0.0d, -90.0d, 90.0d);
        return changed;
    }

    private boolean normalizeDouble(ConfigurationNode node, double fallback, double min, double max) {
        Object raw = node.raw();
        double value = raw instanceof Number ? ((Number) raw).doubleValue() : fallback;
        double normalized = Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
        if (!(raw instanceof Number) || Double.compare(value, normalized) != 0) {
            node.raw(normalized);
            return true;
        }
        return false;
    }

    private boolean normalizeSpecialInterval(ConfigurationNode node, int fallback, int max) {
        Object raw = node.raw();
        int value = raw instanceof Number ? ((Number) raw).intValue() : fallback;
        int normalized = value == -1 ? -1 : Math.max(1, Math.min(max, value));
        if (!(raw instanceof Number) || value != normalized) {
            node.raw(normalized);
            return true;
        }
        return false;
    }

    private boolean normalizeMaterial(ConfigurationNode node, String fallback) {
        String current = node.getString();
        String normalized;
        if (current == null || current.trim().isEmpty()) {
            normalized = fallback;
        } else {
            normalized = current.trim().toLowerCase(Locale.ROOT);
            if (normalized.indexOf(':') < 0) {
                normalized = "minecraft:" + normalized;
            }
        }
        if (!normalized.equals(current)) {
            node.raw(normalized);
            return true;
        }
        return false;
    }

    private boolean normalizeEnum(ConfigurationNode node, String fallback, Set<String> allowed) {
        String current = node.getString();
        String normalized = current == null ? fallback : current.trim().toUpperCase(Locale.ROOT);
        if ("PERMISSION_NEEDED".equals(normalized)) {
            normalized = "PERMISSION_REQUIRED";
        }
        if (!allowed.contains(normalized)) {
            normalized = fallback;
        }
        if (!normalized.equals(current)) {
            node.raw(normalized);
            return true;
        }
        return false;
    }

    private boolean clampInteger(ConfigurationNode node, int fallback, int min, int max) {
        int original = node.getInt(fallback);
        int normalized = Math.max(min, Math.min(max, original));
        if (node.virtual() || normalized != original || !(node.raw() instanceof Number)) {
            node.raw(normalized);
            return true;
        }
        return false;
    }

    private boolean putDefault(ConfigurationNode node, Object value) {
        if (!node.virtual()) {
            return false;
        }
        node.raw(value);
        return true;
    }

    private boolean copyPresent(ConfigurationNode source, ConfigurationNode destination) {
        if (source.virtual()) {
            return false;
        }
        copy(source, destination);
        return true;
    }

    private void copy(ConfigurationNode source, ConfigurationNode destination) {
        if (source.virtual()) {
            destination.raw(null);
        } else {
            destination.raw(source.raw());
        }
    }
}
