package ap.andruav_ap.guiEvent;

/***
 * AN-0052: posted by {@link ap.andruav_ap.communication.AndruavDeBusClient}
 * each time the comm-server link state de_comm reports on the databus
 * edges (the 9100 module-ID {@code ms.g} socket status and the 1061
 * {@code d_cl_*} events of F5/AN-0072), and when the bus client is torn
 * down. In native mode this - not the WS socket state, which follows the
 * module socket lifetime and survives a dropped server link - is what
 * the Com tile and the connection-gated menu items must read.
 */
public class GUIEvent_CommLineChanged {

    /*** The new comm-line state; also {@code false} when the bus client shut down. */
    public final boolean online;

    public GUIEvent_CommLineChanged(final boolean online) {
        this.online = online;
    }
}
