package com.andruav.protocol.commands.binaryMessages;


/**
 * Created by M.Hefny on 26-Dec-14.
 */
public class AndruavBinaryHelper {

    /***
     * concatenate string with binary data and retunrs a byte[]
     * inbetween it adds 0 to make a null terminated string can be used to retrieve original string text.
     *
     * @param txt    text is added at the beginning
     * @param Binary binary is concatenated after putting 0 in between.
     * @return
     * @throws org.json.JSONException
     */
    public static byte[] joinBinarywithText(final String txt, final byte[] Binary) {
        final byte[] msgHeader = txt.getBytes();
        final byte[] msgBinary = Binary;

        final byte[] buf = new byte[msgHeader.length + 1 + msgBinary.length];

        System.arraycopy(msgHeader, 0, buf, 0, msgHeader.length);
        buf[msgHeader.length] = 0;
        //buf[msgHeader.length + 1]=0;
        //buf[msgHeader.length + 2]=' ';
        System.arraycopy(msgBinary, 0, buf, msgHeader.length + 1, msgBinary.length);

        return buf;
    }


    public static int getSplitIndex(byte[] msgBinary) {
        return getSplitIndex(msgBinary, (byte) 0);
    }

    /***
     * search for splitter delimiter between string and binary part
     *
     * @param msgBinary
     * @return
     */
    public static int getSplitIndex(byte[] msgBinary, byte delimeter) {
        // Extract the string content of the message
        int i = delimeter;
        int len = msgBinary.length;

        while (i < len) {
            if ((msgBinary[i] == 0)) {
                return i;

            }
            i += 1;
        }
        return -1;
    }
}
