package ap.andruavmiddlelibrary;


import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.util.Pair;
import androidx.collection.SimpleArrayMap;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.FeatureSwitch;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.greenrobot.event.EventBus;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.andruav.event.networkEvent.EventLoginClient;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Created by M.Hefny on 11-Oct-14.
 * Changed by M.Hefny on 21-Mar-20.
 */
public class LoginClient {

    private final static OkHttpClient mclientHTTP = new OkHttpClient();

    private  final static String  pageNameDebug = "https://192.168.1.144:19408";  //"https://192.168.1.139:19408"; //"https://192.168.2.42:19108/?";
    private  final static String  pageNameRelease = "https://cloud.ardupilot.org:19408";
    private final static String  WEBMOFTA7Local = "KEY=000000000000-0000-0000-0000-000000000000";

    public final static int ERR_SUCCESS                 = 0;
    public final static int ERR_BAD_EMAIL               = 1;
    public final static int ERR_ACCOUNT_NOT_FOUND       = 2;
    public final static int ERR_DUPLICATE_ENTRY         = 3;
    public final static int ERR_OLD_APP_VERSION         = 4;
    public final static int ERR_NO_AVAILABLE_SERVERS    = 5;
    public final static int ERR_NO_PERMISSIONS          = 6;
    public final static int ERR_UNKNOWN                 = 999;
    public final static int ERR_SUCCESS_DISPLAY_MESSAGE = 1000;
    public final static int ERR_SERVER_UNREACHABLE      = 1001;

    public final static int CMD_RegisterAccount =1;
    public final static int CMD_UpdateAccount =2;
    public final static int CMD_RetrieveAccessCode =3;
    public final static int CMD_RetrieveAccountName =4;
    public final static int CMD_ValidateAccount =5;
    public final static int CMD_LinkPID2Account =6;


    public final static String CONST_CMD_CREATE_ACCESSCODE   		= "c";
    public final static String CONST_CMD_REGENERATE_ACCESSCODE      = "r";
    public final static String CONST_CMD_GET_ACCOUNT_NAME           = "g";


    public final static String CONST_AGENT_FUNCTION                 = "/agent";
    public final static String CONST_AGENT_LOGIN_COMMAND            = "/al";
    public final static String CONST_AGENT_ACCOUNT_MANAGMENT        = "/aam";
    public final static String CONST_ACCOUNT_NAME_PARAMETER         = "acc";
    public final static String CONST_ACCESS_CODE_PARAMETER          = "pwd";
    public final static String CONST_APP_NAME_PARAMETER             = "app";
    public final static String CONST_GROUP_PARAMETER                = "gr";
    public final static String CONST_APP_VER_PARAMETER              = "ver";
    public final static String CONST_EXTRA_PARAMETER                = "ex";


    public final static String CONST_COMMAND                        = "cm";
    public final static String CONST_SUB_COMMAND                    = "scm";
    public final static String CONST_SENDER_ID                      = "sid";
    public final static String CONST_PERMISSION                     = "per";
    public final static String CONST_COMM_SERVER                    = "cs";
    public final static String CONST_COMM_SERVER_PUBLIC_HOST        = "g";
    public final static String CONST_COMM_SERVER_PORT               = "h";
    public final static String CONST_COMM_SERVER_AUTH_KEY           = "sak";
    public final static String CONST_COMM_SERVER_LOGIN_TEMP_KEY     = "f";
    public final static String CONST_ERROR                          = "e";
    public final static String CONST_ERROR_MSG                      = "em";


    public static int LastError;
    public static String LastMessage;
    public static String AccessCode;
    public static SimpleArrayMap<String,String> Parameters = new SimpleArrayMap<String, String>();


    public static String getPageName ()
    {
        if (FeatureSwitch.DEBUG_MODE) {
            // #1: LOCAL CHANGE
            return "https://" + AndruavSettings.AuthIp  + ":" + AndruavSettings.AuthPort;
        }else
            return "https://" + AndruavSettings.AuthIp  + ":" + AndruavSettings.AuthPort;
    }

    public static String getWSURL ()
    {
        String url;
        int portnum = Integer.parseInt(AndruavSettings.WebServerPort );
        url = AndruavSettings.WebServerURL  + ":" + portnum + "?" +  "f=" + AndruavSettings.WEBMOFTA7 +  "&s=" + AndruavSettings.andruavWe7daBase.PartyID;

        return url;
    }

