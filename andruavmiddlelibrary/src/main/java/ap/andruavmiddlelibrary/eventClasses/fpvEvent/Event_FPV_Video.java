package ap.andruavmiddlelibrary.eventClasses.fpvEvent;

/**
 * Created by M.Hefny on 06-Jan-15.
 */
public class Event_FPV_Video {


    /***
     * Localimages means that it is taken by the mobile.
     * It is used by Andruav to sendMessageToModule it to other Andruav or save in with KML file.
     */
    public Boolean isLocalImage;
    public byte[] ImageBytes;

    /***
     * Path of the saved omage instead of sending it in ImageBytes
     */
    public Event_FPV_Video()
    {

    }
}
