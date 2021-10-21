package rcmobile.FPV.activities.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.webkit.WebView;

import rcmobile.andruavmiddlelibrary.factory.io.FileHelper;

/**
 * Created by mhefny on 4/13/16.
 */
public class GenericCamWebView extends WebView {
    public GenericCamWebView(Context counterMspSetRaw, AttributeSet attributeSet, int i) {
        super(counterMspSetRaw, attributeSet, i);
    }

    public GenericCamWebView(Context mavState, AttributeSet sh) {
        super(mavState, sh);
    }

    public GenericCamWebView(Context e) {
        super(e);
    }

    /**
     * @param me
     * @param widgetAppCompatLight
     * @param mChannels
     * @param width
     * @deprecated
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public GenericCamWebView(Context me, AttributeSet widgetAppCompatLight, int mChannels, boolean width) {
        super(me, widgetAppCompatLight, mChannels, width);
    }



    public void loadPageforIPWebCam(int htmlFile, String url)
    {
        String i = FileHelper.readTextfromStream(getResources().openRawResource(htmlFile));
        i = i.replace("#####", url);

        String inputTypeAndruavError = Base64.encodeToString(i.getBytes(), Base64.DEFAULT);
        this.loadData(inputTypeAndruavError, "text/html; charset=utf-8", "base64");

    }


    public void loadPageforWebRTC(int htmlFile, String roomName)
    {
        String i = FileHelper.readTextfromStream(getResources().openRawResource(htmlFile));
        i = i.replace("#####", roomName);

        String inputTypeAndruavError = Base64.encodeToString(i.getBytes(), Base64.DEFAULT);
        this.loadData(inputTypeAndruavError, "text/html; charset=utf-8", "base64");
    }


}



