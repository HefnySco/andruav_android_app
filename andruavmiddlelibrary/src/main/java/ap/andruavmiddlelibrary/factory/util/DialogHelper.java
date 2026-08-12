package ap.andruavmiddlelibrary.factory.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import ap.andruavmiddlelibrary.R;


/**
 * Created by M.Hefny on 01-Oct-14.
 */
public class DialogHelper {

    /***
     * Builds the shared dark "magnetic" card dialog (see dialog_magnetic_modal.xml) used
     * by all doModalDialog() overloads, with 1 or 2 buttons wired up and dismiss-on-click.
     */
    private static AlertDialog buildMagneticDialog (final Context context, final CharSequence title, final CharSequence message,
                                                      final CharSequence positiveText, final DialogInterface.OnClickListener positiveListener,
                                                      final boolean showNegative, final CharSequence negativeText, final DialogInterface.OnClickListener negativeListener){

        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_magnetic_modal, null);
        ((TextView) view.findViewById(R.id.magneticdlg_txtTitle)).setText(title);
        ((TextView) view.findViewById(R.id.magneticdlg_txtMessage)).setText(message);

        final AlertDialog dialog = new AlertDialog.Builder(context).setView(view).setCancelable(true).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final Button btnPositive = view.findViewById(R.id.magneticdlg_btnPositive);
        btnPositive.setText(positiveText == null ? context.getString(android.R.string.ok) : positiveText);
        btnPositive.setOnClickListener(v -> {
            if (positiveListener != null) positiveListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
            dialog.dismiss();
        });

        if (showNegative) {
            final Button btnNegative = view.findViewById(R.id.magneticdlg_btnNegative);
            btnNegative.setVisibility(View.VISIBLE);
            btnNegative.setText(negativeText == null ? context.getString(android.R.string.no) : negativeText);
            btnNegative.setOnClickListener(v -> {
                if (negativeListener != null) negativeListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
                dialog.dismiss();
            });
        }

        return dialog;
    }

    public static void doModalDialog (Context context, CharSequence title, CharSequence message, String okMessage){

        buildMagneticDialog(context, title, message, okMessage, null, false, null, null).show();
    }

    public static void doModalDialog (Context context, CharSequence title, CharSequence message, CharSequence okMessage, DialogInterface.OnClickListener onClick){

        buildMagneticDialog(context, title, message, okMessage, onClick, false, null, null).show();
    }

    public static void doModalDialog (Context context, CharSequence title, CharSequence message
            , CharSequence okText, DialogInterface.OnClickListener okListener
            , CharSequence noText, DialogInterface.OnClickListener NoListener){

        buildMagneticDialog(context, title, message, okText, okListener, true, noText, NoListener).show();
    }


    public static Dialog doModalProgressDialog (final Context context, final String title, final String msg)
    {
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_magnetic_progress, null);
        ((TextView) view.findViewById(R.id.magneticprogress_txtTitle)).setText(title);
        ((TextView) view.findViewById(R.id.magneticprogress_txtMessage)).setText(msg);

        final AlertDialog dialog = new AlertDialog.Builder(context).setView(view).setCancelable(false).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
        return dialog;
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

        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_magnetic_reminder, null);
        ((TextView) view.findViewById(R.id.magneticreminder_txtTitle)).setText(title);
        ((TextView) view.findViewById(R.id.magneticreminder_txtMessage)).setText(message);

        final AlertDialog dialog = new AlertDialog.Builder(context).setView(view).setCancelable(true).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final CheckBox chkReminder = view.findViewById(R.id.magneticreminder_chkskip);
        chkReminder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                callBack.writePreference(!isChecked);
            }
        });

        final Button btnPositive = view.findViewById(R.id.magneticreminder_btnPositive);
        btnPositive.setText(okMessage == null ? context.getString(android.R.string.ok) : okMessage);
        btnPositive.setOnClickListener(v -> {
            callBack.onOK();
            dialog.dismiss();
        });

        dialog.show();
    }

}