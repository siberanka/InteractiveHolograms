package com.siberanka.interactiveholograms.platform.bukkit;

import com.siberanka.interactiveholograms.api.utils.Log;
import com.siberanka.interactiveholograms.platform.api.resource.SaveResourceService;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitSaveResourceService implements SaveResourceService {

    private final JavaPlugin plugin;

    public BukkitSaveResourceService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void saveResource(String resourceName, boolean overwrite) {
        try {
            plugin.saveResource(resourceName, overwrite);
            Log.info("Saved resource '%s'.", resourceName);
        } catch (IllegalArgumentException e) {
            Log.error("Failed to save resource '%s'.", e, resourceName);
        }
    }
}
