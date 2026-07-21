package com.siberanka.interactiveholograms.display.integration;

import java.util.Collection;

public interface ModelCatalog {
    Collection<String> providers();
    Collection<String> models(ModelProvider provider);
    Collection<String> animations(ModelProvider provider, String model);
    boolean isAvailable(ModelProvider provider);
}
