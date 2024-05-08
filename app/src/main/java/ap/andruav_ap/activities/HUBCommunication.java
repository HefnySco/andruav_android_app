package ap.andruav_ap.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.activities.login.LoginScreenFactory;
import ap.andruav_ap.activities.baseview.BaseAndruavShasha;
import ap.andruav_ap.App;
import ap.andruavmiddlelibrary.LoginClient;
import com.andruav.event.networkEvent.EventLoginClient;
import com.andruav.event.networkEvent.EventSocketState;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.preference.PreferenceValidator;
import ap.andruav_ap.R;
import com.andruav.FeatureSwitch;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;

import com.andruav.AndruavEngine;
import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;

import com.andruav.AndruavSettings;

import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;

public class HUBCommunication extends BaseAndruavShasha {

    //////  Attributes
    private HUBCommunication Me;
    private EditText    mtxtWebServerIP;
    private EditText    mtxtWebServerPort;
    private EditText    mtxtWebUserName;
    private Spinner     mspinnerGroupNo;

    private EditText    mtxtWebDescription;
    private CheckBox    mcheckAndruavServer;
    private CheckBox    mchkEncryptWebServer;
    private TextView    mtxtEncryptionKey;


    private Menu mMenu;
    private Handler mhandle;
    protected String text;


    ////// EOF  Attributes

    //////////BUS EVENT

    public void onEvent (EventLoginClient event_LoginClient) {

        Message msg = new Message();
        msg.obj = event_LoginClient;
        mhandle.sendMessageDelayed(msg,0);
    }


    public void onEvent (final Andruav_2MR Andruav_2MR) {
        Message msg = new Message();
        msg.obj = Andruav_2MR;
        mhandle.sendMessageDelayed(msg,0);
    }





    public void onEvent (final EventSocketState event) {
        Message msg = new Message();
        msg.obj = event;
        mhandle.sendMessageDelayed(msg,0);
    }

   ///////////////////

