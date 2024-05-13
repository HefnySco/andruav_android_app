package ap.andruavmiddlelibrary.webrtc.classes;


import android.os.Handler;
import android.os.Message;

import com.andruav.AndruavFacade;
import com.andruav.AndruavSettings;
import com.andruav.event.droneReport_Event.Event_Signalling;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnectionFactory;
import java.util.HashMap;
import de.greenrobot.event.EventBus;

/**
 * Created by mhefny on 4/28/16.
 */
public class AndruavPeerConnectionClientClient extends PeerConnectionClientBase {



    public void onEvent (final Event_Signalling a7adath_signalling)
    {


        if (!(a7adath_signalling.jsonObject instanceof JSONObject)) return; // Ignore if not valid JSON.


        final Message msg = mhandler.obtainMessage();
        msg.obj = a7adath_signalling;
        mhandler.sendMessageDelayed(msg,0);
    }

    private void initHandler ()
    {
        killHandler();

        mhandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                final Event_Signalling a7adath_signalling = (Event_Signalling) msg.obj;
                final JSONObject jsonMessage = a7adath_signalling.jsonObject;

                try {
                    mRtcListener.onDebug(new PnRTCResala(jsonMessage));

                    final String peerId     = jsonMessage.getString(PnRTCResala.JSON_NUMBER);
                    final String channel    = jsonMessage.getString(PnRTCResala.JSON_CHANNEL);
                    JSONObject packet = jsonMessage.getJSONObject(PnRTCResala.JSON_PACKET);
                    PnPeer peer;
                    if (!peers.containsKey(peerId+channel)){
                        if (packet.has(PnRTCResala.JSON_HANGUP))
                        {
                            // sometimes hangeout is sent back from watcher and you already removed pc from the list.
                            return ;
                        }

                        // Possibly threshold number of allowed users
                        peer = addPeer(peerId,channel, a7adath_signalling.andruavUnitBase);
                    } else {
                        peer = peers.get(peerId+channel);
                    }
                    if (peer.getStatus().equals(PnPeer.STATUS_DISCONNECTED)) return; // Do nothing if disconnected.


                    if (packet.has(PnRTCResala.JSON_JOINME)) {
                        actionMap.get(PnUserJoinMeAction.TRIGGER).execute(peerId,channel,packet);
                        return;
                    }
                    if (packet.has(PnRTCResala.JSON_USERMSG)) {
                        actionMap.get(PnUserMessageAction.TRIGGER).execute(peerId,channel,packet);
                        return;
                    }
                    if (packet.has(PnRTCResala.JSON_HANGUP)){
                        actionMap.get(PnUserHangupAction.TRIGGER).execute(peerId,channel,packet);
                        return;
                    }
                    if (packet.has(PnRTCResala.JSON_THUMBNAIL)) {
                        return;   // No handler for thumbnail or hangup yet, will be separate controller callback
                    }
                    if (packet.has(PnRTCResala.JSON_SDP)) {
                        if(!peer.received) {
                            peer.setReceived(true);
                            mRtcListener.onDebug(new PnRTCResala("SDP - " + peer));
                            // Todo: reveivercb(peer);onMessage
                        }
                        String type = packet.getString(PnRTCResala.JSON_TYPE);
                        actionMap.get(type).execute(peerId,channel, packet);
                        return;
                    }
                    if (packet.has(PnRTCResala.JSON_ICE)){
                        actionMap.get(AddIceCandidateAction.TRIGGER).execute(peerId,channel,packet);
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }

        };
    }





    @Override
    public void unInit()
    {
        killHandler();
        EventBus.getDefault().unregister(this);
    }

    private void killHandler()
    {
        if (mhandler== null) return;

        mhandler.removeCallbacksAndMessages(null);
        mhandler= null;

    }



    public AndruavPeerConnectionClientClient(final PeerConnectionFactory pcFactory , final PnSignalingParams signalingParams, final PnRTCListener rtcListener){
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        this.signalingParams = signalingParams;
        this.mRtcListener = rtcListener;
        this.pcFactory = pcFactory;
        this.peers = new HashMap<String, PnPeer>();
        init();
        initHandler();
    }




    /**
     * Send SDP Offers/Answers and ICE candidates to peers.
     * @param toID The id or "number" that you wish to transmit a message to.
     * @param packet The JSON data to be transmitted
     */
    @Override
    void transmitMessage(final String toID, final JSONObject packet, final String channel, final int delay){
        try {
            final JSONObject message = new JSONObject();
            message.put(PnRTCResala.JSON_PACKET, packet);
            //message.put(PnRTCResala.JSON_PACKET, packet);
            message.put(PnRTCResala.JSON_ID, AndruavSettings.andruavWe7daBase.PartyID); //Todo: session id, unused in js SDK?
            message.put(PnRTCResala.JSON_NUMBER, PeerConnectionManager.CameraID);
            message.put(PnRTCResala.JSON_CHANNEL, channel); // this is a single video track

            AndruavFacade.sendWebRTCSignalingJSONMessage(message,toID,true);
            //Log.e("offer TX",message.toString());
            //   mRtcListener.onDebug(new PnRTCResala(message));


        } catch (JSONException e){
            e.printStackTrace();
        }

    }


    /***
     * Some times a Source has bad or think It connects to Me so I sendMessageToModule him ASK to Disconnect
     * @param userID
     */
    public static void sendHangUpTo (final String userID)
    {
        try {
            if (AndruavSettings.andruavWe7daBase.getIsCGS())
            {
                return ;
            }
            final JSONObject packet =  AndruavPeerConnectionClientClient.generateHangupPacket(PeerConnectionManager.CameraID) ;

            AndruavFacade.sendWebRTCSignalingJSONMessage(packet,userID,true);


        } catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * @param userId Your id. Used to tag the message before publishing it to another user.
     * @return
     */
    public static JSONObject generateHangupPacket(final String userId){
        JSONObject json = new JSONObject();
        try {
            JSONObject packet = new JSONObject();
            packet.put(PnRTCResala.JSON_HANGUP, true);
            json.put(PnRTCResala.JSON_PACKET, packet);
            json.put(PnRTCResala.JSON_ID, AndruavSettings.andruavWe7daBase.PartyID); //Todo: session id, unused in js SDK?
            json.put(PnRTCResala.JSON_NUMBER, userId);
            packet.put(PnRTCResala.JSON_CHANNEL, userId); // this is a single video track

        } catch (JSONException e){
            e.printStackTrace();
        }
        return json;
    }



}
