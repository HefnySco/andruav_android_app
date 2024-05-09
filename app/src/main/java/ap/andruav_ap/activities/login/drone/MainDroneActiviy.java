package ap.andruav_ap.activities.login.drone;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
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

import java.io.UnsupportedEncodingException;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;
import ap.andruav_ap.R;
import ap.andruav_ap.activities.baseview.BaseAndruavShasha;
import com.andruav.event.networkEvent.EventLoginClient;
import ap.andruavmiddlelibrary.LoginClient;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.preference.Preference;

import static ap.andruavmiddlelibrary.LoginClient.CONST_ACCOUNT_NAME_PARAMETER;
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
        protected EditText edtAccessCode;

        protected TextView txtSubscribe;
        private ProgressDialog mprogressDialog;
        private int merrorCode;
        private String email;


        //////////BUS EVENT

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
                    if (event_LoginClient.Cmd == LoginClient.CMD_RetrieveAccountName) {
                        exitProgressDialog();

                        if (event_LoginClient.LastError == 0) {
                            email = event_LoginClient.Parameters.get(CONST_ACCOUNT_NAME_PARAMETER);

                            AndruavEngine.notification().Speak(getString(R.string.login_action_joined));
                            savePreference();
                            DialogHelper.doModalDialog(Me, getString(R.string.login_login), getString(R.string.login_action_joined), null,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            App.restartApp(1000, false);
                                        }
                                    });
                            App.stopAndruavWS(true); // destroy WS
                            App.defineAndruavUnit(false);
                            LoginClient.LinkPartyID2AccessCode(AndruavSettings.andruavWe7daBase.PartyID, AndruavSettings.AccountName);


                        } else if (event_LoginClient.LastError == LoginClient.ERR_SERVER_UNREACHABLE) {
                            DialogHelper.doModalDialog(Me, getString(R.string.login_login), getString(R.string.login_action_unreachable), null);
                            AndruavEngine.notification().Speak(getString(R.string.login_action_unreachable));
                            email = "";
                        } else {
                            DialogHelper.doModalDialog(Me, getString(R.string.login_login), event_LoginClient.Parameters.get(CONST_ERROR_MSG), null);
                            AndruavEngine.notification().Speak(getString(R.string.login_action_badaccesscode));
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
            //medtEmail.setText(Preference.getLoginUserName(null));
            edtAccessCode.setText(Preference.getLoginAccessCode(null));

        }

        private void initGUI() {

            btnJoin         = findViewById(R.id.droneloginactivity_btnSaveAccessCode);
            txtSubscribe    = findViewById(R.id.droneloginactivity_txtSubscribe);
            edtAccessCode   = findViewById(R.id.droneloginactivity_edtAccessCode);
            edtAccessCode.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    btnJoin.setEnabled((edtAccessCode.getText().length() > 0));
                }
            });

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
         * Retreives Account Name -email- of an Access Key and saves both
         */
        private void doSaveAccessCode()
        {
            if (edtAccessCode.getText().length() == 0)
            {
                DialogHelper.doModalDialog(this, getString(R.string.login_access_code), getString(R.string.err_nullValue), null);
            }
            else {
                doProgressDialog();
                try
                {
                    AndruavSettings.AuthIp = Preference.getAuthServerURL(null);
                    AndruavSettings.AuthPort =Preference.getAuthServerPort(null);


                    LoginClient.RetrieveAccountName(edtAccessCode.getText().toString());
                }
                catch (UnsupportedEncodingException e )
                {
                    AndruavEngine.log().logException("exception_log", e);
                    DialogHelper.doModalDialog(Me,getString(R.string.action_login),getString(R.string.err_loginfailed),null);
                }
            }

        }

        private void doProgressDialog()
        {
            mprogressDialog = new ProgressDialog(DroneLoginShasha.this);
            mprogressDialog.setMessage(getString(R.string.action_init));
            mprogressDialog.setTitle(getString(R.string.action_connect));
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
                DialogHelper.doModalDialog(this,getString(R.string.action_login),getString(R.string.err_accountNotFound),null);
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
                GMail.sendGMail(this, getString(R.string.email_title), getString(R.string.email_to), getString(R.string.email_subject), getString(R.string.email_body), null);
            }

            return super.onOptionsItemSelected(item);
        }
    }
}
