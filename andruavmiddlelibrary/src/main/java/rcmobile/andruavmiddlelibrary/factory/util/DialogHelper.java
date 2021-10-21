package rcmobile.andruavmiddlelibrary.factory.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import rcmobile.andruavmiddlelibrary.R;


/**
 * Created by M.Hefny on 01-Oct-14.
 */
public class DialogHelper {

    public static void doModalDialog (Context context, CharSequence title, CharSequence message, String okMessage){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);

        if (okMessage == null) okMessage = context.getString(android.R.string.ok);
        builder.setPositiveButton(okMessage, null);

        builder.show();
    }

    public static void doModalDialog (Context context, CharSequence title, CharSequence message, CharSequence okMessage, DialogInterface.OnClickListener onClick){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);

        if (okMessage == null) okMessage = context.getString(android.R.string.ok);
        builder.setPositiveButton(okMessage, onClick);
        builder.show();
    }

    public static void doModalDialog (Context context, CharSequence title, CharSequence message
            , CharSequence okText, DialogInterface.OnClickListener okListener
            , CharSequence noText, DialogInterface.OnClickListener NoListener){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);

        if (okText == null) okText = context.getString(android.R.string.ok);
        builder.setPositiveButton(okText, okListener);
        if (noText == null) noText = context.getString(android.R.string.no);
        builder.setNegativeButton(noText, NoListener);
        builder.show();
    }


    public static ProgressDialog doModalProgressDialog (final Context context, final String title, final String msg)
    {
        final ProgressDialog dialog = new ProgressDialog(context); // this = YourActivity
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle(title);
        dialog.setMessage(msg);
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        return  dialog;
    }

    public interface DialogReminderPreferenceCallBack
    {
        boolean readPreference();

        void writePreference (boolean value);

        void onDismiss();
        void onCancel();
        void onOK();
    }

    public static void doModalCustomDialogReminder (Context context, CharSequence title, CharSequence message, String okMessage,final  DialogReminderPreferenceCallBack callBack){

        if (!callBack.readPreference()) return ;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater adbInflater = LayoutInflater.from(context);
        View viewDlg = adbInflater.inflate(R.layout.dialog_reminder,null);
        builder.setView(viewDlg);
        builder.setTitle(title);
        builder.setMessage(message);

        CheckBox chkReminder = viewDlg.findViewById(R.id.dialog_reminder_chkskip);
        //chkReminder.setChecked(callBack.readPreference());
        chkReminder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                callBack.writePreference(!isChecked);
            }
        });
        if (okMessage == null) okMessage = context.getString(android.R.string.ok);
        builder.setPositiveButton(okMessage, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callBack.onOK();
            }
        });

        builder.show();
    }

}