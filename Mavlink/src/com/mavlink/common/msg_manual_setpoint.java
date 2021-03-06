/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE MANUAL_SETPOINT PACKING
package com.mavlink.common;
import com.mavlink.MAVLinkPacket;
import com.mavlink.messages.MAVLinkMessage;
import com.mavlink.messages.MAVLinkPayload;

import org.json.JSONException;
import org.json.JSONObject;

        
/**
 * Setpoint in roll, pitch, yaw and thrust from the operator
 */
public class msg_manual_setpoint extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_MANUAL_SETPOINT = 81;
    public static final int MAVLINK_MSG_LENGTH = 22;
    private static final long serialVersionUID = MAVLINK_MSG_ID_MANUAL_SETPOINT;

      
    /**
     * Timestamp (time since system boot).
     */
    public long time_boot_ms;
      
    /**
     * Desired roll rate
     */
    public float roll;
      
    /**
     * Desired pitch rate
     */
    public float pitch;
      
    /**
     * Desired yaw rate
     */
    public float yaw;
      
    /**
     * Collective thrust, normalized to 0 .. 1
     */
    public float thrust;
      
    /**
     * Flight mode switch position, 0.. 255
     */
    public short mode_switch;
      
    /**
     * Override mode switch position, 0.. 255
     */
    public short manual_override_switch;
    

    /**
     * Generates the payload for a mavlink message for a message of this type
     * @return
     */
    @Override
    public MAVLinkPacket pack() {
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH,isMavlink2);
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_MANUAL_SETPOINT;
        
        packet.payload.putUnsignedInt(time_boot_ms);
        packet.payload.putFloat(roll);
        packet.payload.putFloat(pitch);
        packet.payload.putFloat(yaw);
        packet.payload.putFloat(thrust);
        packet.payload.putUnsignedByte(mode_switch);
        packet.payload.putUnsignedByte(manual_override_switch);
        
        if (isMavlink2) {
            
        }
        return packet;
    }

    /**
     * Decode a manual_setpoint message into this class fields
     *
     * @param payload The message to decode
     */
    @Override
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();
        
        this.time_boot_ms = payload.getUnsignedInt();
        this.roll = payload.getFloat();
        this.pitch = payload.getFloat();
        this.yaw = payload.getFloat();
        this.thrust = payload.getFloat();
        this.mode_switch = payload.getUnsignedByte();
        this.manual_override_switch = payload.getUnsignedByte();
        
        if (isMavlink2) {
            
        }
    }

    /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_manual_setpoint() {
        this.msgid = MAVLINK_MSG_ID_MANUAL_SETPOINT;
    }
    
    /**
     * Constructor for a new message, initializes msgid and all payload variables
     */
    public msg_manual_setpoint( long time_boot_ms, float roll, float pitch, float yaw, float thrust, short mode_switch, short manual_override_switch) {
        this.msgid = MAVLINK_MSG_ID_MANUAL_SETPOINT;

        this.time_boot_ms = time_boot_ms;
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
        this.thrust = thrust;
        this.mode_switch = mode_switch;
        this.manual_override_switch = manual_override_switch;
        
    }
    
    /**
     * Constructor for a new message, initializes everything
     */
    public msg_manual_setpoint( long time_boot_ms, float roll, float pitch, float yaw, float thrust, short mode_switch, short manual_override_switch, int sysid, int compid, boolean isMavlink2) {
        this.msgid = MAVLINK_MSG_ID_MANUAL_SETPOINT;
        this.sysid = sysid;
        this.compid = compid;
        this.isMavlink2 = isMavlink2;

        this.time_boot_ms = time_boot_ms;
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
        this.thrust = thrust;
        this.mode_switch = mode_switch;
        this.manual_override_switch = manual_override_switch;
        
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a mavlink packet
     *
     */
    public msg_manual_setpoint(MAVLinkPacket mavLinkPacket) {
        this.msgid = MAVLINK_MSG_ID_MANUAL_SETPOINT;
        
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.isMavlink2 = mavLinkPacket.isMavlink2;
        unpack(mavLinkPacket.payload);
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from JSON Object
     */
    public msg_manual_setpoint(JSONObject jo) {
        this.msgid = MAVLINK_MSG_ID_MANUAL_SETPOINT;

        readJSONheader(jo);
        
        this.time_boot_ms = jo.optLong("time_boot_ms",0);
        this.roll = (float)jo.optDouble("roll",0);
        this.pitch = (float)jo.optDouble("pitch",0);
        this.yaw = (float)jo.optDouble("yaw",0);
        this.thrust = (float)jo.optDouble("thrust",0);
        this.mode_switch = (short)jo.optInt("mode_switch",0);
        this.manual_override_switch = (short)jo.optInt("manual_override_switch",0);
        
        
    }
    
    /**
     * Convert this class to a JSON Object
     */
    @Override
    public JSONObject toJSON() throws JSONException {
        final JSONObject jo = getJSONheader();
        
        jo.put("time_boot_ms", time_boot_ms);
        jo.put("roll", (double)roll);
        jo.put("pitch", (double)pitch);
        jo.put("yaw", (double)yaw);
        jo.put("thrust", (double)thrust);
        jo.put("mode_switch", mode_switch);
        jo.put("manual_override_switch", manual_override_switch);
        
        
        return jo;
    }

                  
    /**
     * Returns a string with the MSG name and data
     */
    @Override
    public String toString() {
        return "MAVLINK_MSG_ID_MANUAL_SETPOINT - sysid:"+sysid+" compid:"+compid+" time_boot_ms:"+time_boot_ms+" roll:"+roll+" pitch:"+pitch+" yaw:"+yaw+" thrust:"+thrust+" mode_switch:"+mode_switch+" manual_override_switch:"+manual_override_switch+"";
    }
    
    /**
     * Returns a human-readable string of the name of the message
     */
    @Override
    public String name() {
        return "MAVLINK_MSG_ID_MANUAL_SETPOINT";
    }
}
        