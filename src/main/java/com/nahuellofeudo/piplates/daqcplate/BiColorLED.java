package com.nahuellofeudo.piplates.daqcplate;

/**
 * LED selector for setLED, getLED, toggleLED, clearLED
 * Created by nahuellofeudo on 9/3/16.
 */
public enum BiColorLED {
    GREEN(0),
    RED(1);

    private int value;

    BiColorLED(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
