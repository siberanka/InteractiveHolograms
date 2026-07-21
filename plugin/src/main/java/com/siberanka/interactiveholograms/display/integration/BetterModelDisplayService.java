package com.siberanka.interactiveholograms.display.integration;

import com.siberanka.interactiveholograms.api.utils.Log;
import com.siberanka.interactiveholograms.display.DisplayBase;
import com.siberanka.interactiveholograms.display.DisplayService;
import com.siberanka.interactiveholograms.platform.api.data.DecentLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Optional packet-only BetterModel dummy tracker bridge. */
public final class BetterModelDisplayService {

    private final JavaPlugin plugin;
    private final DisplayService displays;
    private final Map<String, Handle> handles = new HashMap<>();
    private BukkitTask task;

    public BetterModelDisplayService(JavaPlugin plugin, DisplayService displays) {
        this.plugin = plugin;
        this.displays = displays;
    }

    public void start() {
        if (Bukkit.getPluginManager().getPlugin("BetterModel") != null && task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::synchronize, 1L, 10L);
            Log.info("BetterModel packet-only dummy tracker integration enabled.");
        }
    }

    public void shutdown() {
        if (task != null) task.cancel();
        task = null;
        handles.values().forEach(Handle::close);
        handles.clear();
    }

    private void synchronize() {
        Set<String> live = new HashSet<>();
        for (DisplayBase display : displays.getRegisteredDisplays()) {
            if (!usesBetterModel(display) || !display.getSettings().isEnabled()) continue;
            live.add(display.getName());
            Handle current = handles.get(display.getName());
            if (current == null || !current.model.equals(display.getModel())) {
                if (current != null) current.close();
                Handle created = create(display);
                if (created != null) handles.put(display.getName(), created);
            } else {
                current.move(toBukkit(display.getLocation()));
            }
        }
        handles.keySet().removeIf(name -> {
            if (live.contains(name)) return false;
            handles.get(name).close();
            return true;
        });
    }

    private boolean usesBetterModel(DisplayBase display) {
        String provider = display.getModelProvider();
        return display.getModel() != null && provider != null
                && ("BETTERMODEL".equalsIgnoreCase(provider) || "MYTHICMOBS".equalsIgnoreCase(provider));
    }

    private Handle create(DisplayBase display) {
        try {
            Location location = toBukkit(display.getLocation());
            if (location == null) return null;
            Class<?> betterModel = Class.forName("kr.toxicity.model.api.BetterModel");
            Object renderer = betterModel.getMethod("modelOrNull", String.class).invoke(null, display.getModel());
            if (renderer == null) {
                Log.warn("BetterModel model '%s' for hologram '%s' was not found.", display.getModel(), display.getName());
                return null;
            }
            Class<?> adapter = Class.forName("kr.toxicity.model.api.bukkit.platform.BukkitAdapter");
            Object adaptedLocation = adapter.getMethod("adapt", Location.class).invoke(null, location);
            Method create = findSingleArgumentMethod(renderer.getClass(), "create", adaptedLocation.getClass());
            Object tracker = create.invoke(renderer, adaptedLocation);
            return new Handle(display.getModel(), tracker, adapter, adaptedLocation.getClass(), location);
        } catch (ReflectiveOperationException | LinkageError exception) {
            Log.warn("Could not create BetterModel hologram '%s'.", exception, display.getName());
            return null;
        }
    }

    private Method findSingleArgumentMethod(Class<?> type, String name, Class<?> argument) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(argument)) return method;
        }
        throw new NoSuchMethodException(type.getName() + '.' + name);
    }

    private Location toBukkit(DecentLocation source) {
        World world = Bukkit.getWorld(source.getWorldName());
        return world == null ? null : new Location(world, source.getX(), source.getY(), source.getZ(), source.getYaw(), source.getPitch());
    }

    private static final class Handle {
        private final String model;
        private final Object tracker;
        private final Class<?> adapter;
        private final Class<?> platformLocation;
        private Location last;

        private Handle(String model, Object tracker, Class<?> adapter, Class<?> platformLocation, Location last) {
            this.model = model; this.tracker = tracker; this.adapter = adapter; this.platformLocation = platformLocation; this.last = last;
        }

        private void move(Location location) {
            if (location == null || location.equals(last)) return;
            try {
                Object adapted = adapter.getMethod("adapt", Location.class).invoke(null, location);
                tracker.getClass().getMethod("location", platformLocation).invoke(tracker, adapted);
                last = location;
            } catch (ReflectiveOperationException ignored) { }
        }

        private void close() {
            try { tracker.getClass().getMethod("close").invoke(tracker); }
            catch (ReflectiveOperationException ignored) { }
        }
    }
}
