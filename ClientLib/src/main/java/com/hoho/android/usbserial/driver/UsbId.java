/* Copyright 2012 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: http://code.google.com/p/usb-serial-for-android/
 */
package com.hoho.android.usbserial.driver;

/**
 * Registry of USB vendor/product ID constants.
 *
 * Culled from various sources; see
 * <a href="http://www.linux-usb.org/usb.ids">usb.ids</a> for one listing.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public final class UsbId {

    public static final int VENDOR_FTDI = 0x0403;
    public static final int FTDI_FT232R = 0x6001;
    public static final int FTDI_FT2232H = 0x6010;
    public static final int FTDI_FT4232H = 0x6011;
    public static final int FTDI_FT232H = 0x6014;
    public static final int FTDI_FT231X = 0x6015;
    public static final int DEVICE_PIXHAWK_2_CUBE = 0x0016;

    public static final int VENDOR_PX4    = 0x26AC;
    public static final int DEVICE_PX4FMU = 0x0011;

    public static final int VENDOR_ARDUINO2 = 0x26ac;
    public static final int PIXHAWK = 0x0011;


    public static final int VENDOR_ATMEL = 0x03EB;
    public static final int ATMEL_LUFA_CDC_DEMO_APP = 0x2044;

    public static final int VENDOR_ARDUINO = 0x2341;
    public static final int ARDUINO_UNO = 0x0001;
    public static final int ARDUINO_MEGA_2560 = 0x0010;
    public static final int ARDUINO_SERIAL_ADAPTER = 0x003b;
    public static final int ARDUINO_MEGA_ADK = 0x003f;
    public static final int ARDUINO_MEGA_2560_R3 = 0x0042;
    public static final int ARDUINO_UNO_R3 = 0x0043;
    public static final int ARDUINO_MEGA_ADK_R3 = 0x0044;
    public static final int ARDUINO_SERIAL_ADAPTER_R3 = 0x0044;
    public static final int ARDUINO_LEONARDO = 0x8036;
    public static final int ARDUINO_MICRO = 0x8037;

    public static final int VENDOR_VAN_OOIJEN_TECH = 0x16c0;
    public static final int VAN_OOIJEN_TECH_TEENSYDUINO_SERIAL = 0x0483;

    public static final int VENDOR_LEAFLABS = 0x1eaf;
    public static final int LEAFLABS_MAPLE = 0x0004;
    
    public static final int VENDOR_SILAB = 0x10c4;
    public static final int SILABS_CP2102 = 0xea60;
    public static final int SILABS_CP2105 = 0xea70;
    public static final int SILABS_CP2108 = 0xea71;
    public static final int SILABS_CP2110 = 0xea80;

    public static final int VENDOR_PROLIFIC = 0x067b;
    public static final int PROLIFIC_PL2303 = 0x2303;

    public static final int VENDOR_UBLOX = 0x1546;
    public static final int DEVICE_UBLOX_5 = 0x01a5;
    public static final int DEVICE_UBLOX_6 = 0x01a6;
    public static final int DEVICE_UBLOX_7 = 0x01a7;
    public static final int DEVICE_UBLOX_8 = 0x01a8;

    public static final int VENDOR_OPENPILOT = 0x20A0;
    public static final int DEVICE_REVOLUTION = 0x415E;
    public static final int DEVICE_OPLINK = 0x415C;
    public static final int DEVICE_SPARKY2 = 0x41D0;
    public static final int DEVICE_CC3D = 0x415D;

    public static final int VENDOR_ARDUPILOT_CHIBIOS1 = 0x0483;
    public static final int VENDOR_ARDUPILOT_CHIBIOS2 = 0x1209;
    public static final int DEVICE_ARDUPILOT_CHIBIOS  =  0x5740;
    public static final int DEVICE_ARDUPILOT_CHIBIOS2 =  0x5741;

    public static final int VENDOR_DRAGONLINK = 0x1fc9;
    public static final int DEVICE_DRAGONLINK = 0x0083;

    public static final int VENDOR_RADIOLINK_MINI = 0x26ac;
    public static final int DEVICE_RADIOLINK_MINI = 0x0016;


    public static final int VENDOR_ARDUPILOT_FUTURE = 0x16D0;
    public static final int DEVICE_ARDUPILOT_FUTURE = 0x0E65;


    public static final int VENDOR_CUBE_BOARD       = 0x16D0;
    public static final int DEVICE_CubeBlack_PLUS   = 0x1101;
    public static final int DEVICE_CubeBlack        = 0x1011;
    public static final int DEVICE_CubeBlack_BOOT   = 0x1001;
    public static final int DEVICE_CubeOrange       = 0x1016;
    public static final int DEVICE_CubePurple       = 0x1015;
    public static final int DEVICE_CubePurple_BOOT  = 0x1005;
    public static final int DEVICE_CubeYellow       = 0x1012;
    public static final int DEVICE_CubeYellow_BOOT  = 0x1002;


    public static final int VENDOR_VRBrain_BOARD    = 0x27AC;
    public static final int DEVICE_VRBrain_v51      = 0x1151;
    public static final int DEVICE_VRBrain_v52      = 0x1152;
    public static final int DEVICE_VRBrain_v54      = 0x1154;
    public static final int DEVICE_VRCore_v10       = 0x1910;
    public static final int DEVICE_VRUBrain_v51     = 0x1351;

    public static final int VENDOR_Holybro_BOARD    = 0x3612;
    public static final int DEVICE_Holybro_Durandal = 0x004B;

    // at www.linux-usb.org/usb.ids listed for NXP/LPC1768, but all processors supported by ARM mbed DAPLink firmware report these ids
    public static final int VENDOR_ARM = 0x0d28;
    public static final int ARM_MBED = 0x0204;


    private UsbId() {
        throw new IllegalAccessError("Non-instantiable class.");
    }

}
