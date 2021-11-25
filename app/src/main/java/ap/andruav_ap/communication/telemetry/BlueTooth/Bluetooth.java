package ap.andruav_ap.communication.telemetry.BlueTooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.interfaces.INotification;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import com.andruav.notification.PanicFacade;


/**
 * Created by M.Hefny on 08-Oct-14.
 */

public class Bluetooth {
public boolean Connected = false;

private BluetoothAdapter mBluetoothAdapter = null;
private BluetoothSocket btSocket = null;
private OutputStream outStream = null;
private BufferedInputStream inStream = null;
private final Object sync_outStream = new Object();
private final Object sync_inStream = new Object();
// Well known SPP UUID (will *probably* map to
// RFCOMM channel 1 (default) if not in use);
// see comments in onResume().
private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


// ==> hardcode your server's MAC address here <==
public String address = "";

        Context context;

public boolean ConnectionLost = false;

public int ReconnectTry = 0;

        Handler handler;




public Bluetooth(Context con) {
        context = con;

       ////M.Hefny GetAdapter();
        }

public Boolean GetAdapter() {
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    return mBluetoothAdapter != null;


}

public void Enable()
{
   /* if (!mBluetoothAdapter.isEnabled()) {
        Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

        Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
                Toast.LENGTH_LONG).show();
    }
    else{
        Toast.makeText(getApplicationContext(),"Bluetooth is already on",
                Toast.LENGTH_LONG).show();
    }
     */

    if (!mBluetoothAdapter.isEnabled()) {
        mBluetoothAdapter.enable();
    }
}

public synchronized boolean isEnabled ()
{
    if (mBluetoothAdapter==null)
        return false;

    return mBluetoothAdapter.isEnabled();
}

public synchronized void disable() {

        if (mBluetoothAdapter == null) return;
        try {
             cancelDiscovery(); // safe to call
             mBluetoothAdapter.disable();

        } catch (Exception e) {

        }

}

public synchronized Boolean isConnected()
{
    return Connected;
}

public Boolean isDiscovering() {
    try {
           if (isEnabled()) return false;

       return  mBluetoothAdapter.isDiscovering();
    } catch (Exception e) {
        return false;
    }

}

public Boolean cancelDiscovery() {
    try {
        if (isDiscovering())
        {
            return  mBluetoothAdapter.cancelDiscovery();
        }
        return true;
    } catch (Exception e) {
        return false;
    }

}

public Boolean startDiscovery() {
    try {
        return  mBluetoothAdapter.startDiscovery();
    } catch (Exception e) {
        return false;
    }

}

public java.util.Set<android.bluetooth.BluetoothDevice> getBondedDevices()
{
    return mBluetoothAdapter.getBondedDevices();
}



public void GetRemoteDevice(String MAC) {

    //TODO: Fix Toasts and strings here
       ///////////// Toast.makeText(context, context.getString(R.string.Connecting), Toast.LENGTH_LONG).show();
        // app.Speak("Connecting");

        address = MAC;

        // When this returns, it will 'know' about the server,
        // via it's MAC address.
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        // We need two things before we can successfully connect
        // (authentication issues aside): a MAC address, which we
        // already have, and an RFCOMM channel.
        // Because RFCOMM channels (aka ports) are limited in
        // number, Android doesn't allow you to use them directly;
        // instead you request a RFCOMM mapping based on a service
        // ID. In our case, we will use the well-known SPP Service
        // ID. This ID is in UUID (GUID to you Microsofties)
        // format. Given the UUID, Android will handle the
        // mapping for you. Generally, this will return RFCOMM 1,
        // but not always; it depends what other BlueTooth services
        // are in use on your Android device.
        try {
        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
       ///////// Toast.makeText(context, context.getString(R.string.Unabletoconnect), Toast.LENGTH_LONG).show();
        }

        // Discovery may be going on, e.g., if you're running a
        // 'scan for devices' search from your handset's Bluetooth
        // settings, so we call cancelDiscovery(). It doesn't hurt
        // to call it, but it might hurt not to... discovery is a
        // heavyweight process; you don't want it in progress when
        // a connection attempt is made.
        mBluetoothAdapter.cancelDiscovery();
        }

public synchronized void Connect(String MAC) {

        // Blocking connect, for a simple client nothing else can
        // happen until a successful connection is made, so we
        // don't care if it blocks.
        if (!mBluetoothAdapter.isEnabled()) return ;

        try {

        GetRemoteDevice(MAC);
        btSocket.connect();
        exception_BT_disconnect_err =1; // reset error counters
        Connected = true;
        ConnectionLost = false;
        ReconnectTry = 0;
       ////////// Toast.makeText(context, context.getString(R.string.Connected), Toast.LENGTH_LONG).show();

        // app.Speak("Connected");

        } catch (IOException e) {
        try {
        btSocket.close();
        Connected = false;
        ConnectionLost = true;
      /////////////  Toast.makeText(context, context.getString(R.string.Unabletoconnect), Toast.LENGTH_LONG).show();
        // app.Speak("Unable to connect");

        } catch (IOException e2) {
            AndruavEngine.log().logException(AndruavSettings.AccessCode, "exception_BT", e2);
        }
        }


        try {
        inStream = new BufferedInputStream(btSocket.getInputStream());
        outStream = btSocket.getOutputStream();

        } catch (IOException ex) {
            AndruavEngine.log().logException(AndruavSettings.AccessCode, "exception_BT", ex);
       }


        }

