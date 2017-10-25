package com.nahuellofeudo.piplates.daqcplate;

import com.nahuellofeudo.piplates.InvalidAddressException;
import com.nahuellofeudo.piplates.InvalidParameterException;
import com.nahuellofeudo.piplates.PiPlate;
import com.nahuellofeudo.piplates.PiPlateException;

/**
 * Created by nahuellofeudo on 8/31/16.
 */
public class DAQCPlate extends PiPlate {

    // The VCC Calibration value for ADC
    double vccValue;

    /**
     * Constructor
     * @param address the address of the DAQCPlate in the range [0..7]
     * @throws InvalidAddressException if the address is invalid
     */
    public DAQCPlate(int address) throws InvalidAddressException {
        super(address);

        // Calibrate VCC Value (for DAC)
        try {
            vccValue = getADC(8);
        } catch (InvalidParameterException e) {
            vccValue = 0;
        }
    }

    /**
     * Base address for the DAQCplate
     */
    @Override
    protected int getBaseAddr() {
        return 8;
    }

    /* --------- System functions --------- */

    /**
     * Reads and returns the board's identifier string
     * @return a string ID read from the board
     */
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

    /**
     * Ping the PiPlate
     * @return addr - base_addr if the plate is there, 0 otherwise;
     */
    public byte getAddr() {
        byte [] response = ppCommand(0x00, 0, 0, 1);
        return (byte) (response[0] - getBaseAddr());
    }

    
    /* --------- Interrupt Control Functions --------- */
    /**
     * Enable triggering an interrupt when an ENABLED event occurs
     */
    public void intEnable() {
        ppCommand(0x04, 0, 0, 0);
    }

    /**
     * Disable triggering interrupts
     */
    public void intDisable() {
        ppCommand(0x04, 0, 0, 0);
    }

    /**
     * Read the interrupt flags
     * @return integer with all the interrupt flags
     */
    public int getIntFlags() {
        byte [] resp=ppCommand(0x06, 0, 0, 2);
        int value=(256 * resp[0] + resp[1]);
        return value;
    }


    /* ---------  Digital Input Functions --------- */
    /**
     * Returns the value of a specific input bit
     * @param bit the bit number to return
     * @return true if the input bit's state is high, false otherwise.
     * @throws InvalidParameterException
     */
    public boolean getDINBit(int bit) throws InvalidParameterException {
        validateBitNum(bit);
        byte [] resp=ppCommand(0x20, bit, 0, 1);
        return (resp[0] > 0);
    }

    /**
     * Returns the value of all digital inputs
     * @return the values of all 8 digital inputs
     */
    public byte getDINAll() {
        byte [] resp = ppCommand(0x25, 0, 0, 1);
        return resp[0];
    }


    /**
     * Enables the triggering of an interrupt on digital input change
     * @param bit the bit (input) to trigger an interrupt on
     * @param edge the edge on which to trigger the interrupt (Rising, Falling or Both)
     * @throws InvalidParameterException
     */
    public void enableDINInterrupt(int bit, InterruptEdge edge) throws InvalidParameterException {
        validateBitNum(bit);
        switch (edge) {
            case FALLING_EDGE:
                ppCommand(0x21, bit, 0, 0);
                break;
            case RISING_EDGE:
                ppCommand(0x22, bit, 0, 0);
                break;
            case BOTH_EDGES:
                ppCommand(0x23, bit, 0, 0);
                break;
        }
    }


    /**
     * Disables the interrupt-on-change on the digital input bit
     * @param bit digital input on which to disable interrupt-on-change
     * @throws InvalidParameterException
     */
    public void disableDINInterrupt(int bit) throws InvalidParameterException {
        validateBitNum(bit);
        ppCommand(0x24, bit, 0, 0);
    }


