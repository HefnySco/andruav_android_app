package ap.andruav_ap.activities.remote;

import android.app.Activity;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.andruav.Constants;
import com.andruav.util.Maths;

import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Created by mhefny on 2/28/17.
 */

public class RemoteChannelsTrackingSettings extends Fragment implements IFragmentSave {



    private EditText edtX_P;
    private EditText edtX_I;
    private EditText edtX_D;
    private EditText edtY_P;
    private EditText edtY_I;
    private EditText edtY_D;


    public static RemoteChannelsTrackingSettings newInstance(String param1, String param2) {
        RemoteChannelsTrackingSettings fragment = new RemoteChannelsTrackingSettings();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    private View Me;
    private boolean mloaded = false;
    private RemoteChannelsTrackingSettings.OnFragmentInteractionListener mListener;

    private void initGUI () {

        mloaded = true;

        edtX_P = Me.findViewById(R.id.remote_tracking_fragement_edt_X_P);
        edtX_I = Me.findViewById(R.id.remote_tracking_fragement_edt_X_I);
        edtX_D = Me.findViewById(R.id.remote_tracking_fragement_edt_X_D);

        edtY_P = Me.findViewById(R.id.remote_tracking_fragement_edt_Y_P);
        edtY_I = Me.findViewById(R.id.remote_tracking_fragement_edt_Y_I);
        edtY_D = Me.findViewById(R.id.remote_tracking_fragement_edt_Y_D);

        readRemoteSettings();

    }



    private boolean readRemoteSettings ()
    {


        edtX_P.setText(String.valueOf(Preference.getTrackingValue(null, Constants.CONST_TRACKING_INDEX_X_P)));
        edtX_I.setText(String.valueOf(Preference.getTrackingValue(null, Constants.CONST_TRACKING_INDEX_X_I)));
        edtX_D.setText(String.valueOf(Preference.getTrackingValue(null, Constants.CONST_TRACKING_INDEX_X_D)));

        edtY_P.setText(String.valueOf(Preference.getTrackingValue(null, Constants.CONST_TRACKING_INDEX_Y_P)));
        edtY_I.setText(String.valueOf(Preference.getTrackingValue(null, Constants.CONST_TRACKING_INDEX_Y_I)));
        edtY_D.setText(String.valueOf(Preference.getTrackingValue(null, Constants.CONST_TRACKING_INDEX_Y_D)));

        return true;
    }


    private boolean isBadValue(final EditText edt, int maxValue)
    {
        try {
            final String value = edt.getText().toString();
            int myNum = Integer.parseInt(value);
            int validValue = (int)Maths.Constraint(0,myNum,maxValue);
            if (validValue!=myNum)
            {
                edt.setText(String.valueOf(validValue));
                edt.setBackgroundColor(getResources().getColor(R.color.btn_TXT_ERROR));
              return true;
            }

            return false;

        } catch(NumberFormatException nfe) {
            System.out.println("Could not parse " + nfe);
            edt.setBackgroundColor(getResources().getColor(R.color.btn_TXT_ERROR));
            return true;
        }
    }

    private boolean validateValues ()
    {
        boolean bad = false;
        bad = bad || isBadValue(edtX_P, 300);
        bad = bad || isBadValue(edtX_I, 300);
        bad = bad || isBadValue(edtX_D, 300);
        bad = bad || isBadValue(edtY_P, 300);
        bad = bad || isBadValue(edtY_I, 300);
        bad = bad || isBadValue(edtY_D, 300);

        return !bad;
    }

    private boolean saveRemoteSettings()
    {

        if (!validateValues ())
        {
            DialogHelper.doModalDialog(this.getActivity(), getString(R.string.actionremote_settings), getString(R.string.err_remote_range_adjusted), null);

            return false ;
        }

        Preference.setTrackingValue(null, Constants.CONST_TRACKING_INDEX_X_P,edtX_P.getText().toString());
        Preference.setTrackingValue(null, Constants.CONST_TRACKING_INDEX_X_I,edtX_I.getText().toString());
        Preference.setTrackingValue(null, Constants.CONST_TRACKING_INDEX_X_D,edtX_D.getText().toString());

        Preference.setTrackingValue(null, Constants.CONST_TRACKING_INDEX_Y_P,edtY_P.getText().toString());
        Preference.setTrackingValue(null, Constants.CONST_TRACKING_INDEX_Y_I,edtY_I.getText().toString());
        Preference.setTrackingValue(null, Constants.CONST_TRACKING_INDEX_Y_D,edtY_D.getText().toString());

        return true;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        super.onCreateView(inflater,container,savedInstanceState);

        Me =  inflater.inflate(R.layout.fragment_remote_channels_tracking_setting, container, false);

        return Me;

    }



    @Override
    public void onStart()
    {
        super.onStart();

        //http://stackoverflow.com/questions/13303469/edittext-settext-not-working-with-fragment
        // BUG: if you move this to onCreateView you will find text bix is corrupted.
        initGUI();



    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (RemoteChannelsTrackingSettings.OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }




    @Override
    public void onDetach() {
        super.onDetach();
        //mListener = null;
    }

    @Override
    public boolean Save() {
        return saveRemoteSettings();


    }

    @Override
    public boolean Refresh() {
        DialogHelper.doModalDialog(this.getActivity(), getString(R.string.actionremote_settings)
                , getString(R.string.conf_Refresh), null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Preference.FactoryReset_RC(null);
                        readRemoteSettings();
                    }
                },null,null);

        return true;

    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


}
