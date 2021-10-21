package rcmobile.andruavmiddlelibrary.webrtc.classes;

import android.os.Handler;
import android.util.Log;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import rcmobile.andruavmiddlelibrary.webrtc.events.Event_WebRTC;

import static rcmobile.andruavmiddlelibrary.webrtc.classes.PnPeer.STATUS_DISCONNECTED;

/**
 * Created by mhefny on 4/28/16.
 */
public class PeerConnectionClientBase {

    SessionDescription localSdp  = null; // either offer or answer SDP
    MediaStream localMediaStream = null;
    int MAX_CONNECTIONS = Integer.MAX_VALUE;
    PeerConnectionFactory pcFactory;
    PnRTCListener mRtcListener;
    PnSignalingParams signalingParams;
    protected int answerRecieved = 0;
    protected Map<String,PnAction> actionMap;
    protected Map<String,PnPeer> peers;
    protected Handler mhandler;
    private Iterator<String> peerIds;



    public void setLocalMediaStream(MediaStream localStream){
        this.localMediaStream = localStream;
        mRtcListener.onLocalStream(localStream);
    }

    public MediaStream getLocalMediaStream(){
        return this.localMediaStream;
    }




    protected PnPeer addPeer(final String id, final String channel, final AndruavUnitBase andruavUnitBase) {
        PnPeer peer = new PnPeer(id, channel, this, andruavUnitBase);
        peers.put(id + channel, peer);
        return peer;
    }


    protected PnPeer removePeer(final String id, final String channel) {
        final String pid = id+channel;
        final PnPeer peer = peers.get(pid);
        mhandler.post(new Runnable() {
            @Override
            public void run() {
                final PnPeer peer = peers.get(pid);
                try {
                    if (peer.getStatus()!=STATUS_DISCONNECTED) {
                        peer.pc.close();
                    }
                    peers.remove(pid);
                    /*if (peerIds != null) {
                        peerIds.remove();
                        return ;
                    }*/
                }
                catch (final Exception ex)
                {
                    // possibke exception leads to skip removing step.
                    return ;
                }
            }
        });

        return peer;
    }

    protected List<PnPeer> getPeers(){
        return new ArrayList<PnPeer>(this.peers.values());
    }


    void transmitMessage(final String toID, JSONObject packet, final String channel, final int delay){
        return;
    }


    protected void init(){
        this.actionMap = new HashMap<String, PnAction>();
        this.actionMap.put(CreateOfferAction.TRIGGER,     new CreateOfferAction());
        this.actionMap.put(CreateAnswerAction.TRIGGER,    new CreateAnswerAction());
        this.actionMap.put(SetRemoteSDPAction.TRIGGER,    new SetRemoteSDPAction());
        this.actionMap.put(AddIceCandidateAction.TRIGGER, new AddIceCandidateAction());
        this.actionMap.put(PnUserHangupAction.TRIGGER,    new PnUserHangupAction());
        this.actionMap.put(PnUserMessageAction.TRIGGER,   new PnUserMessageAction());
        this.actionMap.put(PnUserJoinMeAction.TRIGGER,   new PnUserJoinMeAction());

    }


    public void unInit()
    {}



    /**
     * Close connection (hangup) no a certain peer.
     * @param id PnPeer id to close connection with
     */
    public void closeConnection(final String id, final String channel){
        JSONObject packet = new JSONObject();
        try {
            if (!this.peers.containsKey(id+channel)) return;
            PnPeer peer = this.peers.get(id+channel);
            peer.hangup();
            packet.put(PnRTCResala.JSON_HANGUP, true);
            transmitMessage(id, packet,channel,0);
            mRtcListener.onPeerConnectionClosed(peer);
        } catch (Exception e){
            AndruavEngine.log().logException("rtc", e);
        }
    }

    /**
     * Close connections (hangup) on all open connections.
     */
    public void closeAllConnections() {
        peerIds = this.peers.keySet().iterator();
        while (peerIds.hasNext()) {
            PnPeer p = this.peers.get(peerIds.next());
            closeConnection(p.id,p.mChannel);
        }
        peerIds = null;
    }


    public void setRTCListener(PnRTCListener listener){
        this.mRtcListener = listener;
    }





