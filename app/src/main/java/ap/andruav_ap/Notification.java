package ap.andruav_ap;

/**
 * Created by M.Hefny on 07-Oct-14.
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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.NotificationCompat;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import com.andruav.AndruavEngine;
import com.andruav.interfaces.INotification;

import java.util.Random;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.activities.main.MainScreen;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import ap.andruavmiddlelibrary.factory.tts.TTS;


public  class Notification implements INotification{

    
    Random rnd = new Random();
    NotificationManager mNotificationManager;
    Context context;


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
        EventBus.getDefault().register(this);
    }

    public void displayNotification (final Spanned title, final Spanned text, final boolean Sound, final int Id, final boolean isPresistant) {
        displayNotification(title.toString(), text.toString(), Sound, Id, isPresistant);

    }


    public void displayNotification(String title, String text, boolean Sound, int Id, boolean isPresistant) {

        displayNotification(R.drawable.ic_logo2, title, text, Sound, Id, isPresistant);
    }


    /**
     * Send simple notification using the NotificationCompat API.
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
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setSmallIcon(smallLogo).setContentTitle(title).setContentText(text);
        if (Sound)
            mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        // mBuilder.setOnlyAlertOnce(false);
        mBuilder.setTicker(title + ":" + text);
        mBuilder.setOngoing(isPresistant);
        mBuilder.setAutoCancel(true);

        Intent notificationIntent = new Intent(context, MainScreen.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(contentIntent);

        mNotificationManager.notify(Id, mBuilder.build());
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
