package ap.andruav_ap.denative.modules;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ap.andruav_ap.denative.providers.phone.PhoneDevices;
import ap.andruav_ap.denative.providers.phone.PhoneOutput;
import ap.andruav_ap.denative.providers.sound.SoundClips;
import ap.andruav_ap.denative.providers.sound.SoundProvider;
import de.databus.CModule;

/**
 * AN-0107: runs {@link PhoneGpioModule} and {@link PhoneSoundModule} as two
 * extra databus modules next to andruav_phone (each its own CModule and
 * socket, as de_rpi_gpio and the sound module are on a Pi), so de_comm
 * advertises a "gpio" and a "snd" module for the phone in its 1004 m1 list.
 */
public final class PhoneBusModules {

    private static final String TAG = "PhoneBusModules";
    private static final long POLL_MS = 1000;
    private static final long ANNOUNCE_DELAY_MS = 2000;

    private final List<CModule> m_modules = new ArrayList<>();
    // the phone output drivers (torch blink, strobe Activity) run on the main
    // looper, like every other caller of the choke points
    private final Handler m_main = new Handler(Looper.getMainLooper());
    private ScheduledExecutorService m_timer;
    // a stop that raced the (off-main-thread) start wins
    private boolean m_stopped;

    /** Binds both modules to de_comm's s2s port; call off the main thread. */
    public synchronized void start(final int s2sPort, final String version,
                                   final SoundProvider soundProvider) {
        if (m_stopped || !m_modules.isEmpty()) return;

        final CModule gpioBus = new CModule();
        final PhoneGpioModule gpio = new PhoneGpioModule(new PhoneGpioModule.Outputs() {
            @Override
            public Boolean read(final String key) {
                final PhoneOutput out = PhoneDevices.output(key);
                return out != null ? out.isOn() : null;
            }

            @Override
            public String write(final String key, final boolean on, final String src) {
                final PhoneOutput out = PhoneDevices.output(key);
                return out != null ? out.write(on, src) : "no output " + key;
            }
        }, sender(gpioBus));
        if (!open(gpioBus, CModule.MODULE_CLASS_GPIO, PhoneGpioModule.MODULE_ID,
                PhoneGpioModule.MODULE_KEY, version, PhoneGpioModule.messageFilter(),
                gpio::onMessage, s2sPort)) {
            return;
        }

        PhoneSoundModule sound = null;
        if (soundProvider != null) {
            final CModule soundBus = new CModule();
            sound = new PhoneSoundModule(new PhoneSoundModule.Sound() {
                @Override
                public void say(final String text, final String language, final int pitch,
                                final int volume, final String src) {
                    soundProvider.sayCommanded(text, language, pitch, volume, src);
                }

                @Override
                public void playFile(final String file, final String src) {
                    soundProvider.playFileCommanded(file, src);
                }

                @Override
                public List<PhoneSoundModule.Clip> library() {
                    final List<PhoneSoundModule.Clip> clips = new ArrayList<>();
                    for (SoundClips.Surface surface : soundProvider.surfaces()) {
                        clips.add(new PhoneSoundModule.Clip(surface.name, surface.file));
                    }
                    return clips;
                }
            }, sender(soundBus));
            open(soundBus, PhoneSoundModule.MODULE_CLASS_SOUND, PhoneSoundModule.MODULE_ID,
                    PhoneSoundModule.MODULE_KEY, version, PhoneSoundModule.messageFilter(),
                    sound::onMessage, s2sPort);
        }

        final PhoneSoundModule soundModule = sound;
        m_timer = Executors.newSingleThreadScheduledExecutor();
        // first status/list once de_comm has the registrations
        m_timer.schedule(() -> m_main.post(() -> {
            gpio.announce();
            if (soundModule != null) soundModule.announce();
        }), ANNOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
        m_timer.scheduleWithFixedDelay(() -> m_main.post(gpio::pollChanges),
                ANNOUNCE_DELAY_MS + POLL_MS, POLL_MS, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        m_stopped = true;
        if (m_timer != null) {
            m_timer.shutdownNow();
            m_timer = null;
        }
        for (CModule module : m_modules) {
            try {
                module.uninit();
            } catch (Exception e) {
                Log.e(TAG, "module uninit failed", e);
            }
        }
        m_modules.clear();
    }

    private interface MessageHandler {
        void onMessage(JSONObject jMsg);
    }

    private boolean open(final CModule module, final String moduleClass, final String id,
                         final String key, final String version, final JSONArray filter,
                         final MessageHandler handler, final int s2sPort) {
        module.defineModule(moduleClass, id, key, version, filter);
        module.setOnReceive((message, len, jMsg) -> m_main.post(() -> {
            try {
                handler.onMessage(jMsg);
            } catch (Exception e) {
                Log.e(TAG, id + " message failed", e);
            }
        }));
        try {
            module.init("127.0.0.1", s2sPort, "0.0.0.0", 0, 0);
        } catch (Exception e) {
            Log.e(TAG, id + " init failed", e);
            return false;
        }
        m_modules.add(module);
        Log.i(TAG, id + " (" + moduleClass + ") on 127.0.0.1:" + s2sPort);
        return true;
    }

    private static PhoneGpioModule.Sender sender(final CModule module) {
        return (target, ms, type) -> module.sendJMSG(target, ms, type, false);
    }
}
