package rcmobile.andruavmiddlelibrary.webrtc;

import org.webrtc.MediaStream;

import rcmobile.andruavmiddlelibrary.webrtc.classes.PnPeer;


/**
 * Created by mhefny on 2/27/16.
 */
public interface IRTCListener {

    void onLocalStream(final MediaStream localStream) ;

    void onAddRemoteStream(final MediaStream remoteStream, final PnPeer peer);

    void onRemoveRemoteStream(final MediaStream remoteStream, final PnPeer peer);

    void onPeerConnectionClosed(final PnPeer peer);


    void onPeerConnected(final String  userId);
}