    /***
    * Retrieves account name -email- using Access code
    * @param AccessCode
    */
    public static void RetrieveAccountName (String AccessCode) throws UnsupportedEncodingException {

        final Pair<String, String>[] urls = new Pair[] {
                new Pair(CONST_SUB_COMMAND, CONST_CMD_GET_ACCOUNT_NAME), //URLEncoder.encode(accessCode,"UTF-8").replaceAll("%40","@")),
                new Pair(CONST_ACCESS_CODE_PARAMETER, AccessCode.trim()), //URLEncoder.encode(accessCode,"UTF-8").replaceAll("%40","@")),
                new Pair(CONST_APP_VER_PARAMETER, URLEncoder.encode(AndruavEngine.getPreference().getVersionName(),"UTF-8")),
                new Pair(CONST_APP_NAME_PARAMETER, "andruav"),
                new Pair(CONST_EXTRA_PARAMETER, "Andruav Mobile"),
        };



        SendRequest(CMD_RetrieveAccountName, CONST_AGENT_FUNCTION + CONST_AGENT_ACCOUNT_MANAGMENT , urls, null);

        return;
    }


    /***
     * validate/Login account [username & password] returns ACCOUNT_SID
     * @param AccountName
     * @param AccessCode
     */
    public static void ValidateAccount (final String AccountName,final String AccessCode,final String Group, final ILoginClientCallback iLoginClientCallback) throws UnsupportedEncodingException {

        final Pair<String, String>[] urls = new Pair[] {
                new Pair(CONST_ACCOUNT_NAME_PARAMETER, AccountName.trim()), //URLEncoder.encode(accessCode,"UTF-8").replaceAll("%40","@")),
                new Pair(CONST_ACCESS_CODE_PARAMETER, AccessCode.trim()), // read from preference not from AndruavSettings.andruavwe7da.
                new Pair(CONST_GROUP_PARAMETER, Group.trim()), //URLEncoder.encode(accessCode,"UTF-8").replaceAll("%40","@")),
                new Pair(CONST_APP_VER_PARAMETER, URLEncoder.encode(AndruavEngine.getPreference().getVersionName(),"UTF-8")),
                new Pair(CONST_APP_NAME_PARAMETER, "andruav"),
                new Pair(CONST_EXTRA_PARAMETER, "Andruav Mobile"),
        };

        SendRequest(CMD_ValidateAccount, CONST_AGENT_FUNCTION + CONST_AGENT_LOGIN_COMMAND , urls, iLoginClientCallback);

    }

    /***
     * Access code is sent to email. The same old access code not a newly generated one.
     * As this can be one of two scenarios:
     *  1- user register in a new mobile instead of writing access code.
     *  2- a 3rd party is trying to hack a current user.
     * The return here is a message that appears on a dialog box.
     * @param AccountName
     */
    public static void RetrieveAccessCode (String AccountName) throws UnsupportedEncodingException {


//SendRequest(CMD_RetrieveAccessCode,AccountName,null,url,null);

    }


    /**
     * Sending Register Account Request
     * Check @link LastError for result
     * @param AccountName
     * @return
     */
    public static void RegisterAccount (String AccountName) throws UnsupportedEncodingException {

        final Pair<String, String>[] urls = new Pair[] {
                new Pair(CONST_SUB_COMMAND, CONST_CMD_CREATE_ACCESSCODE), //URLEncoder.encode(accessCode,"UTF-8").replaceAll("%40","@")),
                new Pair(CONST_ACCOUNT_NAME_PARAMETER, AccountName.trim()), //URLEncoder.encode(accessCode,"UTF-8").replaceAll("%40","@")),
                new Pair(CONST_APP_VER_PARAMETER, URLEncoder.encode(AndruavEngine.getPreference().getVersionName(),"UTF-8")),
                new Pair(CONST_APP_NAME_PARAMETER, "andruav"),
                new Pair(CONST_EXTRA_PARAMETER, "Andruav Mobile"),
        };

        SendRequest(CMD_RegisterAccount, CONST_AGENT_FUNCTION + CONST_AGENT_ACCOUNT_MANAGMENT , urls, null);
    }

    /**
     * Generate new password
     * Check @link LastError for result
     * @param AccountName
     * @return
     */
    public static void UpdateAccount (String AccountName) throws UnsupportedEncodingException {

        final Pair<String, String>[] urls = new Pair[] {
                new Pair(CONST_SUB_COMMAND, CONST_CMD_REGENERATE_ACCESSCODE), //URLEncoder.encode(accessCode,"UTF-8").replaceAll("%40","@")),
                new Pair(CONST_ACCOUNT_NAME_PARAMETER, AccountName.trim()), //URLEncoder.encode(accessCode,"UTF-8").replaceAll("%40","@")),
                new Pair(CONST_APP_VER_PARAMETER, URLEncoder.encode(AndruavEngine.getPreference().getVersionName(),"UTF-8")),
                new Pair(CONST_APP_NAME_PARAMETER, "andruav"),
                new Pair(CONST_EXTRA_PARAMETER, "Andruav Mobile"),
        };

        SendRequest(CMD_UpdateAccount, CONST_AGENT_FUNCTION + CONST_AGENT_ACCOUNT_MANAGMENT , urls, null);
            }


