package ap.andruav_ap.activities.login;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;

import java.io.UnsupportedEncodingException;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.activities.baseview.BaseAndruavShasha;
import ap.andruav_ap.App;
import ap.andruavmiddlelibrary.LoginClient;
import com.andruav.event.networkEvent.EventLoginClient;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;

import static ap.andruavmiddlelibrary.LoginClient.CONST_ACCESS_CODE_PARAMETER;
import static ap.andruavmiddlelibrary.LoginClient.CONST_ACCOUNT_NAME_PARAMETER;
import static ap.andruavmiddlelibrary.LoginClient.CONST_ERROR;


public class GCSLoginShasha extends BaseAndruavShasha {

    //////  Attributes
    protected GCSLoginShasha Me;
    protected EditText medtEmail;
    protected EditText medtAccessCode;
    protected CheckBox mchkAutoLogin;
    protected Button mbtnSaveLogin;
    protected Button   mbtnRegister;
    protected Button   mbtnRegenerate;
    private Handler mhandle;
    private ProgressDialog mprogressDialog;
    private int merrorCode;

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
                switch (event_LoginClient.Cmd)
                {
                    case LoginClient.CMD_RegisterAccount:
                       if (event_LoginClient.LastError==LoginClient.ERR_SUCCESS)
                        {
                            String accessCode = LoginClient.Parameters.get(CONST_ACCESS_CODE_PARAMETER);
                            medtAccessCode.setText(accessCode);
                            Preference.setLoginAccessCode(null, accessCode);

                            DialogHelper.doModalDialog(Me, getString(R.string.login_register_Title), "Access code is:" + accessCode + "\r\n" + getString(R.string.login_action_chkemail), null,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                         App.restartApp(1000,false);

                                        }
                                    });

