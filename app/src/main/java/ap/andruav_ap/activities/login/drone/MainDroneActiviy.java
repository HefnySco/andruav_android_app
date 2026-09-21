package ap.andruav_ap.activities.login.drone;

import org.greenrobot.eventbus.Subscribe;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.UnsupportedEncodingException;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;
import ap.andruav_ap.App;
import ap.andruav_ap.R;
import ap.andruav_ap.activities.baseview.BaseAndruavShasha;
import com.andruav.event.networkEvent.EventLoginClient;
import ap.andruavmiddlelibrary.LoginClient;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.preference.Preference;

import static ap.andruavmiddlelibrary.LoginClient.CONST_ERROR_MSG;

public class MainDroneActiviy extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drone_activiy);
    }

    public static class DroneLoginShasha extends BaseAndruavShasha {



        //////  Attributes
        protected DroneLoginShasha Me;
        private Handler mhandle;
        protected Button btnJoin;
        protected Button btnScanQR;
        protected EditText edtEmail;
        protected EditText edtAccessCode;
        private ActivityResultLauncher<ScanOptions> mbarcodeLauncher;
        private ActivityResultLauncher<String> mcameraPermissionLauncher;

        protected TextView txtSubscribe;
        private ProgressDialog mprogressDialog;
        private int merrorCode;
        private String email;


        //////////BUS EVENT

        @Subscribe
        public void onEvent (final EventLoginClient event_LoginClient) {

            Message msg = new Message();
            msg.obj = event_LoginClient;
            mhandle.sendMessageDelayed(msg,0);
        }


        ///////////////////

        private void UIHandler () {

            mhandle = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    final EventLoginClient event_LoginClient = (EventLoginClient)msg.obj;
                    if (event_LoginClient.Cmd == LoginClient.CMD_ValidateAccount) {
                        exitProgressDialog();

                        if (event_LoginClient.LastError == 0) {
                            AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.login_action_joined));
                            savePreference();
                            DialogHelper.doModalDialog(Me, getString(ap.andruavmiddlelibrary.R.string.login_login), getString(ap.andruavmiddlelibrary.R.string.login_action_joined), null,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            App.restartApp(1000, false);
                                        }
                                    });
                            App.stopAndruavWS(true); // destroy WS
                            App.defineAndruavUnit();
                        } else if (event_LoginClient.LastError == LoginClient.ERR_SERVER_UNREACHABLE) {
                            DialogHelper.doModalDialog(Me, getString(ap.andruavmiddlelibrary.R.string.login_login), getString(ap.andruavmiddlelibrary.R.string.login_action_unreachable), null);
                            AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.login_action_unreachable));
                            email = "";
                        } else {
                            DialogHelper.doModalDialog(Me, getString(ap.andruavmiddlelibrary.R.string.login_login), event_LoginClient.Parameters.get(CONST_ERROR_MSG), null);
                            AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.login_action_badaccesscode));
                            email = "";

                        }
                    }
                }
            };

        }


        private boolean savePreference ()
        {
            //Preference.isLoginAuto(null,mchkAutoLogin.isChecked());
            Preference.setLoginUserName(null,email);
            Preference.setLoginAccessCode(null, edtAccessCode.getText().toString());

            AndruavSettings.AccountName = Preference.getLoginUserName(null);
            AndruavSettings.AccessCode = Preference.getLoginAccessCode(null); // update
            //TTS.getInstance().Speak(getString(R.string.action_saved));

            return true;
        }

        private void readPreference ()
        {
            // Do Save
            //mchkAutoLogin.setChecked(Preference.isLoginAuto(null));
            edtEmail.setText(Preference.getLoginUserName(null));
            edtAccessCode.setText(Preference.getLoginAccessCode(null));

        }

        private void initGUI() {

            btnJoin         = findViewById(R.id.droneloginactivity_btnSaveAccessCode);
            btnScanQR       = findViewById(R.id.droneloginactivity_btnScanQR);
            txtSubscribe    = findViewById(R.id.droneloginactivity_txtSubscribe);
            edtEmail        = findViewById(R.id.droneloginactivity_edtEmail);
            edtAccessCode   = findViewById(R.id.droneloginactivity_edtAccessCode);

            mbarcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
                if (result == null || result.getContents() == null) return;
                applyQrLoginPayload(result.getContents());
            });
            mcameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    launchQrScanner();
                } else {
                    DialogHelper.doModalDialog(Me, getString(ap.andruavmiddlelibrary.R.string.login_scan_qr), getString(ap.andruavmiddlelibrary.R.string.login_qr_camera_permission), null);
                }
            });
            btnScanQR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ContextCompat.checkSelfPermission(DroneLoginShasha.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        launchQrScanner();
                    } else {
                        mcameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                    }
                }
            });
            TextWatcher enableWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    btnJoin.setEnabled((edtEmail.getText().length() > 0) && (edtAccessCode.getText().length() > 0));
                }
            };
            edtEmail.addTextChangedListener(enableWatcher);
            edtAccessCode.addTextChangedListener(enableWatcher);

            txtSubscribe.setOnClickListener(new View.OnClickListener(){
                                                public void onClick(View v){
                                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://cloud.ardupilot.org:8001/accounts.html"));
                                                    startActivity(browserIntent);
                                                }});

            btnJoin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doSaveAccessCode();
                }
            });

            UIHandler();


        }


        /***
         * Login using email (Account Name) & Access Code via /agent/al,
         * mirroring the C++ DE client doAuthentication().
         */
        private void doSaveAccessCode()
        {
            if (edtEmail.getText().length() == 0)
            {
                DialogHelper.doModalDialog(this, getString(ap.andruavmiddlelibrary.R.string.login_email), getString(ap.andruavmiddlelibrary.R.string.err_nullValue), null);
            }
            else if (edtAccessCode.getText().length() == 0)
            {
                DialogHelper.doModalDialog(this, getString(ap.andruavmiddlelibrary.R.string.login_access_code), getString(ap.andruavmiddlelibrary.R.string.err_nullValue), null);
            }
            else {
                doProgressDialog();
                try
                {
                    AndruavSettings.AuthIp = Preference.getAuthServerURL(null);
                    AndruavSettings.AuthPort =Preference.getAuthServerPort(null);

                    email = edtEmail.getText().toString().trim();
                    LoginClient.ValidateAccount(email, edtAccessCode.getText().toString(), Preference.getWebServerGroupName(null), null);
                }
                catch (UnsupportedEncodingException e )
                {
                    AndruavEngine.log().logException("exception_log", e);
                    DialogHelper.doModalDialog(Me,getString(ap.andruavmiddlelibrary.R.string.action_login),getString(ap.andruavmiddlelibrary.R.string.err_loginfailed),null);
                }
            }

        }

        private void launchQrScanner() {
            final ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt(getString(ap.andruavmiddlelibrary.R.string.login_scan_qr));
            options.setBeepEnabled(false);
            options.setOrientationLocked(true);
            mbarcodeLauncher.launch(options);
        }

        /***
         * Parses a "de_login" QR payload produced by the webclient team-admin
         * page or the auth-server admin UI, fills the login form and signs in.
         * Payload: {"v":1,"t":"de_login","acc":..,"pwd":..,"ah":..,"ap":..,"gr":..}
         * ah/ap/gr are optional; ah/ap are also stored as the custom (local)
         * server values so the HUB sheet reflects the scanned server.
         */
        private void applyQrLoginPayload(final String contents) {
            try {
                final JSONObject json = new JSONObject(contents);
                if (!"de_login".equals(json.optString("t"))) {
                    DialogHelper.doModalDialog(Me, getString(ap.andruavmiddlelibrary.R.string.login_scan_qr), getString(ap.andruavmiddlelibrary.R.string.login_qr_invalid), null);
                    return;
                }
                final String acc = json.optString("acc", "").trim();
                final String pwd = json.optString("pwd", "").trim();
                if (acc.isEmpty() || pwd.isEmpty()) {
                    DialogHelper.doModalDialog(Me, getString(ap.andruavmiddlelibrary.R.string.login_scan_qr), getString(ap.andruavmiddlelibrary.R.string.login_qr_invalid), null);
                    return;
                }

                edtEmail.setText(acc);
                edtAccessCode.setText(pwd);

                final String authHost = json.optString("ah", "").trim();
                if (!authHost.isEmpty()) {
                    Preference.setAuthServerURL(null, authHost);
                    Preference.setLocalServerURL(null, authHost);
                    final String cloudHost = getString(ap.andruavmiddlelibrary.R.string.pref_auth_URL);
                    Preference.isLocalServer(null, !cloudHost.equalsIgnoreCase(authHost));
                }
                final int authPort = json.optInt("ap", 0);
                if (authPort > 0) {
                    Preference.setAuthServerPort(null, authPort);
                    Preference.setLocalServerPort(null, authPort);
                }
                final String group = json.optString("gr", "").trim();
                if (!group.isEmpty()) {
                    Preference.setWebServerGroupName(null, group.toLowerCase());
                }

                doSaveAccessCode();
            } catch (Exception e) {
                DialogHelper.doModalDialog(Me, getString(ap.andruavmiddlelibrary.R.string.login_scan_qr), getString(ap.andruavmiddlelibrary.R.string.login_qr_invalid), null);
            }
        }

        private void doProgressDialog()
        {
            mprogressDialog = new ProgressDialog(DroneLoginShasha.this);
            mprogressDialog.setMessage(getString(ap.andruavmiddlelibrary.R.string.action_init));
            mprogressDialog.setTitle(getString(ap.andruavmiddlelibrary.R.string.action_connect));
            mprogressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mprogressDialog.show();
        }

        private void exitProgressDialog()
        {
            //Update UI here if needed
            if (mprogressDialog != null) {
                // Just protection here
                mprogressDialog.dismiss();
            }
        }



            @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Me = this;
            App.ForceLanguage();


            setContentView(R.layout.activity_drone_login);




            initGUI();
        }


        @Override
        protected void onResume() {
            super.onResume();
            NetInfoAdapter.Update();

            // The activity has become visible (it is now "resumed").
            if (merrorCode== LoginClient.ERR_ACCOUNT_NOT_FOUND)
            {
                DialogHelper.doModalDialog(this,getString(ap.andruavmiddlelibrary.R.string.action_login),getString(ap.andruavmiddlelibrary.R.string.err_accountNotFound),null);
            }
            merrorCode = LoginClient.ERR_SUCCESS;
            readPreference();
            EventBus.getDefault().register(this);


            // for Aman function
            App.startSensorService();

        }

        @Override
        protected void onPause() {
            // Another activity is taking focus (this activity is about to be "paused").
            super.onPause();
            EventBus.getDefault().unregister(this);
        }



            @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_login, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();

            if (id == R.id.mi_login_Help) {
                GMail.sendGMail(this, getString(ap.andruavmiddlelibrary.R.string.email_title), getString(ap.andruavmiddlelibrary.R.string.email_to), getString(ap.andruavmiddlelibrary.R.string.email_subject), getString(ap.andruavmiddlelibrary.R.string.email_body), null);
            }

            return super.onOptionsItemSelected(item);
        }
    }
}