    public static void LinkPartyID2AccessCode (final String PID, final String AccountName)
    {
        try {

            AndruavEngine.log().log(Preference.getLoginUserName(null), "netType", NetInfoAdapter.getInfoJSON());

            String url = "cmd=l&pid=" + URLEncoder.encode(PID, "UTF-8") + "&acc=" + URLEncoder.encode(AccountName, "UTF-8");

            //SendRequest(CMD_LinkPID2Account, AccountName, null, url,null);
        }
        catch (Exception e)
        {

        }
    }

    private static void SendRequest (final int cmd, final String urlRoute, final  Pair<String, String>[] urls, final ILoginClientCallback iLoginClientCallback) throws UnsupportedEncodingException {




        final String url;
        url= getPageName() + urlRoute;
        new AsyncTask<Void, Integer, Void>(){

          @Override
            protected Void doInBackground(Void... params) {
                // TODO Auto-generated method stub
                   OkHttpClient.Builder clientBuilder = mclientHTTP.newBuilder().readTimeout(100, TimeUnit.SECONDS);
               final EventLoginClient eventLoginClient = new EventLoginClient(cmd,urls[0].second,urls[1].second,LoginClient.LastError,LoginClient.LastMessage,Parameters);

              try {
                      boolean allowUntrusted = true;

                      if (allowUntrusted) {
                          final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                              @Override
                              public X509Certificate[] getAcceptedIssuers() {
                                  X509Certificate[] cArrr = new X509Certificate[0];
                                  return cArrr;
                              }

                              @Override
                              public void checkServerTrusted(final X509Certificate[] chain,
                                                             final String authType) {
                                  return ;
                              }

                              @Override
                              public void checkClientTrusted(final X509Certificate[] chain,
                                                             final String authType) {
                                  return ;
                              }
                          }};

                          SSLContext sslContext = SSLContext.getInstance("SSL");

                          sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                          clientBuilder.sslSocketFactory(sslContext.getSocketFactory());

                          HostnameVerifier hostnameVerifier = (hostname, session) -> true;
                          clientBuilder.hostnameVerifier(hostnameVerifier);
                      }



                      FormBody.Builder builder = new FormBody.Builder();

                      for (int i=0;i<urls.length;++i)
                      {
                        builder.add(urls[i].first, urls[i].second);
                      }

                  RequestBody formBody = builder.build();


                  Request request = new Request.Builder()
                              .url(url)
                              .post(formBody)
                              .build();


                  final Call call = clientBuilder.build().newCall(request);
                  Response response = call.execute();


                  ParseReply(response.body().string());

                  eventLoginClient.LastError = LastError;

                  if (iLoginClientCallback ==null) {
                            if (!response.isSuccessful()) {
                                throw new IOException("Unexpected code " + response);
                            }
                            else {
                                EventBus.getDefault().post(eventLoginClient);
                            }
                        }
                        else
                        {
                            // NOTE HERE WE DONT SEND NOTIFICATION ON EVENTBUS IF THERE IS A CALLBACK
                            if (!response.isSuccessful()) {
                                iLoginClientCallback.onError();

                            }
                            else {
                                iLoginClientCallback.onSuccess(eventLoginClient);
                            }
                        }

                    System.out.println(response.body().string());
                } catch (ConnectException error)
                {
                    // this is hen Net is OFF
                    if (iLoginClientCallback !=null) {
                        iLoginClientCallback.onError();

                    }
                    else {

                        eventLoginClient.LastMessage = error.getMessage();
                        eventLoginClient.LastError = LoginClient.ERR_SERVER_UNREACHABLE;
                        EventBus.getDefault().post(eventLoginClient);
                    }
                    return  null;
                } catch (IOException error)
                {
                    AndruavEngine.log().logException(urls[0].second, "exception_log", error);
                    if (iLoginClientCallback !=null) {
                        iLoginClientCallback.onError();

                    }
                    else {
                        eventLoginClient.LastMessage = error.getMessage();
                        eventLoginClient.LastError = LoginClient.ERR_SERVER_UNREACHABLE;
                        EventBus.getDefault().post(eventLoginClient);
                    }
                    return null;
                } catch (IllegalStateException error)
                {
                    // leave it empty
                    if (!error.getMessage().equals("closed")) {

                        if (iLoginClientCallback !=null) {
                            iLoginClientCallback.onError();

                        }
                        else {
                            eventLoginClient.LastMessage = error.getMessage();
                            eventLoginClient.LastError = LoginClient.ERR_SERVER_UNREACHABLE;
                            EventBus.getDefault().post(eventLoginClient);
                        }
                    }

                    return null;
                }
                catch (Exception error) {
                   // ExceptionHTTPLogger.logException(App.Account_SID, "exception", error);
                    if (iLoginClientCallback !=null) {
                        iLoginClientCallback.onError();
                    }
                    else {
                        eventLoginClient.LastMessage = error.getMessage();
                        eventLoginClient.LastError = LoginClient.ERR_SERVER_UNREACHABLE;
                        EventBus.getDefault().post(eventLoginClient);
                    }
                    return null;
                }
                    return null;
            }


        }.execute();

        return ;
    }

    private static void ParseReply (String body)
    {

        try {

             Parameters.clear();
            LastMessage="";
            LastError=ERR_UNKNOWN;

            JSONObject json_receive_data = new JSONObject(body);

            if (json_receive_data.has(CONST_COMMAND)) {
                Parameters.put(CONST_COMMAND,json_receive_data.getString(CONST_COMMAND));

            }



            if (json_receive_data.has(CONST_ACCOUNT_NAME_PARAMETER)) {
                Parameters.put(CONST_ACCOUNT_NAME_PARAMETER,json_receive_data.getString(CONST_ACCOUNT_NAME_PARAMETER));

            }


            if (json_receive_data.has(CONST_ERROR)) {
                LastError = json_receive_data.getInt(CONST_ERROR);
                Parameters.put(CONST_ERROR,String.valueOf(LastError));
            }


            if (json_receive_data.has(CONST_ERROR_MSG)) {
                LastMessage = json_receive_data.getString(CONST_ERROR_MSG);
                Parameters.put(CONST_ERROR_MSG,json_receive_data.getString(CONST_ERROR_MSG));
            }


            if (json_receive_data.has(CONST_SENDER_ID)) {
                Parameters.put(CONST_SENDER_ID,json_receive_data.getString(CONST_SENDER_ID));
            }

            if (json_receive_data.has(CONST_ACCESS_CODE_PARAMETER)) {
                Parameters.put(CONST_ACCESS_CODE_PARAMETER,json_receive_data.getString(CONST_ACCESS_CODE_PARAMETER));
            }

            if (json_receive_data.has(CONST_ACCOUNT_NAME_PARAMETER)) {
                Parameters.put(CONST_ACCOUNT_NAME_PARAMETER,json_receive_data.getString(CONST_ACCOUNT_NAME_PARAMETER));
            }

            if (json_receive_data.has(CONST_PERMISSION)) {
                Parameters.put(CONST_PERMISSION,json_receive_data.getString(CONST_PERMISSION));
            }

            if (json_receive_data.has(CONST_COMM_SERVER)) {

                JSONObject json_server_data = json_receive_data.getJSONObject(CONST_COMM_SERVER);

                if (json_server_data.has(CONST_COMM_SERVER_PUBLIC_HOST)) {
                    Parameters.put(CONST_COMM_SERVER_PUBLIC_HOST, json_server_data.getString(CONST_COMM_SERVER_PUBLIC_HOST));
                }

                if (json_server_data.has(CONST_COMM_SERVER_PORT)) {
                    Parameters.put(CONST_COMM_SERVER_PORT, json_server_data.getString(CONST_COMM_SERVER_PORT));
                }
                if (json_server_data.has(CONST_COMM_SERVER_LOGIN_TEMP_KEY)) { // WebSocket KEY
                    Parameters.put(CONST_COMM_SERVER_LOGIN_TEMP_KEY, json_server_data.getString(CONST_COMM_SERVER_LOGIN_TEMP_KEY));
                }
                else
                {
                    Parameters.put ("ak",WEBMOFTA7Local);
                }
            }
            Log.d("ws",body);

            return ;
        } catch (Exception e) {
            AndruavEngine.log().logException(AndruavSettings.AccessCode, "AUTH_FAILED", e);
            LastError = 999;
            LastMessage = "Site is down ... please try later";
            Log.d("ws","Site is down ... please try later");

            return ;
        }



    }



}
