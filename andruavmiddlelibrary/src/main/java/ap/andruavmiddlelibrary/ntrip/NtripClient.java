package ap.andruavmiddlelibrary.ntrip;

import android.util.Base64;

import com.andruav.AndruavEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import ap.andruavmiddlelibrary.preference.Preference;

/**
 * NTRIP v1 client: connects to a caster, authenticates, and hands out the raw RTCM3 byte
 * stream in chunks of at most {@link #MAX_CHUNK_LENGTH} bytes.
 *
 * This class is pure networking - it must not know that MAVLink exists, which keeps it unit
 * testable and reusable. Chunking, FC-link gating and message packing live in the caller
 * (RtcmInjector / DroneKitServer.do_InjectRTCM).
 *
 * Threading mirrors BlueToothFCB.MakePersistentConnection: one dedicated reader thread with
 * a kill flag, a BLOCKING read (never a sleep-poll loop), and stop() that closes the socket
 * because a parked read() only wakes when its socket closes - then joins with a timeout.
 */
public class NtripClient {

    /**
     * GPS_RTCM_DATA.data is exactly 180 bytes, so the stream is read in 180-byte chunks and
     * each is forwarded as one unfragmented message - no fragmentation bookkeeping needed
     * (ArduPilot's inject_data() pushes bytes at the GPS, which does its own RTCM3 framing).
     */
    public static final int MAX_CHUNK_LENGTH = 180;

    /***
     * Receiver of NTRIP stream data and connection state. Callbacks run on the reader
     * thread - implementations must handle their own threading if needed.
     */
    public interface NtripListener {
        /*** Called for every chunk read from the caster; 1 <= length <= MAX_CHUNK_LENGTH. */
        void onRtcmData(final byte[] buffer, final int length);

        /*** Connection state change, for UI/log. message is null on connect, an error line on disconnect. */
        void onNtripState(final boolean connected, final String message);
    }

    private static final int CONNECT_TIMEOUT_MS   = 10000;
    private static final long BACKOFF_INITIAL_MS  = 2000;
    private static final long BACKOFF_MAX_MS      = 60000;
    private static final long GGA_UPLOAD_PERIOD_MS = 10000;
    private static final int  HTTP_LINE_LIMIT     = 256;

    private final NtripListener mListener;

    private volatile boolean mKillMe = true;
    private Thread mThread;
    private Socket mSocket;

    /** Latest GGA sentence (without line terminator) to upload to the caster, or null. */
    private volatile String mGgaSentence = null;

    /** Set by the reader thread on HTTP 401/404 so the backoff jumps straight to the max. */
    private volatile boolean mAuthOrMountFailure = false;



    public NtripClient (final NtripListener listener) {
        mListener = listener;
    }

    /***
     * Latest GGA sentence for VRS / network-RTK casters. Called from any thread; uploaded to
     * the caster roughly every 10 s while connected.
     */
    public void setGgaSentence (final String ggaSentence) {
        mGgaSentence = ggaSentence;
    }

    public boolean isRunning () {
        return !mKillMe;
    }

