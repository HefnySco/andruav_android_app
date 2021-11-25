package ap.andruavmiddlelibrary.factory.tts;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.andruav.AndruavEngine;

import java.util.Locale;

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
    Context context;
    public boolean TTSinit = false;
    private boolean initialized = false;
    private String text;
    public  boolean muteTTS = false;

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
        CreateTTS();
        Log.d(AndruavEngine.getPreference().TAG(), "text to speach init TTSinit " + TTSinit);
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

                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    //Log.e(AndruavMo7arek.getPreference().TAG(), "This Language is not supported");
                }

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

    public void Speak(final String text) {

        try {
            if (muteTTS) return;

            Log.d(AndruavEngine.getPreference().TAG(), "Speak:" + text);
            if (TTSinit) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ttsGreater21(text);
                } else {
                    ttsUnder20(text);
                }
            }
        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("TTS", ex);
        }

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