    /* --------- Utility functions for peripherals --------- */
    /**
     * Reads a temperature from a temperature measurement from a DS18B20 connected to a particular Digital Input channel
     * @param channel the channel to which the DS18B20 is connected, in the range [0..7]
     * @param unit the temperature unit to use (Fahrenheit, Celsius or Kelvin)
     * @return the value of temperature, in the selected unit, as read by a DS18B20
     * @throws InvalidParameterException
     * @throws InterruptedException
     */
    public double getTemperature(int channel, TemperatureUnit unit) throws InvalidParameterException, InterruptedException {
        validateAnalogIn(channel);
        ppCommand(0x70, channel, 0, 0);
        Thread.sleep(1000);
        byte [] resp = ppCommand(0x71, channel, 0, 2);

        long temp = resp[0] * 256 + resp[1];
        if (temp>0x8000) temp = temp ^ 0xFFFF;
        temp = -(temp + 1);

        double dblTemp = temp/16.0;

        switch (unit) {
            case CELSIUS:
                break;
            case KELVIN:
                dblTemp -= 273;
                break;
            case FAHRENHEIT:
                dblTemp = dblTemp * 1.8 + 32;
                break;
        }

        return dblTemp;
    }


    /**
     * Reads range information from a HC-SR04 ultrasonic range finder connected to a digital input channel
     * @param channel the channel to which the HC-SR04 is connected, in the range [0..6]
     * @param unit the unit of distance to use when returning the range (Centimeters or Inches)
     * @return the range as measured by the HC-SR04 sensor, in the requested units.
     * @throws PiPlateException
     * @throws InterruptedException
     */
    public double getRange(int channel, DistanceUnit unit) throws PiPlateException, InterruptedException {
        if (channel < 0 || channel > 6) throw new InvalidParameterException("Channel must be in the range [0..6]");
        byte [] resp = ppCommand(0x80, channel, 0, 0);
        Thread.sleep(700);
        resp = ppCommand(0x81, channel, 0, 2);

        long range=resp[0] * 256 + resp[1];
        if (range == 0) throw new PiPlateException("Range sensor error or sensor not present on channel " + channel);

        double dblRange = 0;
        switch (unit) {
            case CENTIMETERS:
                dblRange = range / 58.326;
                break;
            case INCHES:
                dblRange = range / 148.148;
        }
        return dblRange;
    }


    /* --------- ADC Functions --------- */
    /**
     * Get the value of an A/D input
     * @param channel A/D channel to read from
     * @return the value returned by the A/D converter
     * @throws InvalidParameterException
     * @throws InvalidAddressException
     */
    public double getADC(int channel) throws InvalidParameterException {
        validateAnalogIn(channel);
        
//        byte [] resp = ppCommand(0x30, channel, 0, 2, 100);
        byte [] resp = ppCommand(0x30, channel, 0, 2, 1);
        double value = (256 * unsigned(resp[0]) + unsigned(resp[1]));
        value = value*4.096/1024.0;
        if (channel == 8) {
            value = value * 2;
        }
        return value;
    }


    /**
     * Reads the values of all 8 analog inputs
     * @return an array of 8 ints containing the analog values
     */
    public double[] getADCAll() {
        double [] values = new double [8];
        byte[] resp = ppCommand(0x31, 0, 0, 16, 300);
        for (int i = 0; i < 8; i++) {
            values[i] = (256 * unsigned(resp[2 * i]) + unsigned(resp[(2 * i) + 1]));
            values[i] *= 4.096/1024.0;
        }
        return values;
    }


    /* --------- PWM and DAC Output Functions --------- */
    /**
     * Sets a PWM output channel
     * @param channel the channel (0 or 1)
     * @param value the value (0..1023)
     * @throws InvalidParameterException
     */
    public void setPWM(int channel, int value) throws InvalidParameterException {
        if (value < 0 || value > 1023) throw new InvalidParameterException("ERROR: PWM argument out of range - must be between 0 and 1023");
        if (channel != 0 && channel != 1) throw new InvalidParameterException("Error: PWM channel must be 0 or 1");
        byte hibyte = (byte) (value >> 8);
        byte lobyte = (byte) (value - (hibyte << 8));
        ppCommand(0x40+channel, hibyte, lobyte, 0);
    }


