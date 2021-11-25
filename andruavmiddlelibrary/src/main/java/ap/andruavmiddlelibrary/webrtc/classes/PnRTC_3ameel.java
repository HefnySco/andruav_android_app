package ap.andruavmiddlelibrary.webrtc.classes;

import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;

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
        if (AndruavSettings.andruavWe7daBase.getIsCGS())
        {
            pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
            pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
            pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        }
        else
        {
            pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
            pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
            pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        }

        MediaConstraints videoConstraints = null;
        if (Preference.useStreamVideoHD(null)) {
            // Enable HD
            videoConstraints = new MediaConstraints();
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", "1280"));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", "720"));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", "1280"));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight", "720"));
        }


        PnSignalingParams params = new PnSignalingParams(pcConstraints, videoConstraints, null);



        // Add Stun & Turn Servers [ICE SERVERS]

        if (!Preference.useLocalStunServerOnly(null)) {

            List<PeerConnection.IceServer> servers = new ArrayList<PeerConnection.IceServer>(); //XirSysRequest.getXirSysIceServers();
            // Add a local STUN anyway
            String stunIP = Preference.getSTUNServer(null);
            if (!stunIP.isEmpty() && !stunIP.equals("")) {

                if (stunIP.indexOf("stun:") == -1) {
                    stunIP = "stun:" + stunIP; // correct Human error if forget to add stun:
                }
                //params.iceServers.clear();
                // params.iceServers.add(params.iceServers.size(), new PeerConnection.IceServer(stunIP, "", ""));
                servers.add(servers.size(), new PeerConnection.IceServer(stunIP, "", ""));
            }

            if (!servers.isEmpty()) {
                //////  remove default ICE Servers params.iceServers.clear();
                for (int i =0, j = servers.size(); i<j; i = i +1)
                {
                    params.iceServers.add(i,servers.get(i));

                }

            }

        }

        else
        {
            // ONLY use LOCAL STUN
            String stunIP = Preference.getSTUNServer(null);
            if (stunIP.indexOf("stun:") == -1) {
                stunIP = "stun:" + stunIP; // correct Human error if forget to add stun:
            }
            params.iceServers.clear();
            params.iceServers.add(params.iceServers.size(), new PeerConnection.IceServer(stunIP, "", ""));

        }


        return params;


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
