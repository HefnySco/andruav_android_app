package ap.andruav_ap.communication.telemetry;

import com.andruav.event.fcb_event.Event_SocketData;

/**
 * Created by mhefny on 12/30/16.
 */

public interface IEvent_SocketData {

    void onSocketData (final Event_SocketData event);
}
