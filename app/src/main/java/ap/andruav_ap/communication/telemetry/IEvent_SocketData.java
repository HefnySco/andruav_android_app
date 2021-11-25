package ap.andruav_ap.communication.telemetry;

import ap.andruav_ap.communication.telemetry.SerialSocketServer.Event_SocketData;

/**
 * Created by mhefny on 12/30/16.
 */

public interface IEvent_SocketData {

    void onSocketData (final Event_SocketData event);
}
