/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE SET_HOME_POSITION PACKING
package com.MAVLink.common;
import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPayload;
import com.MAVLink.Messages.Units;
import com.MAVLink.Messages.Description;

/**
 * The position the system will return to and land on. The position is set automatically by the system during the takeoff in case it was not explicitly set by the operator before or after. The global and local positions encode the position in the respective coordinate frames, while the q parameter encodes the orientation of the surface. Under normal conditions it describes the heading and terrain slope, which can be used by the aircraft to adjust the approach. The approach 3D vector describes the point to which the system should fly in normal flight mode and then perform a landing sequence along the vector.
 */
public class msg_set_home_position extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_SET_HOME_POSITION = 243;
    public static final int MAVLINK_MSG_LENGTH = 61;
    private static final long serialVersionUID = MAVLINK_MSG_ID_SET_HOME_POSITION;

    
    /**
     * Latitude (WGS84)
     */
    @Description("Latitude (WGS84)")
    @Units("degE7")
    public int latitude;
    
    /**
     * Longitude (WGS84)
     */
    @Description("Longitude (WGS84)")
    @Units("degE7")
    public int longitude;
    
    /**
     * Altitude (MSL). Positive for up.
     */
    @Description("Altitude (MSL). Positive for up.")
    @Units("mm")
    public int altitude;
    
    /**
     * Local X position of this position in the local coordinate frame
     */
    @Description("Local X position of this position in the local coordinate frame")
    @Units("m")
    public float x;
    
    /**
     * Local Y position of this position in the local coordinate frame
     */
    @Description("Local Y position of this position in the local coordinate frame")
    @Units("m")
    public float y;
    
    /**
     * Local Z position of this position in the local coordinate frame
     */
    @Description("Local Z position of this position in the local coordinate frame")
    @Units("m")
    public float z;
    
    /**
     * World to surface normal and heading transformation of the takeoff position. Used to indicate the heading and slope of the ground
     */
    @Description("World to surface normal and heading transformation of the takeoff position. Used to indicate the heading and slope of the ground")
    @Units("")
    public float q[] = new float[4];
    
    /**
     * Local X position of the end of the approach vector. Multicopters should set this position based on their takeoff path. Grass-landing fixed wing aircraft should set it the same way as multicopters. Runway-landing fixed wing aircraft should set it to the opposite direction of the takeoff, assuming the takeoff happened from the threshold / touchdown zone.
     */
    @Description("Local X position of the end of the approach vector. Multicopters should set this position based on their takeoff path. Grass-landing fixed wing aircraft should set it the same way as multicopters. Runway-landing fixed wing aircraft should set it to the opposite direction of the takeoff, assuming the takeoff happened from the threshold / touchdown zone.")
    @Units("m")
    public float approach_x;
    
    /**
     * Local Y position of the end of the approach vector. Multicopters should set this position based on their takeoff path. Grass-landing fixed wing aircraft should set it the same way as multicopters. Runway-landing fixed wing aircraft should set it to the opposite direction of the takeoff, assuming the takeoff happened from the threshold / touchdown zone.
     */
    @Description("Local Y position of the end of the approach vector. Multicopters should set this position based on their takeoff path. Grass-landing fixed wing aircraft should set it the same way as multicopters. Runway-landing fixed wing aircraft should set it to the opposite direction of the takeoff, assuming the takeoff happened from the threshold / touchdown zone.")
    @Units("m")
    public float approach_y;
    
    /**
     * Local Z position of the end of the approach vector. Multicopters should set this position based on their takeoff path. Grass-landing fixed wing aircraft should set it the same way as multicopters. Runway-landing fixed wing aircraft should set it to the opposite direction of the takeoff, assuming the takeoff happened from the threshold / touchdown zone.
     */
    @Description("Local Z position of the end of the approach vector. Multicopters should set this position based on their takeoff path. Grass-landing fixed wing aircraft should set it the same way as multicopters. Runway-landing fixed wing aircraft should set it to the opposite direction of the takeoff, assuming the takeoff happened from the threshold / touchdown zone.")
    @Units("m")
    public float approach_z;
    
    /**
     * System ID.
     */
    @Description("System ID.")
    @Units("")
    public short target_system;
    
    /**
     * Timestamp (UNIX Epoch time or time since system boot). The receiving end can infer timestamp format (since 1.1.1970 or since system boot) by checking for the magnitude of the number.
     */
    @Description("Timestamp (UNIX Epoch time or time since system boot). The receiving end can infer timestamp format (since 1.1.1970 or since system boot) by checking for the magnitude of the number.")
    @Units("us")
    public long time_usec;
    

    /**
     * Generates the payload for a mavlink message for a message of this type
     * @return
     */
    @Override
    public MAVLinkPacket pack() {
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH,isMavlink2);
        packet.sysid = sysid;
        packet.compid = compid;
        packet.msgid = MAVLINK_MSG_ID_SET_HOME_POSITION;

        packet.payload.putInt(latitude);
        packet.payload.putInt(longitude);
        packet.payload.putInt(altitude);
        packet.payload.putFloat(x);
        packet.payload.putFloat(y);
        packet.payload.putFloat(z);
        
        for (int i = 0; i < q.length; i++) {
            packet.payload.putFloat(q[i]);
        }
                    
        packet.payload.putFloat(approach_x);
        packet.payload.putFloat(approach_y);
        packet.payload.putFloat(approach_z);
        packet.payload.putUnsignedByte(target_system);
        
        if (isMavlink2) {
             packet.payload.putUnsignedLong(time_usec);
            
        }
        return packet;
    }

    /**
     * Decode a set_home_position message into this class fields
     *
     * @param payload The message to decode
     */
    @Override
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();

        this.latitude = payload.getInt();
        this.longitude = payload.getInt();
        this.altitude = payload.getInt();
        this.x = payload.getFloat();
        this.y = payload.getFloat();
        this.z = payload.getFloat();
        
        for (int i = 0; i < this.q.length; i++) {
            this.q[i] = payload.getFloat();
        }
                
        this.approach_x = payload.getFloat();
        this.approach_y = payload.getFloat();
        this.approach_z = payload.getFloat();
        this.target_system = payload.getUnsignedByte();
        
        if (isMavlink2) {
             this.time_usec = payload.getUnsignedLong();
            
        }
    }

    /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_set_home_position() {
        this.msgid = MAVLINK_MSG_ID_SET_HOME_POSITION;
    }

    /**
     * Constructor for a new message, initializes msgid and all payload variables
     */
    public msg_set_home_position( int latitude, int longitude, int altitude, float x, float y, float z, float[] q, float approach_x, float approach_y, float approach_z, short target_system, long time_usec) {
        this.msgid = MAVLINK_MSG_ID_SET_HOME_POSITION;

        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.x = x;
        this.y = y;
        this.z = z;
        this.q = q;
        this.approach_x = approach_x;
        this.approach_y = approach_y;
        this.approach_z = approach_z;
        this.target_system = target_system;
        this.time_usec = time_usec;
        
    }

    /**
     * Constructor for a new message, initializes everything
     */
    public msg_set_home_position( int latitude, int longitude, int altitude, float x, float y, float z, float[] q, float approach_x, float approach_y, float approach_z, short target_system, long time_usec, int sysid, int compid, boolean isMavlink2) {
        this.msgid = MAVLINK_MSG_ID_SET_HOME_POSITION;
        this.sysid = sysid;
        this.compid = compid;
        this.isMavlink2 = isMavlink2;

        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.x = x;
        this.y = y;
        this.z = z;
        this.q = q;
        this.approach_x = approach_x;
        this.approach_y = approach_y;
        this.approach_z = approach_z;
        this.target_system = target_system;
        this.time_usec = time_usec;
        
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a mavlink packet
     *
     */
    public msg_set_home_position(MAVLinkPacket mavLinkPacket) {
        this.msgid = MAVLINK_MSG_ID_SET_HOME_POSITION;

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
        return "MAVLINK_MSG_ID_SET_HOME_POSITION - sysid:"+sysid+" compid:"+compid+" latitude:"+latitude+" longitude:"+longitude+" altitude:"+altitude+" x:"+x+" y:"+y+" z:"+z+" q:"+q+" approach_x:"+approach_x+" approach_y:"+approach_y+" approach_z:"+approach_z+" target_system:"+target_system+" time_usec:"+time_usec+"";
    }

    /**
     * Returns a human-readable string of the name of the message
     */
    @Override
    public String name() {
        return "MAVLINK_MSG_ID_SET_HOME_POSITION";
    }
}
        