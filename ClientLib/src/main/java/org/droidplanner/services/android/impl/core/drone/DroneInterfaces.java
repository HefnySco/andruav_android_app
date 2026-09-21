package org.droidplanner.services.android.impl.core.drone;

import android.os.Bundle;

import com.o3dr.services.android.lib.drone.property.Parameter;

import org.droidplanner.services.android.impl.core.drone.autopilot.Drone;

public class DroneInterfaces {

    /**
     * Sets of drone events used for broadcast throughout the app.
     */
    public enum DroneEventsType {
        /**
         * Denotes vehicle altitude change event.
         */
        ALTITUDE,

        /**
         *
         */
        ORIENTATION,

        /**
         * Denotes vehicle speed change event.
         */
        SPEED,

        /**
         *
         */
        BATTERY,

        /**
         *
         */
        GUIDEDPOINT,

        /**
         * Denotes vehicle attitude change event.
         */
        ATTITUDE,

        /**
         *
         */
        RADIO,

        /**
         *
         */
        ARMING,

        /**
         *
         */
        AUTOPILOT_WARNING,

        /**
         *
         */
        MODE,

        /**
         *
         */
        STATE,

        /**
         *
         */
        MISSION_UPDATE,

        /**
         *
         */
        MISSION_RECEIVED,

        /**
         *
         */
        TYPE,

        /**
         *
         */
        HOME,

        /**
         *
         */
        HEARTBEAT_TIMEOUT,

        /**
         *
         */
        HEARTBEAT_FIRST,

        /**
         *
         */
        HEARTBEAT_RESTORED,

        /**
         *
         */
        DISCONNECTED,

        /**
         * Successful connection event.
         */
        CONNECTED,

        /**
         * Connection initiated event.
         */
        CONNECTING,

        /**
         *
         */
        MISSION_SENT,

        /**
         *
         */
        MISSION_WP_UPDATE,

        /**
         * Announces that a new version for the firmware has been received
         */
        FIRMWARE,

        /**
         * The ekf status was updated.
         */
        EKF_STATUS_UPDATE,

        /**
         * The horizontal position is ok, and the home position is available.
         */
        EKF_POSITION_STATE_UPDATE,

        /**
         * A mission item has been reached.
         */
        MISSION_WP_REACHED,


    }

    public interface OnDroneListener<T extends Drone> {
        void onDroneEvent(DroneEventsType event, T drone);
    }

    public interface AttributeEventListener {
        void onAttributeEvent(String attributeEvent, Bundle eventInfo);
    }

    public interface OnParameterManagerListener {
        void onBeginReceivingParameters();

        void onParameterReceived(Parameter parameter, int index, int count);

        void onEndReceivingParameters();
    }

}