    /**TODO: Add a max user threshold.
     * Connect with another user by their ID.
     * @param userId The user to establish a WebRTC connection with
     * @return boolean value of success
     */
    boolean connect(final String userId, final String channel,final AndruavUnitBase andruavUnitBase) {
        if (!peers.containsKey(userId)) { // Prevents duplicate dials.
            if (peers.size() < MAX_CONNECTIONS) {
                final PnPeer peer = addPeer(userId, channel, andruavUnitBase);
               // if (localMediaStream!=null)  peer.pc.addStream(this.localMediaStream);  //DONT addStream ... u r GCS ... why do u want to add a stream?
                try {
                    actionMap.get(CreateOfferAction.TRIGGER).execute(userId,channel, new JSONObject());
                } catch (JSONException e){
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        }
        this.mRtcListener.onDebug(new PnRTCResala("CONNECT FAILED. Duplicate dial or max peer " +
                "connections exceeded. Max: " + MAX_CONNECTIONS + " Current: " + this.peers.size()));
        return false;
    }

    /***
     * Join a Stream of a Broadcaster... Receive Stream
     * @param userId
     * @return
     */
    boolean joinStream(final String userId, String channel) {

        try {
            if ((channel == null) || (channel.isEmpty()))
            {
                channel = "default";
            }
            AndruavEngine.getEventBus().post(new Event_WebRTC(userId, channel, Event_WebRTC.EVENT_CONNECTION_REQUEST));
            JSONObject packet = new JSONObject();
            packet.put(PnRTCResala.JSON_JOINME, true);
            transmitMessage(userId, packet,channel,0);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return false;
    }

    /////////////////////////////////////////////////////////////////////////////////


    protected interface PnAction{
        void execute(final String peerId, final String channel, final JSONObject payload) throws JSONException;
    }

    protected class CreateOfferAction implements PnAction{

        public static final String TRIGGER = "init";
        public void execute(final String peerId, final String channel, final JSONObject payload) throws JSONException {
            answerRecieved = 0;
            Log.d("offer action","CreateOfferAction");
            PnPeer peer = peers.get(peerId + channel);
            peer.setDialed(true);
            peer.setType(PnPeer.TYPE_ANSWER);
            peer.pc.createOffer(peer, signalingParams.pcConstraints);
        }
    }

    protected class CreateAnswerAction implements PnAction{
        public static final String TRIGGER = "offer";
        public void execute(final String peerId, final String channel, final JSONObject payload) throws JSONException {
            Log.d("offer action","CreateAnswerAction");
            PnPeer peer = peers.get(peerId + channel);
            peer.setType(PnPeer.TYPE_OFFER);
            peer.setStatus(PnPeer.STATUS_CONNECTED);
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);
            peer.pc.createAnswer(peer, signalingParams.pcConstraints);
        }
    }

    protected class SetRemoteSDPAction implements PnAction{
        public static final String TRIGGER = "answer";
        public void execute(final String peerId, final String channel, final JSONObject payload) throws JSONException {
            //  if (answerRecieved >2) return ;
            answerRecieved +=1;


            PnPeer peer = peers.get(peerId + channel);


            Log.d("offer action","SetRemoteSDPAction - iceConnectionState" + peer.pc.iceConnectionState().toString());


            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );

            String sdp_desc = sdp.description;
            sdp_desc = PnSignalingParams.preferCodec(sdp_desc, PnSignalingParams.VideoCodec.H264.toString(), false);
            SessionDescription sdpRemote = new SessionDescription(sdp.type, sdp_desc);


            peer.pc.setRemoteDescription(peer, sdpRemote);
        }
    }

    protected class AddIceCandidateAction implements PnAction{
        public static final String TRIGGER = "candidate";
        public void execute(final String peerId, final String channel, final JSONObject payload) throws JSONException {
            Log.d("offer action","AddIceCandidateAction");
            PeerConnection pc = peers.get(peerId + channel).pc;
            if (pc.getRemoteDescription() != null) {
                IceCandidate candidate = new IceCandidate(
                        payload.getString("sdpMid"),
                        payload.getInt("sdpMLineIndex"),
                        payload.getString("candidate")
                );
                pc.addIceCandidate(candidate);
            }
        }
    }

    protected class PnUserHangupAction implements PnAction{
        public static final String TRIGGER = PnRTCResala.JSON_HANGUP;
        public void execute(final String peerId, final String channel, final JSONObject payload) throws JSONException {
            Log.d("offer action","PnUserHangupAction");
            PnPeer peer = peers.get(peerId + channel);
            peer.hangup();
            mRtcListener.onPeerConnectionClosed(peer);
            // Todo: Consider Callback?
        }
    }

    protected class PnUserMessageAction implements PnAction{
        public static final String TRIGGER = PnRTCResala.JSON_USERMSG;
        public void execute(final String peerId, final String channel, final JSONObject payload) throws JSONException {
            Log.d("offer action","AddIceCandidateAction");
            JSONObject msgJson = payload.getJSONObject(PnRTCResala.JSON_USERMSG);
            PnPeer peer = peers.get(peerId + channel);
            mRtcListener.onMessage(peer, msgJson);
        }
    }


    protected class PnUserJoinMeAction implements PnAction{
        public static final String TRIGGER = PnRTCResala.JSON_JOINME;
        public void execute(final String peerId, final String channel, final JSONObject payload) throws JSONException {
            actionMap.get(CreateOfferAction.TRIGGER).execute(peerId, channel, new JSONObject());

        }
    }



}
