package com.siberanka.interactiveholograms.nms.v1_16_R3;

import com.siberanka.interactiveholograms.nms.api.InteractiveHologramsNmsException;
import com.siberanka.interactiveholograms.shared.reflect.ReflectField;
import net.minecraft.server.v1_16_R3.Entity;

import java.util.concurrent.atomic.AtomicInteger;

class EntityIdGenerator {

    private static final ReflectField<AtomicInteger> ENTITY_COUNT_FIELD = new ReflectField<>(Entity.class, "entityCount");

    int getFreeEntityId() {
        try {
            /*
             * We are getting the new entity ids the same way as the server does. This is to ensure
             * that the ids are unique and don't conflict with any other entities.
             */
            AtomicInteger entityCount = ENTITY_COUNT_FIELD.get(null);
            return entityCount.incrementAndGet();
        } catch (Exception e) {
            throw new InteractiveHologramsNmsException("Failed to get new entity ID", e);
        }
    }

}
