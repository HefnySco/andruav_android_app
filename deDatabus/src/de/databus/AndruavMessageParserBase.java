package de.databus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;

import static de.databus.Messages.*;

/**
 * Java port of de_databus/python/de_message_parser_base.py —
 * equivalent to C++ CAndruavMessageParserBase.
 * Abstract base class for parsing Andruav messages.
 */
public abstract class AndruavMessageParserBase {

    private boolean _is_binary = false;
    private boolean _is_system = false;
    private boolean _is_inter_module = false;
    protected CFacade_Base _facade = null;  // set by subclass

    public void parse_message(JSONObject andruav_message, String full_message,
                              int full_message_length) {
        parse_message(andruav_message, (Object) full_message, full_message_length);
    }

    public void parse_message(JSONObject andruav_message, byte[] full_message,
                              int full_message_length) {
        parse_message(andruav_message, (Object) full_message, full_message_length);
    }

    private void parse_message(JSONObject andruav_message, Object full_message,
                               int full_message_length) {
        int message_type = andruav_message.optInt(ANDRUAV_PROTOCOL_MESSAGE_TYPE, 0);

        // Check if message is binary (doesn't end with '}')
        if (full_message instanceof byte[]) {
            this._is_binary = true;
        } else {
            String s = (String) full_message;
            this._is_binary = !(s.endsWith("}")
                    || (s.length() > 1 && s.charAt(s.length() - 2) == '}'));
        }

        // Get permission if available
        int permission = 0;
        if (_validate_field(andruav_message, ANDRUAV_PROTOCOL_MESSAGE_PERMISSION, Number.class)) {
            permission = ((Number) andruav_message.get(ANDRUAV_PROTOCOL_MESSAGE_PERMISSION)).intValue();
        }

        // Check if message is from system
        this._is_system = _validate_field(andruav_message, ANDRUAV_PROTOCOL_SENDER, String.class)
                && ANDRUAV_PROTOCOL_SENDER_COMM_SERVER.equals(
                        andruav_message.get(ANDRUAV_PROTOCOL_SENDER));

        // Check if message is inter-module
        this._is_inter_module = _validate_field(andruav_message, INTERMODULE_ROUTING_TYPE, String.class)
                && CMD_TYPE_INTERMODULE.equals(andruav_message.get(INTERMODULE_ROUTING_TYPE));

        // Handle RemoteExecute messages
        if (message_type == TYPE_AndruavMessage_RemoteExecute) {
            parse_remote_execute(andruav_message);
            return;
        }

        // Parse default commands and custom commands
        _parse_default_command(andruav_message, full_message, full_message_length,
                message_type, permission);
        parse_command(andruav_message, full_message, full_message_length, message_type, permission);
    }

    private void _parse_default_command(JSONObject andruav_message, Object full_message,
                                        int full_message_length, int message_type, int permission) {
        Object cmd = andruav_message.opt(ANDRUAV_PROTOCOL_MESSAGE_CMD);
        if (!(cmd instanceof JSONObject)) cmd = new JSONObject();

        if (message_type == TYPE_AndruavMessage_CONFIG_ACTION) {
            _handle_config_action(andruav_message, (JSONObject) cmd);
        }
    }

    private void _handle_config_action(JSONObject andruav_message, JSONObject cmd) {
        String module_key = "";

        // Validate action field
        if (!_validate_field(cmd, "a", Number.class)) return;

        // Check module key if specified
        if (_validate_field(cmd, "b", String.class)) {
            module_key = _get_module_key();
            if (!module_key.equals(cmd.get("b"))) return;
        }

        int action = ((Number) cmd.get("a")).intValue();

        if (action == CONFIG_ACTION_Restart) {
            System.exit(0);
        } else if (action == CONFIG_ACTION_APPLY_CONFIG) {
            Object config = cmd.opt("c");
            System.out.println(config);
            ConfigFile config_file = ConfigFile.get_instance();
            config_file.update_json(config instanceof JSONObject ? config.toString() : "{}");
        } else if (action == CONFIG_REQUEST_FETCH_CONFIG_TEMPLATE) {
            if (!andruav_message.has(ANDRUAV_PROTOCOL_SENDER)) return;

            String sender = andruav_message.getString(ANDRUAV_PROTOCOL_SENDER);

            System.out.println("\nCONFIG_REQUEST_FETCH_CONFIG_TEMPLATE");

            try {
                String file_content = new String(Files.readAllBytes(Paths.get("template.json")),
                        StandardCharsets.UTF_8);
                JSONObject file_content_json = new JSONObject(file_content);
                this._facade.API_sendConfigTemplate(sender, module_key, file_content_json, true);
            } catch (IOException | org.json.JSONException e) {
                System.out.println("\ncannot read template.json");
                if (this._facade != null) {
                    this._facade.sendErrorMessage("", 0, ERROR_3DR,
                            NOTIFICATION_TYPE_ERROR, "cannot read template.json");
                    this._facade.API_sendConfigTemplate(sender, module_key, new JSONObject(), true);
                }
            }
        }
        // CONFIG_REQUEST_FETCH_CONFIG: not implemented (same as Python)
    }

    private boolean _validate_field(JSONObject json_obj, String field_name, Class<?> expected_type) {
        return json_obj.has(field_name) && expected_type.isInstance(json_obj.get(field_name));
    }

    /** Module key - subclass injects its own. */
    protected String _get_module_key() {
        return "";
    }

    public boolean is_binary()        { return _is_binary; }
    public boolean is_system()        { return _is_system; }
    public boolean is_inter_module()  { return _is_inter_module; }

    /** Parse remote execute commands - must be implemented by subclass. */
    public abstract void parse_remote_execute(JSONObject andruav_message);

    /** Parse custom commands - must be implemented by subclass. */
    public abstract void parse_command(JSONObject andruav_message, Object full_message,
                                       int full_message_length, int message_type, int permission);
}
