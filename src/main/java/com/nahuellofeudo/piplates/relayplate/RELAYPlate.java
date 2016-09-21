package com.nahuellofeudo.piplates.relayplate;

import com.nahuellofeudo.piplates.InvalidAddressException;
import com.nahuellofeudo.piplates.InvalidParameterException;
import com.nahuellofeudo.piplates.PiPlate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nahuellofeudo on 9/3/16.
 */
public class RELAYPlate extends PiPlate {
    static Logger log = LoggerFactory.getLogger(RELAYPlate.class);

    /**
     * Constructor
     * @param address the address of the DAQCPlate in the range [0..7]
     * @throws InvalidAddressException if the address is invalid
     */
    public RELAYPlate(int address) throws InvalidAddressException {
        super(address);

        this.address = address;
    }


    /**
     * Return the base address of the RELAYPlate
     */
    @Override
    protected int getBaseAddr() {
        return 24;
    }


    /* --------- Relay functions --------- */
    public void relayOn(int relay) throws InvalidParameterException {
        validateRelay(relay);
        ppCommand(0x10, relay, 0, 0);
    }

    public void relayOff(int relay) throws InvalidParameterException {
        validateRelay(relay);
        ppCommand(0x11, relay, 0, 0);
    }

    public void relayToggle(int relay) throws InvalidParameterException {
        validateRelay(relay);
        ppCommand(0x12, relay, 0, 0);
    }

    public void relayAll(int relays) throws InvalidParameterException {
        if (relays < 0 || relays > 127) throw new InvalidParameterException("Relays parameter must be between 0 and 127");
        ppCommand(0x13, relays, 0, 0);
    }


    public int relayState() {
        byte [] resp = ppCommand(0x14, 0, 0, 1);
        return resp[0];
    }


    /* --------- LED functions --------- */

    public void setLED() {
        ppCommand(0x60, 0, 0, 0);
    }

    public void clearLED() {
        ppCommand(0x61, 0, 0, 0);
    }

    public void toggleLED() {
        ppCommand(0x62, 0, 0, 0);
    }


    /* --------- System functions --------- */
    public String getId() {
        int ID_LENGTH = 20;
        byte [] resp = ppCommand(0x01, 0, 0, ID_LENGTH);
        int length = ID_LENGTH;
        for (int x = 0; x < ID_LENGTH; x++) {
            if (resp[0] == 0) {
                length = x;
                break;
            }
        }
        return new String(resp, 0, length);
    }


    /* --------- These methods verify parameters --------- */
    private void validateRelay (int relay) throws InvalidParameterException {
        if (relay < 1 || relay > 7) throw new InvalidParameterException("Relay parameter must be in the range [1..7]");
    }

}
