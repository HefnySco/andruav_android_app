package rcmobile.FPV.communication.telemetry;

import rcmobile.FPV.communication.telemetry.SerialSocketServer.Event_SocketData;

/**
 * Created by mhefny on 12/30/16.
 */

public interface IEvent_SocketData {

    void onSocketData (final Event_SocketData event);
}