                            AndruavEngine.notification().Speak(getString(R.string.login_action_registered));
                            AndruavEngine.notification().Speak(getString(R.string.login_action_chkemail));
                            savePreference();
                            App.stopAndruavWS(true); // destroy WS
                            App.defineAndruavUnit(true);
                            LoginClient.LinkPartyID2AccessCode(AndruavSettings.andruavWe7daBase.PartyID,AndruavSettings.AccountName);
                            AndruavEngine.log().log(medtAccessCode.getText().toString(),"Account Registered" , medtEmail.getText().toString() );
                            exitProgressDialog(); //BUG: http://localhost:8080/mantis/view.php?id=45
                        }
                        else if (event_LoginClient.LastError==LoginClient.ERR_DUPLICATE_ENTRY) {
                                // account exists and password will be resent via email
                                // clear to allow user to get it from email
                                medtAccessCode.setText("");
                                Preference.setLoginAccessCode(null,LoginClient.LastMessage);
                                // save with null in access code.
                                savePreference();
                                // resend the password pls
                                try
                                {

                                    DialogHelper.doModalDialog(Me,getString(R.string.login_register_Title),getString(R.string.login_action_accontexist),null,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    try {
                                                        LoginClient.RetrieveAccessCode(event_LoginClient.AccountName);
                                                    } catch (UnsupportedEncodingException e) {
                                                        AndruavEngine.log().logException("exception_log", e);
                                                        DialogHelper.doModalDialog(Me,getString(R.string.action_login),getString(R.string.err_loginfailed),null);
                                                    }
                                                }
                                            });


                                    // dont call exitProgressDialog(); //BUG: http://localhost:8080/mantis/view.php?id=45
                                }
                                catch (Exception e )
                                {
                                    AndruavEngine.log().logException("exception_log", e);
                                    DialogHelper.doModalDialog(Me,getString(R.string.action_login),getString(R.string.err_loginfailed),null);
                                }
                       }
                       else {
                           exitProgressDialog(); //BUG: http://localhost:8080/mantis/view.php?id=45

                       }


                        break;

                    case LoginClient.CMD_UpdateAccount:

                        if (event_LoginClient.LastError==0)
                        {
                            // clear to allow user to get it from email
                            medtAccessCode.setText("");
                            AndruavEngine.notification().Speak(getString(R.string.login_action_chkemail));
                            savePreference();
                        }

                        exitProgressDialog();
                        DialogHelper.doModalDialog(Me,getString(R.string.action_regenerate),event_LoginClient.LastMessage,null);

                        break;

                    case LoginClient.CMD_RetrieveAccessCode:
                        exitProgressDialog();

                        if (event_LoginClient.LastError==0) {
                            medtAccessCode.setEnabled(true);

                            medtAccessCode.setText("");  // clear to allow user to get it from email
                            DialogHelper.doModalDialog(Me, getString(R.string.action_save), getString(R.string.login_action_chkemail), null);
                            AndruavEngine.notification().Speak(getString(R.string.login_action_chkemail));
                            savePreference();
                        }
                        else if (event_LoginClient.LastError ==LoginClient.ERR_SERVER_UNREACHABLE)
                        {
                            DialogHelper.doModalDialog(Me, getString(R.string.login_login), getString(R.string.login_action_unreachable), null);
                            AndruavEngine.notification().Speak(getString(R.string.login_action_unreachable));

                        }
                        else {
                            DialogHelper.doModalDialog(Me, getString(R.string.login_register_Title), event_LoginClient.LastMessage, null);
                        }
                        break;

                    case LoginClient.CMD_RetrieveAccountName:
                        exitProgressDialog();
                        if (event_LoginClient.LastError ==LoginClient.ERR_SERVER_UNREACHABLE)
                        {
                            DialogHelper.doModalDialog(Me, getString(R.string.login_login), getString(R.string.login_action_unreachable), null);
                            AndruavEngine.notification().Speak(getString(R.string.login_action_unreachable));
                            medtEmail.setText("");
                        }
                        else if ((event_LoginClient.Parameters.indexOfKey(CONST_ERROR) !=-1) && ( Integer.parseInt(event_LoginClient.Parameters.get(CONST_ERROR))==0)) {
                            medtEmail.setText(event_LoginClient.Parameters.get(CONST_ACCOUNT_NAME_PARAMETER));
                            DialogHelper.doModalDialog(Me,getString(R.string.action_save),getString(R.string.login_action_joined),null,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            App.restartApp(1000,false);
                                        }
                                    });
                            AndruavEngine.notification().Speak(getString(R.string.login_action_joined));
                            savePreference();
                            App.stopAndruavWS(true); // destroy WS
                            App.defineAndruavUnit(true);
                            LoginClient.LinkPartyID2AccessCode(AndruavSettings.andruavWe7daBase.PartyID,AndruavSettings.AccountName);

                        }
                        else
                        {
                            DialogHelper.doModalDialog(Me,getString(R.string.action_save),event_LoginClient.LastMessage,null);
                            AndruavEngine.notification().Speak(getString(R.string.login_action_badaccesscode));
                            medtEmail.setText("");

                        }



                        break;

                }
            }
        };

    }

    private void doRegister()
    {
        NetInfoAdapter.Update();
        if ((NetInfoAdapter.isWifiInternetEnabled()==false) && (NetInfoAdapter.isMobileNetworkConnected()==false))
        {
            DialogHelper.doModalDialog(this,getString(R.string.gen_connection),getString(R.string.err_no_internet),null);
            return ;
        }

        if (medtEmail.getText().length() == 0)
        {
            DialogHelper.doModalDialog(this,getString(R.string.login_register_Title),getString(R.string.err_nullValue),null);
            return ;
        }
        else
        {
            doProgressDialog();
            try
            {
                LoginClient.RegisterAccount(medtEmail.getText().toString());
            }
            catch (UnsupportedEncodingException e )
            {
                AndruavEngine.log().logException("exception_log", e);
                DialogHelper.doModalDialog(Me,getString(R.string.action_login),getString(R.string.err_loginfailed),null);
            }
        }
    }


    /***
     * Retreives Account Name -email- of an Access Key and saves both
     */
    private void doSaveAccessCode()
    {
        if (medtAccessCode.getText().length() == 0)
        {
            DialogHelper.doModalDialog(this,getString(R.string.login_access_code),getString(R.string.err_nullValue),null);
            return ;
        }
        else {
            doProgressDialog();
            try
            {
                LoginClient.RetrieveAccountName(medtAccessCode.getText().toString());
            }
            catch (UnsupportedEncodingException e )
            {
                AndruavEngine.log().logException("exception_log", e);
                DialogHelper.doModalDialog(Me,getString(R.string.action_login),getString(R.string.err_loginfailed),null);
            }
        }

    }

    private void doRegenerateAccessCode()
    {
        if ((NetInfoAdapter.isWifiInternetEnabled()==false) && (NetInfoAdapter.isMobileNetworkConnected()==false))
        {
            DialogHelper.doModalDialog(this,getString(R.string.gen_connection),getString(R.string.err_no_internet),null);
            return ;
        }

        if (medtEmail.getText().length() == 0)
        {
            DialogHelper.doModalDialog(this,getString(R.string.login_register_Title),getString(R.string.err_nullValue),null);
            return ;
        }
        else
        {
            doProgressDialog();
            try
            {
                LoginClient.UpdateAccount(medtEmail.getText().toString());
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
        mprogressDialog = new ProgressDialog(GCSLoginShasha.this);
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


    private void initGUI()
    {
        medtEmail = findViewById(R.id.login_edtEmail);
        medtEmail.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                if (medtEmail.getText().length()==0)
                {
                    mbtnRegenerate.setEnabled(false);
                    mbtnRegister.setEnabled(false);
                }
                else
                {
                    mbtnRegenerate.setEnabled(true);
                    mbtnRegister.setEnabled(true);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });
        medtAccessCode = findViewById(R.id.login_edtAccessCode);
        medtAccessCode.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                mbtnSaveLogin.setEnabled(medtAccessCode.getText().length() != 0);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });


        mchkAutoLogin = findViewById(R.id.login_chkAutoStart);
        mbtnRegister = findViewById(R.id.login_btnRegister);
        mbtnSaveLogin = findViewById(R.id.login_btnSaveAccessCode);
        mbtnRegenerate= findViewById(R.id.login_btnRegenerateAccessKey);

        mbtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                doRegister();
            }
        });


        mbtnSaveLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSaveAccessCode();
            }
        });


        mbtnRegenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(Me);
                builder.setMessage(getString(R.string.gen_regenerate))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                doRegenerateAccessCode();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }


        });
        UIHandler();
    }

    /***
     * enable and disable controls based on selectted action from dialog
     * 0: registration
     * 1: join using known access code
     * 2: regenerate access code
     * @param action
     */
    public void updateActionStep (int action)
    {
        switch (action)
        {
            case 0: //registration
                mbtnRegister.setEnabled(true);
                mbtnSaveLogin.setEnabled(false);
                mbtnRegenerate.setEnabled(false);
                medtEmail.setEnabled(true);
                medtAccessCode.setEnabled(false);
                medtAccessCode.setText("");
                break;
            case 1: //join using known access code
                mbtnRegister.setEnabled(false);
                mbtnSaveLogin.setEnabled(true);
                mbtnRegenerate.setEnabled(false);
                medtEmail.setEnabled(false);
                medtAccessCode.setEnabled(true);
                medtEmail.setText("");
                break;
            case 2: //regenerate access code
                mbtnRegister.setEnabled(true);
                mbtnSaveLogin.setEnabled(false);
                mbtnRegenerate.setEnabled(true);
                medtEmail.setEnabled(true);
                medtAccessCode.setEnabled(false);
                break;



        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Me = this;
        App.ForceLanguage();

        merrorCode = getIntent().getByteExtra("MSG", (byte)LoginClient.ERR_SUCCESS);

        setContentView(R.layout.activity_gcslogin);
        initGUI();
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


    @Override
    protected void onStop() {
        super.onStop();
        // The activity is no longer visible (it is now "stopped")
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
        //EventBus.getDefault().register(this, "onLoginEvent", EventLoginClient.class);
        final CharSequence[] items = { getString(R.string.login_first_section),getString(R.string.login_second_section), getString(R.string.login_third_section) };

        final  AlertDialog.Builder builderRequiredAction = new AlertDialog.Builder(Me);
        builderRequiredAction.setTitle("Registration Actions");
        builderRequiredAction.setSingleChoiceItems(items, -1
                , new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                updateActionStep(item);
                dialog.dismiss();
            }
        });
        builderRequiredAction.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Me.finish();
            }
        });
        builderRequiredAction.create().show();

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
    public void onBackPressed()
    {
        // Dont save here
        // as user may change access code or email with no validation
        // user can save ONLY valid values.
       /* AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.action_savebeforeexit));


        alert.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (savePreference()==false)
                    return ; // dont exit with values not saved.
                Toast.makeText(getApplicationContext(), getString(R.string.action_done), Toast.LENGTH_SHORT).show();
                App.stopAndruavWS();
                finish();
            }
        });

        alert.setNegativeButton(getString(android.R.string.no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                  App.stopAndruavWS();
                  finish();
                    }
                });

        alert.show();
        */
      /* DialogHelper.doModalDialog(this,getString(R.string.action_sysrestart),null,null,new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int whichButton) {
               App.restartApp(1000);
           }
       });
        */


        super.onBackPressed();
    }

    private boolean savePreference ()
    {
        // Verify
        if (medtEmail.getText().length() == 0) {
            DialogHelper.doModalDialog(this, getString(R.string.login_register_Title), getString(R.string.err_nullValue), null);
            return false;
        }

        // Do Save
        Preference.isLoginAuto(null,mchkAutoLogin.isChecked());
        Preference.setLoginUserName(null,medtEmail.getText().toString());
        Preference.setLoginAccessCode(null, medtAccessCode.getText().toString());

        AndruavSettings.AccountName = Preference.getLoginUserName(null);
        AndruavSettings.AccessCode = Preference.getLoginAccessCode(null); // update
        //AndruavMo7arek.notification().Speak(getString(R.string.action_saved));

        return true;
    }

    private void readPreference ()
    {
        // Do Save
        mchkAutoLogin.setChecked(Preference.isLoginAuto(null));
        medtEmail.setText(Preference.getLoginUserName(null));
        medtAccessCode.setText(Preference.getLoginAccessCode(null));

    }
}
