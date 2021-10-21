package rcmobile.andruavmiddlelibrary.webrtc.classes;

import android.util.Log;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.SignalingState;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import rcmobile.andruavmiddlelibrary.webrtc.events.Event_WebRTC;


/**
 * <h1>PubNub Peer object to hold information on {@link PeerConnection}</h1>
 * <pre>
 * Author:  Kevin Gleason - Boston College '16
 * File:    PnPeer.java
 * Date:    7/22/15
 * Use:     Store information about various Peer Connections
 * &copy; 2009 - 2015 PubNub, Inc.
 * </pre>
 */
public class PnPeer implements SdpObserver, PeerConnection.Observer {
    public static final String TAG = "PnPeer";
    public static final String STATUS_CONNECTING = "CONNECTING";
    public static final String STATUS_CONNECTED = "CONNECTED"; // TODO: Where to change status to this?
    public static final String STATUS_DISCONNECTED = "DISCONNECTED";
    public static final String TYPE_NONE = "NONE";
    public static final String TYPE_OFFER = "offer";
    public static final String TYPE_ANSWER = "answer";

    private final PeerConnectionClientBase pcClient;
    PeerConnection pc;
    String id;
    String type;
    String status;
    boolean dialed;
    boolean received;
    String mChannel;
    private final AndruavUnitBase mAndruavUnitBase;
    private MediaStream mMediaStream;

    // Todo: Maybe attach MediaStream as private var?

    public PnPeer(final String id, final String channel, PeerConnectionClientBase pcClient, final AndruavUnitBase andruavUnitBase) {
        Log.d(TAG, "new Peer: " + id);
        this.id = id;
        this.type = TYPE_NONE;
        this.dialed = false;
        this.received = false;
        this.pcClient = pcClient;
        this.mAndruavUnitBase = andruavUnitBase;

        //this.pc = pcClient.pcFactory.createPeerConnection().createPeerConnection(pcClient.signalingParams.iceServers,
        //        pcClient.signalingParams.pcConstraints, this);
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(pcClient.signalingParams.iceServers);
        /// EXTRA SETTINGS HERE IN rtcConfig
        this.pc = pcClient.pcFactory.createPeerConnection(rtcConfig, this);
        setStatus(STATUS_CONNECTING);
        //BUG: seems that pc can be null and crash the APP.
        // not sure why it can be NULL
        mChannel = channel;
        pc.addStream(pcClient.getLocalMediaStream());
    }

    public AndruavUnitBase getConnectedPeer ()
    {
        return mAndruavUnitBase;
    }

    public MediaStream getMediaStream()
    {
        return mMediaStream;
    }


    public synchronized void setStatus(String status) {
        this.status = status;
        pcClient.mRtcListener.onPeerStatusChanged(this);
    }

