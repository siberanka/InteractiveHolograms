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

/** Maintains location-only BetterModel/ModelEngine packet models; never creates a Bukkit entity. */
public final class ModelDisplayService {
    private final JavaPlugin plugin;
    private final DisplayService displays;
    private final Map<String, Handle> handles = new HashMap<>();
    private BukkitTask task;

    public ModelDisplayService(JavaPlugin plugin, DisplayService displays) { this.plugin = plugin; this.displays = displays; }

    public void start() {
        boolean available = enabled("BetterModel") || enabled("ModelEngine");
        if (available && task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::synchronize, 1L, 10L);
            Log.info("Packet-only custom model integrations enabled.");
        }
    }

    public void shutdown() {
        if (task != null) task.cancel();
        task = null; handles.values().forEach(Handle::close); handles.clear();
    }

    private boolean enabled(String name) { return Bukkit.getPluginManager().isPluginEnabled(name); }

    private void synchronize() {
        Set<String> live = new HashSet<>();
        for (DisplayBase display : displays.getRegisteredDisplays()) {
            ModelProvider provider = ModelProvider.parse(display.getModelProvider());
            if (provider == null || provider == ModelProvider.NONE || display.getModel() == null
                    || !display.getSettings().isEnabled()) continue;
            String key = provider.name() + ':' + display.getModel() + ':' + String.valueOf(display.getAnimation());
            live.add(display.getName());
            Handle current = handles.get(display.getName());
            if (current == null || !current.key().equals(key)) {
                if (current != null) current.close();
                Handle created = create(display, provider, key);
                if (created != null) handles.put(display.getName(), created); else handles.remove(display.getName());
            } else current.move(toBukkit(display.getLocation()));
        }
        handles.keySet().removeIf(name -> {
            if (live.contains(name)) return false;
            handles.get(name).close(); return true;
        });
    }

    private Handle create(DisplayBase display, ModelProvider provider, String key) {
        Location location = toBukkit(display.getLocation());
        if (location == null) return null;
        try {
            if (provider == ModelProvider.BETTERMODEL) return createBetterModel(display.getModel(), key, location);
            if (provider == ModelProvider.MODELENGINE) return createModelEngine(display.getModel(), key, location);
            if (provider == ModelProvider.MYTHICMOBS) {
                // A Mythic mob ID is resolved through an installed packet model engine. Never call spawnMob.
                Handle resolved = enabled("BetterModel") ? createBetterModel(display.getModel(), key, location) : null;
                return resolved != null ? resolved : (enabled("ModelEngine") ? createModelEngine(display.getModel(), key, location) : null);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            Log.warn("Could not create packet model for hologram '%s'.", exception, display.getName());
        }
        return null;
    }

    private Handle createBetterModel(String model, String key, Location location) throws ReflectiveOperationException {
        Class<?> api = Class.forName("kr.toxicity.model.api.BetterModel");
        Object renderer = api.getMethod("modelOrNull", String.class).invoke(null, model);
        if (renderer == null) return null;
        Class<?> adapter = Class.forName("kr.toxicity.model.api.bukkit.platform.BukkitAdapter");
        Method adapt = adapter.getMethod("adapt", Location.class);
        Object adapted = adapt.invoke(null, location);
        Object tracker = findCompatible(renderer.getClass(), "create", adapted).invoke(renderer, adapted);
        return new BetterHandle(key, tracker, adapt, adapted.getClass(), location);
    }

    private Handle createModelEngine(String model, String key, Location location) throws ReflectiveOperationException {
        Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
        Object blueprint = api.getMethod("getBlueprint", String.class).invoke(null, model);
        if (blueprint == null) return null;
        Class<?> dummyType = Class.forName("com.ticxo.modelengine.api.entity.Dummy");
        Object dummy = dummyType.getConstructor().newInstance();
        dummyType.getMethod("setLocation", Location.class).invoke(dummy, location);
        invokeOptional(dummy, "setDetectingPlayers", true);
        Object modeled = findCompatibleStatic(api, "createModeledEntity", dummy).invoke(null, dummy);
        Object active = api.getMethod("createActiveModel", String.class).invoke(null, model);
        Method addModel = findMethod(modeled.getClass(), "addModel", 2);
        addModel.invoke(modeled, active, false);
        invokeOptional(modeled, "setBaseEntityVisible", false);
        return new ModelEngineHandle(key, dummy, modeled, location);
    }

    private Location toBukkit(DecentLocation source) {
        World world = Bukkit.getWorld(source.getWorldName());
        return world == null ? null : new Location(world, source.getX(), source.getY(), source.getZ(), source.getYaw(), source.getPitch());
    }

    private static Method findCompatible(Class<?> type, String name, Object argument) throws NoSuchMethodException {
        for (Method method : type.getMethods()) if (method.getName().equals(name) && method.getParameterCount() == 1
                && method.getParameterTypes()[0].isAssignableFrom(argument.getClass())) return method;
        throw new NoSuchMethodException(type.getName() + '.' + name);
    }

    private static Method findCompatibleStatic(Class<?> type, String name, Object argument) throws NoSuchMethodException {
        return findCompatible(type, name, argument);
    }

    private static Method findMethod(Class<?> type, String name, int arguments) throws NoSuchMethodException {
        for (Method method : type.getMethods()) if (method.getName().equals(name) && method.getParameterCount() == arguments) return method;
        throw new NoSuchMethodException(type.getName() + '.' + name);
    }

    private static void invokeOptional(Object target, String name, Object value) {
        try {
            for (Method method : target.getClass().getMethods()) if (method.getName().equals(name) && method.getParameterCount() == 1) {
                method.invoke(target, value); return;
            }
        } catch (ReflectiveOperationException ignored) { }
    }

    private interface Handle { String key(); void move(Location location); void close(); }

    private static final class BetterHandle implements Handle {
        private final String key; private final Object tracker; private final Method adapt; private final Class<?> platformLocation; private Location last;
        private BetterHandle(String key, Object tracker, Method adapt, Class<?> platformLocation, Location last) {
            this.key = key; this.tracker = tracker; this.adapt = adapt; this.platformLocation = platformLocation; this.last = last;
        }
        public String key() { return key; }
        public void move(Location location) {
            if (location == null || location.equals(last)) return;
            try { Object adapted = adapt.invoke(null, location); tracker.getClass().getMethod("location", platformLocation).invoke(tracker, adapted); last = location; }
            catch (ReflectiveOperationException ignored) { }
        }
        public void close() { try { tracker.getClass().getMethod("close").invoke(tracker); } catch (ReflectiveOperationException ignored) { } }
    }

    private static final class ModelEngineHandle implements Handle {
        private final String key; private final Object dummy; private final Object modeled; private Location last;
        private ModelEngineHandle(String key, Object dummy, Object modeled, Location last) {
            this.key = key; this.dummy = dummy; this.modeled = modeled; this.last = last;
        }
        public String key() { return key; }
        public void move(Location location) {
            if (location == null || location.equals(last)) return;
            try { dummy.getClass().getMethod("syncLocation", Location.class).invoke(dummy, location); last = location; }
            catch (ReflectiveOperationException ignored) { }
        }
        public void close() {
            try { modeled.getClass().getMethod("destroy").invoke(modeled); } catch (ReflectiveOperationException ignored) { }
            invokeOptional(dummy, "setRemoved", true);
        }
    }
}
