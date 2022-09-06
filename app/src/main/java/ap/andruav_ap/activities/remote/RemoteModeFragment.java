package ap.andruav_ap.activities.remote;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruav_ap.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RemoteModeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RemoteModeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RemoteModeFragment extends Fragment implements IFragmentSave {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    protected ImageButton[] mImageButtons = new ImageButton[4];
    protected View MeView;
    protected RemoteModeFragment Me;
    protected int mMode;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RemoteModeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RemoteModeFragment newInstance(String param1, String param2) {
        RemoteModeFragment fragment = new RemoteModeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    ImageButton.OnClickListener onC = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mImageButtons[Me.mMode-1].setBackgroundResource(R.drawable.button_shap);
            Me.mMode = Integer.parseInt(view.getTag().toString());
            mImageButtons[Me.mMode-1].setBackgroundResource(R.drawable.button_active);

        }
    };

    private void initGUI ()
    {
        mImageButtons[0] = MeView.findViewById(R.id.fragment_remote_mode_btnMode1);
        mImageButtons[0].setTag(1);
        mImageButtons[1] = MeView.findViewById(R.id.fragment_remote_mode_btnMode2);
        mImageButtons[1].setTag(2);
        mImageButtons[2] = MeView.findViewById(R.id.fragment_remote_mode_btnMode3);
        mImageButtons[2].setTag(3);
        mImageButtons[3]  = MeView.findViewById(R.id.fragment_remote_mode_btnMode4);
        mImageButtons[3].setTag(4);

        mImageButtons[0].setOnClickListener(onC);
        mImageButtons[1].setOnClickListener(onC);
        mImageButtons[2].setOnClickListener(onC);
        mImageButtons[3].setOnClickListener(onC);

        readRemoteSettings();


    }

    protected boolean readRemoteSettings()
    {
        Me.mMode = Preference.getRemoteFlightMode(null);
        for (int i=0;i<4;i=i+1)
        {
            mImageButtons[i].setBackgroundResource(R.drawable.button_shap);
        }
        mImageButtons[mMode-1].setBackgroundResource(R.drawable.button_shape_pressed);

        return true;
    }

    protected boolean saveRemoteSettings()
    {

        Preference.setRemoteFlightMode(null,mMode);
        Toast.makeText(this.getActivity(), getString(R.string.action_saved), Toast.LENGTH_SHORT).show();

        return true;
    }

    public RemoteModeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        Me = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        MeView= inflater.inflate(R.layout.fragment_remote_mode, container, false);

        initGUI();
        return MeView;
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
        return readRemoteSettings();
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
