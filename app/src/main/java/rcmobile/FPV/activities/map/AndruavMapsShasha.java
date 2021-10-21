package rcmobile.FPV.activities.map;


import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.TextView;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.event.droneReport_7adath._7adath_FCB_Changed;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.Constants;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraList;
import com.andruav.util.AndruavLatLngAlt;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import de.greenrobot.event.EventBus;

import rcmobile.FPV.helpers.TouchListener;
import rcmobile.FPV.widgets.camera.AndruavImageView;
import rcmobile.FPV.widgets.camera.AndruavRTCVideoDecorderWidget;
import rcmobile.FPV.widgets.camera.CameraControl_Dlg;
import rcmobile.FPV.activities.remote.RemoteControlWidget;
import rcmobile.FPV.activities.baseview.BaseAndruavShasha;
import rcmobile.FPV.App;

import com.andruav.AndruavFacade;

import rcmobile.FPV.widgets.flightControlWidgets.AndruavUnitInfoWidget;
import rcmobile.FPV.DeviceManagerFacade;
import rcmobile.FPV.widgets.flightControlWidgets.AttitudeWidget;
import rcmobile.FPV.widgets.flightControlWidgets.FCBControl_Dlg;
import rcmobile.FPV.widgets.flightControlWidgets.FlyToPoint_Dlg;
import rcmobile.FPV.widgets.flightControlWidgets.SOSControl_Dlg;
import rcmobile.andruavmiddlelibrary.eventClasses.fpvEvent.Event_FPV_Image;
import rcmobile.FPV.helpers.GUI;
import rcmobile.andruavmiddlelibrary.eventClasses.fpvEvent.Event_FPV_VideoURL;
import rcmobile.andruavmiddlelibrary.preference.Preference;
import rcmobile.FPV.R;

import java.io.File;
import java.io.FileNotFoundException;

import rcmobile.FPV.widgets.sliding.SlidingAndruavUnitList;
import rcmobile.FPV.widgets.sliding.SlidingAndruavUnitItem;
import com.andruav.FeatureSwitch;
import rcmobile.andruavmiddlelibrary.factory.io.FileHelper;
import rcmobile.andruavmiddlelibrary.factory.util.DialogHelper;
import rcmobile.andruavmiddlelibrary.Voting;

import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;


/***
 * Map Activity for tracking Drones.
 */
public class AndruavMapsShasha extends BaseAndruavShasha implements AndruavRTCVideoDecorderWidget.IRTCVideoDecoder , GoogleMap.SnapshotReadyCallback, AndruavMapBaseWidget.IGoogleMapFeedback {


    //////// Attributes

    private AndruavMapsShasha Me;
    private boolean killme = false;
    private Handler mhandle;
    private Button btnTakePhoto;
    private Button btnVideoStream;
    private Button btnRotateImage;
    private Button btnSwitchCamera;
    private Button btnFlightMode;
    private Button btnSOS;
    private Button btnMapType;
    private Button btnSliding;
    private AttitudeWidget attitudeWidget;

    private SlidingAndruavUnitList llSliding;
    private SlidingDrawer slidingDrawer;

    private GoogleMap mMap;
    private AndruavImageView imgSnapShot;
    //private ImageView imgVideoStreaming;
    private AndruavMapWidget msupportmapfragment;
    private AndruavUnitInfoWidget andruavUnitInfoWidget;

    private AndruavRTCVideoDecorderWidget andruavRTCVideoDecorderWidget;


    //private SimpleArrayMap<String,Button> btnContext= new SimpleArrayMap<String,Button>();




    /////////// Remote Control
    protected RemoteControlWidget mRemoteControlWidget;




    private int  m_mapsexception = 5;

    private int measureUnit = Constants.Preferred_UNIT_METRIC_SYSTEM;


    /////////// EOF Attributes


    //////////BUS EVENT



    public void onEvent (final _7adath_FCB_Changed a7adath_fcb_changed)
    {
        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_fcb_changed;
        mhandle.sendMessageDelayed(msg, 0);
    }

    /**
     * Remote IMU events
     *
     * @param event_fpv_image
     */
    public void onEvent(final Event_FPV_Image event_fpv_image) {

        AndruavUnitBase andruavUnit;


        if (event_fpv_image.andruavUnit == null) { //for debug sendMessageToModule to myself
            //no need tp make anything here, as logic of adding new units
            // should be in App.andruavWSClient.andruavUnitMap.get function.
            return;
        }

        if (event_fpv_image.ImageFile!=null) return ;  // this event is sent to save the image not to display it.

        final Message msg = mhandle.obtainMessage();


        if (event_fpv_image.isVideo==false)
        {
            msg.obj = event_fpv_image;
            if (mhandle != null)  mhandle.sendMessageAtFrontOfQueue(msg); // sendMessageToModule Fast Message

        }
        else
        {
            msg.obj = event_fpv_image;
            if (mhandle != null)  mhandle.sendMessageDelayed(msg, 0);

            return;
        }

    }


