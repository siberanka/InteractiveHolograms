package com.siberanka.interactiveholograms.nms.api;

import com.siberanka.interactiveholograms.nms.api.event.NmsEntityInteractEvent;

/**
 * Represents a listener used for handling NMS-related events.
 *
 * @author d0by
 * @since 2.9.0
 */
public interface NmsPacketListener {

    /**
     * Invoked when a player interacts with an entity.
     *
     * @param event The event.
     * @see NmsEntityInteractEvent
     */
    void onEntityInteract(NmsEntityInteractEvent event);

}
