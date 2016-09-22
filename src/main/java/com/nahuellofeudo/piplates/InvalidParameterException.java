package com.nahuellofeudo.piplates;

/**
 * One of the method's parameters is invalid
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
