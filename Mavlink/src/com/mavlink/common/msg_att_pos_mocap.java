/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE ATT_POS_MOCAP PACKING
package com.mavlink.common;
import com.mavlink.MAVLinkPacket;
import com.mavlink.messages.MAVLinkMessage;
import com.mavlink.messages.MAVLinkPayload;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

        
/**
 * Motion capture attitude and position
 */
public class msg_att_pos_mocap extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_ATT_POS_MOCAP = 138;
    public static final int MAVLINK_MSG_LENGTH = 120;
    private static final long serialVersionUID = MAVLINK_MSG_ID_ATT_POS_MOCAP;

      
    /**
     * Timestamp (UNIX Epoch time or time since system boot). The receiving end can infer timestamp format (since 1.1.1970 or since system boot) by checking for the magnitude of the number.
     */
    public long time_usec;
      
    /**
     * Attitude quaternion (w, x, y, z order, zero-rotation is 1, 0, 0, 0)
     */
    public float[] q = new float[4];
      
    /**
     * X position (NED)
     */
    public float x;
      
    /**
     * Y position (NED)
     */
    public float y;
      
    /**
     * Z position (NED)
     */
    public float z;
      
    /**
     * Row-major representation of a pose 6x6 cross-covariance matrix upper right triangle (states: x, y, z, roll, pitch, yaw; first six entries are the first ROW, next five entries are the second ROW, etc.). If unknown, assign NaN value to first element in the array.
     */
    public float[] covariance = new float[21];
    

    /**
     * Generates the payload for a mavlink message for a message of this type
     * @return
     */
    @Override
    public MAVLinkPacket pack() {
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH,isMavlink2);
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_ATT_POS_MOCAP;
        
        packet.payload.putUnsignedLong(time_usec);
        
        for (int i = 0; i < q.length; i++) {
            packet.payload.putFloat(q[i]);
        }
                    
        packet.payload.putFloat(x);
        packet.payload.putFloat(y);
        packet.payload.putFloat(z);
        
        if (isMavlink2) {
             
        for (int i = 0; i < covariance.length; i++) {
            packet.payload.putFloat(covariance[i]);
        }
                    
            
        }
        return packet;
    }

    /**
     * Decode a att_pos_mocap message into this class fields
     *
     * @param payload The message to decode
     */
    @Override
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();
        
        this.time_usec = payload.getUnsignedLong();
         
        for (int i = 0; i < this.q.length; i++) {
            this.q[i] = payload.getFloat();
        }
                
        this.x = payload.getFloat();
        this.y = payload.getFloat();
        this.z = payload.getFloat();
        
        if (isMavlink2) {
              
        for (int i = 0; i < this.covariance.length; i++) {
            this.covariance[i] = payload.getFloat();
        }
                
            
        }
    }

    /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_att_pos_mocap() {
        this.msgid = MAVLINK_MSG_ID_ATT_POS_MOCAP;
    }
    
    /**
     * Constructor for a new message, initializes msgid and all payload variables
     */
    public msg_att_pos_mocap( long time_usec, float[] q, float x, float y, float z, float[] covariance) {
        this.msgid = MAVLINK_MSG_ID_ATT_POS_MOCAP;

        this.time_usec = time_usec;
        this.q = q;
        this.x = x;
        this.y = y;
        this.z = z;
        this.covariance = covariance;
        
    }
    
    /**
     * Constructor for a new message, initializes everything
     */
    public msg_att_pos_mocap( long time_usec, float[] q, float x, float y, float z, float[] covariance, int sysid, int compid, boolean isMavlink2) {
        this.msgid = MAVLINK_MSG_ID_ATT_POS_MOCAP;
        this.sysid = sysid;
        this.compid = compid;
        this.isMavlink2 = isMavlink2;

        this.time_usec = time_usec;
        this.q = q;
        this.x = x;
        this.y = y;
        this.z = z;
        this.covariance = covariance;
        
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a mavlink packet
     *
     */
    public msg_att_pos_mocap(MAVLinkPacket mavLinkPacket) {
        this.msgid = MAVLINK_MSG_ID_ATT_POS_MOCAP;
        
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.isMavlink2 = mavLinkPacket.isMavlink2;
        unpack(mavLinkPacket.payload);
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from JSON Object
     */
    public msg_att_pos_mocap(JSONObject jo) {
        this.msgid = MAVLINK_MSG_ID_ATT_POS_MOCAP;

        readJSONheader(jo);
        
         
        if (jo.has("time_usec")) {
            final JSONArray ja_time_usec = jo.optJSONArray("time_usec");
            if (ja_time_usec == null) {
                this.time_usec = jo.optLong("time_usec", 0);
            } else if (ja_time_usec.length() > 0) {
                this.time_usec = ja_time_usec.optLong(0, 0);
            }
        }
                    
         
        if (jo.has("q")) {
            JSONArray ja_q = jo.optJSONArray("q");
            if (ja_q == null) {
                this.q[0] = (float)jo.optDouble("q", 0);
            } else {
                for (int i = 0; i < Math.min(this.q.length, ja_q.length()); i++) {
                    this.q[i] = (float)ja_q.optDouble(i,0);
                }
            }
        }
                    
        this.x = (float)jo.optDouble("x",0);
        this.y = (float)jo.optDouble("y",0);
        this.z = (float)jo.optDouble("z",0);
        
         
        if (jo.has("covariance")) {
            JSONArray ja_covariance = jo.optJSONArray("covariance");
            if (ja_covariance == null) {
                this.covariance[0] = (float)jo.optDouble("covariance", 0);
            } else {
                for (int i = 0; i < Math.min(this.covariance.length, ja_covariance.length()); i++) {
                    this.covariance[i] = (float)ja_covariance.optDouble(i,0);
                }
            }
        }
                    
        
    }
    
    /**
     * Convert this class to a JSON Object
     */
    @Override
    public JSONObject toJSON() throws JSONException {
        final JSONObject jo = getJSONheader();
        
        jo.put("time_usec", time_usec);
         
        JSONArray ja_q = new JSONArray();
        for (int i = 0; i < this.q.length; i++) {
            ja_q.put(this.q[i]);
        }
        jo.putOpt("q", ja_q);
                
        jo.put("x", (double)x);
        jo.put("y", (double)y);
        jo.put("z", (double)z);
        
         
        JSONArray ja_covariance = new JSONArray();
        for (int i = 0; i < this.covariance.length; i++) {
            ja_covariance.put(this.covariance[i]);
        }
        jo.putOpt("covariance", ja_covariance);
                
        
        return jo;
    }

                
    /**
     * Returns a string with the MSG name and data
     */
    @Override
    public String toString() {
        return "MAVLINK_MSG_ID_ATT_POS_MOCAP - sysid:"+sysid+" compid:"+compid+" time_usec:"+time_usec+" q:"+q+" x:"+x+" y:"+y+" z:"+z+" covariance:"+covariance+"";
    }
    
    /**
     * Returns a human-readable string of the name of the message
     */
    @Override
    public String name() {
        return "MAVLINK_MSG_ID_ATT_POS_MOCAP";
    }
}
        