    /**
     * Returns the current output value of the PWM channel
     * @param channel the channel (0 or 1)
     * @return the value assigned to the PWM output
     * @throws InvalidParameterException
     */
    public int getPWM(int channel) throws InvalidParameterException {
        validatePWMChannel(channel);
        byte [] resp = ppCommand(0x40+channel+2, 0, 0, 2);
        int value = (256 * resp[0] + resp[1]);
        return value;
    }


    /**
     * Sets an analog value in one of the two analog outputs
     * @param channel the output channel (0 or 1)
     * @param value the value (0v to 4.095v)
     * @throws InvalidParameterException
     */
    public void setDAC(int channel, double value) throws InvalidParameterException {
        if (value < 0 || value > 4.095) throw new InvalidParameterException("ERROR: DAC argument out of range - must be between 0 and 4.095 volts");
        value = value/vccValue * 1024;
        this.setPWM(channel, (int) value);
    }


    /**
     * Returns the analog output's value
     * @param channel the output channel (0 or 1)
     * @return the value of the output (0v to 4.095v)
     * @throws InvalidParameterException
     */
    public double getDAC (int channel) throws InvalidParameterException {
        int value = getPWM(channel);
        return (value * vccValue) / 1023;
    }


    /* --------- LED Functions --------- */
    /**
     * Turns on the bi-color led
     * @param led color to turn on
     * @throws PiPlateException
     */
    public void setLED(BiColorLED led) {
        ppCommand(0x60, led.getValue(), 0, 0);
    }


    /**
     * Turns off the bi-color led
     * @param led color to turn off
     * @throws PiPlateException
     */
    public void clearLED(BiColorLED led) {
        ppCommand(0x61, led.getValue(), 0, 0);
    }


    /**
     * Toggles the bi-color LED in the DACQ-Plate
     * @param led the LED to toggle
     * @throws InvalidParameterException
     */
    public void toggleLED(BiColorLED led) {
        ppCommand(0x62, led.getValue(), 0, 0);
    }


    /**
     * Returns the value of the bi-color LED in the DACQ-Plate
     * @param led the LED whose status to return
     * @throws InvalidParameterException
     */
    int getLED(BiColorLED led) {
        byte[] resp = ppCommand(0x63, led.getValue(), 0, 1);
        return resp[0];
    }


    /* --------- Digital Output Functions --------- */
    public void setDOUTbit(int bit) throws InvalidParameterException {
        validateBitNum(bit);
        ppCommand(0x10,bit,0,0);
    }
    
    public void clrDOUTbit(int bit) throws InvalidParameterException {
        validateBitNum(bit);
        ppCommand(0x11,bit,0,0);
    }

    public void toggleDOUTbit(int bit) throws InvalidParameterException {
        validateBitNum(bit);
        ppCommand(0x12,bit,0,0);
    }
    
    public void setDOUTall(int databyte) {
        ppCommand(0x13,databyte,0,0);
    }
    
    public byte getDOUTall() {
        byte [] resp=ppCommand(0x14, 0, 0, 1);
        return resp[0];        
    }
    
    
    /* --------- Methods to validate different parameters --------- */
    private void validateBitNum (int bit) throws InvalidParameterException {
        if (bit < 0 || bit > 7) throw new InvalidParameterException("Bit number parameter must be in the range [0..7]");

    }

    private void validateAnalogIn(int analogIn) throws InvalidParameterException {
        if (analogIn < 0 || analogIn > 8) throw new InvalidParameterException("Input parameter must be in the range [0..7] or 8 for VCC reference");
    }

    private void validatePWMChannel (int channel) throws InvalidParameterException {
        if (channel != 0 && channel != 1) throw new InvalidParameterException("Error: PWM channel must be 0 or 1");
    }
}
