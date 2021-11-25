package ap.andruavmiddlelibrary.factory.communication;


import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import android.telephony.TelephonyManager;
import android.text.format.Formatter;

import com.andruav.AndruavEngine;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by M.Hefny on 16-Sep-14.
 * @see "https://code.google.com/p/ipcamera-for-android/source/browse/trunk/droidipcam/src/teaonly/projects/droidipcam/NetInfoAdapter.java"
 */


public class NetInfoAdapter {

    //private static Map<String,String> infoMap = new HashMap<String, String>();
    private static final JSONObject infoMap = new JSONObject();
    private static final Map<Integer, String> phoneType = new HashMap<Integer, String>();
    private static final Map<Integer, String> networkType = new HashMap<Integer, String>();

    private static final String SOLO_LINK_WIFI_PREFIX = "SoloLink_";


    private static void putInMap(final String key, final String value) {
        try {
            infoMap.put(key, value);
        } catch (JSONException e) {

        }
    }

    /*
    https://www.3ptechies.com/differences-between-gprs-edge-3g-hsdpa-hspa4g-lte.html

    GPRS (114Kbps)
    < EGDE (368Kbps)
    < 3G(3.1Mbps)
    < HSPA (14Mbps)
    < HSPA+(168Mbps)
    < 4G/LTE/WiMAX( above 500Mbps)
     */
    public static final int TELEPHONE_NOTFOUND = 0;
    public static final int TELEPHONE_200G = 1;
    public static final int TELEPHONE_250G = 2;
    public static final int TELEPHONE_275G = 3;
    public static final int TELEPHONE_300G = 4;
    public static final int TELEPHONE_350G = 5;
    public static final int TELEPHONE_375G = 6;
    public static final int TELEPHONE_390G = 7;
    public static final int TELEPHONE_400G = 8;


    static {

        // Initialise some mappings
        phoneType.put(0, "None");
        phoneType.put(1, "GSM");
        phoneType.put(2, "CDMA");

        networkType.put(0, "Unknown");
        networkType.put(1, "GPRS");
        networkType.put(2, "EDGE");
        networkType.put(3, "UMTS");
        networkType.put(4, "CDMA");
        networkType.put(5, "EVDO_0");
        networkType.put(6, "EVDO_A");
        networkType.put(7, "1xRTT");
        networkType.put(8, "HSDPA");
        networkType.put(9, "HSUPA");
        networkType.put(10, "HSPA");
        networkType.put(11, "IDEN");

        putInMap("Cell", "false");
        putInMap("Mobile", "false");
        putInMap("Wi-Fi", "false");

    }

    public static void Update() {
        // Initialise the network information mapping
        final Context context = AndruavEngine.getPreference().getContext();
        putInMap("Cell", "false");
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            putInMap("Cell", "true");
            // if ( tm.getCellLocation() != null) {
            //    infoMap.put("Cell location", tm.getCellLocation().toString());
            // Require USer Permission in Android M No Need for it right now
            // http://stackoverflow.com/questions/32742327/neither-user-10102-nor-current-process-has-android-permission-read-phone-state
            //}
            putInMap("Country", tm.getNetworkCountryIso());

            putInMap("Cell type", getPhoneType(tm.getPhoneType()));
            putInMap("NO", tm.getNetworkOperator());
            putInMap("NW-Name", tm.getNetworkOperatorName());

        }

        // Find out if we're connected to a network

        ConnectivityManager connMgr = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if ((networkInfo != null) && (networkInfo.isConnected()))
        {
            putInMap("WFC", "true");
            if (  wifi.isWifiEnabled() ) {
                WifiInfo wi = wifi.getConnectionInfo();
                putInMap("Wi-Fi", "true");
                int ip = wi.getIpAddress();
                String ipAddress = Formatter.formatIpAddress(ip);
                putInMap("Wi-Fi-IP",ipAddress);
                putInMap("SSID",  wi.getSSID());
                putInMap("Wi-Fi Signal", String.valueOf(wi.getRssi()));
                putInMap("WBSID",wifi.getConnectionInfo().getBSSID());
                putInMap("WSSID",wifi.getConnectionInfo().getSSID());
                putInMap("WMAC",wifi.getConnectionInfo().getMacAddress());

            }
            else
            {
                putInMap("Wi-Fi", "false");
            }
        }
        else
        {
            putInMap("WFC", "false");
        }


        networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if ((networkInfo != null) && (networkInfo.isConnected()))
        {
            putInMap("MC", "true");

            NetworkInterface intf = getInternetInterface();
            putInMap("IP", getIPAddress(intf));
            String type = networkInfo.getTypeName();
            if ( type.equalsIgnoreCase("mobile") ) {
                putInMap("Mobile", "true");
                putInMap("Mobile type", String.valueOf(getNetworkType(tm.getNetworkType())));
            } else {
                putInMap("Mobile", "false");
            }

        }
        else
        {
            putInMap("MC", "false");
        }

    }


    public static String getInfoJSON() {

       return infoMap.toString();

    }

    public static String getInfo(final String key) {
        try {
            return infoMap.has(key) ? infoMap.getString(key) : "";
        }
        catch (JSONException e)
        {
            return "";
        }
    }

    public static boolean isMobileNetworkConnected()
    {
        return getInfo("MC").equalsIgnoreCase("true");

    }


    /***
     * Only in Tethering Server returns false.
     * @return
     */
    public static boolean isWifiInternetEnabled()
    {
        if (getInfo("MC").equalsIgnoreCase("false"))
        {
            return getInfo("WFC").equalsIgnoreCase("true");
        }

        return false;
    }

    public static boolean isTetheringwithMobileNetworkEnabled()
    {
        if (getInfo("MC").equalsIgnoreCase("true"))
        {
            if (getInfo("WFC").equalsIgnoreCase("false"))
            {
                return !getInfo("IP").equals("null");
            }
        }

        return false;
    }


    @Deprecated
    public static String getWifiIP () {
        final Context context = AndruavEngine.getPreference().getContext();
        String ipAddress="";
        if (isTetheringwithMobileNetworkEnabled())
        {
            ipAddress = getInfo("IP");
        }
        else {
            WifiManager wifiMgr =  (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            int ip = wifiInfo.getIpAddress();
            ipAddress = Formatter.formatIpAddress(ip);
        }
        return ipAddress;
    }

    /**
     *
     * @return
     */
    public static Boolean isConnectedViaWifi()
    {
        String iP = getIPWifi();

        return !((iP ==null) || iP.equals("0.0.0.0") || iP.equals(""));
    }

    private static String getPhoneType(final Integer key) {
        if( phoneType.containsKey(key) ) {
            return phoneType.get(key);
        } else {
            return "unknown";
        }
    }

    /***
     * Translate connection tye to easily readable connection
     * @return
     */
    public static int getNetworkType (final int networkType)
    {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return TELEPHONE_200G;


            case TelephonyManager.NETWORK_TYPE_GPRS:
                return TELEPHONE_250G;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return TELEPHONE_275G;


            /**
             From this link https://en.wikipedia.org/wiki/Evolution-Data_Optimized ..NETWORK_TYPE_EVDO_0 & NETWORK_TYPE_EVDO_A
             EV-DO is an evolution of the CDMA2000 (IS-2000) standard that supports high data rates.

             Where CDMA2000 https://en.wikipedia.org/wiki/CDMA2000 .CDMA2000 is a family of 3G[1] mobile technology standards for sending voice,
             data, and signaling data between mobile phones and cell sites.
             */
            case TelephonyManager.NETWORK_TYPE_1xRTT: //~ 50-100 kbps
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return TELEPHONE_300G;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA: //~ 2-14 Mbps
                return TELEPHONE_300G;
            case TelephonyManager.NETWORK_TYPE_HSUPA: //~ 1-23 Mbps
            case TelephonyManager.NETWORK_TYPE_HSPA:  //~ 700-1700 kbps
                return TELEPHONE_350G;
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return TELEPHONE_375G;
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                //Log.d("Type", "3g");
                //For 3g HSDPA , HSPAP(HSPA+) are main  networktype which are under 3g Network
                //But from other constants also it will 3g like HSPA,HSDPA etc which are in 3g case.
                //Some cases are added after  testing(real) in device with 3g enable data
                //and speed also matters to decide 3g network type
                //https://en.wikipedia.org/wiki/4G#Data_rate_comparison
                return TELEPHONE_390G;
            case TelephonyManager.NETWORK_TYPE_LTE:
                //No specification for the 4g but from wiki
                //I found(LTE (Long-Term Evolution, commonly marketed as 4G LTE))
                //https://en.wikipedia.org/wiki/LTE_(telecommunication)
                return TELEPHONE_400G;
            default:
                return TELEPHONE_NOTFOUND;
        }
    }

    private static String getIPAddress( final NetworkInterface intf) {
        String result = "";
        for( Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
            InetAddress inetAddress = enumIpAddr.nextElement();
            result = inetAddress.getHostAddress();
        }
        return result;
    }

    private static NetworkInterface getInternetInterface() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if( ! intf.equals(NetworkInterface.getByName("lo"))) {
                    return intf;
                }
            }
        } catch (SocketException ex) {
        }
        return null;
    }

    /***
     * Gets the IP of Wifi even if tethering Server
     *
     * @return
     */
    public static String getIPWifi ()
    {
        String IP = getIPAddress("wlan0");  // wireless
        if (IP != null)
            return IP;
        IP =  getIPAddress("eth0");   // LAN
        if (IP != null)
            return IP;
        IP =  getIPAddress("ap0");   // Access Point with 3G Card installed.
        if (IP != null)
            return IP;
        IP =  getIPAddress("rmnet_data0");
        if (IP != null)
            return IP;
        IP =  getIPAddress("usb0");
        if (IP != null)
            return IP;
        IP =  getIPAddress("rmnet_data1");
        if (IP != null)
            return IP;
        IP =  getIPAddress("seth_w0");  // seems to be VPN
        if (IP != null)
            return IP;
        IP =  getIPAddress("rmnet4");  // 3G with Data
        if (IP != null)
            return IP;

        IP =  getIPAddress("v4-rmnet0");  // 3G with Data
        if (IP != null)
            return IP;
        IP =  getIPAddress("4-rmnet0");  // 3G with Data
        if (IP != null)
            return IP;
        IP =  getIPAddress("rndis0");  //  USB Tethering
        if (IP != null)
            return IP;

        IP =  getIPAddress("en0");  //  // Tethering with SIM
        if (IP != null)
            return IP;

        IP =  getIPAddress("softap0");  //  WiFi Tethering with SIM 4G
        if (IP != null)
            return IP;

        IP =  getIPAddress("v4-rmnet_data0");  //  USB Tethering with SIM 4G
        return IP;
    }

    public static String getWifiIPBroadcast ()
    {
        String IP = getIPBroadCast("wlan0");  // wireless
        if (IP != null)
            return IP;
        IP =  getIPBroadCast("eth0");   // LAN
        if (IP != null)
            return IP;
        IP =  getIPBroadCast("ap0");   // Access Point with 3G Card installed.
        if (IP != null)
            return IP;
        IP =  getIPBroadCast("rmnet_data0");
        if (IP != null)
            return IP;
        IP =  getIPBroadCast("usb0");
        if (IP != null)
            return IP;
        IP =  getIPBroadCast("rmnet_data1");
        if (IP != null)
            return IP;
        IP =  getIPBroadCast("seth_w0");  // seems to be VPN
        if (IP != null)
            return IP;
        IP =  getIPBroadCast("rmnet4");  // 3G with Data
        if (IP != null)
            return IP;

        IP =  getIPBroadCast("v4-rmnet0");  // 3G with Data
        if (IP != null)
            return IP;
        IP =  getIPBroadCast("4-rmnet0");  // 3G with Data
        if (IP != null)
            return IP;
        IP =  getIPBroadCast("rndis0");  //  USB Tethering
        if (IP != null)
            return IP;

        IP =  getIPAddress("en0");  //  // Tethering with SIM
        if (IP != null)
            return IP;

        IP =  getIPBroadCast("softap0");  //  WiFi Tethering with SIM 4G
        if (IP != null)
            return IP;

        IP =  getIPBroadCast("v4-rmnet_data0");  //  USB Tethering with SIM 4G
        return IP;
    }


    // TCP/HTTP/DNS (depending on the port, 53=DNS, 80=HTTP, etc.)
    public static boolean isOnline() {
        try {
            int timeoutMs = 2500;
            Socket sock = new Socket();
            SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);

            sock.connect(sockaddr, timeoutMs);
            sock.close();

            return true;
        } catch (IOException e) { return false; }
    }

    public static  boolean isWifiConnected ()
    {
        return (getIPWifi () != null);
    }

    /***
     * Gets IP of the 3G
     * @return
     */
    public static String getIP3G ()
    {
        String ret =  getIPAddress("rmnet0");  // Internet with Mobile
        if (ret == null)
        {
            ret = getIPAddress("rmnet1");
        }if (ret == null)
        {
            ret = getIPAddress("rmnet_data0"); // Data SIM
        }
        if (ret == null)
        {
            ret = getIPAddress("pdp0"); // Data SIM
        }
        if (ret == null)
        {
            ret = getIPAddress("ccmni0"); // Data SIM
        }
        if (ret == null)
        {
            ret = getIPAddress("tun0"); // Data SIM
        }
        if (ret == null)
        {
            ret = getIPAddress("clat4"); // LTE
        }
        if (ret == null)
        {
            ret = getIPAddress("seth_lte0"); // LTE
        }
        if (ret == null)
        {
            ret = getIPAddress("rmnet_data2");
        }
        if (ret == null)
        {
            ret = getIPAddress("ccmni1");
        }
        return ret;

    }


    public static String getAllAvailNetwork_Debug ()
    {
        Enumeration<NetworkInterface> en = null;
        try {
            en = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (en == null) return null;

        StringBuilder DebugStr = new StringBuilder("Net");  // used in debugging
        for (;en.hasMoreElements();)
        {
            NetworkInterface intf = en.nextElement();


            DebugStr.append(intf.getName());
            DebugStr.append(":");
            DebugStr.append(intf.getDisplayName());

            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if ((!inetAddress.isLoopbackAddress()) && (inetAddress instanceof Inet4Address) ) {
                    DebugStr.append(":");
                    DebugStr.append(inetAddress.getHostAddress());

                }
            }
            DebugStr.append("-");



        }
        return DebugStr.toString();
    }

    public static String getIPAddress(final String filter)
    {

        Enumeration<NetworkInterface> en = null;
        try {
            en = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (en == null) return null;

        for (;en.hasMoreElements();)
        {
            NetworkInterface intf = en.nextElement();





            if (intf.getName().equals(filter))
            {
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if ((!inetAddress.isLoopbackAddress()) && (inetAddress instanceof Inet4Address) ) {
                        return  (inetAddress.getHostAddress());

                    }
                }
            }
        }
        return null;
    }

    public static String getIPBroadCast(final String filter)
    {

        Enumeration<NetworkInterface> en = null;
        try {
            en = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (en == null) return null;

        for (;en.hasMoreElements();)
        {
            NetworkInterface intf = en.nextElement();
            try {
                if (!intf.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : intf.getInterfaceAddresses()) {
                        if (interfaceAddress.getBroadcast() == null) continue;
                        return interfaceAddress.getBroadcast().toString().substring(1);
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    private static byte[] convert2Bytes(final int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };
        return addressBytes;
    }


    private static boolean mDual3GAccess = false;
    public  static boolean getDual3GAccess ()
    {
        return mDual3GAccess;
    }
    public static void Dual3GAccess (final boolean enable)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // PANIC HERE
            return;
        }
        mDual3GAccess = enable;
        if (enable)
        {
            bringUpCellularNetwork();
        }
        else
        {
            bringDownCellularNetwork();
        }
    }

    static  ConnectivityManager connMgr;
    private static void bringUpCellularNetwork() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return;

        final Context context = AndruavEngine.getPreference().getContext();

        //Timber.i("Setting up cellular network request.");
        connMgr = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkRequest networkReq = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            connMgr.requestNetwork(networkReq, new ConnectivityManager.NetworkCallback() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onAvailable(Network network) {
                    //Timber.i("Setting up process default network: %s", network);
                    ConnectivityManager.setProcessDefaultNetwork(network);
                    //DroidPlannerApp.setCellularNetworkAvailability(true);
                }
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void bringDownCellularNetwork() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return;

        //Timber.i("Bringing down cellular netowrk access.");
        ConnectivityManager.setProcessDefaultNetwork(null);
        // DroidPlannerApp.setCellularNetworkAvailability(false);
    }

    /***
     * copied from Tower .... used to check that mobile is connected to SOLO network.
     * @return
     */
    public static boolean isOnSoloNetwork() {

        final Context context = AndruavEngine.getPreference().getContext();

        if (context == null){
            return false;
        }

        final String connectedSSID = getCurrentWifiLink();
        return connectedSSID != null && connectedSSID.startsWith(SOLO_LINK_WIFI_PREFIX);
    }


    public static String getCurrentWifiLink() {

        final Context context = AndruavEngine.getPreference().getContext();

        if(context == null)
            return null;

        final WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        final WifiInfo connectedWifi = wifiMgr.getConnectionInfo();
        final String connectedSSID = connectedWifi == null ? null : connectedWifi.getSSID().replace("\"", "");
        return connectedSSID;
    }


    /***
     * TRUE only if mobile is connected to wifi or Data
     * @return
     */
    public static boolean isHasValidIPAddress() {
        NetInfoAdapter.Update();
        // if ((NetInfoAdapter.isWifiInternetEnabled()==false) && (NetInfoAdapter.isMobileNetworkConnected()==false))
        // No Internet Access
        return (NetInfoAdapter.getIPWifi() != null) || (NetInfoAdapter.getIP3G() != null);
    }
}