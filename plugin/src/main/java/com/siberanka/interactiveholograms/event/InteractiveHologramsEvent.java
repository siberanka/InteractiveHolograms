package com.siberanka.interactiveholograms.event;

import org.bukkit.event.Event;

/**
 * This is the base event for all InteractiveHolograms events.
 *
 * @author d0by
 * @since 2.7.8
 */
public abstract class InteractiveHologramsEvent extends Event {

    protected InteractiveHologramsEvent() {
        super();
    }

    protected InteractiveHologramsEvent(boolean isAsync) {
        super(isAsync);
    }

}
