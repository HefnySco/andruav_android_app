/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET PACKING
package com.mavlink.common;
import com.mavlink.MAVLinkPacket;
import com.mavlink.messages.MAVLinkMessage;
import com.mavlink.messages.MAVLinkPayload;

import org.json.JSONException;
import org.json.JSONObject;

        
/**
 * The offset in X, Y, Z and yaw between the LOCAL_POSITION_NED messages of MAV X and the global coordinate frame in NED coordinates. Coordinate frame is right-handed, Z-axis down (aeronautical frame, NED / north-east-down convention)
 */
public class msg_local_position_ned_system_global_offset extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET = 89;
    public static final int MAVLINK_MSG_LENGTH = 28;
    private static final long serialVersionUID = MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET;

      
    /**
     * Timestamp (time since system boot).
     */
    public long time_boot_ms;
      
    /**
     * X Position
     */
    public float x;
      
    /**
     * Y Position
     */
    public float y;
      
    /**
     * Z Position
     */
    public float z;
      
    /**
     * Roll
     */
    public float roll;
      
    /**
     * Pitch
     */
    public float pitch;
      
    /**
     * Yaw
     */
    public float yaw;
    

    /**
     * Generates the payload for a mavlink message for a message of this type
     * @return
     */
    @Override
    public MAVLinkPacket pack() {
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH,isMavlink2);
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET;
        
        packet.payload.putUnsignedInt(time_boot_ms);
        packet.payload.putFloat(x);
        packet.payload.putFloat(y);
        packet.payload.putFloat(z);
        packet.payload.putFloat(roll);
        packet.payload.putFloat(pitch);
        packet.payload.putFloat(yaw);
        
        if (isMavlink2) {
            
        }
        return packet;
    }

    /**
     * Decode a local_position_ned_system_global_offset message into this class fields
     *
     * @param payload The message to decode
     */
    @Override
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();
        
        this.time_boot_ms = payload.getUnsignedInt();
        this.x = payload.getFloat();
        this.y = payload.getFloat();
        this.z = payload.getFloat();
        this.roll = payload.getFloat();
        this.pitch = payload.getFloat();
        this.yaw = payload.getFloat();
        
        if (isMavlink2) {
            
        }
    }

    /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_local_position_ned_system_global_offset() {
        this.msgid = MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET;
    }
    
    /**
     * Constructor for a new message, initializes msgid and all payload variables
     */
    public msg_local_position_ned_system_global_offset( long time_boot_ms, float x, float y, float z, float roll, float pitch, float yaw) {
        this.msgid = MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET;

        this.time_boot_ms = time_boot_ms;
        this.x = x;
        this.y = y;
        this.z = z;
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
        
    }
    
    /**
     * Constructor for a new message, initializes everything
     */
    public msg_local_position_ned_system_global_offset( long time_boot_ms, float x, float y, float z, float roll, float pitch, float yaw, int sysid, int compid, boolean isMavlink2) {
        this.msgid = MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET;
        this.sysid = sysid;
        this.compid = compid;
        this.isMavlink2 = isMavlink2;

        this.time_boot_ms = time_boot_ms;
        this.x = x;
        this.y = y;
        this.z = z;
        this.roll = roll;
        this.pitch = pitch;
        this.yaw = yaw;
        
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a mavlink packet
     *
     */
    public msg_local_position_ned_system_global_offset(MAVLinkPacket mavLinkPacket) {
        this.msgid = MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET;
        
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.isMavlink2 = mavLinkPacket.isMavlink2;
        unpack(mavLinkPacket.payload);
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from JSON Object
     */
    public msg_local_position_ned_system_global_offset(JSONObject jo) {
        this.msgid = MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET;

        readJSONheader(jo);
        
        this.time_boot_ms = jo.optLong("time_boot_ms",0);
        this.x = (float)jo.optDouble("x",0);
        this.y = (float)jo.optDouble("y",0);
        this.z = (float)jo.optDouble("z",0);
        this.roll = (float)jo.optDouble("roll",0);
        this.pitch = (float)jo.optDouble("pitch",0);
        this.yaw = (float)jo.optDouble("yaw",0);
        
        
    }
    
    /**
     * Convert this class to a JSON Object
     */
    @Override
    public JSONObject toJSON() throws JSONException {
        final JSONObject jo = getJSONheader();
        
        jo.put("time_boot_ms", time_boot_ms);
        jo.put("x", (double)x);
        jo.put("y", (double)y);
        jo.put("z", (double)z);
        jo.put("roll", (double)roll);
        jo.put("pitch", (double)pitch);
        jo.put("yaw", (double)yaw);
        
        
        return jo;
    }

                  
    /**
     * Returns a string with the MSG name and data
     */
    @Override
    public String toString() {
        return "MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET - sysid:"+sysid+" compid:"+compid+" time_boot_ms:"+time_boot_ms+" x:"+x+" y:"+y+" z:"+z+" roll:"+roll+" pitch:"+pitch+" yaw:"+yaw+"";
    }
    
    /**
     * Returns a human-readable string of the name of the message
     */
    @Override
    public String name() {
        return "MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET";
    }
}
        