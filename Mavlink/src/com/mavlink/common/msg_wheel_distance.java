/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE WHEEL_DISTANCE PACKING
package com.mavlink.common;
import com.mavlink.MAVLinkPacket;
import com.mavlink.messages.MAVLinkMessage;
import com.mavlink.messages.MAVLinkPayload;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

        
/**
 * Cumulative distance traveled for each reported wheel.
 */
public class msg_wheel_distance extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_WHEEL_DISTANCE = 9000;
    public static final int MAVLINK_MSG_LENGTH = 137;
    private static final long serialVersionUID = MAVLINK_MSG_ID_WHEEL_DISTANCE;

      
    /**
     * Timestamp (synced to UNIX time or since system boot).
     */
    public long time_usec;
      
    /**
     * Distance reported by individual wheel encoders. Forward rotations increase values, reverse rotations decrease them. Not all wheels will necessarily have wheel encoders; the mapping of encoders to wheel positions must be agreed/understood by the endpoints.
     */
    public double[] distance = new double[16];
      
    /**
     * Number of wheels reported.
     */
    public short count;
    

    /**
     * Generates the payload for a mavlink message for a message of this type
     * @return
     */
    @Override
    public MAVLinkPacket pack() {
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH,isMavlink2);
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_WHEEL_DISTANCE;
        
        packet.payload.putUnsignedLong(time_usec);
        
        for (int i = 0; i < distance.length; i++) {
            packet.payload.putDouble(distance[i]);
        }
                    
        packet.payload.putUnsignedByte(count);
        
        if (isMavlink2) {
            
        }
        return packet;
    }

    /**
     * Decode a wheel_distance message into this class fields
     *
     * @param payload The message to decode
     */
    @Override
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();
        
        this.time_usec = payload.getUnsignedLong();
         
        for (int i = 0; i < this.distance.length; i++) {
            this.distance[i] = payload.getDouble();
        }
                
        this.count = payload.getUnsignedByte();
        
        if (isMavlink2) {
            
        }
    }

    /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_wheel_distance() {
        this.msgid = MAVLINK_MSG_ID_WHEEL_DISTANCE;
    }
    
    /**
     * Constructor for a new message, initializes msgid and all payload variables
     */
    public msg_wheel_distance( long time_usec, double[] distance, short count) {
        this.msgid = MAVLINK_MSG_ID_WHEEL_DISTANCE;

        this.time_usec = time_usec;
        this.distance = distance;
        this.count = count;
        
    }
    
    /**
     * Constructor for a new message, initializes everything
     */
    public msg_wheel_distance( long time_usec, double[] distance, short count, int sysid, int compid, boolean isMavlink2) {
        this.msgid = MAVLINK_MSG_ID_WHEEL_DISTANCE;
        this.sysid = sysid;
        this.compid = compid;
        this.isMavlink2 = isMavlink2;

        this.time_usec = time_usec;
        this.distance = distance;
        this.count = count;
        
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a mavlink packet
     *
     */
    public msg_wheel_distance(MAVLinkPacket mavLinkPacket) {
        this.msgid = MAVLINK_MSG_ID_WHEEL_DISTANCE;
        
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.isMavlink2 = mavLinkPacket.isMavlink2;
        unpack(mavLinkPacket.payload);
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from JSON Object
     */
    public msg_wheel_distance(JSONObject jo) {
        this.msgid = MAVLINK_MSG_ID_WHEEL_DISTANCE;

        readJSONheader(jo);
        
         
        if (jo.has("time_usec")) {
            final JSONArray ja_time_usec = jo.optJSONArray("time_usec");
            if (ja_time_usec == null) {
                this.time_usec = jo.optLong("time_usec", 0);
            } else if (ja_time_usec.length() > 0) {
                this.time_usec = ja_time_usec.optLong(0, 0);
            }
        }
                    
         
        if (jo.has("distance")) {
            JSONArray ja_distance = jo.optJSONArray("distance");
            if (ja_distance == null) {
                this.distance[0] = jo.optDouble("distance", 0);
            } else {
                for (int i = 0; i < Math.min(this.distance.length, ja_distance.length()); i++) {
                    this.distance[i] = ja_distance.optDouble(i,0);
                }
            }
        }
                    
        this.count = (short)jo.optInt("count",0);
        
        
    }
    
    /**
     * Convert this class to a JSON Object
     */
    @Override
    public JSONObject toJSON() throws JSONException {
        final JSONObject jo = getJSONheader();
        
        jo.put("time_usec", time_usec);
         
        JSONArray ja_distance = new JSONArray();
        for (int i = 0; i < this.distance.length; i++) {
            ja_distance.put(this.distance[i]);
        }
        jo.putOpt("distance", ja_distance);
                
        jo.put("count", count);
        
        
        return jo;
    }

          
    /**
     * Returns a string with the MSG name and data
     */
    @Override
    public String toString() {
        return "MAVLINK_MSG_ID_WHEEL_DISTANCE - sysid:"+sysid+" compid:"+compid+" time_usec:"+time_usec+" distance:"+distance+" count:"+count+"";
    }
    
    /**
     * Returns a human-readable string of the name of the message
     */
    @Override
    public String name() {
        return "MAVLINK_MSG_ID_WHEEL_DISTANCE";
    }
}
        