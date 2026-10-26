package de.databus;

import org.json.JSONObject;

import static de.databus.Messages.*;

/**
 * Java port of de_databus/python/de_facade_base.py — facade helpers
 * modules subclass; thin pass-throughs onto CModule.
 */
public class CFacade_Base {

    protected final CModule m_module;

    public CFacade_Base(CModule m_module) {
        this.m_module = m_module;
    }

    public void requestID(String target_party_id) {
        JSONObject message = new JSONObject();
        message.put("C", TYPE_AndruavMessage_ID);
        this.m_module.sendJMSG(target_party_id, message, TYPE_AndruavMessage_RemoteExecute, true);
    }

    /**
     * EN: error number "not currently processed".
     * IT: info type indicate what component is reporting the error.
     * NT: severity and compliant with ardupilot.
     * DS: description message.
     */
    public void sendErrorMessage(String target_party_id, int error_number, int info_type,
                                 int notification_type, String description) {
        JSONObject message = new JSONObject();
        message.put("EN", error_number);
        message.put("IT", info_type);
        message.put("NT", notification_type);
        message.put("DS", description);

        this.m_module.sendJMSG(target_party_id, message, TYPE_AndruavMessage_Error, false);

        System.out.println("\n -- sendErrorMessage " + description);
    }

    public void API_sendConfigTemplate(String target_party_id, String module_key,
                                       JSONObject json_file_content_json, boolean reply) {
        JSONObject message = new JSONObject();
        message.put("a", CONFIG_STATUS_FETCH_CONFIG_TEMPLATE);
        message.put("b", json_file_content_json);
        message.put("k", module_key);
        message.put("R", reply);

        this.m_module.sendJMSG(target_party_id, message, TYPE_AndruavMessage_CONFIG_STATUS, false);
    }

    // Phase-3 capability advert helpers - pass-throughs to CModule.

    /** Register de.cap/1 advert(s); sets the ID "ch" hash and the
     *  MODULE_CAPABILITIES / CAPABILITY_INVOKE filter entries. */
    public boolean set_capabilities(java.util.List<Object> adverts) {
        return this.m_module.set_capabilities(adverts);
    }

    /** handler(ns, act, params) -> InvokeResult. Idempotent by invoke id. */
    public void on_invoke(CModule.InvokeHandler handler) {
        this.m_module.on_invoke(handler);
    }

    /** MODULE_STATE 6545: changed keys (or a full snapshot). */
    public void publish_state(String ns, JSONObject changed, boolean full) {
        this.m_module.publish_state(ns, changed, full);
    }

    /** Sync_EventFire d="<ns>.<ev>", ed=payload, fresh nonce. */
    public String fire_event(String ns, String ev, Object payload) {
        return this.m_module.fire_event(ns, ev, payload);
    }
}
