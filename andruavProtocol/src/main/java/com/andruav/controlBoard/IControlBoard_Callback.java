package com.andruav.controlBoard;


/**
 * Created by mhefny on 8/27/16.
 */
public interface IControlBoard_Callback {



    void OnSuccess ();

    /***
     * code can be from {@link MAV_RESULT class or MAV_CMD_ACK based on command}
     * @param code
     */
    void OnFailue (int code);

    void OnTimeout ();

}
