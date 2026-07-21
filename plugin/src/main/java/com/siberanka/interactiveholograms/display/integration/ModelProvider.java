package com.siberanka.interactiveholograms.display.integration;

import java.util.Locale;

/** Optional, entity-free model backends. */
public enum ModelProvider {
    NONE,
    BETTERMODEL,
    MYTHICMOBS,
    MODELENGINE;

    public static ModelProvider parse(String value) {
        if (value == null) return null;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
