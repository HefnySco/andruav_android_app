package rcmobile.andruavmiddlelibrary.factory.tts;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.andruav.AndruavSettings;

import java.util.HashMap;

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

    public SoundManager(Context theContext) {
        mContext = theContext;
        mSoundPool =  new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        mSoundPoolMap = new HashMap<Integer, Integer>();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

    }

    public void addSound(final int Index, final int SoundID) {
        mSoundPoolMap.put(Index, mSoundPool.load(mContext, SoundID, 1));
    }

    public void playSound(final int index) {

        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mSoundPool.play(mSoundPoolMap.get(index), streamVolume, streamVolume, 1, 0, 1f);
    }

    /***
     *
     * @param index index of sound in mSoundPoolMap
     * @param volume from 0 to 1.0f
     * @return index of playing sound. this is used to STOP it. this is NOT the input {@param index}.
     */
    public int playLoopedSound(final int index, final float volume) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,(int) (volume * mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);

        return  mSoundPool.play(mSoundPoolMap.get(index), volume, volume, 1, -1, 1f);
    }

    public int playLoopedSound(final int index) {

        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        return playLoopedSound (index, streamVolume);
    }

    public void stopLoopedSound (final int index)
    {
        mSoundPool.stop(index);
    }


    public void playSiren ()
    {
        if (mSirenIndex != -1)
        {
            return;
        }
        mSirenIndex = playLoopedSound(SoundManager.SND_EMERGENCY, SoundManager.HIGHEST_VOLUME);

        AndruavSettings.andruavWe7daBase.setIsWhisling(true);

    }


    public boolean isSirenOn ()
    {
        return  (mSirenIndex != -1);
    }
    public void stopSiren ()
    {
        if (mSirenIndex == -1)
        {
            return;
        }

        mSoundPool.stop(mSirenIndex);
        AndruavSettings.andruavWe7daBase.setIsWhisling(false);

        mSirenIndex=-1;
    }

}