    private void UIHandler () {
        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);


            }
        };
    }


    private void updateUI ()
    {

        mtxtWebServerIP.setEnabled(!mcheckAndruavServer.isChecked());
        mtxtWebServerPort.setEnabled(!mcheckAndruavServer.isChecked());

        if (mcheckAndruavServer.isChecked())
        {
            mtxtWebServerIP.setText(AndruavEngine.getPreference().getContext().getResources().getString(R.string.pref_auth_URL));
            mtxtWebServerPort.setText(String.valueOf(AndruavEngine.getPreference().getContext().getResources().getInteger(R.integer.pref_auth_Port)));
        }
    }

    private void initGUI ()
    {
        mtxtWebServerIP = findViewById(R.id.hubactivity_edtWSIP);
        mtxtWebServerPort = findViewById(R.id.hubactivity_edtWSPort);
        mcheckAndruavServer = findViewById(R.id.hubactivity_chkLocalServer);
        mcheckAndruavServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updateUI();


            }
        });
        mtxtWebUserName = findViewById(R.id.hubactivity_edtWSUserName);




        mspinnerGroupNo     = findViewById(R.id.hubactivity_spinGroupNo);
        String[] category= new String[10];
        for (int i=0;i<10;++i)
        {
            category[i]=String.valueOf(i+1);
        }

        ArrayAdapter aa = new ArrayAdapter(this,android.R.layout.simple_spinner_item, category);
        mspinnerGroupNo.setAdapter(aa);
        mspinnerGroupNo.setEnabled(Preference.enableGroupName(null));


        mtxtWebDescription = findViewById(R.id.hubactivity_edtDescription);
        mchkEncryptWebServer = findViewById(R.id.hubactivity_chkEncryption);
        mtxtEncryptionKey= findViewById(R.id.hubactivity_edtEncryptionKey);

        mtxtWebServerIP.setEnabled(!mcheckAndruavServer.isChecked());
        mtxtWebServerPort.setEnabled(!mcheckAndruavServer.isChecked());


        UIHandler();




    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Me = this;

        setContentView(R.layout.activity_hubcommunication);

       initGUI();
    }


    @Override
    public void onStart()
    {
        super.onStart();
        readPreference();
    updateUI();

    }

    @Override
    public void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        EventBus.getDefault().register(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_hubcommunication, menu);
        mMenu = menu;
        //miConnect = mMenu.findItem(R.id.action_commhub_connect);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case  R.id.action_save:
                savePreference();
                return true;
            case R.id.action_refresh:
                readPreference();
                return true;
            case R.id.mi_hub_Help:
                GMail.sendGMail(this, getString(R.string.email_title),getString(R.string.email_to), getString(R.string.email_subject), getString(R.string.email_body),null);
                return true;


        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.action_savebeforeexit));


        alert.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (savePreference()==false)
                    return ; // dont exit with values not saved.
                Toast.makeText(getApplicationContext(), getString(R.string.action_done), Toast.LENGTH_SHORT).show();
                App.stopAndruavWS(true);
                finish();
            }
        });

        alert.setNegativeButton(getString(android.R.string.no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        App.stopAndruavWS(true);
                        finish();
                    }
                });

        alert.show();

       // super.onBackPressed();
    }

    private void testConnection ()
    {
        NetInfoAdapter.Update();
        if ((NetInfoAdapter.isWifiInternetEnabled()==false) && (NetInfoAdapter.isMobileNetworkConnected()==false))
        {
            DialogHelper.doModalDialog(this,getString(R.string.gen_connection),getString(R.string.err_no_internet),null);
            return ;
        }

        if (AndruavEngine.isAndruavWSStatus(SOCKETSTATE_REGISTERED)) {
                return; // I am already connected.
        }

        if (PreferenceValidator.isInvalidLoginCode())
        {
            LoginScreenFactory.startLoginActivity(Me);
            return ;
        }

            try
            {
                LoginClient.ValidateAccount(Preference.getLoginUserName(null), Preference.getLoginAccessCode(null),Preference.getWebServerGroupName(null), null);
            }
            catch (UnsupportedEncodingException e )
            {
                AndruavEngine.log().logException("exception_log", e);
                DialogHelper.doModalDialog(Me,getString(R.string.action_login),getString(R.string.err_loginfailed),null);
            }

    }

    private void startAndruavConnection () {
        App.startAndruavWS();
    }

    private boolean savePreference ()
    {
        // Verify
        if (mtxtWebServerIP.getText().length() == 0) {
            DialogHelper.doModalDialog(this,getString(R.string.websocket_IP),getString(R.string.err_nullValue),null);
            return false;
        }
        if (mtxtWebServerPort.getText().length() == 0) {
            DialogHelper.doModalDialog(this,getString(R.string.websocket_Port),getString(R.string.err_nullValue),null);
            return false;
        }
        // Verify
        if (mtxtWebUserName.getText().length() == 0) {
            DialogHelper.doModalDialog(this,getString(R.string.websocket_UserName),getString(R.string.err_nullValue),null);
            return false;
        }

        if (AndruavSettings.isValidAndruavUnitName(String.valueOf(mtxtWebUserName.getText()))) {
            DialogHelper.doModalDialog(this,getString(R.string.websocket_UserName),getString(R.string.err_invalid_unitname),null);
            return false;
        }

      /*  if (mtxtWebGroupName.getText().length() == 0) {
            DialogHelper.doModalDialog(this,getString(R.string.websocket_GroupName),getString(R.string.err_nullValue),null);
            return false;
        }
        */
        if (mtxtWebDescription.getText().length() == 0) {
            DialogHelper.doModalDialog(this,getString(R.string.websocket_Description),getString(R.string.err_nullValue),null);
            return false;
        }
        if ((mchkEncryptWebServer.isChecked()) && mtxtEncryptionKey.getText().length() != 16)
        {
            DialogHelper.doModalDialog(this,getString(R.string.websocket_Encryptiom),getString(R.string.err_ws_encrypted_16),null);
            return false;
        }
        // Do Save
        Preference.setAuthServerPort(null, Integer.parseInt(mtxtWebServerPort.getText().toString()));
        String serverURL = mtxtWebServerIP.getText().toString();
        Preference.setAuthServerURL(null, serverURL);
        Preference.setWebServerUserName(null, mtxtWebUserName.getText().toString().toLowerCase());
        if (!Preference.enableGroupName(null))
        {
            Preference.setWebServerGroupName(null, App.getAppContext().getString(R.string.pref_groupname).toLowerCase());
        }
        else {
           // Preference.setWebServerGroupName(null, mtxtWebGroupName.getText().toString().toLowerCase());
            Preference.setWebServerGroupName(null, mspinnerGroupNo.getSelectedItem().toString());

        }


        Preference.setWebServerUserDescription(null, mtxtWebDescription.getText().toString());
        Preference.isLocalServer(null, !mcheckAndruavServer.isChecked());
        //Preference.isEnforceName(null,mchkEnforceName.isChecked());
        Preference.isEnforceName(null,true); // ActivityMosa3ed Fix
        Preference.isEncryptedWS(null, mchkEncryptWebServer.isChecked());
        Preference.setEncryptedWSKey(null, mtxtEncryptionKey.getText().toString());

        App.updateWe7daInfo();

        AndruavSettings.andruavWe7daBase.setVehicleType(Preference.getVehicleType(null));



        AndruavEngine.notification().Speak(getString(R.string.action_saved));

        return true;
    }

    private void readPreference ()
    {
        mtxtWebServerPort.setText(String.valueOf(Preference.getAuthServerPort(null)));
        mtxtWebServerIP.setText(String.valueOf(Preference.getAuthServerURL(null)));
        mtxtWebUserName.setText(String.valueOf(Preference.getWebServerUserName(null)));
        if (!Preference.enableGroupName(null))
        { // reset value
            Preference.setWebServerGroupName(null, App.getAppContext().getString(R.string.pref_groupname).toLowerCase());
        }
        //mtxtWebGroupName.setText(String.valueOf(Preference.getWebServerGroupName(null)));


        mspinnerGroupNo.setSelection(Integer.parseInt(Preference.getWebServerGroupName(null))-1);

        mtxtWebDescription.setText(String.valueOf(Preference.getWebServerUserDescription(null)));
        mcheckAndruavServer.setChecked(!Preference.isLocalServer(null));
        mchkEncryptWebServer.setChecked(Preference.isEncryptedWS(null));
        mtxtEncryptionKey.setText(String.valueOf(Preference.getEncryptedWSKey(null)));

    }



}
