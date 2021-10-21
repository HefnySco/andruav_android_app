package rcmobile.andruavmiddlelibrary.webrtc.classes;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <h1>Used for debug messages and static definitions of JSON keys</h1>
 * <pre>
 * Author:  Kevin Gleason - Boston College '16
 * File:    PnRTCResala.java
 * Date:    7/20/15
 * Use:     Debug messages and JSON key definitions
 * &copy; 2009 - 2015 PubNub, Inc.
 * </pre>
 */
   class PnRTCResala extends JSONObject {
    public static final String JSON_TYPE       = "type";
    public static final String JSON_PACKET     = "packet";
    public static final String JSON_ID         = "id";
    public static final String JSON_NUMBER     = "number"; // Todo: Change to more accurate name.
    public static final String JSON_MESSAGE    = "message";
    public static final String JSON_USERMSG    = "usermsg";
    public static final String JSON_HANGUP     = "hangup";
    public static final String JSON_THUMBNAIL  = "thumbnail";
    public static final String JSON_SDP        = "sdp";
    public static final String JSON_ICE        = "candidate"; // Identify ICE
    public static final String JSON_JOINME     = "joinme";
    public static final String JSON_CHANNEL    = "channel";

    private final String message;
    private final JSONObject json;

    public PnRTCResala(String message){
        super();
        try {
            this.put(JSON_MESSAGE, message);
        } catch (JSONException e){
            throw new RuntimeException("Invalid JSON Payload");
        }
        this.message = message;
        this.json = this;
    }

    public PnRTCResala(JSONObject json){
        super();
        try {
            this.put(JSON_MESSAGE, json);
        } catch (JSONException e){
            throw new RuntimeException("Invalid JSON Payload");
        }
        this.message = json.toString();
        this.json = this;
    }

    public String getMessage() {
        return message;
    }

    public JSONObject getJSON(){
        return this.json;
    }
}
