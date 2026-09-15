package ap.andruavmiddlelibrary.factory.tts;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.andruav.AndruavEngine;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Created by M.Hefny on 16-Sep-14.
 */

/*  MultiWii EZ-ActivityMosa3ed
    Copyright (C) <2012>  Bartosz Szczygiel (eziosoft)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class TTS implements TextToSpeech.OnInitListener {
    private static TTS mTTS;
    private TextToSpeech tts;
    public static final int TTS_CHECK_CODE = 2345;
    final Context context;
    public boolean TTSinit = false;
    private boolean initialized = false;
    private String text;
    public  boolean muteTTS = false;
    // Persistent, user-controlled mute (home screen speaker toggle) - distinct from muteTTS,
    // which is only ever flipped transiently around programmatic UI updates.
    private boolean mSoundEnabled = true;

    /***
     * Repeated-message throttling: minimum gap before the same text is spoken again,
     * the ceiling that gap grows to, and the idle time after which a text is forgotten.
     */
    private static final long REPEAT_MIN_TIME     = 10000;
    private static final long REPEAT_MAX_TIME     = 120000;
    private static final long REPEAT_FORGET_TIME  = 60000;
    private static final int  REPEAT_MAX_TRACKED  = 16;

    private static final class RepeatState {
        long lastSpokenTime;
        long lastRequestTime;
        long interval = REPEAT_MIN_TIME;
    }

    /*** LRU of the last {@link #REPEAT_MAX_TRACKED} spoken messages. */
    private final LinkedHashMap<String, RepeatState> mRepeatStates =
            new LinkedHashMap<String, RepeatState>(REPEAT_MAX_TRACKED, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, RepeatState> eldest) {
                    return size() > REPEAT_MAX_TRACKED;
                }
            };

    private void CreateTTS() {
        Log.d(AndruavEngine.getPreference().TAG(), "CreateTTS");

        if (!initialized) {

            tts = new TextToSpeech(context, this);

            initialized = true;

        }
    }


    public static TTS getInstance ()
    {
        if (mTTS==null) {
            mTTS = new TTS(AndruavEngine.getPreference().getContext());
        }
        return mTTS;
    }

    private TTS(Context context) {
        this.context = context;
        mSoundEnabled = (context instanceof android.content.ContextWrapper)
                ? Preference.isSoundEnabled((android.content.ContextWrapper) context)
                : Preference.isSoundEnabled(null);
        CreateTTS();
        Log.d(AndruavEngine.getPreference().TAG(), "text to speach init TTSinit " + TTSinit);
    }

    public boolean isSoundEnabled() {
        return mSoundEnabled;
    }

    public void setSoundEnabled(final boolean enabled) {
        mSoundEnabled = enabled;
    }

    @Override
    public void onInit(int status) {
         try
         {
            if (status == TextToSpeech.SUCCESS) {

                int result = tts.setLanguage(Locale.ENGLISH);
                Log.d(AndruavEngine.getPreference().TAG(), Locale.getDefault().getLanguage());
                if (Locale.getDefault().getLanguage().equals("de")) {
                    result = tts.setLanguage(Locale.getDefault());
                    Log.d(AndruavEngine.getPreference().TAG(), "german");
                }

                if (Locale.getDefault().getLanguage().equals("hu")) {
                    result = tts.setLanguage(Locale.getDefault());
                    Log.d(AndruavEngine.getPreference().TAG(), "hungarian");
                }

                if (Locale.getDefault().getLanguage().equals("pl")) {
                    result = tts.setLanguage(Locale.getDefault());
                    Log.d(AndruavEngine.getPreference().TAG(), "polish");
                }

                //Log.e(AndruavMo7arek.getPreference().TAG(), "This Language is not supported");

                TTSinit = true;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGreater21(text);
                } else {
                    ttsUnder20(this.text);
                }
                } else {
                TTSinit = false;
            }

        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("TTS", ex);
        }
    }

    /***
     * Speaks a message, throttling repetitions of the SAME text.
     * <br>A message that keeps arriving - e.g. "Connection Lost!" while the socket is
     * retrying - is spoken once, then repeated after {@link #REPEAT_MIN_TIME}, and the
     * gap doubles on every repetition up to {@link #REPEAT_MAX_TIME}, so a persistent
     * failure is announced at a decreasing rate instead of on every single event.
     * <br>Use {@link #SpeakNow(String)} for messages that must never be dropped.
     * @return true if the message was actually handed to the TTS engine.
     */
    public boolean Speak(final String text) {

        if (muteTTS || !mSoundEnabled) return false;
        if (text == null) return false;
        if (!shouldSpeak(text)) return false;

        if (!SpeakNow(text))
        {
            // Engine not ready yet: do not let this attempt consume a repetition slot.
            forgetSpoken(text);
            return false;
        }

        return true;
    }


    /***
     * Speaks a message immediately, bypassing the repeated-message throttling.
     * @return true if the message was actually handed to the TTS engine.
     */
    public boolean SpeakNow(final String text) {

        try {
            if (muteTTS || !mSoundEnabled) return false;
            if (text == null) return false;

            Log.d(AndruavEngine.getPreference().TAG(), "Speak:" + text);
            if (TTSinit) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGreater21(text);
                } else {
                    ttsUnder20(text);
                }
                return true;
            }
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("TTS", ex);
        }

        return false;
    }


    /***
     * Progressive back-off for repeated identical messages.
     * <br>A text that has not been requested for {@link #REPEAT_FORGET_TIME} is
     * forgotten, so once the condition clears the next occurrence is spoken at once.
     */
    private synchronized boolean shouldSpeak (final String text)
    {
        final long now = System.currentTimeMillis();

        RepeatState state = mRepeatStates.get(text);

        if ((state == null) || ((now - state.lastRequestTime) > REPEAT_FORGET_TIME))
        {
            // First time, or the message stopped repeating long enough to start over.
            state = new RepeatState();
            state.lastRequestTime = now;
            state.lastSpokenTime  = now;
            mRepeatStates.put(text, state);
            return true;
        }

        state.lastRequestTime = now;

        if ((now - state.lastSpokenTime) < state.interval)
        {
            return false;
        }

        state.lastSpokenTime = now;
        state.interval = Math.min(state.interval * 2, REPEAT_MAX_TIME);

        return true;
    }


    private synchronized void forgetSpoken (final String text)
    {
        mRepeatStates.remove(text);
    }



    @SuppressWarnings("deprecation")
    private void ttsUnder20(final String text) {
       tts.speak(text, TextToSpeech.QUEUE_ADD, null);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(final String text) {
        String utteranceId=this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId);
    }


}