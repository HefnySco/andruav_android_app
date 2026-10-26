package ap.andruavmiddlelibrary.factory.tts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;

import java.io.File;
import java.util.HashMap;

import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Created by M.Hefny on 07-Oct-14.
 */
public class SoundManager {


    public final static int SND_ERR =1;
    public final static int SND_EMERGENCY =2;
    public final static float HIGHEST_VOLUME =1.0f;


    private final SoundPool mSoundPool;
    private final HashMap<Integer, Integer> mSoundPoolMap;
    private final AudioManager mAudioManager;
    private final Context mContext;
    private int mSirenIndex =-1;
    private boolean mEnabled;

    public SoundManager(Context theContext) {
        mContext = theContext;
        mSoundPool =  new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        mSoundPoolMap = new HashMap<Integer, Integer>();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        // Preference.isSoundEnabled(null) would read Android's *default* prefs file, while the
        // home-screen toggle writes via an Activity context into the app's named "RCMOBILE_FPV"
        // prefs file - passing a null context here would silently desync the two, always
        // defaulting back to enabled on every restart.
        mEnabled = (mContext instanceof android.content.ContextWrapper)
                ? Preference.isSoundEnabled((android.content.ContextWrapper) mContext)
                : Preference.isSoundEnabled(null);

    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(final boolean enabled) {
        mEnabled = enabled;
        if (!mEnabled) {
            final SoundDispatch.Worker worker = SoundDispatch.get();
            if (worker != null) {
                worker.onStopAll();
            } else {
                stopSiren();
            }
        }
    }

    public void addSound(final int Index, final int SoundID) {
        mSoundPoolMap.put(Index, mSoundPool.load(mContext, SoundID, 1));
    }

    /***
     * SoundPool index -> "sound" clip key for the worker path: index 0 is the
     * short beep, {@link #SND_ERR} the error tone, {@link #SND_EMERGENCY} the siren.
     */
    private static String clipKeyForIndex(final int index) {
        switch (index) {
            case 0:              return "beep";
            case SND_ERR:        return "error";
            case SND_EMERGENCY:  return "siren";
            default:             return null;
        }
    }

    public void playSound(final int index) {

        final SoundDispatch.Worker worker = SoundDispatch.get();
        if (worker != null) {
            final String clipKey = clipKeyForIndex(index);
            if (clipKey != null) worker.onClip(clipKey, false, false, SoundDispatch.SRC_GCS);
            return;
        }

        if (!mEnabled) return;

        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mSoundPool.play(mSoundPoolMap.get(index), streamVolume, streamVolume, 1, 0, 1f);
    }

    /***
     *
     * @param index index of sound in mSoundPoolMap
     * @param volume from 0 to 1.0f
     * @return index of playing sound. this is used to STOP it. this is NOT the input {@param index}.
     */
    public int playLoopedSound(final int index, final float volume, final boolean force) {
        if (!mEnabled && !force) return -1;

        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,(int) (volume * mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);

        return  mSoundPool.play(mSoundPoolMap.get(index), volume, volume, 1, -1, 1f);
    }

    public int playLoopedSound(final int index, final boolean force) {

         if (!mEnabled && !force) return -1; // should we add this here ??

        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        return playLoopedSound (index, streamVolume, force);
    }

    public void stopLoopedSound (final int index)
    {
        mSoundPool.stop(index);
    }


    public void playSiren ()
    {
        final SoundDispatch.Worker worker = SoundDispatch.get();
        if (worker != null) {
            // The DE sound worker owns the siren clip now; force keeps the
            // emergency siren audible when the home-screen sound is muted.
            worker.onClip("siren", true, true, SoundDispatch.SRC_GCS);
            return;
        }

        if (mSirenIndex != -1)
        {
            return;
        }

        mSirenIndex = playLoopedSound(SoundManager.SND_EMERGENCY, SoundManager.HIGHEST_VOLUME, true);

        AndruavSettings.andruavWe7daBase.setIsWhisling(true);

    }


    public boolean isSirenOn ()
    {
        final SoundDispatch.Worker worker = SoundDispatch.get();
        if (worker != null)
        {
            return worker.isClipPlaying("siren");
        }
        return  (mSirenIndex != -1);
    }
    public void stopSiren ()
    {
        final SoundDispatch.Worker worker = SoundDispatch.get();
        if (worker != null)
        {
            worker.onStopClip("siren");
            return;
        }

        if (mSirenIndex == -1)
        {
            return;
        }

        mSoundPool.stop(mSirenIndex);
        AndruavSettings.andruavWe7daBase.setIsWhisling(false);

        mSirenIndex=-1;
    }


    /***
     * Plays a sound file by NAME from the app's external files dir ("sounds").
     * <br>The name is resolved inside that directory only: the canonical path must stay
     * under it, so path traversal ("../") and absolute paths are rejected.
     * <br>With the DE sound provider running the request is enqueued on its worker
     * (src "gcs") so a library file emits sound.&lt;clip&gt;.started/finished facts.
     */
    public void playFile (final String fileName)
    {
        final SoundDispatch.Worker worker = SoundDispatch.get();
        if (worker != null) {
            worker.onPlayFile(fileName, SoundDispatch.SRC_GCS);
            return;
        }

        if (!mEnabled) return;
        if ((fileName == null) || fileName.isEmpty()) return;

        try {
            final File dir = mContext.getExternalFilesDir("sounds");
            if (dir == null) return;

            final File dirCanonical = dir.getCanonicalFile();
            final File file = new File(dirCanonical, fileName).getCanonicalFile();
            if (!file.getPath().startsWith(dirCanonical.getPath() + File.separator)) {
                Log.w(AndruavEngine.getPreference().TAG(), "Sound_PlayFile rejected path: " + fileName);
                return;
            }

            final MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC).build());
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(final MediaPlayer mp) {
                    mp.release();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(final MediaPlayer mp, final int what, final int extra) {
                    mp.release();
                    return true;
                }
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("SoundManager", ex);
        }
    }

}
