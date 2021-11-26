package ap.andruav_ap;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.interfaces.INotification;
import com.andruav.sensors.AndruavIMU;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.event.EventBus;
import com.andruav.AndruavFacade;

import ap.andruavmiddlelibrary.eventClasses.fpvEvent.Event_FPV_Image;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import ap.andruavmiddlelibrary.factory.io.FileHelper;
import ap.andruavmiddlelibrary.factory.util.Image_Helper;
import com.andruav.event.droneReport_7adath._7adath_IMU_Ready;
import ap.andruavmiddlelibrary.database.DaoManager;
import ap.andruavmiddlelibrary.database.GenericDataDao;
import ap.andruavmiddlelibrary.database.GenericDataRow;
//import rcmobile.sensors.SensorEvents.Event_IMU;

/**
 * Creates Trip information. A folder for each login. It creates a KML file and a folder for photoe and another one for videos.
 * Created by M.Hefny on 01-Nov-14.
 */
public class KMLFileHandler {

    //////////////////// Attributes

    private HandlerThread mhandlerThread;
    private Handler mhandler;
    private Boolean mkillMe = false;

    private FileOutputStream mfileOutputStream = null;
    private File mfile;
    private Boolean mclearToWrite;

    // KML Specific Data
    private String Name;
    private File Root;
    private File KMZ;
    private File IMG;
    private String VID;

    private StringBuilder  Images;
    private StringBuilder  Path;
    private String mfolderTitle;
    private Location mlastLocation;

    private boolean stopKML = false; // used in case of Storage Error
    // EOF KML Specific Data

    // EOF Attributes

    //////////BUS EVENT


    public void onEvent (final Event_ShutDown_Signalling event)
    {
        if (event.CloseOrder != 2) return ;


        this.shutDown();
        App.KMLFile = null;
    }

    /***
     * Local IMU events
     * @param event_IMU
     */
    public void onEvent (final _7adath_IMU_Ready event_IMU)
    {

        if (stopKML || (!mclearToWrite)) return ;

        //if (event_IMU.getCurrentLocation()==null) return ;
        AndruavIMU andruavIMU= AndruavSettings.andruavWe7daBase.LastEvent_IMU;
        if (andruavIMU.getCurrentLocation() == null ) return ;

        Message msg = new Message();
        if (mlastLocation != null)
        {
            if ((mlastLocation.getLatitude()  == andruavIMU.getCurrentLocation().getLatitude()) &&
                (mlastLocation.getLongitude() == andruavIMU.getCurrentLocation().getLongitude()) &&
                (mlastLocation.getAltitude()  == andruavIMU.getCurrentLocation().getAltitude())
                )
                return ;
        }
        mlastLocation = andruavIMU.getCurrentLocation();
        msg.obj = event_IMU;
        if (mhandler != null)  mhandler.sendMessageDelayed(msg,0);
    }

    public void onEvent (final Event_FPV_Image event_fpv_image)
    {
        if (stopKML || (!mclearToWrite)) return ;

        if (event_fpv_image.ImageFile==null) return ;

        //TODO: HERE WE Ignore saving remote images
        // dont save remote images in ur KML file .. you might need that in future
       // if (event_fpv_image.isLocalImage==false) return ;
        if (!event_fpv_image.isLocalImage) return ;

        Message msg = new Message();

        msg.obj = event_fpv_image;
        if (mhandler != null)  mhandler.sendMessageDelayed(msg,0);
    }
    ///////////////////


