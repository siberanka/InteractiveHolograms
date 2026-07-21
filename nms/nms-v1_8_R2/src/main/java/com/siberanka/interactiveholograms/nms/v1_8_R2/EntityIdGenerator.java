package com.siberanka.interactiveholograms.nms.v1_8_R2;

import com.siberanka.interactiveholograms.nms.api.InteractiveHologramsNmsException;
import com.siberanka.interactiveholograms.shared.reflect.ReflectField;
import net.minecraft.server.v1_8_R2.Entity;

class EntityIdGenerator {

    private static final ReflectField<Integer> ENTITY_COUNT_FIELD = new ReflectField<>(Entity.class, "entityCount");

    int getFreeEntityId() {
        try {
            /*
             * We are getting the new entity ids the same way as the server does. This is to ensure
             * that the ids are unique and don't conflict with any other entities.
             */
            int entityCount = ENTITY_COUNT_FIELD.get(null);
            ENTITY_COUNT_FIELD.set(null, entityCount + 1);
            return entityCount;
        } catch (Exception e) {
            throw new InteractiveHologramsNmsException("Failed to get new entity ID", e);
        }
    }

}
