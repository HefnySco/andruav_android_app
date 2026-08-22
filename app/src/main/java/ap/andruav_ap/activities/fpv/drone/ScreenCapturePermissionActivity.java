package ap.andruav_ap.activities.fpv.drone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import ap.andruav_ap.App;

/**
 * A transparent, orientation-unlocked activity whose sole purpose is to host the system
 * MediaProjection permission dialog. It must be a separate activity (not the landscape-locked
 * {@link FPVDroneRTCWebCamActivity}) because the permission dialog forces a portrait rotation
 * that destroys/crashes the FPV activity's WebRTC EGL surfaces.
 * <p>
 * On approval, the result-Intent is stored in {@link App#sScreenCaptureIntent} so that a later
 * mid-flight stream request (remote or local) can start screen capture with no dialog. The
 * activity finishes immediately after.
 * <p>
 * Typical flow: before flight, the user long-presses the camera-swap button on the FPV screen,
 * which launches this activity. The dialog appears, the user approves, and the intent is stored.
 * Later, when a stream request arrives (from web or from a subsequent long-press), the stored
 * intent is used directly — no dialog, no rotation, no crash.
 */
public class ScreenCapturePermissionActivity extends Activity {

    private static final String TAG = "ScreenCapturePerm";
    private static final int REQUEST_CODE = 7702;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG, "Screen capture requires Android 5.0+");
            finish();
            return;
        }

        // If we already have a pre-granted intent, no need to ask again.
        if (App.hasScreenCaptureIntent()) {
            Log.d(TAG, "Screen capture intent already granted, finishing");
            finish();
            return;
        }

        final MediaProjectionManager mpm =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mpm == null) {
            Log.w(TAG, "MediaProjectionManager not available");
            finish();
            return;
        }

        startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Log.d(TAG, "MediaProjection permission granted - starting screen capture");
                // Stash it too (available for a later remote mid-flight start via
                // App.hasScreenCaptureIntent(), same as before) in case the immediate start below
                // doesn't end up consuming it for some reason - startFPVStreamingServiceScreen()
                // clears this the moment it's actually used, so no double-use risk either way.
                App.sScreenCaptureIntent = data;
                // Kick the (likely already-running, whether camera-streaming or idling)
                // FPVStreamingService immediately with this grant. onStartCommand() swaps the
                // capture source in place if a PeerConnectionManager already exists, or does a
                // fresh start otherwise - either way nothing else would trigger this on its own.
                App.startFPVStreamingServiceScreen(data);
            } else {
                Log.d(TAG, "MediaProjection permission denied");
            }
        }
        finish();
    }
}