    private void initHandler ()
    {
        mhandlerThread = new HandlerThread("KML Filer");
        mhandlerThread.start(); //NOTE: mhandlerThread.getLooper() will return null if not started.

        mhandler = new Handler(mhandlerThread.getLooper())
        {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (mkillMe) return;

                if (msg.obj instanceof _7adath_IMU_Ready)
                {
                    final AndruavIMU event_imu = AndruavSettings.andruavWe7daBase.getActiveIMU();

                    final Location loc = event_imu.getCurrentLocation();
                    if (loc==null) return;

                    addWayPoint(loc.getLongitude(),loc.getLatitude(),loc.getAltitude());


                }else
                if (msg.obj instanceof Event_FPV_Image)
                {
                    Event_FPV_Image event_fpv_image = (Event_FPV_Image) msg.obj;
                    addImages (event_fpv_image);
                }


            }
        };
    }


    public String getVideoPath()
    {
        if (VID == null) return null;
        return VID;
    }

    public KMLFileHandler()
    {
        mclearToWrite = false;
        final QueryBuilder qb = DaoManager.getGenericDataDao().queryBuilder();
        qb.where(qb.or(GenericDataDao.Properties.Type.eq(GenericDataRow.DB_TYPE_KML_POINT),GenericDataDao.Properties.Type.eq(GenericDataRow.DB_TYPE_KML_IMAGE))).buildDelete().executeDeleteWithoutDetachingEntities();
        initHandler();
    }

    private void writeKMLHeader ()
    {
        writeDirect("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\" xmlns:kml=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" + "<Folder>\n" + "<name>" + mfolderTitle + "</name>");
    }

    private void writeKMLFooter ()
    {
        writeDirect("</Folder>\n");
        writeDirect("</kml>");
    }



    public void addImages (Event_FPV_Image event_fpv_image)
    {
        try {
            // {@see <a href=http://localhost:8080/mantis/view.php?id=26>Avoid attaching null mLocation </a>}
            if (event_fpv_image.ImageLocation == null) return;

            StringBuilder img = new StringBuilder();

            String popDescritpion;
            popDescritpion = "<br><font color=#36AB36><b>lng:&nbsp;</font><font color=#75A4D3>" + event_fpv_image.ImageLocation.getLongitude()
                    + "</font><br><font color=#36AB36><b>lat:&nbsp;</font><font color=#75A4D3>" + event_fpv_image.ImageLocation.getLatitude()
                    + "</font><br><font color=#36AB36><b>alt:&nbsp;</font><font color=#75A4D3>" + event_fpv_image.ImageLocation.getAltitude()
                    + "</font><br>filename:&nbsp;" + event_fpv_image.ImageFile.getName()
                    + "</font><br>sender:&nbsp;" + event_fpv_image.Sender;
            img.append("\r\n<Placemark>\r\n<name>FPV_CAM</name>\r\n<Snippet>").append(event_fpv_image.Description).append("</Snippet>\r\n<Style>\r\n<IconStyle>\r\n<Icon>\r\n");
            img.append("<href>files/").append(event_fpv_image.ImageFile.getName()).append("</href>\r\n</Icon>\r\n</IconStyle>\r\n</Style>\r\n");
            img.append("<description><![CDATA[\r\n<img src='files/").append(event_fpv_image.ImageFile.getName()).append("' width='400' />").append(popDescritpion).append("<br/>]]>\n</description>\n<Point>\n<coordinates>");
            img.append(event_fpv_image.ImageLocation.getLongitude()).append(",").append(event_fpv_image.ImageLocation.getLatitude());
            img.append("</coordinates>\r\n</Point>\r\n</Placemark>\r\n");

            DaoManager.getGenericDataDao().insert(
                    new GenericDataRow(null, (long) GenericDataRow.DB_TYPE_KML_IMAGE, img.toString())
            );

            Images.append(img.toString());
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("exception_kml", ex);
        }
    }

    public void initImages()
    {
        Images = new StringBuilder();
    }

    public void finalizeImages ( )
    {
        writeDirect(Images.toString());

    }


    public void addExtendedData (String displayName, String value)
    {
        Path.append("<ExtendedData>\r\n<Data name=\"string\">\r\n<displayName>");
        Path.append(displayName);
        Path.append("\r\n</displayName>\r\n<value>");
        Path.append(value);
        Path.append("</value>\r\n</Data>\r\n</ExtendedData>\r\n");
    }

    private void addWayPoint (double longitude, double latitude, double altitude)
    {

        try {
            StringBuilder point = new StringBuilder();
            point.append(longitude);
            point.append(",");
            point.append(latitude);
            point.append(",");
            point.append(altitude);
            point.append("\r\n");
            DaoManager.getGenericDataDao().insert(
                    new GenericDataRow(null, (long) GenericDataRow.DB_TYPE_KML_POINT, point.toString())
            );
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("exception_kml", ex);

        }
        //Path.append(point.toString());

    }

    private void initWayPoint ()
    {
        Path = new StringBuilder();

        Path.append("<Placemark>\r\n" +
                "        <name>Flight Path</name>\r\n" +
                "          <LineString>\n" +
                "           <extrude>1</extrude>\r\n" +
                "           <altitudeMode>relativeToGround</altitudeMode>\r\n" +
                "           <coordinates>\r\n");

    }

    private void finalizeWayPoint()
    {
        final List waypoints = DaoManager.getGenericDataDao().queryBuilder().where(GenericDataDao.Properties.Type.eq(GenericDataRow.DB_TYPE_KML_POINT)).list();
        int size = waypoints.size();
        for (int i=0;i<size;++i)
        {
            Path.append(((GenericDataRow)waypoints.get(i)).getData());
        }
        Path.append("</coordinates>\r\n");
        Path.append("</LineString>\r\n");
        Path.append("</Placemark>\r\n");
        writeDirect(Path.toString());

    }


    private void writeDirect (String text)
    {
        if (mfileOutputStream != null) {
            try {
                mfileOutputStream.write(text.getBytes());
            } catch (IOException ex) {
                AndruavEngine.log().logException("exception_kml", ex);
            }
        }
    }

    public void openKMZ(String kmlFileName)
    {
        try {
            mfolderTitle = kmlFileName;
            if ((kmlFileName == null) || (kmlFileName.length()==0))
            {
                kmlFileName = "FPV_KML";
            }

            String timeStamp =
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(new Date());

            kmlFileName +="_" + timeStamp;

            File root = FileHelper.GetFolder("AndruavKML",null);
            if (root == null)
            {
                stopKML = true;
                EventBus.getDefault().register(this);
                return ;
            }
            KMZ = FileHelper.GetFolder(kmlFileName.replace(' ','_'),root.getAbsolutePath());
            IMG = FileHelper.GetFolder("files",KMZ.getAbsolutePath());
            final File videoFile = FileHelper.GetFolder("video",KMZ.getAbsolutePath());
            if (videoFile!= null) VID = videoFile.getAbsolutePath();

            mfile = new File(KMZ, "path.kml");
            if (!mfile.exists())
            {
                if (!mfile.createNewFile())
                {
                    return ;
                }
            }

            mfileOutputStream = new FileOutputStream(mfile.getAbsolutePath());

            initWayPoint();
            initImages();
            writeKMLHeader();
            mclearToWrite = true;

            EventBus.getDefault().register(this);


        } catch (Exception ex) {
            AndruavEngine.log().logException("exception_kml", ex);
            AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_KMLFILE, INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_KMLERROR, "Cannot create KML File", null);

        }

    }


    public void Write (String text)
    {
        Message msg = new Message();
        msg.obj = text;
        mhandler.sendMessageDelayed(msg,0);
    }

    public void shutDown()
    {
        mclearToWrite = false;
        mkillMe = true;
        EventBus.getDefault().unregister(this);
        File imageFolder = getImageFolder();
        if (imageFolder != null)
        Image_Helper.galleryAddPic(App.context,imageFolder.getAbsolutePath() );

        if (mhandler != null) {
            mhandler.removeCallbacksAndMessages(null);
            mhandler = null;
        }

        if (mhandlerThread != null)
        {
            mhandlerThread.quit();
        }
        try {
            if (mfileOutputStream != null) {
                finalizeWayPoint();
                finalizeImages();
                writeKMLFooter();
                mfileOutputStream.flush();
                mfileOutputStream.close();
            }
        } catch (IOException ex) {
            AndruavEngine.log().logException("exception_kml", ex);
        }
    }

    public File getImageFolder()
    {
        if (IMG == null) return null;
        return IMG;
    }

    public File getKMZFolder()
    {
        if (KMZ == null) return null;
        return KMZ;
    }

    public File getRootFolder()
    {
        if (Root == null) return null;
        return Root;
    }
}
