package org.droidplanner.services.android.impl.communication.connection.usb;

import static com.hoho.android.usbserial.driver.UsbSerialPort.STOPBITS_1;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

public class UsbHohoConnection extends UsbConnection.UsbConnectionImpl {


    private static final String TAG = UsbHohoConnection.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    public final static int     USB_READ_WAIT = 500;
    public final static int     USB_WRITE_WAIT = 500;

    private static final IntentFilter intentFilter = new IntentFilter(ACTION_USB_PERMISSION);
    private enum UsbPermission { Unknown, Requested, Granted, Denied }
    private boolean mConnected = false;
    private UsbPermission usbPermission = UsbPermission.Unknown;

    private final AtomicReference<Bundle> extrasHolder = new AtomicReference<>();
    private final PendingIntent usbPermissionIntent;

    private UsbSerialPort usbSerialPort;
    private SerialInputOutputManager usbIoManager;

    private ScheduledExecutorService scheduler;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // enter here when user press ok or cancel as a reply for "Access Permission" dialog.
            final String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                removeWatchdog();
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        //call method to set up device communication
                        try {
                            UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
                            openUsbDevice(driver, extrasHolder.get());
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    } else {
                        LinkConnectionStatus connectionStatus = LinkConnectionStatus
                            .newFailedConnectionStatus(LinkConnectionStatus.LINK_UNAVAILABLE, "Unable to access usb device.");
                        onUsbConnectionStatus(connectionStatus);
                    }
                } else {
                    Log.d(TAG, "permission denied for device " + device);
                    LinkConnectionStatus connectionStatus = LinkConnectionStatus
                        .newFailedConnectionStatus(LinkConnectionStatus.PERMISSION_DENIED, "USB Permission denied.");
                    onUsbConnectionStatus(connectionStatus);
                }
            }
      }
  };

    protected UsbHohoConnection(Context context, UsbConnection parentConn, int baudRate) {
        super(context, parentConn, baudRate);
        this.usbPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);

    }
    private void registerUsbPermissionBroadcastReceiver() {
        mContext.registerReceiver(broadcastReceiver, intentFilter);
    }

    private void unregisterUsbPermissionBroadcastReceiver() {
        try {
            mContext.unregisterReceiver(broadcastReceiver);
        }catch(IllegalArgumentException e){
            Timber.e(e, "Receiver was not registered.");
        }
    }

    private void removeWatchdog() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }
    private final Runnable permissionWatchdog = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Permission request timeout.");
            LinkConnectionStatus connectionStatus = LinkConnectionStatus
                    .newFailedConnectionStatus(LinkConnectionStatus.TIMEOUT, "Unable to get usb access.");
            onUsbConnectionStatus(connectionStatus);

            removeWatchdog();
        }
    };
    @Override
    protected void closeUsbConnection() throws IOException {
        mConnected = false;
        if(usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {}
        usbSerialPort = null;
    }

    @Override
    protected void openUsbConnection(Bundle extras) throws IOException {
        extrasHolder.set(extras);
        registerUsbPermissionBroadcastReceiver();

        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

        for (UsbDevice v : usbManager.getDeviceList().values())
            device = v;
        if (device == null) {
            Log.d(TAG, "connection failed: device not found");
            return;
        }

        // Determine class that handles this USB based on vendor & product id
        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) {
            // you can define your custome filters here. // we already defined them as default values.
            driver = CustomProber.getCustomProber().probeDevice(device);
        }

        if (driver == null) {
            Log.d(TAG, "connection failed: no driver for device");
            return;
        }

        // if no port then return.
        if (driver.getPorts().size() < 1) {
            Log.d(TAG, "connection failed: not enough ports at device");
            return;
        }

        openUsbDevice(driver, extras);
    }

    protected void openUsbDevice (UsbSerialDriver driver, Bundle extras) throws IOException {
        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

        usbSerialPort = driver.getPorts().get(0);

        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            // enter here if user has not given permission for accessing USB.
            usbPermission = UsbPermission.Requested;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if(usbConnection == null) {
            Log.d(TAG,"connection failed: open failed");
            return;
        }

        if (!usbManager.hasPermission(driver.getDevice())) {
            Log.d(TAG, "connection failed: permission denied");
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(permissionWatchdog, 15, TimeUnit.SECONDS);
            Log.d(TAG, "Requesting permission to access usb device " + driver.getDevice().getDeviceName());
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);

            return ;
        }

        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(mBaudRate, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            Log.d(TAG,"connected");
            mConnected = true;
            onUsbConnectionOpened(extras);

        } catch (Exception e) {
            Log.d(TAG,"connection failed: " + e.getMessage());
            closeUsbConnection();
        }
    }

    @Override
    protected int readDataBlock(byte[] readData) throws IOException {
        // Read data from driver. This call will return up to readData.length bytes.
        // If no data is received it will timeout after 200ms (as set by parameter 2)
        if(usbSerialPort == null)
            throw new IOException("Device is unavailable.");

        int iavailable = 0;
        try {
            iavailable = usbSerialPort.read(readData, USB_READ_WAIT);
        } catch (NullPointerException e) {
            final String errorMsg = "Error Reading: " + e.getMessage()
                    + "\nAssuming inaccessible USB device.  Closing connection.";
            Log.e(TAG, errorMsg, e);
            throw new IOException(errorMsg, e);
        }

        if (iavailable == 0)
            iavailable = -1;
        return iavailable;
    }

    @Override
    protected void sendBuffer(byte[] buffer) {
        if (usbSerialPort != null) {
            try {
                usbSerialPort.write(buffer, USB_WRITE_WAIT);
            } catch(IOException e){
                Log.e(TAG, "Error Sending: " + e.getMessage(), e);
            }
        }
    }
}
