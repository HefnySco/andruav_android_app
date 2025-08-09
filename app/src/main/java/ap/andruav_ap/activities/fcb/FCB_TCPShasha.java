package ap.andruav_ap.activities.fcb;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.activities.baseview.BaseAndruavShasha;
import ap.andruav_ap.App;
import com.andruav.AndruavFacade;

import ap.andruav_ap.activities.fcb.drone.Adapter_DroneTelemetry;
import ap.andruav_ap.activities.fcb.drone.ListItem_DroneTelemetry;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.TelemetryProtocol;
import com.andruav.event.droneReport_Event.Event_FCB_Changed;

import com.andruav.event.fcb_event.Event_SocketAction;
import ap.andruav_ap.widgets.SmartOptimization_Dlg;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;

public class FCB_TCPShasha extends BaseAndruavShasha implements Adapter_DroneTelemetry.OnCustomClickListener {

    protected FCB_TCPShasha Me;


    private Button      btnSocketListen;
    private EditText    edtIP;
    private EditText    edtPort;
    private TextView    txtStatus;
    private EditText    txtlog;
    private ListView    lstTelemetryUnits;

    private Adapter_DroneTelemetry TelemetryUnitsAdapter;

    protected Handler mhandle;

    protected String address;

    Boolean killMe = false;


    public void onEvent (final Event_SocketAction eventSocketAction) {
        final Message msg = mhandle.obtainMessage();
        msg.obj = eventSocketAction;
        mhandle.sendMessageDelayed(msg, 0);
    }




    public void onEvent (final Event_FCB_Changed a7adath_fcb_changed)
    {
        if (!a7adath_fcb_changed.andruavUnitBase.IsMe()) return;

        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_fcb_changed;
        mhandle.sendMessageDelayed(msg, 0);
    }


