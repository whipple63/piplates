package com.nahuellofeudo.piplates;

/**
 * The LED selected
 * Created by nahuellofeudo on 8/31/16.
 */
public class InvalidParameterException extends PiPlateException {
    public InvalidParameterException() {
    }

    public InvalidParameterException(String message) {
        super(message);
    }

    public InvalidParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidParameterException(Throwable cause) {
        super(cause);
    }
}
