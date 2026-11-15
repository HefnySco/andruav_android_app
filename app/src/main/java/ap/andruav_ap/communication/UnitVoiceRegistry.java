package ap.andruav_ap.communication;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitMapBase;
import com.andruav.protocol.commands.textMessages.AndruavMessage_String;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * App-side registry of remote-unit capability namespaces (VC-9).
 *
 * The protocol parser has no typed class for the unit-capabilities message (1089), so it
 * arrives as an {@link AndruavMessage_String} and stays unprocessed - which means the WS
 * client posts the raw {@link Andruav_2MR} on the EventBus. We sniff those posts for the
 * {"caps":{...}} payload shape and keep the advertised namespace set per partyID, so the
 * voice UI can gate Listen/Talk on ns=microphone / ns=speaker like the webclient does.
 */
public class UnitVoiceRegistry {

    public static final String NS_MICROPHONE = "microphone";
    public static final String NS_SPEAKER = "speaker";

    private static final UnitVoiceRegistry sInstance = new UnitVoiceRegistry();

    private final Map<String, Set<String>> mCapsByParty = new HashMap<>();
    private boolean mRegistered = false;

    public static synchronized void init() {
        if (sInstance.mRegistered) return;
        sInstance.mRegistered = true;
        EventBus.getDefault().register(sInstance);
    }

    public static UnitVoiceRegistry getInstance() {
        return sInstance;
    }

    @Subscribe
    public void onEvent(final Andruav_2MR andruav2MR) {
        if ((andruav2MR == null) || (andruav2MR.partyID == null)) return;
        if (!(andruav2MR.andruavMessageBase instanceof AndruavMessage_String)) return;

        final String text = ((AndruavMessage_String) andruav2MR.andruavMessageBase).MessageText;
        if ((text == null) || !text.contains("caps")) return;

        try {
            final JSONObject payload = new JSONObject(text);
            final JSONObject caps = payload.optJSONObject("caps");
            if (caps == null) return;
            applyCaps(andruav2MR.partyID, caps, payload.optBoolean("full", false));
        }
        catch (Exception ignored) { }
    }

    private synchronized void applyCaps(final String partyID, final JSONObject caps, final boolean full) {
        Set<String> namespaces = mCapsByParty.get(partyID);
        if ((namespaces == null) || full) {
            namespaces = new HashSet<>();
            mCapsByParty.put(partyID, namespaces);
        }

        final Iterator<String> keys = caps.keys();
        while (keys.hasNext()) {
            final String namespace = keys.next();
            if (caps.isNull(namespace)) {
                namespaces.remove(namespace);
            }
            else {
                namespaces.add(namespace);
            }
        }

        if (namespaces.isEmpty()) mCapsByParty.remove(partyID);
    }

    public synchronized boolean hasCapability(final String partyID, final String namespace) {
        final Set<String> namespaces = mCapsByParty.get(partyID);
        return (namespaces != null) && namespaces.contains(namespace);
    }

    /**
     * Picks the remote unit to voice-dial: the GUI-activated unit when it advertises the
     * namespace, otherwise the first remote unit that does. Own unit and GCS parties are
     * never dialed.
     */
    public synchronized String findUnitWith(final String namespace) {
        final AndruavUnitMapBase unitMap = AndruavEngine.getAndruavWe7daMapBase();
        if (unitMap == null) return null;

        String fallback = null;
        for (int i = 0, count = unitMap.size(); i < count; ++i) {
            final AndruavUnitBase unit = unitMap.valueAt(i);
            if ((unit == null) || unit.IsMe() || unit.getIsCGS()) continue;
            if ((unit.PartyID == null) || !hasCapability(unit.PartyID, namespace)) continue;
            if (unit.isGUIActivated) return unit.PartyID;
            if (fallback == null) fallback = unit.PartyID;
        }
        return fallback;
    }
}
