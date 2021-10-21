package com.andruav;

/**
 * Created by M.Hefny on 20-Jul-15.
 */
public class BinaryHelper {
    public static void putByte(byte data, byte[]packet, int index) {
        packet[index]   = data;
    }

    public static void putBoolean (boolean data,  byte[]packet, int index)
    {
        if (data)
        {
            packet[index] =1;
        }
        else
        {
            packet[index] =0;
        }
    }

    public static void putShort(short data, byte[]packet, int index) {
        packet[index]   = ((byte)(data));
        packet[index+1] = ((byte)(data >> 8));

    }

    public static void putInt(int data, byte[]packet, int index) {
        packet[index]   = ((byte)(data));
        packet[index+1] = ((byte)(data >> 8));
        packet[index+2] = ((byte)(data >> 16));
        packet[index+3] = ((byte)(data >> 24));

    }


    public static void putDouble(double ddata, byte[]packet, int index) {

        long data = Double.doubleToLongBits(ddata);
        packet[index]   = ((byte)(data));
        packet[index+1] = ((byte)(data >> 8));
        packet[index+2] = ((byte)(data >> 16));
        packet[index+3] = ((byte)(data >> 24));
        packet[index+4] = ((byte)(data >> 32));
        packet[index+5] = ((byte)(data >> 40));
        packet[index+6] = ((byte)(data >> 48));
        packet[index+7] = ((byte)(data >> 56));
    }


    public static void putLong(long data, byte[]packet, int index) {

        packet[index]   = ((byte)(data));
        packet[index+1] = ((byte)(data >> 8));
        packet[index+2] = ((byte)(data >> 16));
        packet[index+3] = ((byte)(data >> 24));
        packet[index+4] = ((byte)(data >> 32));
        packet[index+5] = ((byte)(data >> 40));
        packet[index+6] = ((byte)(data >> 48));
        packet[index+7] = ((byte)(data >> 56));
    }



    public static void putFloat(float data, byte[]packet, int index) {
        int fdata= Float.floatToIntBits(data);
        packet[index]   = ((byte)(fdata));
        packet[index+1] = ((byte)(fdata >> 8));
        packet[index+2] = ((byte)(fdata >> 16));
        packet[index+3] = ((byte)(fdata >> 24));
    }


    public static boolean getBoolean(byte[]packet, int index) {
        return (packet[index]==1);
    }


    public static float getFloat(byte[]packet, int index) {
        return (Float.intBitsToFloat(getInt(packet, index)));
    }

    public static long getLong(byte[]packet, int index) {

        long result =0;
        result  |= ((packet[index]   & (long)0xFF));
        result  |= ((packet[index+1] & (long)0xFF) << 8);
        result  |= ((packet[index+2] & (long)0xFF) << 16);
        result  |= ((packet[index+3] & (long)0xFF) << 24);
        result  |= ((packet[index+4] & (long)0xFF) << 32);
        result  |= ((packet[index+5] & (long)0xFF) << 40);
        result  |= ((packet[index+6] & (long)0xFF) << 48);
        result  |= ((packet[index+7] & (long)0xFF) << 56);

        return result;
    }


    public static double getDouble(byte[]packet, int index) {

        long result =0;
        result  |= ((packet[index]   & (long)0xFF));
        result  |= ((packet[index+1] & (long)0xFF) << 8);
        result  |= ((packet[index+2] & (long)0xFF) << 16);
        result  |= ((packet[index+3] & (long)0xFF) << 24);
        result  |= ((packet[index+4] & (long)0xFF) << 32);
        result  |= ((packet[index+5] & (long)0xFF) << 40);
        result  |= ((packet[index+6] & (long)0xFF) << 48);
        result  |= ((packet[index+7] & (long)0xFF) << 56);

        return Double.longBitsToDouble(result);
    }


    public static int getInt(byte[]packet, int index) {

        int result =0;
        result  |= ((packet[index]   & 0xFF));
        result  |= ((packet[index+1] & 0xFF) << 8);
        result  |= ((packet[index+2] & 0xFF) << 16);
        result  |= ((packet[index+3] & 0xFF) << 24);

        return result;
    }

    public static short getShort(byte[]packet, int index) {

        short result =0;
        result  |= ((packet[index]   & (short)0xFF));
        result  |= ((packet[index+1] & (short)0xFF) << 8);

        return result;
    }

    public static byte getByte(byte[]packet, int index) {

        return packet[index];

    }
}
