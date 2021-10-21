package com.andruav.media;

/**
 * Created by mhefny on 20/10/17.
 */

public class Event_VideoTrack {

    public String mVideoID;
    public boolean mIsVideo;
    public String mCameraLocalName;

    public Event_VideoTrack (final String videoID, final boolean isVideo, final String cameraLocalName)
    {
        mVideoID = videoID;
        mIsVideo = isVideo;
        mCameraLocalName = cameraLocalName;
    }
}