    private void enableGUISerialSocket (final boolean enable)
    {
        lstTelemetryUnits.setEnabled(enable);
        if (enable)
        {
            NetInfoAdapter.Update();
            String ip = NetInfoAdapter.getIPWifi();
            if (ip==null) ip ="127.0.0.1";
            edtIP.setText(ip);
            edtPort.setText(String.valueOf(Preference.getSerialServerPort(null)));
            edtPort.setEnabled(!App.isSocketListenerRunning());
            btnSocketListen.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                  stopSocketListener();

                }

            });
            if (!App.isSocketListenerRunning()) {
                btnSocketListen.setVisibility(View.INVISIBLE);
            }

            lstTelemetryUnits.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            lstTelemetryUnits.setAdapter(TelemetryUnitsAdapter);

        }

    }




    /***
     * Refresh view list with available telemetry-active units
     */
    private final Runnable runnabbleRefreshList = new Runnable() {
        @Override
        public void run() {
            if (AndruavEngine.getAndruavWS() == null) return ;
            for (int i = 0; i < AndruavEngine.getAndruavWe7daMapBase().size(); i++) {
                AndruavUnitShadow andruavUnit = (AndruavUnitShadow) AndruavEngine.getAndruavWe7daMapBase().valueAt(i);
                if (!andruavUnit.getIsCGS()) {

                    // 1- hasTelemetry
                    boolean hasTelemetry = ((andruavUnit.getTelemetry_protocol() != TelemetryProtocol.TelemetryProtocol_No_Telemetry));

                    // 2- Prop Index
                    final int index = TelemetryUnitsAdapter.getPosition(andruavUnit.UnitID);

                    if ((index == -1) && hasTelemetry)
                    {   // item not existed as telemetry
                            //add to list
                            if (!andruavUnit.getIsShutdown())
                            {
                                // dont add shutdown items
                                TelemetryUnitsAdapter.add(andruavUnit);
                                TelemetryUnitsAdapter.notifyDataSetChanged();
                                if ((AndruavSettings.remoteTelemetryAndruavWe7da != null) && (!AndruavSettings.remoteTelemetryAndruavWe7da.Equals(andruavUnit))) { // this is new one

                                } else {
                                    mhandle.postDelayed(this, 500); // we need to highlight it fast
                                }
                            }
                                continue;
                    }

                    if ((index != -1) && hasTelemetry)
                    {   // item exists and has telemetry
                            if ((AndruavSettings.remoteTelemetryAndruavWe7da != null) && AndruavSettings.remoteTelemetryAndruavWe7da.UnitID.equals(andruavUnit.UnitID)) {   // it is already connected to me
                                // tech-android: you cannot update after you add ... you need another loop

                                // Enh; put here code for handling Shutdown Drones.

                                // item is selected but it is SHUT DOWN :(
                                if (andruavUnit.getIsShutdown()) { // it is disconnected
                                    Me.selectItem(AndruavSettings.remoteTelemetryAndruavWe7da, false, false);
                                    // reclick selected on
                                    AndruavFacade.StopTelemetry();
                                    AndruavEngine.notification().Speak(App.getAppContext().getString(ap.andruavmiddlelibrary.R.string.err_serialsocket_no_remote_telemetry));
                                } else {  // item is already selected
                                    Me.selectItem(AndruavSettings.remoteTelemetryAndruavWe7da, true, false);
                                }

                            } else {
                                // it is not connected as Active Telemetry but available
                                if (andruavUnit.getIsShutdown()) { // it is disconnected
                                    enableItem(index, false);

                                } else {
                                    enableItem(index, AndruavSettings.andruavWe7daBase.canTelemetry());
                                }
                            }
                        continue;
                    }

                    if ((index != -1) && hasTelemetry) { // item exists and HAS Telemetry Selected ... This means a problem...
                        if ((AndruavSettings.remoteTelemetryAndruavWe7da != null) && AndruavSettings.remoteTelemetryAndruavWe7da.UnitID.equals(andruavUnit.UnitID)) {   // it is already connected to me

                            Me.selectItem(AndruavSettings.remoteTelemetryAndruavWe7da, true, true);
                        }
                        continue;
                    }

                }
            }
            if (!killMe) {
                mhandle.postDelayed(this, 4000);
            }
        }
    };


    private void UIHandler () {
        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String htmlText;
                if (msg.obj instanceof Event_SocketAction) {
                    final Event_SocketAction event_socketAction = (Event_SocketAction) msg.obj;
                    switch (event_socketAction.socketAction) {
                        case Event_SocketAction.SOCKETACTION_STARTED:
                            txtStatus.setText(getString(ap.andruavmiddlelibrary.R.string.gen_serialsocket_started));
                            htmlText = "<font color=#36AB36>" + "Telemetry TCP server started" + "</font><br>";
                            txtlog.append(Html.fromHtml(htmlText));
                            break;
                        case Event_SocketAction.SOCKETACTION_CLOSED:
                            txtStatus.setText(getString(ap.andruavmiddlelibrary.R.string.gen_serialsocket_started));
                            htmlText = "<font color=#F75050>" + "Telemetry TCP server stopped" + "</font><br>";
                            txtlog.append(Html.fromHtml(htmlText));
                            break;
                        case Event_SocketAction.SOCKETACTION_CLIENT_CONNECTED:
                            htmlText = "<font color=#75A4D3>" + "client connected </font><font color=#36AB36>" + event_socketAction.clientSocketIP +  "</font><br>";
                            txtlog.append(Html.fromHtml(htmlText));
                            if (AndruavSettings.remoteTelemetryAndruavWe7da == null )
                            {
                                AndruavEngine.notification().Speak(App.getAppContext().getString(ap.andruavmiddlelibrary.R.string.err_serialsocket_no_remote_telemetry));
                            }
                            break;
                        case Event_SocketAction.SOCKETACTION_CLIENT_DISCONNECTED:
                            htmlText = "<font color=#75A4D3>" + "client disconnected </font><font color=#36AB36>" + event_socketAction.clientSocketIP + "</font><br>";
                            txtlog.append(Html.fromHtml(htmlText));
                            break;

                    }
                }
                else if (msg.obj instanceof Event_FCB_Changed)
                {
                    final Event_FCB_Changed adath_fcb_changed = (Event_FCB_Changed) msg.obj;
                    final AndruavUnitShadow andruavWe7da = (AndruavUnitShadow) adath_fcb_changed.andruavUnitBase;
                    final int index = TelemetryUnitsAdapter.getPosition(andruavWe7da.UnitID);
                    boolean hasTelemetry = (andruavWe7da.getTelemetry_protocol() != TelemetryProtocol.TelemetryProtocol_No_Telemetry);

                    if (index != -1) { // item exists and HAS Telemetry Selected ... This means a problem...
                        if ((AndruavSettings.remoteTelemetryAndruavWe7da != null) && AndruavSettings.remoteTelemetryAndruavWe7da.UnitID.equals(andruavWe7da.UnitID)) {   // it is already connected to me

                            Me.selectItem(AndruavSettings.remoteTelemetryAndruavWe7da, true, !hasTelemetry);
                        }
                    }
                }

            }
        };


    }



    private void initGUI ()
    {

         setContentView(R.layout.activityfcb_serialsocket);
         txtlog              = findViewById(R.id.fcbactivity_serialsocket_edtlog);
         txtStatus           = findViewById(R.id.fcbactivity_serialsocket_txtStatus);
         edtIP               = findViewById(R.id.fcbactivity_serialsocket_edtip);
         edtPort             = findViewById(R.id.fcbactivity_serialsocket_edtport);
         btnSocketListen     = findViewById(R.id.fcbactivity_serialsocket_btnSerialSocket);
         lstTelemetryUnits   = findViewById(R.id.fcbactivity_serialsocket_lstTelemetryUnits);

         final boolean  bRunning = App.isSocketListenerRunning();

         btnSocketListen.setText( getString((bRunning)?ap.andruavmiddlelibrary.R.string.gen_on:ap.andruavmiddlelibrary.R.string.gen_off));
         btnSocketListen.setBackgroundResource(R.drawable.button_shap);
         TelemetryUnitsAdapter = new Adapter_DroneTelemetry(Me,Me);

        final boolean  canTelemetry = AndruavSettings.andruavWe7daBase.canTelemetry();
         if (!canTelemetry)
         {
             DialogHelper.doModalDialog(Me,"Insufficient Permission","Your Access Code does not have enough permission.","OK");

         }
         enableGUISerialSocket(canTelemetry);
         UIHandler();


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AndruavSettings.andruavWe7daBase.getIsCGS()) this.finish();
        try {
            Me = this;
            Window win = getWindow();
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            initGUI();

        } catch (Exception e) {
            AndruavEngine.log().logException("FCB_TCP", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_fcb, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.mi_fcb_Help) {
            GMail.sendGMail(this, getString(ap.andruavmiddlelibrary.R.string.email_title), getString(ap.andruavmiddlelibrary.R.string.email_to), getString(ap.andruavmiddlelibrary.R.string.email_subject), getString(ap.andruavmiddlelibrary.R.string.email_body), null);
        } else if (id == R.id.mi_fcb_smarttelemetry) {
            if (AndruavSettings.remoteTelemetryAndruavWe7da != null)
            {
                SmartOptimization_Dlg smartOptimization_dlg = SmartOptimization_Dlg.newInstance(AndruavSettings.remoteTelemetryAndruavWe7da.UnitID);
                smartOptimization_dlg.show(fragmentManager,"fragment_edit_name");

            }
            else
            {
                DialogHelper.doModalDialog(this,"Error","No Active Telemetry","OK");
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume ()
    {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        killMe = false;
        onUpdateStatus();
        EventBus.getDefault().register(this);
        mhandle.postDelayed(runnabbleRefreshList, 1000);


    }


    @Override
    protected void onPause ()
    {
        super.onPause();
        if (AndruavSettings.remoteTelemetryAndruavWe7da==null)
        { // in case there is no active telemetry please stop Socket Listener.
            stopSocketListener();
        }

        killMe = true;
        EventBus.getDefault().unregister(this);

        if (edtPort != null) {
                if (edtPort.getText().length() == 0) {
                    Preference.setSerialServerPort(null, 9891);
                } else
                {
                    String s = edtPort.getText().toString();
                    int port=9891;
                    try {

                            port = Integer.parseInt(s);
                            if ((port < 0 ) || (port > 65535))
                            {
                                port = 9891;
                            }

                    }
                        catch (final NumberFormatException e)
                    {
                        port = 9891;
                    }
                        finally {
                    Preference.setSerialServerPort(null, port);
                }
            }
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // Unregister broadcast listeners

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub

        super.onActivityResult(requestCode, resultCode, data);
    }

    protected boolean validatePortNumber ()
    {

        if (edtPort.getText().length() == 0) {
           return false;
        }
        String s = edtPort.getText().toString();
        int port=9891;
        try {

            port = Integer.parseInt(s);
            if ((port < 0 ) || (port > 65535))
            {
                port = 9891;
            }
            Preference.setSerialServerPort(null, port);
        }
        catch (final NumberFormatException e)
        {
            return false;
        }

        return true;
    }

    private void stopSocketListener()
    {

        AndruavUnitBase oldTelemetry=null;
        if (AndruavSettings.remoteTelemetryAndruavWe7da!= null) {
            oldTelemetry = AndruavSettings.remoteTelemetryAndruavWe7da;
        }

        if (App.isSocketListenerRunning()) {
            // stop Me
            App.stopsocketListener();
            btnSocketListen.setText(getString(ap.andruavmiddlelibrary.R.string.gen_off));
            btnSocketListen.setBackgroundResource(R.drawable.button_shap);
            btnSocketListen.setVisibility(View.INVISIBLE);
            AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_No_Telemetry);  // reset telemetry
        }

        if (oldTelemetry != null)
        {   // suspend old sender drone.
            AndruavFacade.StopTelemetry();
        }

    }

    private void toggleSocketListener() {

        if (!validatePortNumber())
        {
            DialogHelper.doModalDialog(this,getString(ap.andruavmiddlelibrary.R.string.gen_connection),getString(ap.andruavmiddlelibrary.R.string.err_invalid_portnum),getString(android.R.string.ok));
            return ;
        }





        stopSocketListener();

        NetInfoAdapter.Update();

        if (NetInfoAdapter.isConnectedViaWifi()) {
                // Start Me
                edtIP.setText(NetInfoAdapter.getIPWifi()); // when enter the screen with no network...then activate network.... we need to update IP


        }
        else
        {
            edtIP.setText("127.0.0.1");
        }

        App.startsocketListener();
        btnSocketListen.setText(getString(ap.andruavmiddlelibrary.R.string.gen_on));
        btnSocketListen.setBackgroundResource(R.drawable.button_active);
        btnSocketListen.setVisibility(View.VISIBLE);
        AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry);  // reset telemetry


        // sendMessageToModule my new telemetry status now

        AndruavFacade.broadcastID(); // the ID holds my new telemetry status.

        if(AndruavSettings.andruavWe7daBase.telemetry_protocol == TelemetryProtocol.TelemetryProtocol_No_Telemetry)
        {
            TelemetryUnitsAdapter.clear();
        }

        edtPort.setEnabled(!App.isSocketListenerRunning());
    }

    private void onUpdateStatus()
    {
        if (App.isSocketListenerRunning())
        {
                txtStatus.setText(getString(ap.andruavmiddlelibrary.R.string.gen_serialsocket_started));
                btnSocketListen.setBackgroundResource(R.drawable.button_active);
        }
        else
        {
                if (NetInfoAdapter.isConnectedViaWifi())
                {
                    txtStatus.setText(getString(ap.andruavmiddlelibrary.R.string.gen_serialsocket_stopped));
                    btnSocketListen.setBackgroundResource(R.drawable.button_shap);
                }
                else
                {
                    txtStatus.setText(getString(ap.andruavmiddlelibrary.R.string.gen_serialsocket_no_wifi));
                }
        }

    }

    private void enableItem (final int index, final boolean select)
    {
        if (index ==-1) return ;

        View vi = lstTelemetryUnits.getChildAt(index);
        if (vi==null) return ;
        Adapter_DroneTelemetry.ViewHolder holder=(Adapter_DroneTelemetry.ViewHolder)vi.getTag();
        holder.btnSelected.setChecked(false);
        holder.btnSelected.setEnabled(select);
    }

    private void selectItem (final AndruavUnitBase andruavWe7da, final boolean select, final boolean error)
    {
        if (andruavWe7da==null) return ;

        int index = TelemetryUnitsAdapter.getPosition(andruavWe7da.UnitID);
        if (index ==-1) return ;

        View vi = lstTelemetryUnits.getChildAt(index);
        if (vi==null) return ;
        Adapter_DroneTelemetry.ViewHolder holder=(Adapter_DroneTelemetry.ViewHolder)vi.getTag();

        int color = getResources().getColor(R.color.btn_TXT_BLUE_DARKER);
        if (error)
        {
            color = getResources().getColor(R.color.btn_TXT_ERROR);
        }
        else
        {
            if (select)
            {
                color = getResources().getColor(R.color.btn_TXT_GREEN_DARKER);
            }
        }

        holder.txtUnitName.setTextColor(color);
        holder.btnSelected.setChecked(select);
    }



   @Override
    public void onItemClick(View aView, int position) {
       if (!App.isSocketListenerRunning()) {
               toggleSocketListener(); // Start...This is to make it easy for users.
       }

       if (AndruavSettings.andruavWe7daBase.getTelemetry_protocol() == TelemetryProtocol.TelemetryProtocol_No_Telemetry)
       { // nothing is selected
            ((ToggleButton)(aView)).setChecked(false);
            return ;
       }

       // any GCS has Telemetry = Unknown
       // we need to reset to unknown as we might have switch from MW to MAVLINK
       // Reset to unknown ...
       AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry);
       ListItem_DroneTelemetry droneTelemetry = (ListItem_DroneTelemetry) TelemetryUnitsAdapter.getItem(position);
       if (droneTelemetry == null) return ;

       // reclick selected on
       if ((droneTelemetry.getAndruavWe7da()!= null) &&(droneTelemetry.getAndruavWe7da().Equals(AndruavSettings.remoteTelemetryAndruavWe7da)))
       {
            selectItem(AndruavSettings.remoteTelemetryAndruavWe7da, false, false);
            AndruavFacade.StopTelemetry();
            AndruavEngine.notification().Speak(App.getAppContext().getString(ap.andruavmiddlelibrary.R.string.err_serialsocket_no_remote_telemetry));
            return ;
       }

       // old one was selected and we need to uncheck it
       AndruavUnitBase oldTelemetry = AndruavSettings.remoteTelemetryAndruavWe7da;
       if (oldTelemetry != null)
       {
            // unselect old one
            selectItem(oldTelemetry, false, true);
            AndruavFacade.StopTelemetry();

       }

       // activate newly selected one
       final AndruavUnitShadow we7da = droneTelemetry.getAndruavWe7da();

       AndruavFacade.StartTelemetry(we7da,Preference.getSmartMavlinkTelemetry(null));
    }
}