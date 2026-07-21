package com.siberanka.interactiveholograms.shared;

/**
 * A generic exception that may be thrown by the internals of InteractiveHolograms.
 */
public class InteractiveHologramsException extends RuntimeException {
    public InteractiveHologramsException(String message) {
        super(message);
    }

    public InteractiveHologramsException(String message, Throwable cause) {
        super(message, cause);
    }
}
