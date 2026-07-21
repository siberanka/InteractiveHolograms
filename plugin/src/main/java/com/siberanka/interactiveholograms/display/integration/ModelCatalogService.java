package com.siberanka.interactiveholograms.display.integration;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Dynamically reads optional plugin registries without hard dependencies. */
public final class ModelCatalogService implements ModelCatalog {

    private static final long CACHE_MILLIS = 5_000L;
    private final Map<ModelProvider, CacheEntry> cache = new java.util.EnumMap<>(ModelProvider.class);

    @Override
    public Collection<String> providers() {
        Collection<String> result = new ArrayList<>();
        result.add(ModelProvider.NONE.name());
        for (ModelProvider provider : ModelProvider.values()) {
            if (provider != ModelProvider.NONE && isAvailable(provider)) result.add(provider.name());
        }
        return result;
    }

    @Override
    public Collection<String> models(ModelProvider provider) {
        if (provider == null || provider == ModelProvider.NONE || !isAvailable(provider)) return Collections.emptyList();
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(provider);
        if (entry != null && now - entry.createdAt < CACHE_MILLIS) return entry.values;
        Collection<String> loaded = Collections.unmodifiableCollection(loadModels(provider));
        cache.put(provider, new CacheEntry(now, loaded));
        return loaded;
    }

    @Override
    public Collection<String> animations(ModelProvider provider, String model) {
        if (provider == null || model == null || !isAvailable(provider)) return Collections.emptyList();
        try {
            Object source;
            switch (provider) {
                case BETTERMODEL:
                    source = invokeStatic("kr.toxicity.model.api.BetterModel", "modelOrNull", model);
                    return namesFrom(invokeAny(source, "animations", "getAnimations"));
                case MODELENGINE:
                    source = invokeStatic("com.ticxo.modelengine.api.ModelEngineAPI", "getBlueprint", model);
                    return namesFrom(invokeAny(source, "getAnimations", "animations"));
                case MYTHICMOBS:
                    // Mythic mob animations are supplied by its BetterModel/ModelEngine model.
                    Collection<String> better = animations(ModelProvider.BETTERMODEL, model);
                    return better.isEmpty() ? animations(ModelProvider.MODELENGINE, model) : better;
                default:
                    return Collections.emptyList();
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isAvailable(ModelProvider provider) {
        if (provider == null || provider == ModelProvider.NONE) return true;
        String plugin;
        switch (provider) {
            case BETTERMODEL: plugin = "BetterModel"; break;
            case MYTHICMOBS: plugin = "MythicMobs"; break;
            case MODELENGINE: plugin = "ModelEngine"; break;
            default: return false;
        }
        return Bukkit.getPluginManager().isPluginEnabled(plugin);
    }

    private Collection<String> loadModels(ModelProvider provider) {
        try {
            switch (provider) {
                case BETTERMODEL:
                    return namesFrom(invokeStatic("kr.toxicity.model.api.BetterModel", "modelKeys"));
                case MODELENGINE:
                    Object registry = invokeStatic("com.ticxo.modelengine.api.ModelEngineAPI", "getModelRegistry");
                    return namesFrom(invokeAny(registry, "getOrderedId", "getKeys", "keys"));
                case MYTHICMOBS:
                    Object api = invokeStatic("io.lumine.mythic.bukkit.MythicBukkit", "inst");
                    Object manager = invokeAny(api, "getMobManager");
                    return namesFrom(invokeAny(manager, "getMythicMobNames", "getMobNames", "getMobTypes"));
                default:
                    return Collections.emptyList();
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return Collections.emptyList();
        }
    }

    static Collection<String> namesFrom(Object value) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (value instanceof Map) value = ((Map<?, ?>) value).keySet();
        if (value instanceof Iterable) {
            for (Object entry : (Iterable<?>) value) addName(names, entry);
        } else if (value instanceof Object[]) {
            for (Object entry : (Object[]) value) addName(names, entry);
        } else if (value != null) {
            addName(names, value);
        }
        return new ArrayList<>(names);
    }

    private static void addName(Set<String> names, Object entry) {
        if (entry == null) return;
        if (entry instanceof CharSequence || entry instanceof Enum) {
            names.add(entry.toString());
            return;
        }
        for (String method : new String[]{"getInternalName", "getName", "getId", "id"}) {
            try {
                Object name = entry.getClass().getMethod(method).invoke(entry);
                if (name != null) { names.add(name.toString()); return; }
            } catch (ReflectiveOperationException ignored) { }
        }
    }

    private static Object invokeStatic(String className, String method, Object... arguments) throws ReflectiveOperationException {
        Class<?> type = Class.forName(className);
        return findMethod(type, method, arguments).invoke(null, arguments);
    }

    private static Object invokeAny(Object target, String... names) throws ReflectiveOperationException {
        if (target == null) return null;
        for (String name : names) {
            try {
                return target.getClass().getMethod(name).invoke(target);
            } catch (NoSuchMethodException ignored) { }
        }
        throw new NoSuchMethodException(target.getClass().getName());
    }

    private static Method findMethod(Class<?> type, String name, Object[] arguments) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals(name)
                    || method.getParameterCount() != arguments.length) continue;
            boolean compatible = true;
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i] != null && !method.getParameterTypes()[i].isAssignableFrom(arguments[i].getClass())) compatible = false;
            }
            if (compatible) return method;
        }
        throw new NoSuchMethodException(type.getName() + '.' + name);
    }

    private static final class CacheEntry {
        private final long createdAt;
        private final Collection<String> values;
        private CacheEntry(long createdAt, Collection<String> values) { this.createdAt = createdAt; this.values = values; }
    }
}
