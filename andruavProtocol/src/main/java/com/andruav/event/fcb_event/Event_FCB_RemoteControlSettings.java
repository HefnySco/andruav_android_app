package com.andruav.event.fcb_event;

public class Event_FCB_RemoteControlSettings {


    /***
     * RX Channels will be supplied by AndruavDrone.
     * Ground Pilot using RX Channel will not be able to control drone in anyway using TX device.
     */

    // there is no RCChannel info sent to Drone.
    public static final int RC_SUB_ACTION_RELEASED                      = 0;
    // 1500 channels values are sent. TX is no longer effective.
    public static final int RC_SUB_ACTION_CENTER_CHANNELS               = 1;
    // last TX readings are freezed and sent as fixed values. TX is no longer effective.
    public static final int RC_SUB_ACTION_FREEZE_CHANNELS               = 2;
    // RCChannels is being sent to Drone. TX  is no longer effective for some channels.
    public static final int RC_SUB_ACTION_JOYSTICK_CHANNELS             = 4; // like mission planner joystick
    // Velocity is sent for Thr, Pitch, Roll , YAWRate ... applicable in Arducopter and Rover
    // Drone may switch {@link _7adath_FCB_RemoteControlSettings#RC_SUB_ACTION_JOYSTICK_CHANNELS} to this automatically if drone mode is guided.
    public static final int RC_SUB_ACTION_JOYSTICK_CHANNELS_GUIDED      = 8; // like mission planner joystick



    public int rcSubAction;


    public Event_FCB_RemoteControlSettings(final int rcsubsction)
    {
        rcSubAction = rcsubsction;
    }


}
