package ap.andruav.denative.fclink;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import com.andruav.Constants;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.FtdiSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Serves a USB-serial or Bluetooth flight-controller link on a loopback TCP
 * socket so de_mavlink can use fcb_connection_uri tcp://127.0.0.1:5760
 * unchanged (megaplan decision N4).
 *
 * One client at a time; when a second connection arrives it replaces the
 * current one. When the device link drops the client socket is closed too,
 * the device is retried every {@link #RETRY_MS} ms, and the next client is
 * accepted again. Baud rate and link source come from the existing FCB
 * connection settings (Preference / FcbConnectionSheet).
 */
public final class FcLinkBridge {

    private static final String TAG = "FcLinkBridge";

    public static final int TCP_PORT = 5760;
    private static final String HOST = "127.0.0.1";
    private static final int RETRY_MS = 2000;
    private static final int STATS_INTERVAL_MS = 2000;
    private static final int PUMP_BUF = 4096;
    private static final int USB_IO_TIMEOUT_MS = 500;
    private static final String STATS_FILE = "fc_link_stats.txt";
    private static final String ACTION_USB_PERMISSION = "ap.andruav.denative.USB_PERMISSION";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static FcLinkBridge instance;

    /** Starts the bridge once for the :de_mavlink process lifetime. */
    public static synchronized void start(final Context context) {
        if (instance != null) {
            return;
        }
        instance = new FcLinkBridge(context.getApplicationContext());
        instance.startLocked();
    }

    private final Context appContext;
    private final AtomicLong bytesToFc = new AtomicLong();
    private final AtomicLong bytesFromFc = new AtomicLong();
    private final AtomicLong reconnects = new AtomicLong();

    private ServerSocket serverSocket;
    private volatile FcLink activeLink;
    private volatile Socket clientSocket;
    private volatile boolean pumpsRunning;
    private volatile boolean permissionGranted;
    private volatile String lastError = "";

    private interface FcLink {
        String name();
        InputStream input() throws IOException;
        OutputStream output() throws IOException;
        void close();
    }

    private static final class UsbSerialLink implements FcLink {
        private final UsbSerialPort port;
        private final UsbDeviceConnection connection;
        private final String name;

        UsbSerialLink(UsbSerialPort port, UsbDeviceConnection connection, String name) {
            this.port = port;
            this.connection = connection;
            this.name = name;
        }

        @Override
        public String name() { return name; }

        @Override
        public InputStream input() {
            return new InputStream() {
                @Override
                public int read(byte[] buffer, int offset, int length) throws IOException {
                    byte[] chunk = new byte[length];
                    int n = port.read(chunk, USB_IO_TIMEOUT_MS);
                    if (n <= 0) {
                        return 0;
                    }
                    System.arraycopy(chunk, 0, buffer, offset, n);
                    return n;
                }
                @Override public int read() throws IOException {
                    byte[] one = new byte[1];
                    return (read(one, 0, 1) == 1) ? (one[0] & 0xff) : -1;
                }
            };
        }

        @Override
        public OutputStream output() {
            return new OutputStream() {
                @Override
                public void write(byte[] buffer, int offset, int length) throws IOException {
                    byte[] slice = (offset == 0 && length == buffer.length)
                            ? buffer : java.util.Arrays.copyOfRange(buffer, offset, offset + length);
                    port.write(slice, USB_IO_TIMEOUT_MS);
                }
                @Override public void write(int value) throws IOException {
                    port.write(new byte[]{(byte) value}, USB_IO_TIMEOUT_MS);
                }
            };
        }

        @Override
        public void close() {
            try { port.close(); } catch (IOException ignored) { }
            try { connection.close(); } catch (Exception ignored) { }
        }
    }

    private static final class BluetoothLink implements FcLink {
        private final BluetoothSocket socket;

        BluetoothLink(BluetoothSocket socket) { this.socket = socket; }

        @Override
        public String name() { return "bt"; }

        @Override
        public InputStream input() throws IOException { return socket.getInputStream(); }

        @Override
        public OutputStream output() throws IOException { return socket.getOutputStream(); }

        @Override
        public void close() {
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_USB_PERMISSION.equals(intent.getAction())) {
                return;
            }
            permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
            if (!permissionGranted) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.w(TAG, "usb permission denied for " + (device == null ? "device" : device.getDeviceName()));
            }
        }
    };

    private FcLinkBridge(Context appContext) {
        this.appContext = appContext;
    }

    private void startLocked() {
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            appContext.registerReceiver(usbPermissionReceiver, filter);
        }
        new Thread(this::acceptLoop, "fc_link_accept").start();
        new Thread(this::linkLoop, "fc_link_device").start();
        new Thread(this::statsLoop, "fc_link_stats").start();
        Log.i(TAG, "listening on " + HOST + ":" + TCP_PORT);
    }

    private void acceptLoop() {
        while (true) {
            try {
                if (serverSocket == null || serverSocket.isClosed()) {
                    serverSocket = new ServerSocket(TCP_PORT, 1, InetAddress.getByName(HOST));
                }
                Socket accepted = serverSocket.accept();
                closeClient();
                accepted.setTcpNoDelay(true);
                clientSocket = accepted;
                Log.i(TAG, "client connected " + accepted.getRemoteSocketAddress());
                maybeStartPumps();
            } catch (IOException e) {
                lastError = "accept: " + e.getMessage();
                sleepQuiet(RETRY_MS);
            }
        }
    }

    private void linkLoop() {
        while (true) {
            int commType = Preference.getFCBTargetComm(null);
            if (commType != Preference.FCB_COM_USB && commType != Preference.FCB_COM_BT) {
                // 'none': the FC is reached over UDP/TCP by de_mavlink itself.
                sleepQuiet(RETRY_MS);
                continue;
            }
            FcLink link = null;
            try {
                link = (commType == Preference.FCB_COM_USB) ? openUsbLink() : openBluetoothLink();
            } catch (IOException e) {
                lastError = e.getMessage();
            }
            if (link == null) {
                sleepQuiet(RETRY_MS);
                continue;
            }
            reconnects.incrementAndGet();
            activeLink = link;
            Log.i(TAG, "link up: " + link.name());
            maybeStartPumps();
            waitForLinkDeath(link);
        }
    }

    private void maybeStartPumps() {
        synchronized (this) {
            if (pumpsRunning || activeLink == null || clientSocket == null) {
                return;
            }
            pumpsRunning = true;
        }
        final FcLink link = activeLink;
        final Socket socket = clientSocket;
        Thread toFc = new Thread(() -> pump(socket, link, true), "fc_link_to_fc");
        Thread fromFc = new Thread(() -> pump(socket, link, false), "fc_link_from_fc");
        toFc.start();
        fromFc.start();
    }

    private void pump(Socket socket, FcLink link, boolean toFc) {
        byte[] buffer = new byte[PUMP_BUF];
        try {
            InputStream input = toFc ? socket.getInputStream() : link.input();
            OutputStream output = toFc ? link.output() : socket.getOutputStream();
            while (true) {
                int n = input.read(buffer);
                if (n < 0) {
                    break;
                }
                if (n == 0) {
                    continue;
                }
                output.write(buffer, 0, n);
                (toFc ? bytesToFc : bytesFromFc).addAndGet(n);
            }
        } catch (IOException e) {
            lastError = "pump: " + e.getMessage();
        } finally {
            onPumpFailure();
        }
    }

    private void onPumpFailure() {
        synchronized (this) {
            pumpsRunning = false;
        }
        FcLink link = activeLink;
        activeLink = null;
        if (link != null) {
            link.close();
        }
        closeClient();
        synchronized (this) {
            notifyAll();
        }
    }

    private void waitForLinkDeath(FcLink link) {
        synchronized (this) {
            while (activeLink == link) {
                try {
                    wait(RETRY_MS);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void closeClient() {
        Socket socket = clientSocket;
        clientSocket = null;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private FcLink openUsbLink() throws IOException {
        UsbManager usbManager = (UsbManager) appContext.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new IOException("no UsbManager");
        }
        List<UsbSerialDriver> drivers = new ArrayList<>(
                UsbSerialProber.getDefaultProber().findAllDrivers(usbManager));
        ProbeTable customTable = new ProbeTable();
        customTable.addProduct(0x16d0, 0x087e, CdcAcmSerialDriver.class);
        customTable.addProduct(0x1234, 0x0001, FtdiSerialDriver.class);
        customTable.addProduct(0x1234, 0x0002, FtdiSerialDriver.class);
        drivers.addAll(new UsbSerialProber(customTable).findAllDrivers(usbManager));
        if (drivers.isEmpty()) {
            throw new IOException("no usb serial device");
        }
        UsbSerialDriver driver = drivers.get(0);
        UsbDevice device = driver.getDevice();
        if (!usbManager.hasPermission(device)) {
            if (!permissionGranted) {
                requestUsbPermission(usbManager, device);
                throw new IOException("waiting usb permission");
            }
            if (!usbManager.hasPermission(device)) {
                throw new IOException("usb permission not granted yet");
            }
        }
        UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection == null) {
            throw new IOException("usb openDevice failed");
        }
        UsbSerialPort port = driver.getPorts().get(0);
        int baud = usbBaudRate();
        port.open(connection);
        try {
            port.setParameters(baud, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException | RuntimeException e) {
            try { port.close(); } catch (IOException ignored) { }
            connection.close();
            throw new IOException("setParameters failed: " + e.getMessage());
        }
        return new UsbSerialLink(port, connection, "usb:" + device.getDeviceName() + " @" + baud);
    }

    private void requestUsbPermission(UsbManager usbManager, UsbDevice device) {
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
        Intent intent = new Intent(ACTION_USB_PERMISSION).setPackage(appContext.getPackageName());
        usbManager.requestPermission(device, PendingIntent.getBroadcast(appContext, 0, intent, flags));
        Log.i(TAG, "requested usb permission for " + device.getDeviceName());
    }

    private int usbBaudRate() {
        int selector = Preference.getFCBUSBBaudRateSelector(null);
        int[] values = Constants.baudRateItemsValue;
        if (selector < 0 || selector >= values.length) {
            return 57600;
        }
        return values[selector];
    }

    private FcLink openBluetoothLink() throws IOException {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new IOException("no bluetooth adapter");
        }
        String address = Preference.getFCBBlueToothMAC(null);
        BluetoothDevice device;
        try {
            device = adapter.getRemoteDevice(address);
        } catch (IllegalArgumentException e) {
            throw new IOException("bad bluetooth address");
        }
        if (device == null) {
            throw new IOException("no bluetooth device configured");
        }
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
        adapter.cancelDiscovery();
        socket.connect();
        return new BluetoothLink(socket);
    }

    private void statsLoop() {
        File stats = new File(appContext.getFilesDir(), STATS_FILE);
        while (true) {
            FcLink link = activeLink;
            Socket client = clientSocket;
            String line = "mode:" + commName(Preference.getFCBTargetComm(null))
                    + " link:" + (link == null ? "down" : link.name())
                    + " client:" + (client == null ? "none" : "connected")
                    + " bytes_in:" + bytesFromFc.get()
                    + " bytes_out:" + bytesToFc.get()
                    + " reconnects:" + reconnects.get()
                    + (lastError.isEmpty() ? "" : " err:" + lastError);
            try (FileOutputStream out = new FileOutputStream(stats)) {
                out.write(line.getBytes());
            } catch (IOException ignored) {
            }
            sleepQuiet(STATS_INTERVAL_MS);
        }
    }

    private static String commName(int commType) {
        switch (commType) {
            case Preference.FCB_COM_USB: return "usb";
            case Preference.FCB_COM_BT:  return "bt";
            case Preference.FCB_COM_TCP: return "tcp";
            case Preference.FCB_COM_UDP: return "udp";
            default: return "?";
        }
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
