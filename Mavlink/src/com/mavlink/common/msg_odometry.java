/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE ODOMETRY PACKING
package com.mavlink.common;
import com.mavlink.MAVLinkPacket;
import com.mavlink.messages.MAVLinkMessage;
import com.mavlink.messages.MAVLinkPayload;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

        
/**
 * Odometry message to communicate odometry information with an external interface. Fits ROS REP 147 standard for aerial vehicles (http://www.ros.org/reps/rep-0147.html).
 */
public class msg_odometry extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_ODOMETRY = 331;
    public static final int MAVLINK_MSG_LENGTH = 232;
    private static final long serialVersionUID = MAVLINK_MSG_ID_ODOMETRY;

      
    /**
     * Timestamp (UNIX Epoch time or time since system boot). The receiving end can infer timestamp format (since 1.1.1970 or since system boot) by checking for the magnitude of the number.
     */
    public long time_usec;
      
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
     * Quaternion components, w, x, y, z (1 0 0 0 is the null-rotation)
     */
    public float[] q = new float[4];
      
    /**
     * X linear speed
     */
    public float vx;
      
    /**
     * Y linear speed
     */
    public float vy;
      
    /**
     * Z linear speed
     */
    public float vz;
      
    /**
     * Roll angular speed
     */
    public float rollspeed;
      
    /**
     * Pitch angular speed
     */
    public float pitchspeed;
      
    /**
     * Yaw angular speed
     */
    public float yawspeed;
      
    /**
     * Row-major representation of a 6x6 pose cross-covariance matrix upper right triangle (states: x, y, z, roll, pitch, yaw; first six entries are the first ROW, next five entries are the second ROW, etc.). If unknown, assign NaN value to first element in the array.
     */
    public float[] pose_covariance = new float[21];
      
    /**
     * Row-major representation of a 6x6 velocity cross-covariance matrix upper right triangle (states: vx, vy, vz, rollspeed, pitchspeed, yawspeed; first six entries are the first ROW, next five entries are the second ROW, etc.). If unknown, assign NaN value to first element in the array.
     */
    public float[] velocity_covariance = new float[21];
      
    /**
     * Coordinate frame of reference for the pose data.
     */
    public short frame_id;
      
    /**
     * Coordinate frame of reference for the velocity in free space (twist) data.
     */
    public short child_frame_id;
      
    /**
     * Estimate reset counter. This should be incremented when the estimate resets in any of the dimensions (position, velocity, attitude, angular speed). This is designed to be used when e.g an external SLAM system detects a loop-closure and the estimate jumps.
     */
    public short reset_counter;
      
    /**
     * Type of estimator that is providing the odometry.
     */
    public short estimator_type;
    

    /**
     * Generates the payload for a mavlink message for a message of this type
     * @return
     */
    @Override
    public MAVLinkPacket pack() {
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH,isMavlink2);
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_ODOMETRY;
        
        packet.payload.putUnsignedLong(time_usec);
        packet.payload.putFloat(x);
        packet.payload.putFloat(y);
        packet.payload.putFloat(z);
        
        for (int i = 0; i < q.length; i++) {
            packet.payload.putFloat(q[i]);
        }
                    
        packet.payload.putFloat(vx);
        packet.payload.putFloat(vy);
        packet.payload.putFloat(vz);
        packet.payload.putFloat(rollspeed);
        packet.payload.putFloat(pitchspeed);
        packet.payload.putFloat(yawspeed);
        
        for (int i = 0; i < pose_covariance.length; i++) {
            packet.payload.putFloat(pose_covariance[i]);
        }
                    
        
        for (int i = 0; i < velocity_covariance.length; i++) {
            packet.payload.putFloat(velocity_covariance[i]);
        }
                    
        packet.payload.putUnsignedByte(frame_id);
        packet.payload.putUnsignedByte(child_frame_id);
        
        if (isMavlink2) {
             packet.payload.putUnsignedByte(reset_counter);
             packet.payload.putUnsignedByte(estimator_type);
            
        }
        return packet;
    }

    /**
     * Decode a odometry message into this class fields
     *
     * @param payload The message to decode
     */
    @Override
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();
        
        this.time_usec = payload.getUnsignedLong();
        this.x = payload.getFloat();
        this.y = payload.getFloat();
        this.z = payload.getFloat();
         
        for (int i = 0; i < this.q.length; i++) {
            this.q[i] = payload.getFloat();
        }
                
        this.vx = payload.getFloat();
        this.vy = payload.getFloat();
        this.vz = payload.getFloat();
        this.rollspeed = payload.getFloat();
        this.pitchspeed = payload.getFloat();
        this.yawspeed = payload.getFloat();
         
        for (int i = 0; i < this.pose_covariance.length; i++) {
            this.pose_covariance[i] = payload.getFloat();
        }
                
         
        for (int i = 0; i < this.velocity_covariance.length; i++) {
            this.velocity_covariance[i] = payload.getFloat();
        }
                
        this.frame_id = payload.getUnsignedByte();
        this.child_frame_id = payload.getUnsignedByte();
        
        if (isMavlink2) {
             this.reset_counter = payload.getUnsignedByte();
             this.estimator_type = payload.getUnsignedByte();
            
        }
    }

    /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_odometry() {
        this.msgid = MAVLINK_MSG_ID_ODOMETRY;
    }
    
    /**
     * Constructor for a new message, initializes msgid and all payload variables
     */
    public msg_odometry( long time_usec, float x, float y, float z, float[] q, float vx, float vy, float vz, float rollspeed, float pitchspeed, float yawspeed, float[] pose_covariance, float[] velocity_covariance, short frame_id, short child_frame_id, short reset_counter, short estimator_type) {
        this.msgid = MAVLINK_MSG_ID_ODOMETRY;

        this.time_usec = time_usec;
        this.x = x;
        this.y = y;
        this.z = z;
        this.q = q;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.rollspeed = rollspeed;
        this.pitchspeed = pitchspeed;
        this.yawspeed = yawspeed;
        this.pose_covariance = pose_covariance;
        this.velocity_covariance = velocity_covariance;
        this.frame_id = frame_id;
        this.child_frame_id = child_frame_id;
        this.reset_counter = reset_counter;
        this.estimator_type = estimator_type;
        
    }
    
    /**
     * Constructor for a new message, initializes everything
     */
    public msg_odometry( long time_usec, float x, float y, float z, float[] q, float vx, float vy, float vz, float rollspeed, float pitchspeed, float yawspeed, float[] pose_covariance, float[] velocity_covariance, short frame_id, short child_frame_id, short reset_counter, short estimator_type, int sysid, int compid, boolean isMavlink2) {
        this.msgid = MAVLINK_MSG_ID_ODOMETRY;
        this.sysid = sysid;
        this.compid = compid;
        this.isMavlink2 = isMavlink2;

        this.time_usec = time_usec;
        this.x = x;
        this.y = y;
        this.z = z;
        this.q = q;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.rollspeed = rollspeed;
        this.pitchspeed = pitchspeed;
        this.yawspeed = yawspeed;
        this.pose_covariance = pose_covariance;
        this.velocity_covariance = velocity_covariance;
        this.frame_id = frame_id;
        this.child_frame_id = child_frame_id;
        this.reset_counter = reset_counter;
        this.estimator_type = estimator_type;
        
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a mavlink packet
     *
     */
    public msg_odometry(MAVLinkPacket mavLinkPacket) {
        this.msgid = MAVLINK_MSG_ID_ODOMETRY;
        
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.isMavlink2 = mavLinkPacket.isMavlink2;
        unpack(mavLinkPacket.payload);
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from JSON Object
     */
    public msg_odometry(JSONObject jo) {
        this.msgid = MAVLINK_MSG_ID_ODOMETRY;

        readJSONheader(jo);
        
         
        if (jo.has("time_usec")) {
            final JSONArray ja_time_usec = jo.optJSONArray("time_usec");
            if (ja_time_usec == null) {
                this.time_usec = jo.optLong("time_usec", 0);
            } else if (ja_time_usec.length() > 0) {
                this.time_usec = ja_time_usec.optLong(0, 0);
            }
        }
                    
        this.x = (float)jo.optDouble("x",0);
        this.y = (float)jo.optDouble("y",0);
        this.z = (float)jo.optDouble("z",0);
         
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
                    
        this.vx = (float)jo.optDouble("vx",0);
        this.vy = (float)jo.optDouble("vy",0);
        this.vz = (float)jo.optDouble("vz",0);
        this.rollspeed = (float)jo.optDouble("rollspeed",0);
        this.pitchspeed = (float)jo.optDouble("pitchspeed",0);
        this.yawspeed = (float)jo.optDouble("yawspeed",0);
         
        if (jo.has("pose_covariance")) {
            JSONArray ja_pose_covariance = jo.optJSONArray("pose_covariance");
            if (ja_pose_covariance == null) {
                this.pose_covariance[0] = (float)jo.optDouble("pose_covariance", 0);
            } else {
                for (int i = 0; i < Math.min(this.pose_covariance.length, ja_pose_covariance.length()); i++) {
                    this.pose_covariance[i] = (float)ja_pose_covariance.optDouble(i,0);
                }
            }
        }
                    
         
        if (jo.has("velocity_covariance")) {
            JSONArray ja_velocity_covariance = jo.optJSONArray("velocity_covariance");
            if (ja_velocity_covariance == null) {
                this.velocity_covariance[0] = (float)jo.optDouble("velocity_covariance", 0);
            } else {
                for (int i = 0; i < Math.min(this.velocity_covariance.length, ja_velocity_covariance.length()); i++) {
                    this.velocity_covariance[i] = (float)ja_velocity_covariance.optDouble(i,0);
                }
            }
        }
                    
        this.frame_id = (short)jo.optInt("frame_id",0);
        this.child_frame_id = (short)jo.optInt("child_frame_id",0);
        
        this.reset_counter = (short)jo.optInt("reset_counter",0);
        this.estimator_type = (short)jo.optInt("estimator_type",0);
        
    }
    
    /**
     * Convert this class to a JSON Object
     */
    @Override
    public JSONObject toJSON() throws JSONException {
        final JSONObject jo = getJSONheader();
        
        jo.put("time_usec", time_usec);
        jo.put("x", (double)x);
        jo.put("y", (double)y);
        jo.put("z", (double)z);
         
        JSONArray ja_q = new JSONArray();
        for (int i = 0; i < this.q.length; i++) {
            ja_q.put(this.q[i]);
        }
        jo.putOpt("q", ja_q);
                
        jo.put("vx", (double)vx);
        jo.put("vy", (double)vy);
        jo.put("vz", (double)vz);
        jo.put("rollspeed", (double)rollspeed);
        jo.put("pitchspeed", (double)pitchspeed);
        jo.put("yawspeed", (double)yawspeed);
         
        JSONArray ja_pose_covariance = new JSONArray();
        for (int i = 0; i < this.pose_covariance.length; i++) {
            ja_pose_covariance.put(this.pose_covariance[i]);
        }
        jo.putOpt("pose_covariance", ja_pose_covariance);
                
         
        JSONArray ja_velocity_covariance = new JSONArray();
        for (int i = 0; i < this.velocity_covariance.length; i++) {
            ja_velocity_covariance.put(this.velocity_covariance[i]);
        }
        jo.putOpt("velocity_covariance", ja_velocity_covariance);
                
        jo.put("frame_id", frame_id);
        jo.put("child_frame_id", child_frame_id);
        
        jo.put("reset_counter", reset_counter);
        jo.put("estimator_type", estimator_type);
        
        return jo;
    }

                                      
    /**
     * Returns a string with the MSG name and data
     */
    @Override
    public String toString() {
        return "MAVLINK_MSG_ID_ODOMETRY - sysid:"+sysid+" compid:"+compid+" time_usec:"+time_usec+" x:"+x+" y:"+y+" z:"+z+" q:"+q+" vx:"+vx+" vy:"+vy+" vz:"+vz+" rollspeed:"+rollspeed+" pitchspeed:"+pitchspeed+" yawspeed:"+yawspeed+" pose_covariance:"+pose_covariance+" velocity_covariance:"+velocity_covariance+" frame_id:"+frame_id+" child_frame_id:"+child_frame_id+" reset_counter:"+reset_counter+" estimator_type:"+estimator_type+"";
    }
    
    /**
     * Returns a human-readable string of the name of the message
     */
    @Override
    public String name() {
        return "MAVLINK_MSG_ID_ODOMETRY";
    }
}
        