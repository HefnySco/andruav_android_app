package ap.andruav_ap.activities.remote;

import android.app.Activity;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.DeviceManagerFacade;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_RemoteEngaged_CMD;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RemoteControlTestFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RemoteControlTestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RemoteControlTestFragment extends Fragment implements IFragmentSave{
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

    protected RemoteControlWidget mRemoteControlWidget;
    private TextView mReadings;

    /////////// EOF Attributes


    //////////BUS EVENT

    public void onEvent(Event_RemoteEngaged_CMD event_remoteEngaged_cmd) {

        final Message msg = mhandle.obtainMessage();
        msg.obj = event_remoteEngaged_cmd;

        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);
    }

//    public void onEvent(EventRemote_ChannelsCMD eventRemote_channelsCMD) {
//        final Message msg = mhandle.obtainMessage();
//        msg.obj = eventRemote_channelsCMD;
//
//        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);
//    }

    ///EOF Event BUS




    /***
     * Event to UI gate to enable access UI safely.
     */
    private void UIHandler () {
        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);


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
    public static RemoteControlTestFragment newInstance(String param1, String param2) {
        RemoteControlTestFragment fragment = new RemoteControlTestFragment();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
       View v =  inflater.inflate(R.layout.fragment_remote_control_test, container, false);

        if (v.isInEditMode()) return v;
        mReadings = v.findViewById(R.id.remotecontrolactivity_readings);

        mRemoteControlWidget = v.findViewById(R.id.remotecontrolactivity_remotecontrolwidget);
        if (DeviceManagerFacade.hasMultitouch())
        {
            mRemoteControlWidget.updateSettings();
        }


        return v;
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

        if (DeviceManagerFacade.hasMultitouch() == false)
        {
            DialogHelper.doModalDialog(Me, getString(R.string.title_activity_remotecontrol), getString(R.string.err_feature_multitouch), null, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Me.finish();
                }
            });
        }
        mRemoteControlWidget.updateSettings();

    }

    @Override
    public void onPause()
    {
        EventBus.getDefault().unregister(this);


        super.onPause();

    }


    public void OnRemoteEngaged(Event_RemoteEngaged_CMD event_remoteEngaged_cmd) {

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
