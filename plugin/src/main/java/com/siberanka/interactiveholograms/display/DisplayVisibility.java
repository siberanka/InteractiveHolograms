package com.siberanka.interactiveholograms.display;

import java.util.Locale;

/** Controls which players receive a packet-only hologram. */
public enum DisplayVisibility {
    ALL,
    PERMISSION_REQUIRED,
    MANUAL;

    public static DisplayVisibility parse(String value) {
        if (value == null) {
            return ALL;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("PERMISSION_NEEDED".equals(normalized) || "PERMISSION".equals(normalized)) {
            return PERMISSION_REQUIRED;
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return ALL;
        }
    }
}
