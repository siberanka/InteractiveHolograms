package com.siberanka.interactiveholograms.nms.paper_v1_21_R6;

import com.siberanka.interactiveholograms.nms.api.InteractiveHologramsNmsException;
import org.bukkit.NamespacedKey;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

final class EntityTypeRegistry {

    private EntityTypeRegistry() {
        throw new IllegalStateException("Utility class");
    }

    static double getEntityTypeHeight(org.bukkit.entity.EntityType entityType) {
        return findEntityTypes(entityType).getDimensions().height();
    }

    static EntityType<?> findEntityTypes(org.bukkit.entity.EntityType entityType) {
        NamespacedKey namespacedKey = getNamespacedKey(entityType);
        String key = namespacedKey.getKey();
        Optional<EntityType<?>> entityTypes = EntityType.byString(key);
        if (entityTypes.isPresent()) {
            return entityTypes.get();
        }
        throw new InteractiveHologramsNmsException("Invalid entity type: " + entityType);
    }

    private static NamespacedKey getNamespacedKey(org.bukkit.entity.EntityType entityType) {
        try {
            return entityType.getKey();
        } catch (IllegalStateException e) {
            throw new InteractiveHologramsNmsException("Couldn't get key for entity type: " + entityType);
        }
    }

}
