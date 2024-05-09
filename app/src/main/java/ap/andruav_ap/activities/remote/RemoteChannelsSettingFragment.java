package ap.andruav_ap.activities.remote;

import android.app.Activity;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.andruav.AndruavFacade;
import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.controlBoard.ControlBoardBase;
import com.andruav.util.Maths;

import ap.andruav_ap.helpers.RemoteControl;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruav_ap.R;
import ap.andruav_ap.widgets.ChannelSettingsWidget;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RemoteChannelsSettingFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RemoteChannelsSettingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RemoteChannelsSettingFragment extends Fragment implements IFragmentSave{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private OnFragmentInteractionListener mListener;
    private boolean mloaded = false;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RemoteChannelsSettingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RemoteChannelsSettingFragment newInstance(String param1, String param2) {
        RemoteChannelsSettingFragment fragment = new RemoteChannelsSettingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    final ChannelSettingsWidget[] remoteChannelSetting = new ChannelSettingsWidget[8];

    View Me;

    private void initGUI ()
    {


        mloaded = true;

        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_2_PITCH] = Me.findViewById(R.id.rcsettingsactivity_ch_Pitch);
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_1_ROLL]  = Me.findViewById(R.id.rcsettingsactivity_ch_Roll);
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_4_YAW]  = Me.findViewById(R.id.rcsettingsactivity_ch_YAW);
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_3_THROTTLE]  = Me.findViewById(R.id.rcsettingsactivity_ch_Throttle);
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_5_AUX1]  = Me.findViewById(R.id.rcsettingsactivity_ch5_AUX1);
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_6_AUX2]  = Me.findViewById(R.id.rcsettingsactivity_ch6_AUX2);
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_7_AUX3]  = Me.findViewById(R.id.rcsettingsactivity_ch7_AUX3);
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_8_AUX4]  = Me.findViewById(R.id.rcsettingsactivity_ch8_AUX4);


        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_2_PITCH].setChannelName("Pitch");
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_1_ROLL].setChannelName("Roll");
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_4_YAW].setChannelName("Yaw");
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_3_THROTTLE].setChannelName("Throttle");
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_5_AUX1].setChannelName("AUX1");
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_6_AUX2].setChannelName("AUX2");
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_7_AUX3].setChannelName("AUX3");
        remoteChannelSetting[ControlBoardBase.CONST_CHANNEL_8_AUX4].setChannelName("AUX4");

        readRemoteSettings();


    }




    private boolean readRemoteSettings()
    {
        for (int i=0;i<8;i=i+1)
        {
            remoteChannelSetting[i].setMaxValue(Preference.getChannelmaxValue(null, i));
            remoteChannelSetting[i].setMinValue(Preference.getChannelminValue(null,i));
            remoteChannelSetting[i].setIsReturnToCenter(Preference.isChannelReturnToCenter(null,i));
            remoteChannelSetting[i].setIsReverse(Preference.isChannelReversed(null,i));
            remoteChannelSetting[i].setDRRatioValue(Preference.getChannelDRValues(null, i));
        }

        return true;

    }

    private boolean saveRemoteSettings()
    {
        if (!validateChannelValues()) return false;

        for (int i=0;i<8;i=i+1)
        {
            Preference.setChannelmaxValue(null, i, remoteChannelSetting[i].getMaxValue());
            Preference.setChannelminValue(null, i, remoteChannelSetting[i].getMinValue());
            Preference.isChannelReturnToCenter(null, i, remoteChannelSetting[i].getIsReturnToCenter());
            Preference.isChannelReversed(null, i, remoteChannelSetting[i].getIsReverse());
            Preference.setChannelDRValues(null, i, remoteChannelSetting[i].getDRRatioValue());
        }

        if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
            RemoteControl.loadDualRates();
            AndruavSettings.andruavWe7daBase.setRTC(Preference.isChannelReturnToCenter(null));
            AndruavFacade.sendRemoteControlSettingsMessage(Preference.isChannelReturnToCenter(null),null);
        }
        else {
            RemoteControl.loadRTC();
        }

        Toast.makeText(this.getActivity(),getString(R.string.action_saved),Toast.LENGTH_SHORT).show();
        return true;
    }


    private Boolean validateChannelValues ()
    {

        boolean res = true;
        for (int j=0;j<4;++j)
        {
            int min = remoteChannelSetting[j].getMinValue();
            int max = remoteChannelSetting[j].getMaxValue();


            int mincorrected = (int) Maths.Constraint(Constants.Default_RC_MIN_VALUE,min,Constants.Default_RC_MID_LVALUE);
            int maxcorrected = (int) Maths.Constraint(Constants.Default_RC_MID_HVALUE,max,Constants.Default_RC_MAX_VALUE);

            if ((min != mincorrected) || (max != maxcorrected))
            {
                res = false;

                remoteChannelSetting[j].setBackgroundColor(getResources().getColor(R.color.btn_TXT_ERROR));
                remoteChannelSetting[j].setMinValue(mincorrected);
                remoteChannelSetting[j].setMaxValue(maxcorrected);

            }
        }

        if (!res)
        {
            DialogHelper.doModalDialog(this.getActivity(), getString(R.string.actionremote_settings), getString(R.string.err_remote_range_adjusted), null);
        }

        for (int i=4;i<8;i=i+1) {
            int min = remoteChannelSetting[i].getMinValue();
            int max = remoteChannelSetting[i].getMaxValue();
            if (min < Constants.Default_RC_MIN_VALUE) {
                min = Constants.Default_RC_MIN_VALUE;
            }
            if (max > Constants.Default_RC_MAX_VALUE)
            {
                max = Constants.Default_RC_MAX_VALUE;

            }
            if (min >= max)
            {
                // value range is inverted
                DialogHelper.doModalDialog(this.getActivity(),getString(R.string.actionremote_settings),getString(R.string.err_remote_min_max),null);
                return false;
            }
            if (max-min < 200)
            {
                DialogHelper.doModalDialog(this.getActivity(), getString(R.string.actionremote_settings), getString(R.string.err_remote_min_max_range), null);
                return false;
            }
        }


        return res;
    }

    public RemoteChannelsSettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        super.onCreateView(inflater,container,savedInstanceState);

        Me =  inflater.inflate(R.layout.fragment_remote_channels_setting, container, false);

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
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity
                    + " must implement OnFragmentInteractionListener");
        }
    }




    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    }
