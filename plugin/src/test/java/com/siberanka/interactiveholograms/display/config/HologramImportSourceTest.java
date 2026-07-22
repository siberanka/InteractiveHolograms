package com.siberanka.interactiveholograms.display.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HologramImportSourceTest {
    @Test
    void parsesDisplayNamesAndAliasesCaseInsensitively() {
        assertEquals(HologramImportSource.DECENT_HOLOGRAMS, HologramImportSource.parse("decentholograms"));
        assertEquals(HologramImportSource.FANCY_HOLOGRAMS, HologramImportSource.parse("Fancy"));
        assertEquals(HologramImportSource.HOLOGRAPHIC_DISPLAYS, HologramImportSource.parse("hd"));
        assertNull(HologramImportSource.parse("unknown"));
    }
}
