package com.siberanka.interactiveholograms.display.integration;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelCatalogServiceTest {
    @Test
    void extractsAndSortsRegistryMapKeys() {
        Map<String, Object> registry = new LinkedHashMap<>();
        registry.put("zombie", new Object()); registry.put("Dragon", new Object());
        assertEquals(Arrays.asList("Dragon", "zombie"), ModelCatalogService.namesFrom(registry));
    }

    @Test
    void extractsMythicStyleInternalNames() {
        assertEquals(Arrays.asList("Alpha", "Beta"), ModelCatalogService.namesFrom(Arrays.asList(
                new MobType("Beta"), new MobType("Alpha"))));
    }

    public static final class MobType {
        private final String internalName;
        MobType(String internalName) { this.internalName = internalName; }
        public String getInternalName() { return internalName; }
    }
}
