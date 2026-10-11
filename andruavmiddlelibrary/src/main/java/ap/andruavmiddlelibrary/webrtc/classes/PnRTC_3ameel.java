package ap.andruavmiddlelibrary.webrtc.classes;

import com.andruav.andruavUnit.AndruavUnitBase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;

import java.util.ArrayList;
import java.util.List;

import ap.andruavmiddlelibrary.preference.Preference;


/**
 * <h1>Main WebRTC Signaling class, holds all functions to set up {@link org.webrtc.PeerConnection}</h1>
 * <pre>
 * Author:  Kevin Gleason - Boston College '16
 * File:    PnRTC_3ameel.java
 * Date:    7/20/15
 * Use:     PubNub WebRTC Signaling
 * &copy; 2009 - 2015 PubNub, Inc.
 * </pre>
 */
public class PnRTC_3ameel {
   // private PnSignalingParams pnSignalingParams;
    private final PeerConnectionClientBase pcClient;

    /**
     * Slightly more verbose constructor. Requires a valid Pub and Sub key. Get your Pub/Sub keys for free at
     *  https://admin.pubnub.com/#/register and find keys on developer portal.
     *
     */
    public PnRTC_3ameel(final PeerConnectionFactory pcFactory) {


        this.pcClient = new AndruavPeerConnectionClientClient(pcFactory, setIceServers(), new PnRTCListener() {});

    }


    protected PnSignalingParams setIceServers()
    {

        final MediaConstraints pcConstraints = new MediaConstraints();
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        // Actual capture resolution is fixed directly on the VideoCapturer by
        // PeerConnectionManager (per-facing Camera Resolution preference); videoConstraints here
        // is legacy getUserMedia-style SDP constraint plumbing with no reader (PnRTC_3ameel#videoConstraints()
        // is never called), so it's left at the default rather than tied to a resolution preference.
        PnSignalingParams params = new PnSignalingParams(pcConstraints, null, null);



        // ICE servers: built-in defaults unless the "STUN Server" preference holds either
        //  - a JSON list (same entries as "iceServers" in de_camera.config.module.json), which replaces the defaults, or
        //  - a single plain "stun:host:port", which is added in front of the defaults.
        final String custom = Preference.getSTUNServer(null).trim();
        if (!custom.isEmpty()) {
            if (custom.startsWith("[") || custom.startsWith("{")) {
                final List<PeerConnection.IceServer> parsed = parseIceServers(custom);
                if (!parsed.isEmpty()) {
                    params.iceServers = parsed;
                }
            } else {
                params.addIceServers(new PeerConnection.IceServer(
                        custom.contains("stun:") || custom.contains("turn:") ? custom : "stun:" + custom, "", ""));
            }
        }

        return params;


    }

    /**
     * Parse <code>{ "urls": "...", "username": "...", "password": "..." }, ...</code> entries.
     * Surrounding [ ] are optional so the list can be pasted straight from the config file.
     * Returns an empty list if the text is invalid.
     */
    static List<PeerConnection.IceServer> parseIceServers(final String text) {
        final List<PeerConnection.IceServer> servers = new ArrayList<>();
        try {
            String json = text.trim();
            if (json.startsWith("{")) {
                json = "[" + json.replaceAll(",\\s*$", "") + "]";
            }
            final JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                final JSONObject o = array.getJSONObject(i);
                final List<String> urls = new ArrayList<>();
                final Object u = o.get("urls");
                if (u instanceof JSONArray) {
                    for (int k = 0; k < ((JSONArray) u).length(); k++) urls.add(((JSONArray) u).getString(k));
                } else {
                    urls.add(u.toString());
                }
                servers.add(PeerConnection.IceServer.builder(urls)
                        .setUsername(o.optString("username", ""))
                        .setPassword(o.optString("password", ""))
                        .createIceServer());
            }
        } catch (JSONException | IllegalArgumentException e) {
            android.util.Log.e("PnRTC", "Invalid ICE servers list in preferences, using defaults: " + e.getMessage());
            servers.clear();
        }
        return servers;
    }

    /**
     * Return the {@link PnRTC_3ameel} peer connection constraints.
     * @return Peer Connection Constrains
     */
    public MediaConstraints pcConstraints() {
        return pcClient.signalingParams.pcConstraints;
    }

    /**
     * Return the {@link PnRTC_3ameel} video constraints.
     * @return Video Constrains
     */
    public MediaConstraints videoConstraints() {
        return this.pcClient.signalingParams.videoConstraints;
    }

    /**
     * Return the {@link PnRTC_3ameel} audio constraints.
     * @return Audio Constrains
     */
    public MediaConstraints audioConstraints() {
        return pcClient.signalingParams.audioConstraints;
    }



    /**
     * Need to attach mediaStream before you can connect.
     * @param mediaStream Not null local media stream
     */
    public void attachLocalMediaStream(MediaStream mediaStream){
        this.pcClient.setLocalMediaStream(mediaStream);
    }

    /**
     * Attach custom listener for callbacks!
     * @param listener The listener which extends PnRTCListener to implement callbacks
     */
    public void attachRTCListener(PnRTCListener listener){
        this.pcClient.setRTCListener(listener);
    }

    /**
     * Set the maximum simultaneous connections allowed
     * @param max Max simultaneous connections
     */
    public void setMaxConnections(int max){
        this.pcClient.MAX_CONNECTIONS = max;
    }


    /**
     * Connect with another user by their ID.
     * @param userId The user to establish a WebRTC connection with
     */
    public void connect(String userId,final AndruavUnitBase andruavUnitBase){
        this.pcClient.connect(userId,PeerConnectionManager.CameraID, andruavUnitBase);
    }


    public void joinStream (final String userId, final String channel){
        this.pcClient.joinStream(userId,channel);
    }


    public boolean hasActivePeers ()
    {
        return (this.pcClient.peers.size() !=0);
    }

    /**
     * Close a single peer connection. Send a PubNub hangup signal as well
     * @param userId User to close a connection with
     */
    public void closeConnection(final String userId, final String channel){
        this.pcClient.closeConnection(userId,channel);
    }

    /**
     * Close all peer connections. Send a PubNub hangup signal as well.
     */
    public void closeAllConnections(){
        this.pcClient.closeAllConnections();
    }

    /**
     * Send a custom JSONObject user message to a single peer.
     * @param userId user to sendMessageToModule a message to
     * @param message the JSON message to pass to a peer.
     */
    public void transmit(String userId, JSONObject message){
        JSONObject usrMsgJson = new JSONObject();
        try {
            usrMsgJson.put(PnRTCResala.JSON_USERMSG, message);
            this.pcClient.transmitMessage(userId, usrMsgJson,PeerConnectionManager.CameraID,0);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }


    /**
     * Call this method in Activity.onDestroy() to clost all open connections and clean up
     *   instance for garbage collection.
     */
    public void onDestroy() {
        this.pcClient.unInit();
        this.pcClient.closeAllConnections();

    }

}
