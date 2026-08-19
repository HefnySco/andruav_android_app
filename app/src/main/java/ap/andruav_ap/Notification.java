package ap.andruav_ap;

import org.greenrobot.eventbus.Subscribe;

/*
  Created by M.Hefny on 07-Oct-14.
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

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.NotificationCompat;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import com.andruav.AndruavEngine;
import com.andruav.interfaces.INotification;

import java.util.Random;

import org.greenrobot.eventbus.EventBus;
import ap.andruav_ap.activities.main.MainScreen;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import ap.andruavmiddlelibrary.factory.tts.TTS;


public  class Notification implements INotification{

    public static final String CHANNEL_ID = "andruav_notifications";
    /** High-importance channel used for the full-screen FPV-start notification (Android 14+). */
    public static final String CHANNEL_ID_FPV_URGENT = "andruav_fpv_urgent";
    /** Notification / PendingIntent request code for the FPV full-screen intent. */
    public static final int FPV_URGENT_NOTIFICATION_ID = INotification.INFO_TYPE_CAMERA;

    final long SPEEK_MIN_TIME = 500;
    Random rnd = new Random();
    NotificationManager mNotificationManager;
    Context context;
    long last_speek_time = 0;

    @Subscribe
    public void onEvent (final Event_ShutDown_Signalling event)
    {
        if (event.CloseOrder != 4) return ;


        this.shutDown();

    }

    public void shutDown()
    {
        EventBus.getDefault().unregister(this);
        AndruavEngine.setNotificationHandler(null);
        Cancel(INotification.INFO_TYPE_TELEMETRY);
        Cancel(INotification.INFO_TYPE_PROTOCOL);
        Cancel(INotification.INFO_TYPE_CAMERA);
        Cancel(INotification.INFO_TYPE_KMLFILE);
    }

    public void init (final Context context) {
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Andruav Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);

            // High-importance channel for the full-screen FPV-start notification. On Android 14+
            // a camera/microphone foreground service cannot be started from the background, so a
            // remote FPV request posts a full-screen notification here to bring the app to the
            // foreground first; the actual service start then happens from the resumed Activity.
            NotificationChannel fpvChannel = new NotificationChannel(CHANNEL_ID_FPV_URGENT,
                    "Andruav FPV Requests", NotificationManager.IMPORTANCE_HIGH);
            fpvChannel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            mNotificationManager.createNotificationChannel(fpvChannel);
        }

        EventBus.getDefault().register(this);
    }

    public void displayNotification (final Spanned title, final Spanned text, final boolean Sound, final int Id, final boolean isPresistant) {
        displayNotification(title.toString(), text.toString(), Sound, Id, isPresistant);

    }


    public void displayNotification(String title, String text, boolean Sound, int Id, boolean isPresistant) {

        displayNotification(R.drawable.ic_logo2, title, text, Sound, Id, isPresistant);
    }


    /*
      Send simple notification using the NotificationCompat API.
     http://javatechig.com/android/android-notification-example-using-notificationcompat
    public void sendNotification(View view) {

        // Use NotificationCompat.Builder to set up our notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        //icon appears in device notification bar andpublic void Speak (String message); right hand corner of notification
        builder.setSmallIcon(R.drawable.ic_stat_notification);

        // This intent is fired when notification is clicked
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://javatechig.com/"));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Set the intent that will fire when the user taps the notification.
        builder.setContentIntent(pendingIntent);

        // Large icon appears on the left of the notification
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

        // Content title, which appears in large type at the top of the notification
        builder.setContentTitle("Notifications Title");

        // Content text, which appears in smaller text below the title
        builder.setContentText("Your notification content here.");

        // The subtext, which appears under the text on newer devices.
        // This will show-up in the devices with Android 4.2 and above only
        builder.setSubText("Tap to view documentation about notifications.");

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Will display the notification in the notification bar
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}*/


    /***
     *
     * @param sevirity contains values such as  {@link INotification#NOTIFICATION_TYPE_NORMAL}
     * @return
     */
    public int getAndruavLogo (final int sevirity)
    {
        int smallLogo;
        switch (sevirity)
        {
            case NOTIFICATION_TYPE_ERROR:
                smallLogo = R.drawable.logo_red_32x32;
                break;
            case NOTIFICATION_TYPE_WARNING:
                smallLogo = R.drawable.logo_yellow_32x32;
                break;
            case NOTIFICATION_TYPE_NORMAL:
                smallLogo = R.drawable.logo_32x32;
                break;
            default:
                smallLogo = R.drawable.logo_32x32;
                break;
        }

        return smallLogo;
    }


    public void displayNotification(final int sevirity, final String title, final String text, final boolean Sound, final int Id, final boolean isPresistant) {

        displayNotificationwithLogo(getAndruavLogo(sevirity), Html.fromHtml(title), Html.fromHtml(text), Sound, Id, isPresistant);
    }

    public void displayNotification(final int sevirity, final Spanned title, final Spanned text, final boolean Sound, final int Id, final boolean isPresistant) {

        displayNotificationwithLogo(getAndruavLogo(sevirity), title, text, Sound, Id, isPresistant);
    }


    public void displayNotificationwithLogo(final int smallLogo, final Spanned title, final Spanned text, final boolean Sound, int Id, final boolean isPresistant) {
        if (Id == 0) {
            Id = NOTIFICATION_TYPE_GENERIC;
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(smallLogo).setContentTitle(title).setContentText(text);
        if (Sound)
            mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        // mBuilder.setOnlyAlertOnce(false);
        mBuilder.setTicker(title + ":" + text);
        mBuilder.setOngoing(isPresistant);
        mBuilder.setAutoCancel(true);

        Intent notificationIntent = new Intent(context, MainScreen.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        mBuilder.setContentIntent(contentIntent);

        mNotificationManager.notify(Id, mBuilder.build());
    }


    /**
     * Posts a high-priority notification with a full-screen intent to bring the app to the
     * foreground. Used when a remote FPV/video request arrives while the app is backgrounded:
     * Android 14+ forbids starting a camera/microphone foreground service from the background,
     * so the app must be foregrounded first. If the screen is on the notification appears as a
     * heads-up banner; if the screen is off the full-screen intent launches directly. Tapping
     * the notification (or the full-screen launch) brings the app to the foreground, where the
     * resumed Activity re-posts the pending FPV event and starts the service safely.
     */
    public void displayFullScreenNotificationForFPV() {
        // Use the launcher intent so the correct home screen (MainScreen or ModuleScreen) is
        // shown, rather than hard-coding one.
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchIntent == null) {
            launchIntent = new Intent(context, MainScreen.class);
        }
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent fullScreenIntent = PendingIntent.getActivity(context, 0, launchIntent, pendingFlags);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID_FPV_URGENT)
                .setSmallIcon(R.drawable.ic_logo2)
                .setContentTitle("Andruav")
                .setContentText("Remote video request - tap to start streaming")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenIntent, true)
                .setAutoCancel(true)
                .setContentIntent(fullScreenIntent);

        mNotificationManager.notify(FPV_URGENT_NOTIFICATION_ID, mBuilder.build());
    }


    /***
     * Canel a notification in Main toolbar
     * @param Id
     */
    public void Cancel(int Id) {
        mNotificationManager.cancel(Id);
    }

    @Override
    public void Speak(final String message) {
        long now = System.currentTimeMillis();
        if ((now - last_speek_time) < SPEEK_MIN_TIME)
        {
            return ;
        }
        last_speek_time = now;
        SpeakNow(message);
    }


    @Override
    public void SpeakNow(final String message) {
        TTS.getInstance().Speak(message);
    }

    /***
     * cancel all notifications in toolbar.
     */
    public void CancelAll()
    {
        mNotificationManager.cancelAll();
    }




    public void showSnack (final int sevitiry, final String title, final String text) {

        final Activity act = App.activeActivity;
        if (act == null)
        {
            return;
        }

        View v = act.findViewById(android.R.id.content);
        String stext = "";

        if ((text != null) && !text.isEmpty()) {
            stext = "<b>" + title + "</b> "; //<br>";
        }
        stext += text;

        showSnack(v,sevitiry,Html.fromHtml(stext));
    }


    public void showSnack (final int sevitiry,final Spanned text)
    {
        final Activity act = App.activeActivity;
        if (act == null)
        {
            return;
        }
        View v = act.findViewById(android.R.id.content);
        showSnack(v,sevitiry,text);
    }

    public void showSnack (final View v,final int sevitiry,final String text) {

        showSnack(v,sevitiry,Html.fromHtml(text));

    }

    public void showSnack (final View v,final int sevitiry,final Spanned text)
    {

        Snackbar snackbar = Snackbar.make(v.findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG);

        View snackbarLayout = snackbar.getView();
        TextView textView = snackbarLayout.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setCompoundDrawablesWithIntrinsicBounds(getAndruavLogo(sevitiry), 0, 0, 0);
        snackbar.show();
    }
}
