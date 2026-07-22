package com.siberanka.interactiveholograms.display.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

/** Every legacy hologram storage format supported by the modern importer. */
public enum HologramImportSource {
    DECENT_HOLOGRAMS("DecentHolograms", "plugins/DecentHolograms/holograms", "decent", "dh"),
    FANCY_HOLOGRAMS("FancyHolograms", "plugins/FancyHolograms/holograms.yml", "fancy", "fh"),
    HOLOGRAPHIC_DISPLAYS("HolographicDisplays", "plugins/HolographicDisplays/database.yml", "hd"),
    CMI("CMI", "plugins/CMI/Saves/holograms.yml"),
    FUTURE_HOLOGRAMS("FutureHolograms", "plugins/FutureHolograms/holograms.yml", "future"),
    GHOLO("GHolo", "plugins/GHolo/data/h.data", "gh"),
    HOLOGRAMS("Holograms", "plugins/Holograms/holograms.yml");

    private final String displayName;
    private final String defaultPath;
    private final String[] aliases;

    HologramImportSource(String displayName, String defaultPath, String... aliases) {
        this.displayName = displayName; this.defaultPath = defaultPath; this.aliases = aliases;
    }

    public String getDisplayName() { return displayName; }
    public String getDefaultPath() { return defaultPath; }

    public static HologramImportSource parse(String value) {
        if (value == null) return null;
        String normalized = value.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
        for (HologramImportSource source : values()) {
            if (source.displayName.toLowerCase(Locale.ROOT).equals(normalized)) return source;
            for (String alias : source.aliases) if (alias.equalsIgnoreCase(value)) return source;
        }
        return null;
    }

    public static Collection<String> displayNames() {
        return Arrays.stream(values()).map(HologramImportSource::getDisplayName).collect(Collectors.toList());
    }
}
