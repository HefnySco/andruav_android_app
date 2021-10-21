package rcmobile.FPV.activities.remote;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
import rcmobile.FPV.R;
import rcmobile.android.valueBar.ValueBar;
import rcmobile.andruavmiddlelibrary.eventClasses.remoteControl.Event_RemoteEngaged_CMD;
import rcmobile.andruavmiddlelibrary.factory.util.DialogHelper;
import rcmobile.andruavmiddlelibrary.preference.Preference;

/**
 * Created by mhefny on 5/26/16.
 */
public class RemoteControlGamePadTestFragment extends Fragment implements IFragmentSave {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;


    //////// Attributes
    Activity Me;
    private Handler mhandle;
    private final int maxChannels =8;
    private final float[] curValues = new float[maxChannels];
    private final float[] minValues = new float[maxChannels];
    private final float[] maxValues = new float[maxChannels];

    private final int currentChannel=0;


    private final ValueBar[] valueBars = new ValueBar[maxChannels] ;
    private final TextView[] txtChannelNames = new TextView[maxChannels];
    private Button btnDone;
    private Button btnCalibrate;
    private final String[] channelNames = {"AIL", "ELE", "THR", "RUD", "AUX1", "AUX2", "AUX3", "AUX4"};

    /////////// EOF Attributes


    //////////BUS EVENT