    /***
     * Starts the reader thread. Reads settings from Preference at connect time so a settings
     * change takes effect on the next reconnect. No-op if already running.
     */
    public void start () {
        if (!mKillMe) return;

        mKillMe = false;
        mAuthOrMountFailure = false;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                readerLoop();
            }
        });
        mThread.setName("NtripClient");
        mThread.start();
    }

    /***
     * Stops the reader thread. Must both set the kill flag AND close the socket: a blocked
     * read() only wakes when its socket closes (the same trap BlueToothFCB.
     * StopPersistentConnection documents). Then joins with a timeout.
     */
    public void stop () {
        mKillMe = true;
        closeSocket();
        if (mThread != null) {
            try {
                mThread.join(3000);
            } catch (InterruptedException ex) {
                AndruavEngine.log().logException("ntrip", ex);
            } finally {
                mThread = null;
            }
        }
    }



    private void closeSocket () {
        final Socket socket = mSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void readerLoop () {
        long backoffMs = BACKOFF_INITIAL_MS;

        while (!mKillMe) {
            boolean wasConnected = false;
            try {
                final boolean connected = connect();
                if (!connected || mKillMe) break;

                wasConnected = true;
                mListener.onNtripState(true, null);
                // A fresh connection is progress: reset the backoff. It is reset again on every
                // successful read below so a stream that keeps flowing reconnects fast.
                backoffMs = BACKOFF_INITIAL_MS;
                mAuthOrMountFailure = false;

                final InputStream inputStream = mSocket.getInputStream();
                final OutputStream outputStream = mSocket.getOutputStream();

                // Reusable buffer - the listener copies what it needs before returning.
                final byte[] buffer = new byte[MAX_CHUNK_LENGTH];
                long lastGgaUploadAt = 0;

                while (!mKillMe) {
                    final int n = inputStream.read(buffer);
                    if (mKillMe) return;
                    if (n > 0) {
                        mListener.onRtcmData(buffer, n);
                        backoffMs = BACKOFF_INITIAL_MS;

                        // GGA upload for VRS casters: written to the same socket's output
                        // stream every ~10 s. Casters that do not need it ignore it.
                        final String gga = mGgaSentence;
                        final long now = System.currentTimeMillis();
                        if ((gga != null) && (now - lastGgaUploadAt >= GGA_UPLOAD_PERIOD_MS)) {
                            lastGgaUploadAt = now;
                            outputStream.write((gga + "\r\n").getBytes());
                            outputStream.flush();
                        }
                    } else {
                        // Stream ended (n == -1) - exit to the reconnect path.
                        break;
                    }
                }
            } catch (final SocketTimeoutException ex) {
                // connect() timeout - ordinary retry, same path as other IO failures.
            } catch (final Exception ex) {
                if (mKillMe) return;
                AndruavEngine.log().logException("ntrip", ex);
            } finally {
                closeSocket();
                // connect() already reported its own specific failure (status line / config
                // error) - only report here when a live stream actually dropped.
                if (!mKillMe && wasConnected) {
                    mListener.onNtripState(false, "connection lost");
                }
            }

            if (mKillMe) return;

            // Exponential backoff: 2 s, 4 s, 8 s ... capped at 60 s. On 401/404 jump straight
            // to the max - retrying fast will not fix credentials or a wrong mountpoint.
            final long waitMs = mAuthOrMountFailure ? BACKOFF_MAX_MS : backoffMs;
            backoffMs = Math.min(backoffMs * 2, BACKOFF_MAX_MS);
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }

    /***
     * Opens the TCP connection and performs the NTRIP v1 handshake. Returns true when the
     * caster accepted the request and raw RTCM bytes follow. Sets mAuthOrMountFailure on
     * 401/404 so the caller backs off at the maximum rate.
     */
    private boolean connect () {
        final String host = Preference.getNtripHost(null);
        final int port;
        try {
            port = Integer.parseInt(Preference.getNtripPort(null));
        } catch (NumberFormatException ex) {
            mAuthOrMountFailure = true;
            mListener.onNtripState(false, "invalid NTRIP port in settings");
            return false;
        }
        final String mountPoint = Preference.getNtripMountPoint(null);
        final String user = Preference.getNtripUser(null);
        final String password = Preference.getNtripPassword(null);

        if ((host == null) || host.isEmpty() || (mountPoint == null) || mountPoint.isEmpty()) {
            mAuthOrMountFailure = true;
            mListener.onNtripState(false, "NTRIP host or mountpoint not configured");
            return false;
        }

        try {
            mSocket = new Socket();
            mSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);

            // NTRIP v1 request - accepted by every caster, including 2.0 ones.
            final String credentials = Base64.encodeToString((user + ":" + password).getBytes(), Base64.NO_WRAP);
            final String request =
                    "GET /" + mountPoint + " HTTP/1.0\r\n"
                    + "User-Agent: NTRIP Andruav\r\n"
                    + "Authorization: Basic " + credentials + "\r\n"
                    + "Accept: */*\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";
            final OutputStream outputStream = mSocket.getOutputStream();
            outputStream.write(request.getBytes());
            outputStream.flush();

            // Response line, then headers up to the blank line - everything after is raw RTCM.
            final String statusLine = readLine(mSocket.getInputStream());
            if (statusLine == null) {
                mListener.onNtripState(false, "no response from caster");
                return false;
            }

            // "ICY 200 OK" (NTRIP v1) or "HTTP/1.x 200 OK" - both mean the stream follows.
            if (statusLine.contains("200")) {
                String line;
                do {
                    line = readLine(mSocket.getInputStream());
                    if (line == null) return false;
                } while (!line.isEmpty());
                return true;
            }

            if (statusLine.contains("401") || statusLine.contains("404")) {
                // Bad credentials / unknown mountpoint - no point in a fast retry.
                mAuthOrMountFailure = true;
            }
            mListener.onNtripState(false, statusLine);
            return false;
        } catch (IOException ex) {
            closeSocket();
            if (!mKillMe) {
                AndruavEngine.log().logException("ntrip", ex);
                mListener.onNtripState(false, ex.getMessage());
            }
            return false;
        }
    }

    /***
     * Reads one CR/LF-terminated line from the stream as ISO-8859-1 (casters send ASCII
     * headers). Returns null on EOF. Headers are bounded to HTTP_LINE_LIMIT so a broken
     * server cannot make this loop allocate without end.
     */
    private static String readLine (final InputStream inputStream) throws IOException {
        final StringBuilder line = new StringBuilder();
        int c;
        while ((c = inputStream.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') {
                line.append((char) c);
                if (line.length() > HTTP_LINE_LIMIT) {
                    return line.toString();
                }
            }
        }
        if ((c == -1) && (line.length() == 0)) return null;
        return line.toString();
    }
}
