package ap.andruavmiddlelibrary.webrtc.classes;

public interface VSink {

    void onFrame(byte[] frame, int offset, int size);

}
