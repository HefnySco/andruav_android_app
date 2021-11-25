package ap.andruavmiddlelibrary.eventClasses.fpvEvent;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.media.Event_VideoTrack;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraList;

import java.util.ArrayList;


/**
 * Carries data of {@link AndruavMessage_CameraList}
 * <br><br>Created by mhefny on 2/5/16.
 */
public class Event_FPV_VideoURL {


    public AndruavUnitBase andruavUnit;

    /***
     * {@link AndruavMessage_CameraList#EXTERNAL_CAMERA_TYPE_IPWEBCAM}, {@link AndruavMessage_CameraList#EXTERNAL_CAMERA_TYPE_UNKNOWN}
     */
    public int ExternalType;

    public ArrayList<Event_VideoTrack> VideoTracks;

    public boolean isReply;
}
