package ap.andruav_ap.denative.providers.phone;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;

import com.andruav.AndruavEngine;
import com.andruav.event.fpv7adath.Event_FPV_CMD;

import ap.andruav_ap.App;

/**
 * {@code phone.torch} driver. The camera torch has exactly one owner at
 * a time: while FPVStreamingService runs, WebRTC owns the camera and the
 * torch goes through {@code PeerConnectionManager.setFlash} - reached by
 * posting the same {@link Event_FPV_CMD#FPV_CMD_FLASHCAM} event the GCS
 * camera-flash command uses. Otherwise the torch is driven directly via
 * {@link CameraManager#setTorchMode} on the back camera's flash unit.
 *
 * {@code strobe_hz > 0} turns a steady "on" into a blink on the main
 * looper (TO-BE section 5.2 torch note).
 */
final class TorchDriver implements PhoneOutput.Driver {

    private final Context m_context;
    private final double m_strobe_hz;
    private final Handler m_handler = new Handler(Looper.getMainLooper());

    private boolean m_blink_lit;
    private Runnable m_blink_task;
    // bumped by every stop: a blink step from an older run must not toggle
    // or re-post itself (apply() may run off the main looper)
    private volatile int m_blink_gen;

    TorchDriver(final Context context, final double strobe_hz) {
        this.m_context = context;
        this.m_strobe_hz = strobe_hz;
    }

    @Override
    public String apply(final boolean on) {
        _stop_blink();
        if (!on) return _set_torch(false);
        if (m_strobe_hz > 0) {
            _start_blink();
            return null;
        }
        return _set_torch(true);
    }

    private synchronized void _start_blink() {
        final long half_period_ms = Math.max(40L, (long) (500.0 / m_strobe_hz));
        final int gen = m_blink_gen;
        m_blink_task = new Runnable() {
            @Override
            public void run() {
                synchronized (TorchDriver.this) {
                    if (gen != m_blink_gen) return;
                }
                m_blink_lit = !m_blink_lit;
                _set_torch(m_blink_lit);
                m_handler.postDelayed(this, half_period_ms);
            }
        };
        m_handler.post(m_blink_task);
    }

    private synchronized void _stop_blink() {
        m_blink_gen++;
        if (m_blink_task != null) {
            m_handler.removeCallbacks(m_blink_task);
            m_blink_task = null;
        }
    }

    private String _set_torch(final boolean on) {
        if (App.isFPVStreamingServiceRunning()) {
            // WebRTC holds the camera: setTorchMode would collide with the
            // capture session, so go through the streaming service instead.
            final Event_FPV_CMD cmd = new Event_FPV_CMD(Event_FPV_CMD.FPV_CMD_FLASHCAM);
            cmd.ACT = on;
            AndruavEngine.getEventBus().post(cmd);
            return null;
        }
        try {
            final CameraManager camera_manager =
                    (CameraManager) m_context.getSystemService(Context.CAMERA_SERVICE);
            final String camera_id = _flash_camera_id(camera_manager);
            if (camera_id == null) return "no camera with a flash unit";
            camera_manager.setTorchMode(camera_id, on);
            return null;
        } catch (Exception e) {
            return "torch: " + e;
        }
    }

    private static String _flash_camera_id(final CameraManager camera_manager)
            throws CameraAccessException {
        if (camera_manager == null) return null;
        String fallback = null;
        for (final String id : camera_manager.getCameraIdList()) {
            try {
                final CameraCharacteristics c = camera_manager.getCameraCharacteristics(id);
                final Boolean has_flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (!Boolean.TRUE.equals(has_flash)) continue;
                final Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) return id;
                if (fallback == null) fallback = id;
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }
}
