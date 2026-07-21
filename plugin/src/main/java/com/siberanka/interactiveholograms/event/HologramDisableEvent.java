package com.siberanka.interactiveholograms.event;

import com.siberanka.interactiveholograms.api.holograms.DisableCause;
import com.siberanka.interactiveholograms.api.holograms.Hologram;
import lombok.Getter;
import org.bukkit.event.HandlerList;

/**
 * This event is called whenever a Hologram gets disabled through
 * {@link Hologram#disable(DisableCause)}
 */
@Getter
public class HologramDisableEvent extends InteractiveHologramsEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Hologram hologram;

    public HologramDisableEvent(boolean async, Hologram hologram) {
        super(async);
        this.hologram = hologram;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
