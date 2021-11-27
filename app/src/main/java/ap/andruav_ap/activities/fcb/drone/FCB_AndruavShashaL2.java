package ap.andruav_ap.activities.fcb.drone;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import java.util.Set;

import ap.andruav_ap.activities.baseview.BaseAndruavShasha_L2;
import ap.andruav_ap.App;
import ap.andruav_ap.communication.telemetry.DroneKit.DroneKitServer;
import ap.andruav_ap.guiEvent.GUIEvent_UpdateConnection;
import ap.andruav_ap.communication.telemetry.TelemetryModeer;
import ap.andruav_ap.DeviceManagerFacade;

import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.FeatureSwitch;
import com.andruav.TelemetryProtocol;
import com.andruav.event.droneReport_Event.Event_FCB_Changed;

import ap.andruav_ap.widgets.SmartOptimization_Dlg;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.tts.TTS;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.factory.util.ProgressDialogHelper;

public class FCB_AndruavShashaL2 extends BaseAndruavShasha_L2 implements Adapter_BluetoothList.OnCustomClickListener{

    private ProgressDialog mprogressDialog;

   // private Button btnScanBlueTooth;

    private TextView txtBluetoothID;
    private TextView txtBluetoothSelect;
    private EditText txtTCPServerIP;
    private EditText txtLocalIP;
    private EditText txtTCPPort;
    private EditText txtUDPPort;
    private Spinner lbUSBBaudRate;
    private EditText txtMavSysID;

    private RadioButton rbBlueTooth;
    private RadioButton rbUSB;
    private RadioButton rbTCP;
    private RadioButton rbUDP;
    private RadioButton rbService_3DR;
    private RadioButton rbNative;

    private Set<BluetoothDevice> pairedDevices;
    private ListView lstbluetoothDevices;
    private Adapter_BluetoothList BTArrayAdapter;

    private Handler mhandle;

    private Menu mMenu;
    private MenuItem miConnect;
    private MenuItem miAutoConnect;
    private MenuItem miSmartTelemetry;
    private MenuItem miDualBand;

    FCB_AndruavShashaL2 Me;

    boolean bSaved = true;
    boolean bIsCOnnecting = false;

    public void onEvent (GUIEvent_UpdateConnection guiEvent_updateConnection)
    {
        if (!guiEvent_updateConnection.andruavUnitBase.IsMe()) return ;
        updateFloatingButton();
    }


    public void onEvent (final Event_FCB_Changed adath_fcb_changed)
    {
        if (!adath_fcb_changed.andruavUnitBase.IsMe()) return;
        Message msg = new Message();
        msg.obj = adath_fcb_changed;
        mhandle.sendMessageDelayed(msg, 0);
    }


