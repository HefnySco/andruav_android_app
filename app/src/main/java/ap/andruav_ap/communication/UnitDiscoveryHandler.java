package ap.andruav_ap.communication;

import com.andruav.AndruavFacade;

/**
 * Consolidates the handling of messages that arrive from a remote unit whose
 * {@link com.andruav.andruavUnit.AndruavUnitShadow} record is not yet known
 * locally (typically because no {@code AndruavMessage_ID} has been received
 * from it yet).
 * <p>
 * Previously this "unknown unit" path was duplicated inline in three branches
 * of {@link AndruavWSClient_TooTallNate#executeRemoteExecuteCMD} (STREAMVIDEO,
 * IMUCTRL and TELEMETRYCTRL), each carrying the same TODO about creating a
 * temporary record so replies can be sent before the unit advertises its full
 * ID. Centralising it here keeps a single place to implement that enhancement
 * and a single place to tune the discovery behaviour.
 */
public class UnitDiscoveryHandler {

    /**
     * Called when a command is received from a unit that is not present in the
     * local unit map. Current behaviour is to ask the unit for its ID; the
     * command itself is dropped until the unit replies.
     *
     * @param partyID the unknown unit's party identifier
     */
    public void handleUnknownUnit(final String partyID) {
        // TODO: enh upi may create a temp record just to be able to sendMessageToModule
        // data to this unit before it replies with full data. Currently we just ask
        // the unit to advertise its ID and drop the incoming command.
        AndruavFacade.requestID(partyID);
    }
}
