package rcmobile.andruavmiddlelibrary.eventClasses.fpvEvent;

import android.graphics.YuvImage;
import android.location.Location;

import com.andruav.andruavUnit.AndruavUnitBase;

import java.io.File;


/**
 * Created by M.Hefny on 06-Nov-14.
 * Image taken by FPV mainly Filehandler catches it to save it with KML file.
 */
public class Event_FPV_Image {



    /***
     * Localimages means that it is taken by the mobile.
     * It is used by Andruav to sendMessageToModule it to other Andruav or save in with KML file.
     */
    public String Sender;
    public Boolean isLocalImage = false;
    public Boolean saveInKML    = true;
    public Boolean isVideo      = false;
    public int videoType        ;
    public byte[] ImageBytes;
    public YuvImage ImageData;
    public long time;

    public AndruavUnitBase andruavUnit;
    /***
     * Path of the saved omage instead of sending it in ImageBytes
     */
    public File ImageFile;
    public Location ImageLocation;
    public String Description;
    public Event_FPV_Image()
    {

    }
}
