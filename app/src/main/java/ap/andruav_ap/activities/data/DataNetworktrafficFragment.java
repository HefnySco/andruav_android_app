package ap.andruav_ap.activities.data;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.fragment.app.Fragment;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.androidplot.pie.PieChart;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.androidplot.ui.widget.TextLabelWidget;
import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.andruav.AndruavEngine;
import java.util.Arrays;
import ap.andruav_ap.App;
import ap.andruav_ap.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DataNetworktrafficFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DataNetworktrafficFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DataNetworktrafficFragment extends Fragment {


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;


    //////// Attributes
    View MeView;
    private Boolean killme = false;
    private Handler mhandle;
    private static final String NO_SELECTION_TXT = "Touch bar to select.";
    private PieChart pieChart;
    private Segment TX_Bytes;
    private Segment RX_Bytes;
    private TextView txtTXRX;


    private BarFormatter formatter1;
    private BarFormatter formatter2;
    private BarFormatter selectionFormatter;

    private TextLabelWidget selectionWidget;

    private Pair<Integer, XYSeries> selection;


    private SimpleXYSeries seriesTXData;
    private SimpleXYSeries seriesRXData;
    private SimpleXYSeries rLvlSeries;

    private SimpleXYSeries Ping_Series;
    private LineAndPointFormatter Ping_SeriesFormat;
    /////////// EOF Attributes

    //////////BUS EVENT



    private final Runnable ScheduledTasks = new Runnable() {
        @Override
        public void run() {
      /* do what you need to do */
            if (AndruavEngine.getAndruavWS() != null) {
                updateTXRX();
            }

            if (!killme) {
                mhandle.postDelayed(this, 5000);
            }
        }
    };

    private void updateTXRX()
    {
        double TX_Text    = AndruavEngine.getAndruavWS()==null? 0 : AndruavEngine.getAndruavWS().TotalBytesSent;
        double RX_Text    = AndruavEngine.getAndruavWS()==null? 0 : AndruavEngine.getAndruavWS().TotalBytesRecieved;
        double TX_Binary    = AndruavEngine.getAndruavWS()==null? 0 : AndruavEngine.getAndruavWS().TotalBinaryBytesSent;
        double RX_Binary   = AndruavEngine.getAndruavWS()==null? 0 : AndruavEngine.getAndruavWS().TotalBinaryBytesRecieved;
        double Ping  = AndruavEngine.getAndruavWS()==null? 0 : AndruavEngine.getAndruavWS().LastPing;

        int RX_TextPackets = AndruavEngine.getAndruavWS()==null? 0 : AndruavEngine.getAndruavWS().TotalPacketsRecieved;
        int TX_TextPackets= AndruavEngine.getAndruavWS()==null? 0 : AndruavEngine.getAndruavWS().TotalPacketsSent;

        int RX_BinaryPackets = AndruavEngine.getAndruavWS()==null? 0 : AndruavEngine.getAndruavWS().TotalBinaryPacketsRecieved;
        int TX_BinaryPackets= AndruavEngine.getAndruavWS()==null? 0 : AndruavEngine.getAndruavWS().TotalBinaryPacketsSent;

        int RX_TotalPackets = RX_TextPackets + RX_BinaryPackets;
        int TX_TotalPackets = TX_TextPackets + TX_BinaryPackets;

        double RX_TotalBytes = RX_Text + RX_Binary;
        double TX_TotalBytes = TX_Text + TX_Binary;

        TX_Bytes.setValue(TX_TotalBytes);
        RX_Bytes.setValue(RX_TotalBytes);
        pieChart.redraw();
        String text = "<font color=#75A4D3>TX: " + String.format("%10.2f",TX_TotalBytes/1024.0)
                + " KB<br>packets:" + TX_TotalPackets
                + "</font><br><font color=#D375D3>RX: " + String.format("%10.2f",RX_TotalBytes/1024.0)
                + "KB<br>packets:" + RX_TotalPackets
                + "</font><br><br><font color=#36AB36>Ping Time: " + String.format("%6.0f ms",Ping)
                + "</font>";
        txtTXRX.setText(Html.fromHtml(text));


      /*
        Iterator<XYSeries> iterator1 = plot.getSeriesSet().iterator();

        while(iterator1.hasNext()) {
            XYSeries setElement = iterator1.next();
            plot.removeSeries(setElement);
        }

        Number[] seriesTest = {TX_Text,RX_Text};
        Number[] seriesBinary= {TX_Binary,RX_Binary};

        seriesTXData = new SimpleXYSeries(Arrays.asList(seriesTest), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "TX");
        seriesRXData = new SimpleXYSeries(Arrays.asList(seriesBinary), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "RX");
       // seriesRXData = new SimpleXYSeries(Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Them");

        plot.addSeries(seriesTXData, formatter1);
        plot.redraw();
        */
    }


    private void UIHandler() {
        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
               super.handleMessage(msg);

            }
        };
    }

    private void initGUI ()
    {
        drawPie();
       // initBarChartTraffic();
        txtTXRX = MeView.findViewById(R.id.dataactivity_txtTXRX);
        final Button btnReset = MeView.findViewById(R.id.fragment_data_network_btnReset);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AndruavEngine.getAndruavWS() == null) return;
                AndruavEngine.getAndruavWS().resetDataCounters();
                mhandle.post(ScheduledTasks);
            }
        });
        updateTXRX();


    }

    private void drawPie() {
        pieChart = MeView.findViewById(R.id.dataactivity_piechart);

        pieChart.getBackgroundPaint().setColor(Color.WHITE);

        TX_Bytes = new Segment(" ", 0.0);
        RX_Bytes = new Segment(" ", 10);
        TX_Bytes.setTitle("TX");
        RX_Bytes.setTitle("RX");

        pieChart.addSeries(TX_Bytes, new SegmentFormatter(getResources().getColor(R.color.btn_TXT_BLUE), Color.BLACK, Color.BLACK, Color.BLACK));
        pieChart.addSeries(RX_Bytes, new SegmentFormatter(getResources().getColor(R.color.btn_TXT_MAGENTA), Color.BLACK, Color.BLACK, Color.BLACK));

        pieChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //showPopup();

            }
        });


    }




    private void initBarChartTraffic()
    {
        //plot = (XYPlot) MeView.findViewById(R.id.mySimpleXYPlot);

        formatter1 = new BarFormatter(Color.argb(200, 100, 150, 100), Color.LTGRAY);
        formatter2 = new BarFormatter(Color.argb(200, 100, 100, 150), Color.LTGRAY);


        //selectionFormatter = new BarFormatter(Color.YELLOW, Color.WHITE);

       /* selectionWidget = new TextLabelWidget(plot.getLayoutManager(), NO_SELECTION_TXT,
                new SizeMetrics(
                        PixelUtils.dpToPix(100), SizeLayoutType.ABSOLUTE,
                        PixelUtils.dpToPix(100), SizeLayoutType.ABSOLUTE),
                TextOrientationType.HORIZONTAL);

        selectionWidget.getLabelPaint().setTextSize(PixelUtils.dpToPix(16));
*/
        // add a dark, semi-transparent background to the selection label widget:




    }





    private void drawPlot() {

        // Create a couple arrays of y-values to plot:
        Number[] series1Numbers = {1, 8, 5, 2, 7, 4};
        Number[] series2Numbers = {4, 6, 3, 8, 2, 10};

        // Turn the above arrays into XYSeries':
        XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers),          // SimpleXYSeries takes a List so turn our array into a List
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
                "Series1");                             // Set the display title of the series

        // same as above
        XYSeries series2 = new SimpleXYSeries(Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series2");

        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
        LineAndPointFormatter series1Format = new LineAndPointFormatter();
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        series1Format.configure(App.context.getApplicationContext(),
                R.xml.line_point_formatter_with_plf1);

    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DataNetworktrafficFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DataNetworktrafficFragment newInstance(String param1, String param2) {
        DataNetworktrafficFragment fragment = new DataNetworktrafficFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public DataNetworktrafficFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
           // mParam1 = getArguments().getString(ARG_PARAM1);
          //  mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        MeView = inflater.inflate(R.layout.fragment_data_networktraffic, container, false);

        // initialize our XYPlot reference:
        UIHandler();
        initGUI();



        return  MeView;
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
    public void onResume() {
        super.onResume();
        killme = false;
        mhandle.postDelayed(ScheduledTasks, 1);

       // drawPlot();

    }

    @Override
    public void onPause() {
        super.onPause();
        killme = true;
        mhandle.removeCallbacksAndMessages(null);
    }

    @Override
    public void onStop ()
    {
        super.onStop();
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
