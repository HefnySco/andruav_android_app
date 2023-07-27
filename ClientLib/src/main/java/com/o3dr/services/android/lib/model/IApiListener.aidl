package com.o3dr.services.android.lib.model;

import com.o3dr.services.android.lib.drone.connection.ConnectionResult;

/**
* DroneAPI event listener. A valid instance must be provided at api registration.
*/
interface IApiListener {

    /**
    * Retrieve the version code for the api.
    */
    int getApiVersionCode();

    /**
    * https://source.android.com/docs/core/architecture/aidl/aidl-annotations#deprecated_in_comments
    * Called when the connection attempt fails.
    * @param result Describe why the connection failed.
    */
    oneway void onConnectionFailed(in ConnectionResult result);

    /**
    * Retrieve the version code for the connected client.
    */
    int getClientVersionCode();
}