    public void onEvent (final Event_FPV_VideoURL event_fpv_videoURL)
    {
        if (event_fpv_videoURL.andruavUnit == null) return ;

        final Message msg = mhandle.obtainMessage();
        msg.obj =event_fpv_videoURL;

        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);
    }

long lastframecount =0;

    /***
     * Event to UI gate to enable access UI safely.
     */
    private void UIHandler () {

        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.obj instanceof Event_FPV_Image) {
                    Event_FPV_Image event_fpv_image = (Event_FPV_Image) msg.obj;
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(event_fpv_image.ImageBytes, 0, event_fpv_image.ImageBytes.length);
                    if (bitmap == null) return; //some times in UDP packet
                    int imageRotatedAngle = 0;


                    final AndruavUnitShadow andruavUnit = (AndruavUnitShadow) (AndruavEngine.getAndruavWe7daMapBase().get(event_fpv_image.Sender));

                    if (andruavUnit != null) {
                            imageRotatedAngle = andruavUnit.PreferredRotationAngle;
                    }


                    if (event_fpv_image.isVideo) {
                        if (lastframecount >= FeatureSwitch.Default_Video_FrameResumeSize) {
                            // skip old image UDP --- almost impossible case
                            lastframecount = 0;
                            if (andruavUnit != null) {
                                AndruavFacade.streamVideoResume(andruavUnit);
                            }
                            return;

                        }
                        lastframecount = lastframecount + 1;


                        //Bitmap rotatedBmp = Image_Helper.rotateImage(bitmap, bitmap.getWidth(), bitmap.getHeight(), imageRotatedAngle); // Preference.getFPVActivityRotation(null));

                        //imgVideoStreaming.setVisibility(View.VISIBLE);  ENABLED BY BUTTON ONLY
                       // imgVideoStreaming.setImageBitmap(bitmap);

                      //  imgVideoStreaming.setScaleType(ImageView.ScaleType.FIT_XY);
                    } else {
                        // Image

                        imgSnapShot.setVisibility(View.VISIBLE);
                        imgSnapShot.setImageBitmap(bitmap);
                        imgSnapShot.setRotation(imageRotatedAngle);
                        imgSnapShot.setScaleType(ImageView.ScaleType.CENTER);
                        if (event_fpv_image.ImageLocation != null) {
                            Marker cmrkCamera = mMap.addMarker(new MarkerOptions()
                                    .draggable(false)
                                    .position(new LatLng(event_fpv_image.ImageLocation.getLatitude(), event_fpv_image.ImageLocation.getLongitude()))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.camera_24x24))
                                    .title(event_fpv_image.Description));
                            cmrkCamera.setAnchor(0.5f, 0.5f);
                        }



                        // Save the remote image Locally in the KML File
                        final File savedImageFile = FileHelper.savePic(event_fpv_image.ImageBytes, null, App.KMLFile.getImageFolder());
                        if (savedImageFile != null) {

                            event_fpv_image.saveInKML = FeatureSwitch.Save_ImageCapturedFromDroneinCGS;
                            event_fpv_image.ImageFile = savedImageFile;
                            EventBus.getDefault().post(event_fpv_image);
                        }

                        Voting.onImageRecieved();
                    }
                } else  if (msg.obj instanceof Event_FPV_VideoURL) {
                    final Event_FPV_VideoURL event_fpv_videoURL = (Event_FPV_VideoURL) msg.obj;
                    switch (event_fpv_videoURL.ExternalType) {
                        case AndruavMessage_CameraList.EXTERNAL_CAMERA_TYPE_RTCWEBCAM:
                            // DroneIP either already exist or sent from Drone as a double check

                            //djiVideoDecoderWidget.setVisibility(View.INVISIBLE);
                            //imgVideoStreaming.setVisibility(View.INVISIBLE);
                            andruavRTCVideoDecorderWidget.setVisibility(View.VISIBLE);


                            break;
                    }
                }
                else if (msg.obj instanceof _7adath_FCB_Changed)
                {
                    final _7adath_FCB_Changed adath_fcb_changed = (_7adath_FCB_Changed) msg.obj;
                    if
                    ( (msupportmapfragment.andruavUnit_selected != null)
                    && (adath_fcb_changed.andruavUnitBase.PartyID.equals(msupportmapfragment.andruavUnit_selected.PartyID))
                    )
                    {
                        guiAdjustSideButtons (adath_fcb_changed.andruavUnitBase);
                    }
                }
            }
        };
    }



    int slidingDelay = 0;
    private final Runnable ScheduledTasks = new Runnable() {
        @Override
        public void run() {
            try {
                /* do what you need to do */
                Marker mrkUser;
                if (AndruavEngine.getAndruavWS() == null) return;
                int s = AndruavEngine.getAndruavWe7daMapBase().size();
                for (int j=0, i = 0; j < s; ++i, ++j) {
                    Location loc;
                    final AndruavUnitShadow andruavWe7da ;

                    andruavWe7da = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().valueAt(j);

                    loc = andruavWe7da.getAvailableLocation();
                    if (loc == null) {
                        loc = new Location("LOCAL");
                        loc.setLatitude(0);
                        loc.setLongitude(0);
                    }


                    MarkerAndruav markerAndruav = msupportmapfragment.markerPlans.get(andruavWe7da.PartyID);
                    if ((markerAndruav == null) || (markerAndruav.marker== null))
                    {
                        // Uint not found --- add it.

                        msupportmapfragment.addNewUnitMarker(loc, andruavWe7da);
                        // sometimes map is not initialized so add it, but it will be added again as the if condition here is by marker.
                        // check to avoid duplication.... internal check is in the addUnitSlider function
                        addUnitinSlider(andruavWe7da);

                    } else {
                        // 5 times delay
                        slidingDelay =slidingDelay +  1;
                        if ((slidingDelay % 5) == 0) {
                            if (!andruavWe7da.getIsCGS()) {
                                // add only Drones not GCS
                                SlidingAndruavUnitItem slidingItemAndruavUnitWidget = (SlidingAndruavUnitItem) llSliding.getByKey(andruavWe7da.PartyID);
                                slidingItemAndruavUnitWidget.setUnit(andruavWe7da); // update text inside.
                                slidingItemAndruavUnitWidget.enableVideoStreaming();
                                if (andruavWe7da.getIsShutdown()) {
                                    markerAndruav.marker.setAlpha(0.5f);
                                } else {
                                    markerAndruav.marker.setAlpha(1.0f);

                                    LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
                                    markerAndruav.marker.setPosition(latLng);
                                    if (markerAndruav.circle != null) {
                                        markerAndruav.circle.setCenter(latLng);
                                        markerAndruav.circle.setRadius(andruavWe7da.getAvailableLocation().getAccuracy());
                                    }
                                }
                            }
                        }
                    }
                }

                andruavUnitInfoWidget.setAndruavUnit(msupportmapfragment.andruavUnit_selected);
                andruavRTCVideoDecorderWidget.setAndruavUnit(msupportmapfragment.andruavUnit_selected);

                if (killme == false) {
                    mhandle.postDelayed(this, 1000);    // update GPS with rate 1Hz
                }
            }
            catch (Exception e)
            {
                // expected for ASYNC Access of arrays
                if (m_mapsexception > 0) {
                    m_mapsexception = m_mapsexception - 1;
                    AndruavEngine.log().logException("maps", e);
                }
            }
        }
    };

    private void addUnitinSlider(final AndruavUnitShadow andruavWe7da) {
        /////////////////////DONT Replicate
        if (andruavWe7da.getIsCGS())
        {
            return;
        }
        final SlidingAndruavUnitItem slidingItemAndruavUnitWidget_old= (SlidingAndruavUnitItem) llSliding.getByKey(andruavWe7da.PartyID);
        if (slidingItemAndruavUnitWidget_old != null) return ;

        ///////////////////// Adding BTN for Sliding
        final SlidingAndruavUnitItem slidingItemAndruavUnitWidget = new SlidingAndruavUnitItem(App.getAppContext(),andruavWe7da);
        slidingItemAndruavUnitWidget.enableVideoStreaming();
        slidingItemAndruavUnitWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                handleSelectedPartyID(msupportmapfragment.markerPlans.getMarker(andruavWe7da.PartyID));
                slidingItemAndruavUnitWidget.enableRemote(false);
            }
        });
        slidingItemAndruavUnitWidget.setOnRemoteClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!GUI.isRemoteEngaged(mRemoteControlWidget)) {
                    DialogHelper.doModalDialog(Me, getString(R.string.actionremote_engage)
                            , getString(R.string.conf_engage_remote), null, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // make it a prime
                                    handleSelectedPartyID(msupportmapfragment.markerPlans.getMarker(andruavWe7da.PartyID));
                                    slidingItemAndruavUnitWidget.enableRemote(GUI.toggleRemote(Me, mRemoteControlWidget, andruavWe7da));

                                }
                            }, null, null);
                }
                else
                {
                    handleSelectedPartyID(msupportmapfragment.markerPlans.getMarker(andruavWe7da.PartyID));
                    slidingItemAndruavUnitWidget.enableRemote(GUI.toggleRemote(Me, mRemoteControlWidget, andruavWe7da));
                }
            }
        });

        slidingItemAndruavUnitWidget.setOnCameraClickListener(view -> {

            if (andruavWe7da == null) return;

            //AndruavFacade.TakeImage(1, 0, (byte) 1, andruavWe7da);
            final FragmentManager fm = getSupportFragmentManager();
            CameraControl_Dlg editNameDialogFragment = CameraControl_Dlg.newInstance(andruavWe7da.UnitID);
            editNameDialogFragment.setAndruavWe7da(andruavWe7da);
            editNameDialogFragment.show(fm, "fragment_edit_name");

        });

        slidingItemAndruavUnitWidget.setOnRecordVideoClickListener(view -> {

            if (andruavWe7da == null) return;
            if (!andruavWe7da.isGUIActivated) {
                // select the drone where you want video from it.
                handleSelectedPartyID(msupportmapfragment.markerPlans.getMarker(andruavWe7da.PartyID));
                slidingItemAndruavUnitWidget.enableRemote(false);
            }
            AndruavFacade.recordVideo((andruavWe7da.VideoRecording == AndruavUnitBase.VIDEORECORDING_OFF), andruavWe7da);
        });

        slidingItemAndruavUnitWidget.setOnVideoStreamingListener(v -> {

            if (andruavWe7da == null) return ;

            toggleVideo(andruavWe7da);

//            llSliding.disableVideos(andruavWe7da.PartyID);
//            //AndruavFacade.streamVideoToggle(andruavWe7da);
//            AndruavFacade.getCameraList(andruavWe7da);
//            slidingItemAndruavUnitWidget.enableVideoStreaming();
//            if (andruavWe7da.VideoStreamingActivated)
//            {
//                handleSelectedPartyID(msupportmapfragment.markerPlans.getMarker(andruavWe7da.PartyID));
//                openVideo(andruavWe7da);
//            }
//            else
//            {
//                closeVideo(andruavWe7da);
//            }
        });

        slidingItemAndruavUnitWidget.setOnModeClickListener(view -> {
             if (andruavWe7da == null) return;
             if (andruavWe7da.isControllable())
             {
                final FragmentManager fm = getSupportFragmentManager();
                FCBControl_Dlg fcbControl_dlg = FCBControl_Dlg.newInstance(andruavWe7da);
                fcbControl_dlg.show(fm, "fragment_edit_name");
             }
             else
             {
                AndruavFacade.connectToFCB (andruavWe7da);
             }
         });
        llSliding.addView(slidingItemAndruavUnitWidget);
    }
    ///////////////////
    private void toggleVideo (AndruavUnitShadow andruavWe7da)
    {
        if (andruavWe7da == null) return ;
        llSliding.disableVideos(andruavWe7da.PartyID);
        //AndruavFacade.streamVideoToggle(andruavWe7da);

        //slidingItemAndruavUnitWidget.enableVideoStreaming();
        if (!andruavWe7da.VideoStreamingActivated)
        {
            AndruavFacade.getCameraList(andruavWe7da);
            handleSelectedPartyID(msupportmapfragment.markerPlans.getMarker(andruavWe7da.PartyID));
            openVideo(andruavWe7da);
        }
        else
        {
            closeVideo(andruavWe7da);
        }
    }

    private void init ()
    {
        // tell others to sendMessageToModule WayPoints
        AndruavFacade.requestWayPoints(null);


    }

    private void initGUI()
    {

        if (isInEditMode) return ;

        andruavRTCVideoDecorderWidget = findViewById(R.id.fpvactivity_widget_andruav_rtc_video_decoder);
        andruavRTCVideoDecorderWidget.setIRTCVideoDecoder(this);
        andruavRTCVideoDecorderWidget.setVisibility(View.INVISIBLE);
        attitudeWidget = findViewById((R.id.fpvactivity_widget_attitude));


        andruavRTCVideoDecorderWidget.setOnClickListener(view -> {
            andruavRTCVideoDecorderWidget.disconnectVideo(null,null);
            AndruavUnitShadow andruavWe7da = andruavRTCVideoDecorderWidget.getAndruavUnit();
            if (andruavWe7da  != null)
            {
                SlidingAndruavUnitItem slidingItemAndruavUnitWidget = (SlidingAndruavUnitItem) llSliding.getByKey(andruavWe7da.PartyID);
                if (slidingItemAndruavUnitWidget != null)
                {
                    slidingItemAndruavUnitWidget.updateVideoStreamingButton();
                }
            }

            andruavRTCVideoDecorderWidget.disconnectVideo(null,null);
            andruavRTCVideoDecorderWidget.setAndruavUnit(null);
            andruavRTCVideoDecorderWidget.setVisibility(View.INVISIBLE);


        });
        imgSnapShot = findViewById(R.id.fpvactivity_imgSnapShot);
        imgSnapShot.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                imgSnapShot.setOnTouchListener(new TouchListener());
                return true;
            }
        });
        imgSnapShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgSnapShot.setVisibility(View.INVISIBLE);
            }
        });
        btnTakePhoto = findViewById(R.id.fpvactivity_btnTakePhoto);
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((msupportmapfragment.andruavUnit_selected != null) && (msupportmapfragment.andruavUnit_selected.Equals(AndruavSettings.andruavWe7daBase)==false)) {
                    // take photos remotely only
                    AndruavFacade.takeImage(msupportmapfragment.andruavUnit_selected.imageTotal, msupportmapfragment.andruavUnit_selected.imageInterval, true, msupportmapfragment.andruavUnit_selected);

                }
            }
        });


        btnVideoStream = findViewById(R.id.fpvactivity_btnVideo);
        btnVideoStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                toggleVideo(msupportmapfragment.andruavUnit_selected);
                if (msupportmapfragment.andruavUnit_selected == null) return ;
            }
        });

        btnRotateImage = findViewById(R.id.fpvactivity_btnRotate);

        btnRotateImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int imageRotatedAngle=0;
                    if (msupportmapfragment.andruavUnit_selected !=null)
                    {
                        imageRotatedAngle =  msupportmapfragment.andruavUnit_selected.PreferredRotationAngle;
                    }
                    imageRotatedAngle = (imageRotatedAngle + 90) % 360;
                    if ((imgSnapShot != null) && (imgSnapShot.getVisibility() == View.VISIBLE)) {

                            if (msupportmapfragment.andruavUnit_selected !=null) {
                                msupportmapfragment.andruavUnit_selected.PreferredRotationAngle = imageRotatedAngle;
                            }
                            imgSnapShot.setRotation(imageRotatedAngle);
                    }
                }
            });

        btnRotateImage.setVisibility(View.GONE);

        btnSwitchCamera = findViewById(R.id.mapactivity_btncameraswitch);
        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((msupportmapfragment.andruavUnit_selected != null) && (msupportmapfragment.andruavUnit_selected.Equals(AndruavSettings.andruavWe7daBase)==false)) {
                    // take photos remotely only
                    AndruavFacade.switchCamera(msupportmapfragment.andruavUnit_selected,msupportmapfragment.andruavUnit_selected.PartyID);
                }
            }
        });

        btnFlightMode = findViewById(R.id.fpvactivity_btnMode);
        btnFlightMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (msupportmapfragment.andruavUnit_selected == null) return;

                if (msupportmapfragment.andruavUnit_selected.isControllable())
                {
                    final FragmentManager fm = getSupportFragmentManager();
                    FCBControl_Dlg fcbControl_dlg = FCBControl_Dlg.newInstance(msupportmapfragment.andruavUnit_selected);
                    fcbControl_dlg.show(fm, "fragment_edit_name");
                }
                else
                {
                    AndruavFacade.connectToFCB (msupportmapfragment.andruavUnit_selected);
                }
            }
        });

        btnSOS = findViewById(R.id.fpvactivity_btnSOS);
        btnSOS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (msupportmapfragment.andruavUnit_selected == null) return;
                final FragmentManager fm2 = getSupportFragmentManager();
                SOSControl_Dlg sosControl_dlg = SOSControl_Dlg.newInstance(msupportmapfragment.andruavUnit_selected);
                sosControl_dlg.show(fm2, "fragment_edit_name");
            }
        });


        btnMapType = findViewById(R.id.mapactivity_btnMapType);
        btnMapType.setOnClickListener(new View.OnClickListener() {
            int ncounter =0;
            @Override
            public void onClick(View view) {
               // public static final int MAP_TYPE_NONE = 0;  << THIS IS SKIPPED
               // public static final int MAP_TYPE_NORMAL = 1;
               // public static final int MAP_TYPE_SATELLITE = 2;
               // public static final int MAP_TYPE_TERRAIN = 3;
               // public static final int MAP_TYPE_HYBRID = 4;
                if (mMap == null) return ; // Bug ID 35
                ncounter+=1;
               mMap.setMapType(ncounter%4 +1);
            }
        });


        llSliding = findViewById(R.id.fpvactivity_Sliding);
        btnSliding = findViewById(R.id.fpvactivity_slidingButton);

        llSliding.allowRemoteCGSSelect(true);
        slidingDrawer = findViewById(R.id.fpvactivity_slidingDrawer);
        if (AndruavSettings.andruavWe7daBase.getIsCGS()==false)
        {
            slidingDrawer.setVisibility(View.INVISIBLE);

        }
        else
        {
            Drawable img;
            int color,colortxt;
            if (AndruavEngine.isAndruavWSStatus(SOCKETSTATE_REGISTERED)) {
                slidingDrawer.unlock();
                color = App.getAppContext().getResources().getColor(R.color.btn_TXT_WHITE);
                colortxt = App.getAppContext().getResources().getColor(R.color.btn_TXT_BLUE);
                img = App.getAppContext().getResources().getDrawable(R.drawable.drone_rad_b_32x32);
            }
            else
            {
                slidingDrawer.lock();
                img = App.getAppContext().getResources().getDrawable(R.drawable.drone_rad_gy_32x32);
                color = getResources().getColor(R.color.btn_TXT_GREY);
                colortxt = getResources().getColor(R.color.btn_TXT_GREY_DARK);
                DialogHelper.doModalDialog(this, "Information", "You need to connect online first.", null);
            }

            btnSliding.setBackgroundColor(color);
            btnSliding.setTextColor(colortxt);
            btnSliding.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);


        }




        guiAdjustSideButtons(null);

        mRemoteControlWidget = findViewById(R.id.remotecontrolactivity_remotecontrolwidget);

        andruavUnitInfoWidget = findViewById(R.id.fpvactivity_widget_andruavinfo);
        andruavUnitInfoWidget.setParentFragmentManager(getSupportFragmentManager());


        if (DeviceManagerFacade.hasMultitouch()) {
            mRemoteControlWidget.updateSettings();
        }
        mRemoteControlWidget.setVisibility(View.INVISIBLE);

    }


    private void guiAdjustSideButtons(final AndruavUnitBase andruavUnit)
    {
        Drawable img,vimg,imgCameraSwitch,imgrotate,imgMode,imgSOS;

        final boolean bEnable = !((andruavUnit==null) || (andruavUnit.getIsCGS()) || (andruavUnit.getIsShutdown()));

        if (bEnable) {
            // Enable Camera
            img = App.getAppContext().getResources().getDrawable(R.drawable.camera_bg_32x32);
            vimg = App.getAppContext().getResources().getDrawable(R.drawable.videocam_gb_32x32);
            imgrotate = App.getAppContext().getResources().getDrawable(R.drawable.rotate_bg2_32x32);
            imgCameraSwitch = App.getAppContext().getResources().getDrawable(R.drawable.camera_switch_gb_32x32);
            if (msupportmapfragment.andruavUnit_selected.useFCBIMU())
            {
                imgMode = App.getAppContext().getResources().getDrawable(R.drawable.flightcontrol_bg_32x32);
             }
            else
            {
                imgMode = App.getAppContext().getResources().getDrawable(R.drawable.flightcontrol_gr_32x32);
            }

            imgSOS = App.getAppContext().getResources().getDrawable(R.drawable.sos_rw2_32x32);

            attitudeWidget.setVisibility(View.VISIBLE);
        }
        else
        {
            img = App.getAppContext().getResources().getDrawable(R.drawable.camera_gy_32x32);
            vimg = App.getAppContext().getResources().getDrawable(R.drawable.videocam_gr_32x32);
            imgrotate = App.getAppContext().getResources().getDrawable(R.drawable.rotate_gy_32x32);
            imgCameraSwitch = App.getAppContext().getResources().getDrawable(R.drawable.camera_switch_gy_32x32);
            imgMode = App.getAppContext().getResources().getDrawable(R.drawable.flightcontrol_gr_32x32);
            imgSOS = App.getAppContext().getResources().getDrawable(R.drawable.sos_gy_32x32);
            attitudeWidget.setVisibility(View.INVISIBLE);
        }


        btnFlightMode.setClickable(bEnable); // && msupportmapfragment.andruavUnit_selected.isControllable());
        btnFlightMode.setCompoundDrawablesWithIntrinsicBounds(null, imgMode, null, null);

        btnSwitchCamera.setClickable(bEnable);
        btnSwitchCamera.setCompoundDrawablesWithIntrinsicBounds(null, imgCameraSwitch, null, null);
        btnTakePhoto.setClickable(bEnable);
        btnTakePhoto.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);
        btnVideoStream.setClickable(bEnable && andruavUnit.isCameraEnabled());
        btnVideoStream.setCompoundDrawablesWithIntrinsicBounds(null, vimg, null, null);
        btnSOS.setCompoundDrawablesWithIntrinsicBounds(null, imgSOS, null, null);
        btnSOS.setClickable(bEnable);
        btnRotateImage.setClickable(bEnable);
        btnRotateImage.setCompoundDrawablesWithIntrinsicBounds(null, imgrotate, null, null);

    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Me = this;

        App.ForceLanguage();

        if (Preference.getFPVActivityRotation(null) == Preference.SCREEN_ORIENTATION_LANDSCAPE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_maps);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FragmentManager fmanager = getSupportFragmentManager();
        Fragment fragment = fmanager.findFragmentById(R.id.map);
        msupportmapfragment = (AndruavMapWidget)fragment;


        init();
        initGUI();
        setupMap1();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mapsactivity, menu);
        return true;
    }


    @Override
    protected void onStart() {
        super.onStart();
        UIHandler();
        setupMap2();


    }

    @Override
    protected void onResume () {
        super.onResume();
        measureUnit = Preference.getPreferredUnits(null);


        AndruavFacade.requestID();

        EventBus.getDefault().register(this);

        if (AndruavSettings.andruavWe7daBase.getIsCGS() == false) {
            App.startSensorService();
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        killme = false;

        if (mRemoteControlWidget !=null)
        {
            mRemoteControlWidget.updateSettings();
        }
        msupportmapfragment.init();

        mhandle.postDelayed(ScheduledTasks, 200);

    }

    @Override
    public void onBackPressed() {
       if (slidingDrawer.isOpened())
        {
            slidingDrawer.close();
            return ;
        }
       else if (imgSnapShot.getVisibility()== View.VISIBLE)
       {
           imgSnapShot.setVisibility(View.GONE);
           return ;
       }
        else if ((msupportmapfragment.andruavUnit_selected != null)
           && (msupportmapfragment.andruavUnit_selected.VideoStreamingActivated))
        {


            andruavRTCVideoDecorderWidget.disconnectVideo(null,null);
            andruavRTCVideoDecorderWidget.setAndruavUnit(null);
            andruavRTCVideoDecorderWidget.setVisibility(View.INVISIBLE);

           closeVideo(msupportmapfragment.andruavUnit_selected);
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public void onPause()
    {
        llSliding.disableVideos(null);
        killme = true;
        EventBus.getDefault().unregister(this);
        if (mhandle != null) {
            mhandle.removeCallbacksAndMessages(null);
        }

        final AndruavUnitShadow andruavUnit = msupportmapfragment.andruavUnit_selected;

        if (andruavUnit != null)
        {
            andruavUnit.isGUIActivated = false;
        }


        // disengaged from Any Remote Drone
        if (mRemoteControlWidget != null) {
            mRemoteControlWidget.stopEngage();
        }
        super.onPause();

        msupportmapfragment.unInit();
        llSliding.clear();


    }

    @Override
    public void onStop() {

        super.onStop();


    }












    ////////////////////// Interface ConnectionCallbacks


    private void setupMap1() {

        //MapsInitializer.initialize(this.getApplicationContext());
        msupportmapfragment.setGoogleMapFeedback(this);
        msupportmapfragment.setUp1();


    }

    private void setupMap2()
    {
        msupportmapfragment.setupMap2();

    }




    /**
     * Selects none or an Andruav Unit as the active one.
     * <br>called by {@link #handleSelectedPartyID(Marker)}
     * @param newContext
     * @return
     */
    private AndruavUnitShadow selectUnit(AndruavUnitShadow newContext)
    {
        // 1- deselect old.
        if (msupportmapfragment.andruavUnit_selected != null) {
            msupportmapfragment.andruavUnit_selected.isGUIActivated = false;
        }

        // 2- unsubscribe old
        if ((msupportmapfragment.andruavUnit_selected != null) && (msupportmapfragment.andruavUnit_selected.Equals(newContext) == false)){
            AndruavFacade.ControlIMU(false, msupportmapfragment.andruavUnit_selected);
            GUI.turnOffRemote(mRemoteControlWidget);
            // BUG:  slidingItemAndruavUnitWidget.enableRemote(false); should be called here to disable the remote icon
            closeVideo(msupportmapfragment.andruavUnit_selected);
        }


        msupportmapfragment.andruavUnit_selected = newContext;

        // 3- check if new selection
        if (newContext == null)
        {
            msupportmapfragment.andruavUnit_selected = null;
            guiAdjustSideButtons(null);
            return null;
        }

        // 4- Do select procedures
        if (newContext.IsMe() )
        {
            // This could be me then Do nothing
            msupportmapfragment.andruavUnit_selected = null;
            return null;
        }


        newContext.isGUIActivated = true;
        guiAdjustSideButtons(newContext); // disable camera button if it is a GCS

        // 5- Start IMU Data
        AndruavFacade.requestPowerInfo(msupportmapfragment.andruavUnit_selected);
        AndruavFacade.ControlIMU(true, msupportmapfragment.andruavUnit_selected);
        attitudeWidget.setAndruavUnit(msupportmapfragment.andruavUnit_selected);

        return newContext;

    }




    private  void handleSelectedWaypoint (Marker marker, AndruavUnitBase andruavWe7da)
    {
        msupportmapfragment.followMarker(marker,true);

       // AndruavWe7da andruavUnit = selectUnit(PartyID);
    }


    /***
     *  1- calls {@link AndruavMapWidget#followMarker(Marker, boolean)}
     * <br> 2- draw circle arround marker to mark mLocation accuracy.
     *
     * @param marker
     */
    private void handleSelectedPartyID(Marker marker) {

        if (marker==null) return;

        msupportmapfragment.followMarker(marker,true);

        final AndruavUnitShadow andruavUnit =(AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(msupportmapfragment.getKeybyMarker(marker));

        AndruavUnitShadow andruavWe7da = selectUnit(andruavUnit);




        if (andruavWe7da != null) {
            msupportmapfragment.showWayPoints(andruavWe7da);
            msupportmapfragment.showHomeLocation(andruavWe7da);
            if (andruavWe7da.getAvailableLocation()!= null) {
                Circle c = mMap.addCircle(new CircleOptions()
                        .center(marker.getPosition())
                        .strokeWidth(1.0f)
                        .strokeColor(R.color.btn_TX_HANDLER));
                c.setRadius(andruavWe7da.getAvailableLocation().getAccuracy());
                //MarkerAndruav markerAndruav = markerPlans.put(andruavUnit.PartyID,c);
            }
        }

    }


    private void openVideo (final AndruavUnitShadow andruavWe7da)
    {
        andruavRTCVideoDecorderWidget.openVideo(andruavWe7da);
    }

    private void closeVideo (final AndruavUnitShadow andruavWe7da)
    {
        andruavRTCVideoDecorderWidget.closeVideo(andruavWe7da);
    }

    private void setupMapListeners ()
    {
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                handleSelectedPartyID(null);
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            /***
             * true if the listener has consumed the event (i.e., the default behavior should not occur), false otherwise (i.e., the default behavior should occur).
             * The default behavior is for the camera to move to the map and an info window to appear.
             * @param marker
             * @return
             */

            Marker mmarker;
            @Override
            public boolean onMarkerClick(final Marker marker) {
                mmarker=marker;
                mhandle.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        // test if it is a waypoint
                        if (mmarker!=null) {
                            MarkerWaypoint markerWaypoint = msupportmapfragment.markerWayPointsHash.get(mmarker);
                            if (markerWaypoint != null) {
                                // This is a waypoint marker.
                                handleSelectedWaypoint(mmarker,markerWaypoint.andruavWe7da);
                                return ;
                            }
                        }

                        // It is a null or vehicle.
                        handleSelectedPartyID(mmarker);

                        return ;
                    }
                },100);


                return true;
            }


        });

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng latLng) {
                //if ((andruavWe7da == null) || (andruavWe7da.IsCGS)) return;

                //if (andruavWe7da.getFlightModeFromBoard() == FlightMode.CONST_FLIGHT_CONTROL_GUIDED)
                //{
                if (msupportmapfragment.targetMarker == null) {
                    Marker targetMarker = mMap.addMarker(new MarkerOptions()
                            .draggable(false)
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_bg_32x32))
                            .title("destination"));
                    targetMarker.setAnchor(0.5f, 1.0f);

                    msupportmapfragment.targetMarker = targetMarker;

                }
                else
                {
                    msupportmapfragment.targetMarker.setPosition(latLng);
                }

                mhandle.post(new Runnable() {
                    @Override
                    public void run() {

                        FlyToPoint_Dlg fcbControl_dlg= FlyToPoint_Dlg.newInstance(new AndruavLatLngAlt(latLng.latitude, latLng.longitude,0));
                        fcbControl_dlg.show(fragmentManager, "fragment_edit_name");

                    }
                });
            }
        });

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker mmarker) {
                return null;
            }

            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker mmarker) {
                try {

                    // Is waypoit Marker
                    if (mmarker!=null) {
                        MarkerWaypoint markerWaypoint = msupportmapfragment.markerWayPointsHash.get(mmarker);
                        if (markerWaypoint != null) {
                            // This is a waypoint marker.

                            final View v = getLayoutInflater().inflate(R.layout.googlemap_infowindow, null);
                            final TextView unitTitle = v.findViewById(R.id.googlemap_infowindow_txtUnitID);
                            final TextView unitDetails = v.findViewById(R.id.googlemap_infowindow_txtDetails);
                            AndruavUnitShadow andruavUnit = selectUnit((AndruavUnitShadow)markerWaypoint.andruavWe7da);
                            unitTitle.setText(GUI.getButtonTextWayPoint(andruavUnit, markerWaypoint.wayPointStep));
                            unitDetails.setText(GUI.getWaypointPopInfo(andruavUnit, markerWaypoint.wayPointStep));
                            return v;
                        }
                    }

                    AndruavUnitShadow andruavUnit =(AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().get(msupportmapfragment.getKeybyMarker(mmarker));

                    andruavUnit = selectUnit(andruavUnit);
                    if (andruavUnit == null) {

                        //return null;
                    }
                    // Getting view from the layout file info_window_layout
                    final View v = getLayoutInflater().inflate(R.layout.googlemap_infowindow, null);

                    // Getting reference to the TextView to set latitude
                    final TextView unitTitle = v.findViewById(R.id.googlemap_infowindow_txtUnitID);
                    final TextView unitDetails = v.findViewById(R.id.googlemap_infowindow_txtDetails);


                    unitTitle.setText(GUI.getButtonText(andruavUnit));
                    unitDetails.setText(GUI.getSpeedPopInfo(andruavUnit, measureUnit));

                    // Returning the view containing InfoWindow contents
                    return v;
                } catch (Exception e) {
                    AndruavEngine.log().logException("maps_popup", e);

                    return null;
                }
            }
        });
    }

    public static Bitmap getScreenShot(View view) {

        View screenView = view.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);

        final File savedImageFile = FileHelper.savePic(bitmap, "screen_shot", App.KMLFile.getImageFolder());

        return bitmap;
    }

    @Override
    public void onSnapshotReady(Bitmap bitmap) {
        if (bitmap == null) return;




        final File savedImageFile = FileHelper.savePic(bitmap, "screen_shot", App.KMLFile.getImageFolder());
        if (savedImageFile != null) {
            final String photoUri;
            try {
                photoUri = MediaStore.Images.Media.insertImage(
                        getContentResolver(), savedImageFile.getAbsolutePath(), null, null);
            } catch (FileNotFoundException e) {
                return;
            }

        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        setupMapListeners();
    }

    @Override
    public void IRTCVideoDecoder_onError() {
        guiAdjustSideButtons(msupportmapfragment.andruavUnit_selected);
        btnVideoStream.setCompoundDrawablesWithIntrinsicBounds(null, App.getAppContext().getResources().getDrawable(R.drawable.videocam_gb_32x32), null, null);

    }

    @Override
    public void IRTCVideoDecoder_onReceivingVideo() {
        guiAdjustSideButtons(msupportmapfragment.andruavUnit_selected);
        btnVideoStream.setClickable(true);
        btnVideoStream.setCompoundDrawablesWithIntrinsicBounds(null, App.getAppContext().getResources().getDrawable(R.drawable.videocam_active_32x32), null, null);

    }

    @Override
    public void IRTCVideoDecoder_onClosingVideo() {
        guiAdjustSideButtons(msupportmapfragment.andruavUnit_selected);
        btnVideoStream.setCompoundDrawablesWithIntrinsicBounds(null, App.getAppContext().getResources().getDrawable(R.drawable.videocam_gb_32x32), null, null);

    }
}