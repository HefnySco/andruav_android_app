/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE ARRAY_TEST_8 PACKING
package com.MAVLink.python_array_test;
import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPayload;
import com.MAVLink.Messages.Units;
import com.MAVLink.Messages.Description;

/**
 * Array test #8.
 */
public class msg_array_test_8 extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_ARRAY_TEST_8 = 17158;
    public static final int MAVLINK_MSG_LENGTH = 24;
    private static final long serialVersionUID = MAVLINK_MSG_ID_ARRAY_TEST_8;

    
    /**
     * Value array
     */
    @Description("Value array")
    @Units("")
    public double ar_d[] = new double[2];
    
    /**
     * Stub field
     */
    @Description("Stub field")
    @Units("")
    public long v3;
    
    /**
     * Value array
     */
    @Description("Value array")
    @Units("")
    public int ar_u16[] = new int[2];
    

    /**
     * Generates the payload for a mavlink message for a message of this type
     * @return
     */
    @Override
    public MAVLinkPacket pack() {
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH,isMavlink2);
        packet.sysid = sysid;
        packet.compid = compid;
        packet.msgid = MAVLINK_MSG_ID_ARRAY_TEST_8;

        
        for (int i = 0; i < ar_d.length; i++) {
            packet.payload.putDouble(ar_d[i]);
        }
                    
        packet.payload.putUnsignedInt(v3);
        
        for (int i = 0; i < ar_u16.length; i++) {
            packet.payload.putUnsignedShort(ar_u16[i]);
        }
                    
        
        if (isMavlink2) {
            
        }
        return packet;
    }

    /**
     * Decode a array_test_8 message into this class fields
     *
     * @param payload The message to decode
     */
    @Override
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();

        
        for (int i = 0; i < this.ar_d.length; i++) {
            this.ar_d[i] = payload.getDouble();
        }
                
        this.v3 = payload.getUnsignedInt();
        
        for (int i = 0; i < this.ar_u16.length; i++) {
            this.ar_u16[i] = payload.getUnsignedShort();
        }
                
        
        if (isMavlink2) {
            
        }
    }

    /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_array_test_8() {
        this.msgid = MAVLINK_MSG_ID_ARRAY_TEST_8;
    }

    /**
     * Constructor for a new message, initializes msgid and all payload variables
     */
    public msg_array_test_8( double[] ar_d, long v3, int[] ar_u16) {
        this.msgid = MAVLINK_MSG_ID_ARRAY_TEST_8;

        this.ar_d = ar_d;
        this.v3 = v3;
        this.ar_u16 = ar_u16;
        
    }

    /**
     * Constructor for a new message, initializes everything
     */
    public msg_array_test_8( double[] ar_d, long v3, int[] ar_u16, int sysid, int compid, boolean isMavlink2) {
        this.msgid = MAVLINK_MSG_ID_ARRAY_TEST_8;
        this.sysid = sysid;
        this.compid = compid;
        this.isMavlink2 = isMavlink2;

        this.ar_d = ar_d;
        this.v3 = v3;
        this.ar_u16 = ar_u16;
        
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a mavlink packet
     *
     */
    public msg_array_test_8(MAVLinkPacket mavLinkPacket) {
        this.msgid = MAVLINK_MSG_ID_ARRAY_TEST_8;

        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.isMavlink2 = mavLinkPacket.isMavlink2;
        unpack(mavLinkPacket.payload);
    }

          
    /**
     * Returns a string with the MSG name and data
     */
    @Override
    public String toString() {
        return "MAVLINK_MSG_ID_ARRAY_TEST_8 - sysid:"+sysid+" compid:"+compid+" ar_d:"+ar_d+" v3:"+v3+" ar_u16:"+ar_u16+"";
    }

    /**
     * Returns a human-readable string of the name of the message
     */
    @Override
    public String name() {
        return "MAVLINK_MSG_ID_ARRAY_TEST_8";
    }
}
        