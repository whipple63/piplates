package com.nahuellofeudo.piplates;

/**
 * Base exception class for all Pi-Plate-related exceptions
 * Created by nahuellofeudo on 8/31/16.
 */
public class PiPlateException extends Exception {
    public PiPlateException() {
    }

    public PiPlateException(String message) {
        super(message);
    }

    public PiPlateException(String message, Throwable cause) {
        super(message, cause);
    }

    public PiPlateException(Throwable cause) {
        super(cause);
    }
}
