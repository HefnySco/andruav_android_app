package com.andruav.controlBoard;


/**
 * Created by mhefny on 8/27/16.
 */
public interface IControlBoard_Callback {



    void OnSuccess ();

    void OnFailue (int code);

    void OnTimeout ();

}