    public void onEvent(Event_RemoteEngaged_CMD event_remoteEngaged_cmd) {

        final Message msg = mhandle.obtainMessage();
        msg.obj = event_remoteEngaged_cmd;

        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);
    }

    ///EOF Event BUS




    /***
     * Event to UI gate to enable access UI safely.
     */
    private void UIHandler () {
        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.obj instanceof Event_RemoteEngaged_CMD)
                {
                    OnRemoteEngaged((Event_RemoteEngaged_CMD)msg.obj);
                }
            }
        };


    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RemoteControlTestFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RemoteControlGamePadTestFragment newInstance(String param1, String param2) {
        RemoteControlGamePadTestFragment fragment = new RemoteControlGamePadTestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // Me = this;

        // getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);




        UIHandler();

    }

    private void initGUI( final View v)
    {
        btnCalibrate = v.findViewById(R.id.gamepad_valuebar_btn_calibrate);
        btnCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i< maxChannels; i = i + 1)
                {
                    maxValues[i]=0.0f;
                    minValues[i]=0.0f;
                    curValues[i]=0.0f;
                }
                btnDone.setVisibility(View.VISIBLE);
                displayValues();
            }
        });

        btnDone = v.findViewById(R.id.gamepad_valuebar_btn_next);
        btnDone.setVisibility(View.INVISIBLE);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateChannellNames()) {
                    savePreference();
                    btnDone.setVisibility(View.INVISIBLE);
                }
                else {
                    DialogHelper.doModalDialog(getActivity(),"Error","Some channels are repeated. Please check red channel labels",null);
                }
            }
        });

        txtChannelNames[0] = v.findViewById(R.id.gamepad_txt_channelname1);
        txtChannelNames[1] = v.findViewById(R.id.gamepad_txt_channelname2);
        txtChannelNames[2] = v.findViewById(R.id.gamepad_txt_channelname3);
        txtChannelNames[3] = v.findViewById(R.id.gamepad_txt_channelname4);
        txtChannelNames[4] = v.findViewById(R.id.gamepad_txt_channelname5);
        txtChannelNames[5] = v.findViewById(R.id.gamepad_txt_channelname6);
        txtChannelNames[6] = v.findViewById(R.id.gamepad_txt_channelname7);
        txtChannelNames[7] = v.findViewById(R.id.gamepad_txt_channelname8);



        valueBars[0] = v.findViewById(R.id.gamepad_valuebar_channel1);
        valueBars[1] = v.findViewById(R.id.gamepad_valuebar_channel2);
        valueBars[2] = v.findViewById(R.id.gamepad_valuebar_channel3);
        valueBars[3] = v.findViewById(R.id.gamepad_valuebar_channel4);
        valueBars[4] = v.findViewById(R.id.gamepad_valuebar_channel5);
        valueBars[5] = v.findViewById(R.id.gamepad_valuebar_channel6);
        valueBars[6] = v.findViewById(R.id.gamepad_valuebar_channel7);
        valueBars[7] = v.findViewById(R.id.gamepad_valuebar_channel8);

        for (int i=0; i<8; ++i)
        {
            valueBars[i].setMinMax(1.0f,2200.0f);
            valueBars[i].setInterval(10.0f); // interval in which can be selected
            valueBars[i].setDrawBorder(false);
            valueBars[i].setTouchEnabled(false);
            valueBars[i].setTag(txtChannelNames[i]);

            curValues[i] = (maxValues[i]+minValues[i]) /2;    // initalize bars
            valueBars[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Assign channel to it.
                    final TextView t = (TextView)   v.getTag();
                    int j = (int) t.getTag();
                    j = j + 1; if (j == 8) j = 0;
                    t.setTag(j);
                    t.setText(channelNames[j]);
                    validateChannellNames();
                }
            });
            txtChannelNames[i].setTag(i);
            txtChannelNames[i].setText(channelNames[i]);
            txtChannelNames[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int j = (int) v.getTag();
                    j = j + 1; if (j == 8) j = 0;
                    v.setTag(j);
                    ((TextView)v).setText(channelNames[j]);
                    validateChannellNames();
                }
            });
        }





        displayValues ();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_remote_control_gamepad, container, false);

        if (v.isInEditMode()) return v;


        loadPreference();

        initGUI(v);


        return v;
    }


    private void loadPreference()
    {
        for (int i = 0 ; i < 8; i = i + 1) {
            maxValues[i] = Preference.getGamePadChannelmaxValue(null,i);
            minValues[i] = Preference.getGamePadChannelminValue(null,i);
        }
    }

    private void savePreference ()
    {
        for (int i = 0 ; i < 8; i = i + 1) {
            Preference.setGamePadChannelmaxValue(null,i, maxValues[i]);
            Preference.setGamePadChannelminValue(null,i, minValues[i]);
        }
    }

    private boolean validateChannellNames()
    {
        boolean res = true;
        for (int i = 0; i< maxChannels; ++i)
        {
            txtChannelNames[i].setTextColor(getResources().getColor(R.color.btn_TXT_BLUE));
        }

        for (int i = 0; i< maxChannels; ++i) {
        for (int j = i+1; j< maxChannels; ++j)
        {
            if (((int)txtChannelNames[i].getTag()) ==  ((int)txtChannelNames[j].getTag()))
            {
                txtChannelNames[i].setTextColor(getResources().getColor(R.color.btn_TXT_ERROR));
                txtChannelNames[j].setTextColor(getResources().getColor(R.color.btn_TXT_ERROR));
                res = false;
            }

        }
        }

        return res;
    }

    private void displayValues ()
    {
        for (int i = 0; i< maxChannels; ++i) {
            if (curValues[i] < minValues[i]) minValues[i] = curValues[i];
            if (curValues[i] > maxValues[i]) maxValues[i] = curValues[i];
            valueBars[i].setValue((curValues[i] - minValues[i]) * 1000.f + 200.0f);
            valueBars[i].invalidate();
        }

    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        //Log.e("gamepad", event.toString());
        // Check that the event came from a game controller
        final int action = event.getAction();

        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) ==
                InputDevice.SOURCE_JOYSTICK &&
                event.getAction() == MotionEvent.ACTION_MOVE) {


            Log.e("gamepad:AIL", String.valueOf(event.getAxisValue(MotionEvent.AXIS_X)));    // AIL
            Log.e("gamepad:PITCH", String.valueOf(event.getAxisValue(MotionEvent.AXIS_Y)));  // PITCH
            Log.e("gamepad:THROT", String.valueOf(event.getAxisValue(MotionEvent.AXIS_Z)));  // THROT
            Log.e("gamepad:RUD", String.valueOf(event.getAxisValue(MotionEvent.AXIS_RZ)));   // RUD
            Log.e("gamepad:CH5", String.valueOf(event.getAxisValue(MotionEvent.AXIS_RY)));   //
            Log.e("gamepad:GYRO", String.valueOf(event.getAxisValue(MotionEvent.AXIS_RX)));  // GYRO

            curValues[0] = event.getAxisValue(MotionEvent.AXIS_X);
            curValues[1] = event.getAxisValue(MotionEvent.AXIS_Y);
            curValues[2] = event.getAxisValue(MotionEvent.AXIS_Z);
            curValues[3] = event.getAxisValue(MotionEvent.AXIS_RZ);
            curValues[4] = event.getAxisValue(MotionEvent.AXIS_RX);
            curValues[5] = event.getAxisValue(MotionEvent.AXIS_RY);

           displayValues();
            return true;
        }

        return false;
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
            throw new ClassCastException(activity.toString()
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
        return false;
    }

    @Override
    public boolean Refresh() {
        return false;
    }



    @Override
    public void onResume ()
    {
        super.onResume();
        //setVolumeControlStream(AudioManager.STREAM_MUSIC);

        EventBus.getDefault().register(this);




    }

    @Override
    public void onPause()
    {
        EventBus.getDefault().unregister(this);


        super.onPause();

    }


    public void OnRemoteEngaged(Event_RemoteEngaged_CMD event_remoteEngaged_cmd) {

    }

    public void OnChannelChanged() {
        //String status = String.format("<font color=#7584D3>channel:<b>%d</b> value:<b>%d</b>",eventRemote_channelsCMD.singleChannelChange,eventRemote_channelsCMD.singleChannelValue);
        //txtStickLeft.setText("channel:" + eventRemote_channelsCMD.singleChannelChange +" value:"+ eventRemote_channelsCMD.singleChannelValue);

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