    private void initGUI ()
    {
        lstbluetoothDevices = findViewById(R.id.fcbdroneactivity_bluetoothList);


        txtBluetoothID      = findViewById(R.id.fcbdroneactivity_edtBTID);
        txtBluetoothSelect  = findViewById(R.id.fcbdroneactivity_txtBTSelect);
        txtTCPServerIP      = findViewById(R.id.fcbdroneactivity_edtTCPServerIP);
        txtTCPPort          = findViewById(R.id.fcbdroneactivity_edtTCPPort);
        txtLocalIP          = findViewById(R.id.fcbdroneactivity_edtLocalIP);
        txtUDPPort          = findViewById(R.id.fcbdroneactivity_edtUDPPort);
        txtUDPPort          = findViewById(R.id.fcbdroneactivity_edtUDPPort);
        lbUSBBaudRate       = findViewById(R.id.fcbdroneactivity_lbUSBBaudRate);
        txtMavSysID         = findViewById(R.id.fcbdroneactivity_edtSysID);



        rbBlueTooth = findViewById(R.id.fcbdroneactivity_dbBT);
        rbUSB = findViewById(R.id.fcbdroneactivity_rbUSB);
        rbTCP = findViewById(R.id.fcbdroneactivity_rbTCP);
        rbUDP = findViewById(R.id.fcbdroneactivity_rbUDP);

        rbService_3DR = findViewById(R.id.fcbdroneactivity_rb3DR);
        rbNative = findViewById(R.id.fcbdroneactivity_rbNative);

        if (FeatureSwitch.Disable_3DRFCBConnections)
        {
            rbService_3DR.setVisibility(View.INVISIBLE);
        }

        if (FeatureSwitch.Disable_NativeFCBConnections)
        {
            rbNative.setVisibility(View.INVISIBLE);
            rbService_3DR.setChecked(true);
        }

        View.OnClickListener BTListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ScanBlueToothTask().execute();
            }
        };

        txtBluetoothID.setOnClickListener(BTListener);
        txtBluetoothSelect.setOnClickListener(BTListener);

        enableBlueTooth();

        enableUSB();



        if (AndruavSettings.andruavWe7daBase.getTelemetry_protocol()== TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry)
        {
            txtMavSysID.setText(String.valueOf(App.droneKitServer.getSysID()));
        }
        else
        {
            txtMavSysID.setText("Unknown");
        }

        final CharSequence[] baudRates = Constants.baudRateItems;

        final ArrayAdapter vt = new ArrayAdapter(this, android.R.layout.simple_spinner_item, baudRates);
        lbUSBBaudRate.setAdapter(vt);


        String ip = NetInfoAdapter.getIPWifi();
        if (ip==null) ip ="127.0.0.1";
        txtLocalIP.setText(ip);

       String[] category= new String[10];
        for (int i=0;i<10;++i)
        {
            category[i]=String.valueOf(i+1);
        }


        /**
         Updates connection method and ActivityMosa3ed
         */
        View.OnClickListener genericClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bSaved = false;
                savePreference();
                updateFloatingButton();
            }
        };

        rbBlueTooth.setOnClickListener(genericClickListener);
        rbUSB.setOnClickListener(genericClickListener);
        rbTCP.setOnClickListener(genericClickListener);
        rbUDP.setOnClickListener(genericClickListener);

        rbNative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbBlueTooth.setChecked(true);
                bSaved = false;
                //setPhysicalConnectionGUI();
                savePreference();
                updateFloatingButton();
            }
        });


        rbService_3DR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bSaved = false;
                if (!DroneKitServer.isValidAndroidVersion()) {
                    //rbService_3DR.setEnabled(false);
                    DialogHelper.doModalDialog(Me, getString(R.string.gen_connection), getString(R.string.err_3dr_mobileversion), null);
                    rbService_3DR.setChecked(false);
                    rbNative.setChecked(false);
                }

                //setPhysicalConnectionGUI();
                savePreference();
                updateFloatingButton();
            }
        });

        loadPreference();
        setPhysicalConnectionGUI();
        //updateFloatingButton();

        lbUSBBaudRate.requestFocus();

        UIHandler();
    }

    private void UIHandler() {

        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.obj instanceof Event_FCB_Changed)
                {
                    Event_FCB_Changed adath_fcb_changed = (Event_FCB_Changed) msg.obj;
                    if ((adath_fcb_changed.andruavUnitBase == null) || (!adath_fcb_changed.andruavUnitBase.IsMe()))
                    {
                        return ;
                    }
                    if (adath_fcb_changed.andruavUnitBase.getTelemetry_protocol()== TelemetryProtocol.TelemetryProtocol_DroneKit_Telemetry)
                    {
                        txtMavSysID.setText(String.valueOf(App.droneKitServer.getSysID()));
                    }

                }
            }
        };
    }


    private void updateFloatingButton ()
    {

        if (miConnect == null)
        {
            // sometimes it is called from outside form before initialization - OnEvent-
            return;
        }
        int drawable=R.drawable.bluetooth_gy_72x72;

        if (bIsCOnnecting || (TelemetryModeer.getConnectionInfo()!= TelemetryModeer.CURRENTCONNECTION_NON))
        {
            rbBlueTooth.setEnabled(false);
            rbUSB.setEnabled(false);
            rbTCP.setEnabled(false);
            rbUDP.setEnabled(false);

            rbService_3DR.setEnabled(false);
            rbNative.setEnabled(false);

            switch (Preference.getFCBTargetLib(null))
            {
                case Preference.FCB_LIB_3DR:
                    drawable = R.drawable.service_3dr_72x72;
                    break;
                case Preference.FCB_LIB_DJI:
                    drawable = R.drawable.dji_72x72;
                    break;
                case Preference.FCB_LIB_NATIVE:
                default:
                    switch (Preference.getFCBTargetComm(null))
                    {
                        case Preference.FCB_COM_BT:
                            drawable = R.drawable.bluetooth_b_72x72;
                            break;
                        case Preference.FCB_COM_TCP:
                            drawable = R.drawable.ip_network_b_72x72;
                            break;
                        case Preference.FCB_COM_UDP:
                            drawable = R.drawable.ip_network_b_72x72;
                            break;
                        case Preference.FCB_COM_USB:
                            drawable = R.drawable.usb2_b_72x72;
                            break;
                    }
                    break;
            }
        }
        else
        {

            setPhysicalConnectionGUI();
            //rbBlueTooth.setEnabled(true);
            //rbUSB.setEnabled(true);
            //rbTCP.setEnabled(true);
            //rbUDP.setEnabled(true);

            rbService_3DR.setEnabled(true);
            rbNative.setEnabled(true);

           switch (Preference.getFCBTargetLib(null))
            {
                case Preference.FCB_LIB_3DR:
                    drawable = R.drawable.service_3dr_gy_72x72;
                    break;
                case Preference.FCB_LIB_DJI:
                    drawable = R.drawable.dji_72x72;
                    break;
                case Preference.FCB_LIB_NATIVE:
                default:
                    switch (Preference.getFCBTargetComm(null))
                    {
                        case Preference.FCB_COM_BT:
                            drawable = R.drawable.bluetooth_gy_72x72;
                            break;
                        case Preference.FCB_COM_TCP:
                            drawable = R.drawable.ip_network_gy_72x72;
                            break;
                        case Preference.FCB_COM_UDP:
                            drawable = R.drawable.ip_network_gy_72x72;
                            break;
                        case Preference.FCB_COM_USB:
                            drawable = R.drawable.usb2_gy_72x72;
                            break;
                    }
                    break;
            }
        }



        miConnect.setIcon(drawable);
        miAutoConnect.setChecked(Preference.isAutoFCBConnect(null));

        if (NetInfoAdapter.getDual3GAccess())
        {
            miDualBand.setIcon(R.drawable.dual_band_active2_72x72);
        }
        else
        {
            miDualBand.setIcon(R.drawable.dual_band_inactive2_72x72);
        }




    }


    private  void enableBlueTooth()
    {
        final boolean enable = DeviceManagerFacade.hasBlueTooth();
        txtBluetoothID.setEnabled(enable);
        txtBluetoothID.setEnabled(enable);
        rbBlueTooth.setEnabled(enable);

    }

    private void enableUSB ()
    {
        final boolean enable =DeviceManagerFacade.hasUSBHost();
        rbUSB.setEnabled(enable);
        lbUSBBaudRate.setEnabled(enable);

    }


    private void setPhysicalConnectionGUI()
    {
        if (rbNative.isChecked())
        {
            enableBlueTooth();
            enableUSB();
            rbTCP.setEnabled(false);
            rbUDP.setEnabled(true);

            return;
        }

        if (rbService_3DR.isChecked())
        {
            enableBlueTooth();
            enableUSB();
            rbTCP.setEnabled(true);
            rbUDP.setEnabled(true);

            /*
            DialogHelper.doModalCustomDialogReminder(Me, getString(R.string.gen_warrning), getString(R.string.err_protocol_doesnt_suppport_telemetry), null, new DialogHelper.DialogReminderPreferenceCallBack() {
                @Override
                public boolean readPreference() {
                    return Preference.gui_ShowAndruav3DRNotice(null);
                }

                @Override
                public void writePreference(boolean value) {
                    Preference.gui_ShowAndruav3DRNotice(null,value);
                }

                @Override
                public void onDismiss() {

                }

                @Override
                public void onCancel() {

                }

                @Override
                public void onOK() {

                }
            });
            */
            return;
        }

        return;

    }


    private void savePreference ()
    {

        Preference.setFCBDroneTCPServerIP(null, txtTCPServerIP.getText().toString());
        Preference.setFCBDroneTCPServerPort(null, txtTCPPort.getText().toString());
        Preference.setFCBDroneUDPServerPort(null, txtUDPPort.getText().toString());

        int FCBTargetLib = Preference.FCB_LIB_NATIVE;
        //if (rbNative.isChecked()) FCBTargetLib = Preference.FCB_LIB_NATIVE;
        if (rbService_3DR.isChecked()) FCBTargetLib = Preference.FCB_LIB_3DR;
        //if (rbDJI.isChecked()) FCBTargetLib = Preference.FCB_LIB_DJI;

        Preference.setFCBTargetLib(null, FCBTargetLib);


        int FCBTargetComm = Preference.FCB_COM_BT;
        if (rbTCP.isChecked()) FCBTargetComm = Preference.FCB_COM_TCP;
        if (rbUDP.isChecked()) FCBTargetComm = Preference.FCB_COM_UDP;
        if (rbUSB.isChecked()) FCBTargetComm = Preference.FCB_COM_USB;

        Preference.setFCBTargetComm(null, FCBTargetComm);

        Preference.setFCBUSBBaudRateSelector(null, (int) lbUSBBaudRate.getSelectedItemId());

        bSaved = true;
    }


    private void loadPreference()
    {
        bSaved = false;

        txtBluetoothID.setText(Preference.getFCBBlueToothName(null));
        txtTCPServerIP.setText(Preference.getFCBDroneTCPServerIP(null));
        txtTCPPort.setText(Preference.getFCBDroneTCPServerPort(null));
        txtUDPPort.setText(Preference.getFCBDroneUDPServerPort(null));

        switch (Preference.getFCBTargetLib(null))
        {
            case Preference.FCB_LIB_3DR:
                rbService_3DR.setChecked(true);
                break;
            case Preference.FCB_LIB_DJI:
                //rbDJI.setChecked(true);
                rbService_3DR.setChecked(true);
                break;
            case Preference.FCB_LIB_NATIVE:
            default:
                //rbNative.setChecked(true);
                rbService_3DR.setChecked(true);
                break;
        }

        switch (Preference.getFCBTargetComm(null))
        {
            case Preference.FCB_COM_BT:
                rbBlueTooth.setChecked(true);
                break;
            case Preference.FCB_COM_TCP:
                rbTCP.setChecked(true);
                break;
            case Preference.FCB_COM_UDP:
                rbUDP.setChecked(true);
                break;
            case Preference.FCB_COM_USB:
                rbUSB.setChecked(true);
                break;
        }

        lbUSBBaudRate.setSelection(Preference.getFCBUSBBaudRateSelector(null));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_fcb_drone, menu);
        mMenu = menu;
        miConnect           =  mMenu.findItem(R.id.mi_fcb_drone_connect);
        miAutoConnect       =  mMenu.findItem(R.id.mi_fcb_autofcbconnect);
        miDualBand          =  mMenu.findItem(R.id.mi_fcb_dualband);
        miSmartTelemetry    =  mMenu.findItem(R.id.mi_fcb_smarttelemetry);


        updateFloatingButton();


        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        updateFloatingButton();

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();


        switch (id)
        {
            case R.id.mi_fcb_drone_connect:
                if ((TelemetryModeer.getConnectionInfo()!= TelemetryModeer.CURRENTCONNECTION_NON) || (App.droneKitServer != null))
                {
                    // DroneKitServer != null means it is active, but does not mean it is connected. i.e. could be waiting for UDP connection.

                   // TTS.getInstance().Speak("Disconnecting");
                    final String connstr = getString(R.string.gen_bluetooth_disconnecting);
                    bIsCOnnecting = false;
                    final ProgressDialog progress = DialogHelper.doModalProgressDialog(this,"FCB",connstr);
                    App.mScheduleHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                        }
                    },1500);

                    Toast.makeText(App.getAppContext(),connstr, Toast.LENGTH_SHORT).show();
                    TelemetryModeer.closeAllConnections();
                    savePreference();


                }
                else
                {
                    final String connstr = getString(R.string.gen_bluetooth_connecting);
                    bIsCOnnecting = true;
                    final ProgressDialog progress = DialogHelper.doModalProgressDialog(this,"FCB",connstr);
                    App.mScheduleHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                        }
                    },1500);

                    TTS.getInstance().Speak(connstr);
                    Toast.makeText(App.getAppContext(),connstr, Toast.LENGTH_SHORT).show();
                    savePreference();
                    TelemetryModeer.connectToPreferredConnection(Me,false);

                }
                updateFloatingButton();
                break;

            case R.id.mi_fcb_dualband:
                NetInfoAdapter.Dual3GAccess(!NetInfoAdapter.getDual3GAccess());
                updateFloatingButton();
                break;
            case R.id.mi_fcb_Help:
                GMail.sendGMail(this, getString(R.string.email_title), getString(R.string.email_to), getString(R.string.email_subject),  getString(R.string.email_body), null);
                break;
            case R.id.mi_fcb_autofcbconnect:
                Preference.isAutoFCBConnect(null,!Preference.isAutoFCBConnect(null));
                item.setChecked(Preference.isAutoFCBConnect(null));
                break;

            case R.id.mi_fcb_smarttelemetry:
                SmartOptimization_Dlg smartOptimization_dlg = SmartOptimization_Dlg.newInstance(AndruavSettings.andruavWe7daBase.UnitID);
                smartOptimization_dlg.show(fragmentManager,"fragment_edit_name");
                break;

        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fcb_drone);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        Me = this;
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initGUI();
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }


    @Override
    protected void onResume ()
    {
        super.onResume();
    }

    @Override
    public void onBackPressed()
    {

        if (lstbluetoothDevices.getVisibility()!=View.INVISIBLE)
        {
            lstbluetoothDevices.setVisibility(View.INVISIBLE);
            return;
        }

        if (!bSaved) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(getString(R.string.action_savebeforeexit));


            alert.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    savePreference();
                    Toast.makeText(getApplicationContext(), getString(R.string.action_done), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

            alert.setNegativeButton(getString(android.R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    });

            alert.show();
        }
        else
        {
            finish();
        }
        // super.onBackPressed();
    }


    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name and the MAC address of the object to the arrayAdapter
                BTArrayAdapter.add(device);
                BTArrayAdapter.notifyDataSetChanged();

            }else
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                ProgressDialogHelper.exitProgressDialog();
            }
            else
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
            {
                ProgressDialogHelper.doProgressDialog(Me,"Bluetooth");
                TTS.getInstance().Speak(getString(R.string.gen_bluetooth_disconnected));
            }
            /*
            else
            if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
            {
                TTS.getInstance().Speak(getString(R.string.gen_bluetooth_connected));
                Toast.makeText(getApplicationContext(),getString(R.string.gen_bluetooth_connected), Toast.LENGTH_LONG).show();

            }
            else
            if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
            {
                TTS.getInstance().Speak(getString(R.string.gen_bluetooth_disconnected));
                Toast.makeText(getApplicationContext(),getString(R.string.gen_bluetooth_disconnected), Toast.LENGTH_LONG).show();
            }
            */

        }
    };



    public boolean turnOnBlueTooth(){

        BTArrayAdapter.clear();
        if (App.BT.Bluetooth.GetAdapter()) {
            App.BT.Bluetooth.Enable();
            registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            registerReceiver(bReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
            registerReceiver(bReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
            return true;
        }
        return false;
    }


    public void turnOffBlueTooth() {
        if ((App.BT.Bluetooth == null) || (!App.BT.Bluetooth.GetAdapter())) {
            App.BT.StopPersistentConnection();
            //killThread();
            //BTArrayAdapter.clear();
            App.BT.Bluetooth.cancelDiscovery();
            App.BT.Bluetooth.disable();
        }

        unregisterReceiver(bReceiver);
    }


    private void enableGUIBlueTooth (boolean enable) {

        if (enable) {
            lstbluetoothDevices.setVisibility(View.VISIBLE);
        }
        else {
            lstbluetoothDevices.setVisibility(View.INVISIBLE);
        }

        BTArrayAdapter = new Adapter_BluetoothList (Me,Me);
        lstbluetoothDevices.setAdapter(BTArrayAdapter);

    }

    public void scanforDevices() {
        if (!App.BT.Bluetooth.isEnabled()) return ;

            //killThread();
            App.BT.StopPersistentConnection();

            if (App.BT.Bluetooth.isDiscovering()) {
                App.BT.Bluetooth.cancelDiscovery();
            }

            BTArrayAdapter.clear();
            App.BT.Bluetooth.startDiscovery();



        }

    @Override
    public void onItemClick(View aView, int position) {

        ListItem_BluetoothUnit listItem_bluetoothUnit =  (ListItem_BluetoothUnit) BTArrayAdapter.getItem(position);
        txtBluetoothID.setText(listItem_bluetoothUnit.deviceName);
        Preference.setFCBBlueToothMAC(null, listItem_bluetoothUnit.getDeviceMAC());
        Preference.setFCBBlueToothName(null, listItem_bluetoothUnit.deviceName);
        lstbluetoothDevices.setVisibility(View.INVISIBLE);
        return;
    }


    private class ScanBlueToothTask extends AsyncTask<Void, Integer, Void> {

            @Override
            protected Void doInBackground(Void... params) {

                try {
                    while (!App.BT.Bluetooth.isEnabled()) {
                        Thread.sleep(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Void params) {
                //listParedDevices();
                scanforDevices();
                //turnOffBlueTooth();

            }

            @Override
            protected void onPreExecute() {
                enableGUIBlueTooth(true);
                if (!turnOnBlueTooth()) {
                    ProgressDialogHelper.exitProgressDialog();
                    this.cancel(true);
                    DialogHelper.doModalDialog(Me, getString(R.string.gen_connection), getString(R.string.err_no_bluetooth), null);
                }


            }
        }


}
