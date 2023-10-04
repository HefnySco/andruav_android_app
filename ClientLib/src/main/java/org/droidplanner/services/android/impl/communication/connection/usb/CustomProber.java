package org.droidplanner.services.android.impl.communication.connection.usb;


import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialProber;

/**
 * add devices here, that are not known to DefaultProber
 *
 * if the App should auto start for these devices, also
 * add IDs to app/src/main/res/xml/device_filter.xml
 */
class CustomProber {

    static UsbSerialProber getCustomProber() {
        ProbeTable customTable = new ProbeTable();
        customTable.addProduct(0x16d0, 0x087e, CdcAcmSerialDriver.class); // e.g. Digispark CDC
        customTable.addProduct(0x1234, 0x0001, FtdiSerialDriver.class); // e.g. device with custom VID+PID
        customTable.addProduct(0x1234, 0x0002, FtdiSerialDriver.class); // e.g. device with custom VID+PID
        return new UsbSerialProber(customTable);
    }

}
