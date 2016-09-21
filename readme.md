# Pi Plates library
A library to interact with Pi-Plates from Java.

Pi-Plates are a family of stackable and interchangeable add-on circuit boards that allow you to interact with the outside world using your Raspberry Pi (http://www.pi-plates.com)

### Requirements
Since this code uses the Pi4J library to control GPIO pins and the SPI bus, which interacts directly with the SoC's registers, your code **must** run as root for now.

For the same reason you cannot just run it on your development PC. Pi4J uses the native WiringPi library (http://wiringpi.com/) which is compiled for ARM and will not run on x86 processors.

I recommend creating a script to build your code locally, transfer it to your Raspberry Pi and start a JVM with remote debugging enabled. Most IDEs will give you the exact parameters to use on your JVM.

### Usage
The library implements two classes that act as interfaces to the two types of Pi-Plates currently supported:
* com.nahuellofeudo.piplates.daqcplate.DAQCPlate
* com.nahuellofeudo.piplates.relayplate.RELAYPlate

You interact with a plate by creating an instance of the corresponding class and passing the plate's address to the constructor. For example, to interact with a DAQC-Plate configured on address 2, you would do:

    // Initialize plate interface
    DAQCPlate daqcPlate = new DAQCPlate(2);

    // Interact with the plate
    double hardwareVersion = daqcPlate.getHWRev();
    double softwareVersion = daqcPlate.getFWRev();
    int analogValue = daqcPlate.getADC(adcChannel);

The code includes synchronization primitives to avoid SPI bus collisions if multiple threads try to communicate at the same time, even with different plates.

The interfaces themselves are modeled following the same patterns as Pi-Plates' own Python library, although some methods and parameters have been renamed to follow Java naming conventions.

All methods include Javadocs with short descriptions of what they do and how to use them.

### Installation
The library is published as a Maven dependency on Maven Central. Just include the following dependency in your POM.XML:

    <dependency>
        <groupId>com.nahuellofeudo</groupId>
        <artifactId>piplates</artifactId>
        <version>0.1-SNAPSHOT</version>
    </dependency>

### Known bugs and limitations
Only DAQCPlate and RELAYPlate are supported, as those are the only ones I personally have. If you want support for MOTORplate feel free to send me one :-)

The library is **SLOW**. I haven't optimized the communication with the different plates yet, so there are many delays that may be longer than necessary or not necessary at all. All communication is synchronized, though, so you can call all methods concurrently and they will execute one at a time and in order.

This is the first public release of this code, so expect the interfaces to change slightly in future releases.

### License
The code is licensed under the GPL license (https://www.gnu.org/licenses/gpl.txt)