    public void Write(byte[] arr) {
        try {
            if (!Connected) return ;
            synchronized(sync_outStream) {
                outStream.write(arr);
            }
        } catch (IOException e) {
            CloseSocket();
            ConnectionLost = true;
        }
    }

    int exception_BT_counter=5;
    int exception_BT_disconnect_err=1;
    public Boolean Send(final byte[] out, final int length) {

        try {
            synchronized(sync_outStream) {
                outStream.write(out, 0, length);
                return true;
            }
        } catch (IOException ex) {
            // java.io.IOException: Connection timed out
            //Connection timed out
            if (exception_BT_counter > 0) {
                exception_BT_counter = exception_BT_counter - 1;
                AndruavEngine.log().logException(AndruavSettings.AccessCode, "exception_BT", ex);
            }
            if (ex.getMessage().equals("Connection timed out"))
            {
                if (exception_BT_disconnect_err>0) {
                    exception_BT_disconnect_err = exception_BT_disconnect_err -1;
                    PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_BLUETOOTH, "Bluetooth connection lost", null);

                }

            }

            return false;
        }
    }


    public void Send(String out) {

        byte[] msgBuffer = out.getBytes();
        Write (msgBuffer);
    }



    public void CloseSocket() {
        if (outStream != null) {
        try {
            synchronized(sync_outStream) {
                outStream.flush();
            }
        } catch (IOException e) {
        }
        }

        try {
        btSocket.close();
        Connected = false;

      /////////////  Toast.makeText(context, context.getString(R.string.Disconnected), Toast.LENGTH_LONG).show();
        // app.Speak("Disconnected");

        } catch (Exception ex) {
            AndruavEngine.log().logException(AndruavSettings.AccessCode, "exception_BT", ex);
        }

        }

    int exception_BT_counter2=5;

    public int available() {
        int a = 0;

        try {
            if (Connected)
            synchronized(sync_inStream) {
                a = inStream.available();
            }
        } catch (IOException ex) {
            if (exception_BT_counter2 > 0) {
                exception_BT_counter2 = exception_BT_counter2 - 1;
                AndruavEngine.log().logException(AndruavSettings.AccessCode, "exception_BT", ex);
            }
            return -1;
        }

        return a;
        }

        int Read32() {
        byte[] b = new byte[4];

        try {
            synchronized(sync_inStream) {inStream.read(b, 0, 4);}
        } catch (IOException ex) {
               AndruavEngine.log().logException(AndruavSettings.AccessCode, "exception_BT", ex);

        }
        return (b[0] & 0xff) + ((b[1] & 0xff) << 8) + ((b[2] & 0xff) << 16) + ((b[3] & 0xff) << 24);
        }

public int Read16() {
        byte[] b = new byte[2];

        try {
            synchronized(sync_inStream) {inStream.read(b, 0, 2);}
        } catch (IOException e) {
        e.printStackTrace();
        }

        return (b[0] & 0xff) + (b[1] << 8);
        }

public int Read8() {
        byte[] b = new byte[1];

        try {
            synchronized(sync_inStream) {inStream.read(b, 0, 1);}
        } catch (IOException e) {
        e.printStackTrace();
        }

        return b[0] & 0xff;
        }

public byte Read() {
        byte a = 0;

        try {
            synchronized(sync_inStream) {
                a = (byte) inStream.read();
            }
        } catch (IOException e) {
        e.printStackTrace();
        }

        return a;
        }

public byte[] ReadFrame(int framesize) {
        byte[] a = new byte[framesize];
        try {
            synchronized(sync_inStream) {
                inStream.read(a, 0, framesize);
            }
        } catch (IOException e) {
        e.printStackTrace();
        }

        return a;

        }

        }