package ap.andruavmiddlelibrary.webrtc.classes;

import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ap.andruavmiddlelibrary.preference.Preference;

/**
 * <h1>Define {@link MediaConstraints} and {@link PeerConnection.IceServer} for WebRTC PeerConnections</h1>
 * <pre>
 * Author:  Kevin Gleason - Boston College '16
 * File:    PnSignalingParams.java
 * Date:    7/20/15
 * Use:     Hold the signaling parameters of a WebRTC PeerConnection
 * &copy; 2009 - 2015 PubNub, Inc.
 * </pre>
 * <p>IceServers allow Trickling, so they are not final.</p>
 *
 */
    class PnSignalingParams {


    /**
     * Renderer type
     * added from  https://github.com/nubomedia-vtt/webrtcpeer-android/blob/master/webrtcpeer-android/src/main/java/fi/vtt/nubomedia/webrtcpeerandroid/NBMMediaConfiguration.java
     */
    public enum RendererType {
        NATIVE, OPENGLES
    }

    /**
     * Audio codec
     * added from  https://github.com/nubomedia-vtt/webrtcpeer-android/blob/master/webrtcpeer-android/src/main/java/fi/vtt/nubomedia/webrtcpeerandroid/NBMMediaConfiguration.java
     */
    public enum AudioCodec {
        OPUS, ISAC
    }

    /**
     * Video codec
     * added from  https://github.com/nubomedia-vtt/webrtcpeer-android/blob/master/webrtcpeer-android/src/main/java/fi/vtt/nubomedia/webrtcpeerandroid/NBMMediaConfiguration.java
     */
    public enum VideoCodec {
        VP8, VP9, H264
    }



    public List<PeerConnection.IceServer> iceServers;
    public final MediaConstraints pcConstraints;
    public final MediaConstraints videoConstraints;
    public final MediaConstraints audioConstraints;

    public PnSignalingParams(
            final List<PeerConnection.IceServer> iceServers,
            final MediaConstraints pcConstraints,
            final MediaConstraints videoConstraints,
            final MediaConstraints audioConstraints) {
        this.iceServers       = (iceServers==null)       ? defaultIceServers()       : iceServers;
        this.pcConstraints    = (pcConstraints==null)    ? defaultPcConstraints()    : pcConstraints;
        this.videoConstraints = (videoConstraints==null) ? defaultVideoConstraints() : videoConstraints;
        this.audioConstraints = (audioConstraints==null) ? defaultAudioConstraints() : audioConstraints;
    }

    /**
     * Default Ice Servers, but specified parameters.
     * @param pcConstraints
     * @param videoConstraints
     * @param audioConstraints
     */
    public PnSignalingParams(
            final MediaConstraints pcConstraints,
            final MediaConstraints videoConstraints,
            final MediaConstraints audioConstraints) {
        this.iceServers       = PnSignalingParams.defaultIceServers();
        this.pcConstraints    = (pcConstraints==null)    ? defaultPcConstraints()    : pcConstraints;
        this.videoConstraints = (videoConstraints==null) ? defaultVideoConstraints() : videoConstraints;
        this.audioConstraints = (audioConstraints==null) ? defaultAudioConstraints() : audioConstraints;
    }

    /**
     * Default media params, but specified Ice Servers
     * @param iceServers
     */
    public PnSignalingParams(final List<PeerConnection.IceServer> iceServers) {
        this.iceServers       = defaultIceServers();
        this.pcConstraints    = defaultPcConstraints();
        this.videoConstraints = defaultVideoConstraints();
        this.audioConstraints = defaultAudioConstraints();
        addIceServers(iceServers);
    }

    /**
     * Default media params and ICE servers.
     */
    public PnSignalingParams() {
        this.iceServers       = defaultIceServers();
        this.pcConstraints    = defaultPcConstraints();
        this.videoConstraints = defaultVideoConstraints();
        this.audioConstraints = defaultAudioConstraints();
    }

    /**
     * The default parameters for media constraints. Might have to tweak in future.
     * @return default parameters
     */
    public static PnSignalingParams defaultInstance() {
        MediaConstraints pcConstraints    = PnSignalingParams.defaultPcConstraints();
        MediaConstraints videoConstraints = PnSignalingParams.defaultVideoConstraints();
        MediaConstraints audioConstraints = PnSignalingParams.defaultAudioConstraints();
        List<PeerConnection.IceServer> iceServers = PnSignalingParams.defaultIceServers();
        return new PnSignalingParams(iceServers, pcConstraints, videoConstraints, audioConstraints);
    }

    private static MediaConstraints defaultPcConstraints(){
        MediaConstraints pcConstraints = new MediaConstraints();
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        return pcConstraints;
    }

    private static MediaConstraints defaultVideoConstraints(){
        MediaConstraints videoConstraints = new MediaConstraints();
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth","1280"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight","720"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", "640"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight","480"));
        return videoConstraints;
    }

    private static MediaConstraints defaultAudioConstraints(){
        MediaConstraints audioConstraints = new MediaConstraints();
        return audioConstraints;
    }

    public static List<PeerConnection.IceServer> defaultIceServers(){
       final  List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>(25);
        if (!Preference.useLocalStunServerOnly(null)) {

            // Extra Defaults - 19 STUN servers + 4 initial = 23 severs (+2 padding) = Array cap 25
            iceServers.add(new PeerConnection.IceServer("turn:airgap.droneengage.com:3478","airgap","1234"));
            iceServers.add(new PeerConnection.IceServer("turn:104.131.188.164:3478","andruav_ap","1234"));
            iceServers.add(new PeerConnection.IceServer("stun:stun3.l.google.com:19302"));
        }
        return iceServers;
    }

    /**
     * Append default servers to the end of given list and set as iceServers instance variable
     * @param iceServers List of iceServers
     */
    public void addIceServers(List<PeerConnection.IceServer> iceServers){
        if(this.iceServers!=null) {
            iceServers.addAll(this.iceServers);
        }
        this.iceServers = iceServers;
    }

    /**
     * Instantiate iceServers if they are not already, and add Ice Server to beginning of list.
     * @param iceServers Ice Server to add
     */
    public void addIceServers(PeerConnection.IceServer iceServers){
        if (this.iceServers == null){
            this.iceServers = new ArrayList<PeerConnection.IceServer>();
        }
        this.iceServers.add(0, iceServers);
    }


    /**
     *
     * @param sdpDescription
     * @param codec
     * @param isAudio
     * @return
     */
    public static String preferCodec(String sdpDescription, String codec, boolean isAudio) {
        String[] lines = sdpDescription.split("\r\n");
        int mLineIndex = -1;
        String codecRtpMap = null;
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);
        String mediaDescription = "m=video ";
        if (isAudio) {
            mediaDescription = "m=audio ";
        }
        for (int i = 0; (i < lines.length) && (mLineIndex == -1 || codecRtpMap == null); i++) {
            if (lines[i].startsWith(mediaDescription)) {
                mLineIndex = i;
                continue;
            }
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1);
                continue;
            }
        }
        if (mLineIndex == -1) {
           // Log.w(TAG, "No " + mediaDescription + " line, so can't prefer " + codec);
            return sdpDescription;
        }
        if (codecRtpMap == null) {
          //  Log.w(TAG, "No rtpmap for " + codec);
            return sdpDescription;
        }
       // Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + ", prefer at " + lines[mLineIndex]);
        String[] origMLineParts = lines[mLineIndex].split(" ");
        if (origMLineParts.length > 3) {
            StringBuilder newMLine = new StringBuilder();
            int origPartIndex = 0;
            // Format is: m=<media> <port> <proto> <fmt> ...
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(origMLineParts[origPartIndex++]).append(" ");
            newMLine.append(codecRtpMap);
            for (; origPartIndex < origMLineParts.length; origPartIndex++) {
                if (!origMLineParts[origPartIndex].equals(codecRtpMap)) {
                    newMLine.append(" ").append(origMLineParts[origPartIndex]);
                }
            }
            lines[mLineIndex] = newMLine.toString();
            //Log.d(TAG, "Change media description: " + lines[mLineIndex]);
        } else {
            //Log.e(TAG, "Wrong SDP media description format: " + lines[mLineIndex]);
        }
        StringBuilder newSdpDescription = new StringBuilder();
        for (String line : lines) {
            newSdpDescription.append(line).append("\r\n");
        }
        return newSdpDescription.toString();
    }

}

