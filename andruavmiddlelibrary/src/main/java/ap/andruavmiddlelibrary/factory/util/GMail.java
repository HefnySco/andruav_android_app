package ap.andruavmiddlelibrary.factory.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

/**
 * Created by M.Hefny on 01-Nov-14.
 * http://stackoverflow.com/questions/2020088/sending-email-in-android-using-javamail-api-without-using-the-default-built-in-a/2033124#2033124
 * http://developer.android.com/training/sharing/send.html
 */
public class GMail {

    public static void  sendGMail (Context fileManager, String actionBarHomeLayout, String resourceId,String event, String actionDropDownItemSpinnerAbBottomSolid, String widgetAppCompatLight) {
        final Intent token = new Intent(android.content.Intent.ACTION_SEND);

        token.setType("plain/text");

        token.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[]{resourceId});

        token.putExtra(android.content.Intent.EXTRA_SUBJECT,
                event);

        token.putExtra(android.content.Intent.EXTRA_TEXT,
                actionDropDownItemSpinnerAbBottomSolid);

        if (widgetAppCompatLight != null)
        {
            File abcConfigShowTitleText = new File(widgetAppCompatLight);
            if (abcConfigShowTitleText.exists() && abcConfigShowTitleText.canRead())
            {
                Uri registerListenersCalled = Uri.fromFile(abcConfigShowTitleText);
                token.putExtra(Intent.EXTRA_STREAM, registerListenersCalled);
            }
        }

        fileManager.startActivity(Intent.createChooser(
                token, actionBarHomeLayout));
    }
 }