    public String getStatus() {
        return status;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public boolean isDialed() {
        return dialed;
    }

    public void setDialed(boolean dialed) {
        this.dialed = dialed;
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }

    public PeerConnection getPc() {
        return pc;
    }

    public String getId() {
        return id;
    }

    public void hangup() {
        if (this.status.equals(STATUS_DISCONNECTED)) return; // Already hung up on.
        this.pcClient.removePeer(this.id, this.mChannel);
        setStatus(STATUS_DISCONNECTED);
    }

    @Override
    public void onCreateSuccess(final SessionDescription sdp) {
        // TODO: modify sdp to use pcParams prefered codecs
        try {
            String sdp_desc = sdp.description;
            sdp_desc = sdp_desc; //PnSignalingParams.preferCodec(sdp_desc, PnSignalingParams.VideoCodec.H264.toString(), true);
            //   Log.d("offer action","onCreateSuccess " + sdp_desc );
            JSONObject payload = new JSONObject();
            payload.put("type", sdp.type.canonicalForm());
            payload.put("sdp", sdp_desc);
            pcClient.transmitMessage(id, payload, mChannel, 0);
            pc.setLocalDescription(PnPeer.this, sdp);
        } catch (JSONException e) {
            AndruavEngine.log().logException("rtc", e);
        }
    }


    @Override
    public void onSetSuccess() {
        Log.d("offer action", "onSetSuccess ");
    }

    @Override
    public void onCreateFailure(String s) {
        // Log.d("offer action","onCreateFailure " + s.toString());
        AndruavEngine.getEventBus().post(new Event_WebRTC(this.id, this.mChannel, Event_WebRTC.EVENT_CONNECTION_ERROR));
    }

    @Override
    public void onSetFailure(String s) {
        //   Log.d("offer action","onSetFailure " + s.toString());
        //NOT ALWAYS A CORRECT FAULT
        ///AndruavMo7arek.getEventBus().post(new Event_WebRTC(this.id, this.mChannel, Event_WebRTC.EVENT_CONNECTION_ERROR));
    }

    @Override
    public void onSignalingChange(SignalingState signalingState) {
        // Log.d("offer action","onSignalingChange " + signalingState.toString());

        switch (signalingState) {
            case CLOSED:
                AndruavEngine.getEventBus().post(new Event_WebRTC(this.id, this.mChannel, Event_WebRTC.EVENT_CLOSED_CONNECTION));
                break;
            case STABLE:
                //AndruavMo7arek.getEventBus().post(new Event_WebRTC(this.id, this.mChannel, Event_WebRTC.EVENT_CONNECT_SUCCEEDED));
                break;
            case HAVE_LOCAL_OFFER:

                break;
            case HAVE_REMOTE_OFFER:
                if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
                    AndruavEngine.getEventBus().post(new Event_WebRTC(this.id, this.mChannel, Event_WebRTC.EVENT_CONNECTION_REQUEST));
                }
                break;

        }

    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        try {
            //   Log.d("offer action","onIceConnectionChange " + iceConnectionState.toString());
            if (this.status.equals(STATUS_DISCONNECTED)) return; // Already hung up on.
            if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                setStatus(STATUS_DISCONNECTED);
                pcClient.removePeer(id, mChannel); // TODO: Ponder. Also, might want to Pub a disconnect.
                AndruavEngine.getEventBus().post(new Event_WebRTC(this.id, this.mChannel, Event_WebRTC.EVENT_CLOSED_CONNECTION));
            }
            else
            {
                if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                    Log.d("Ice Connection","Connected");
                    setStatus(STATUS_CONNECTED);
                    AndruavEngine.getEventBus().post(new Event_WebRTC(this.id, this.mChannel, Event_WebRTC.EVENT_CONNECT_SUCCEEDED));
                }
            }
        } catch (Exception e) {
            AndruavEngine.log().logException("rtc", e);
        }
    }

    // Todo: Look into what this should be used for
    @Override
    public void onIceConnectionReceivingChange(boolean iceConnectionReceivingChange) {
        // Log.d("offer action","onIceConnectionReceivingChange");

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        //  Log.d("offer action","onIceGatheringChange");
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        try {
            //  Log.d("offer action","onIceCandidate");

            JSONObject payload = new JSONObject();
            payload.put("sdpMLineIndex", candidate.sdpMLineIndex);
            payload.put("sdpMid", candidate.sdpMid);
            payload.put("candidate", candidate.sdp);
            pcClient.transmitMessage(id, payload, mChannel, 0);
        } catch (Exception e) {
            AndruavEngine.log().logException("rtc", e);
        }
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        try {
            // Log.d("offer action", "onAddStream " + mediaStream.label());
            // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
            this.mMediaStream = mediaStream;
            pcClient.mRtcListener.onAddRemoteStream(mediaStream, PnPeer.this);
        } catch (Exception e) {
            AndruavEngine.log().logException("rtc", e);
        }
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        try {
            //    Log.d("offer action", "onRemoveStream " + mediaStream.label());
            PnPeer peer = pcClient.removePeer(id, mChannel);
            pcClient.mRtcListener.onRemoveRemoteStream(mediaStream, peer);
        } catch (Exception e) {
            AndruavEngine.log().logException("rtc", e);
        }
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        //  Log.d("offer action", "onDataChannel " + dataChannel.toString());
    }

    @Override
    public void onRenegotiationNeeded() {
        //Log.d(TAG, "onRemoveStream ");

    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

    }

    @Override
    public void onTrack(RtpTransceiver transceiver) {

    }

    /**
     * Overriding toString for debugging purposes.
     *
     * @return String representation of a peer.
     */
    @Override
    public String toString() {
        return this.id + " Status: " + this.status + " Dialed: " + this.dialed +
                " Received: " + this.received + " Type: " + this.type;
    }
}