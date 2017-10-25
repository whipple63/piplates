package com.nahuellofeudo.piplates;

import com.pi4j.io.gpio.*;
import com.pi4j.wiringpi.GpioUtil;
import com.pi4j.wiringpi.Spi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parent class for all PiPlate driver classes
 * Created by nahuellofeudo on 9/3/16.
 */
public abstract class PiPlate {
    static Logger log = LoggerFactory.getLogger(PiPlate.class);

    private static int descriptor;
    private static GpioPinDigitalOutput frame;
    private static GpioPinDigitalInput interrupt;
    public int address;

    /**
     * Constructor for the base class
     * @param address the plate's address
     * @throws InvalidAddressException when address is outside [0..7]
     */
     public PiPlate(int address) throws InvalidAddressException {
         // Allocate GPIO and open SPI channel
         PiPlate.allocateGPIO();

         if (address < 0 || address > 7) throw new InvalidAddressException("Address must be in the range [0..7]");
         this.address = address;
         log.debug("Initializing Pi4J-Core");
    }

    /**
     * Configures the GPIO pins for Frame and Interrupt, and initializes the SPI bus
     */
    private static synchronized void allocateGPIO() {

        // Try to enable non-privilidged GPIO access
        GpioUtil.enableNonPrivilegedAccess();
        
        // Set up port pins
        GpioController gpio = GpioFactory.getInstance();

        if (frame == null) {
            frame = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "Frame", PinState.LOW);
            gpio.setMode(PinMode.DIGITAL_OUTPUT, frame);
        }

        if (interrupt == null) {
            interrupt = gpio.provisionDigitalInputPin(RaspiPin.GPIO_03, "Interrupt", PinPullResistance.PULL_UP);
            gpio.setMode(PinMode.DIGITAL_INPUT, interrupt);
        }

        if (descriptor == 0) {
            // Initialize SPI bus
            log.debug("Initializing SPI bus...");
//            descriptor = Spi.wiringPiSPISetupMode(Spi.CHANNEL_1, 500000, Spi.MODE_0);
            descriptor = Spi.wiringPiSPISetupMode(Spi.CHANNEL_1, 50000, Spi.MODE_0);
        }

        if (descriptor < 0) {
            // SPI could not be initialized. Bail
            log.error("SPI could not be initialized");
            throw new RuntimeException("Error initializing SPI");
        }
    }


    /* -- Utility and auxiliary methods */

    /**
     * Send a command to a plate, optionally returning a response
     * @param command command (plate-dependent)
     * @param parameter1 1st parameter (command-dependent)
     * @param parameter2 2nd parameter (command-dependent)
     * @param bytesToReturn number of bytes to read back from the plate as a response
     * @return a (possibly null) array of bytes with the plate's response
     */
    public byte [] ppCommand(int command, int parameter1, int parameter2, int bytesToReturn, int processingDelay) {
        byte[] packet = new byte[4];
        packet[0] = (byte) (getBaseAddr() + address);
        packet[1] = (byte)command;
        packet[2] = (byte)parameter1;
        packet[3] = (byte)parameter2;

        synchronized (PiPlate.class) {
            // Write command
            frame.high();
            delay();
            transferData(Spi.CHANNEL_1, packet, packet.length);

            // read response (if necessary)
            byte[] returnData = null;
            if (bytesToReturn > 0) {
                returnData = new byte[bytesToReturn];

                delay(processingDelay);
                transferData(Spi.CHANNEL_1, returnData, bytesToReturn);
            }
            frame.low();
            delay();
            return returnData;
        }
    }

    /**
     * ppCommand with a default delay of 1ms
     */
    public byte[] ppCommand(int command, int parameter1, int parameter2, int bytesToReturn) {
        return ppCommand(command, parameter1, parameter2, bytesToReturn, 1);
    }

    /**
     * Transfers data to and/or from a Pi-Plate, one byte at a time, toggling Chip Select between bytes
     * @param channel channel number (normally 1)
     * @param data the data to send, or the buffer where to put the data received
     * @param length number of bytes to send/receive
     */
//    private void transferDataIn(int channel, byte [] data, int length) {
//        byte [] dummy = new byte[1];
//        for(int x = 0; x < length; x++) {
////            dummy[0] = data[x];
//            dummy[0] = 0;
//            Spi.wiringPiSPIDataRW(channel, dummy, 1);
////            try { Thread.sleep(0,1); } catch (Exception e) {}
//            data[x] = dummy[0];
//        }
//    }
    
    private void transferData(int channel, byte [] data, int length) {
//        byte [] dummy = new byte[1];
//        for(int x = 0; x < length; x++) {
//            dummy[0] = data[x];
            Spi.wiringPiSPIDataRW(channel, data, length);
//            try { Thread.sleep(0, 500); } catch (Exception e) {}
//            data[x] = dummy[0];
//        }
    }


    /* --------- System commands --------- */
        /**
     * Returns the Hardware revision
     * @return Double containing hardware revision of the plate
     */
    public double getHWRev() {
        byte [] resp = ppCommand(0x02,0,0,1);
        byte rev = resp[0];
        int whole = rev >> 4;
        int point = rev & 0x0F;
        return whole + (point/10.0);
    }

    /**
     * Returns the firmware version of the plate
     * @return a double with the firmware version
     */
    public double getFWRev() {
        byte [] resp = ppCommand(0x03,0,0,1);
        byte rev = resp[0];
        int whole = rev >> 4;
        int point = rev & 0x0F;
        return whole + (point/10.0);
    }

    /**
     * Java does not support unsigned values. Bytes in the range 0..255 are interpreted as signed bytes in the range (-128..127).
     * This method converts an byte (0..255) into a Java int with the unsigned value represented by val (0..255)
     * This is necessary so that math with values > 127 does not fail
     * @param val the value to convert to unsigned.
     * @return a 32-bit int with the unsigned value of val
     */
    public int unsigned(byte val) {
        return (val & 0xFF);
    }

    /**
     * Pauses the current thread
     * @param milliseconds number of milliseconds to pause
     */
    private void delay(int milliseconds) {
        try {Thread.sleep(milliseconds);} catch (InterruptedException e) {}
    }

    /**
     * One millisecond pause
     */
    private void delay() { delay (1); }

    /**
     * Define the plate's base address
     * Implemented by plate-specific classes
     */
    protected abstract int getBaseAddr();
    public abstract byte getAddr();
}
