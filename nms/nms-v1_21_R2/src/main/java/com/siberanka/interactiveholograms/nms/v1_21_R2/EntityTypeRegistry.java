package com.siberanka.interactiveholograms.nms.v1_21_R2;

import com.siberanka.interactiveholograms.nms.api.InteractiveHologramsNmsException;
import net.minecraft.world.entity.EntityTypes;
import org.bukkit.entity.EntityType;

import java.util.Optional;

final class EntityTypeRegistry {

    private EntityTypeRegistry() {
        throw new IllegalStateException("Utility class");
    }

    static double getEntityTypeHeight(EntityType entityType) {
        return findEntityTypes(entityType).n().b();
    }

    static EntityTypes<?> findEntityTypes(EntityType entityType) {
        String key = entityType.getKey().getKey();
        Optional<EntityTypes<?>> entityTypes = EntityTypes.a(key);
        if (entityTypes.isPresent()) {
            return entityTypes.get();
        }
        throw new InteractiveHologramsNmsException("Invalid entity type: " + entityType);
    }

}
