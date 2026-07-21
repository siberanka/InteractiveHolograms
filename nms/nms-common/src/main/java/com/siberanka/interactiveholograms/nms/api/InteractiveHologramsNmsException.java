package com.siberanka.interactiveholograms.nms.api;

import com.siberanka.interactiveholograms.shared.InteractiveHologramsException;

/**
 * An exception that may be thrown by an NMS implementation.
 *
 * @author d0by
 * @since 2.9.0
 */
public class InteractiveHologramsNmsException extends InteractiveHologramsException {
    public InteractiveHologramsNmsException(String message) {
        super(message);
    }

    public InteractiveHologramsNmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
