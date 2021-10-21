package com.andruav.protocol.commands.textMessages;

import com.andruav.controlBoard.shared.missions.MissionBase;
import com.andruav.controlBoard.shared.missions.MissionDayra;
import com.andruav.controlBoard.shared.missions.MissionEkla3;
import com.andruav.controlBoard.shared.missions.MissionHoboot;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.controlBoard.shared.missions.MissionRTL;
import com.andruav.controlBoard.shared.missions.SplineMission;
import com.andruav.controlBoard.shared.missions.WayPointStep;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Created by mhefny on 7/28/16.
 */
public class AndruavMessage_WayPoints extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_WayPoints = 1027;

    protected MohemmaMapBase mMohemmaMapBase;

    public AndruavMessage_WayPoints() {
        messageTypeID = TYPE_AndruavMessage_WayPoints;
    }

    public MohemmaMapBase getWayPoints() {
        return mMohemmaMapBase;
    }


    public void setWayPoints(MohemmaMapBase mohemmaMapBase) {
        mMohemmaMapBase = mohemmaMapBase;
    }


    @Override
    public void setMessageText(String messageText) throws  JSONException, ParseException {

        JSONObject json_receive_data = new JSONObject(messageText);

        mMohemmaMapBase = new MohemmaMapBase();

        MissionBase missionBase;

        final NumberFormat nf = NumberFormat.getInstance(Locale.US);

        final int numberOfRecords = json_receive_data.getInt("n");

        for (int i = 0; i < numberOfRecords; ++i) {

            JSONObject jobj = json_receive_data.getJSONObject(String.valueOf(i));

            final int waypointType = jobj.getInt("t");
            switch (waypointType )
            {
                case WayPointStep.TYPE_WAYPOINTSTEP:
                    final WayPointStep wayPointStep = new WayPointStep();
                    wayPointStep.Sequence       =  jobj.getInt("s");
                    wayPointStep.Latitude       =  nf.parse(jobj.getString("a")).doubleValue();
                    wayPointStep.Longitude      =  nf.parse(jobj.getString("g")).doubleValue();
                    wayPointStep.Altitude       =  nf.parse(jobj.getString("l")).doubleValue();
                    wayPointStep.Heading        =  nf.parse(jobj.getString("h")).floatValue();
                    wayPointStep.TimeToStay     =  nf.parse(jobj.getString("y")).doubleValue();

                    missionBase = wayPointStep;
                    break;

                case SplineMission.TYPE_SPLINE_WAYPOINT:
                    final SplineMission splineMohemma = new SplineMission();
                    splineMohemma.Sequence       =  jobj.getInt("s");
                    splineMohemma.Latitude       =  nf.parse(jobj.getString("a")).doubleValue();
                    splineMohemma.Longitude      =  nf.parse(jobj.getString("g")).doubleValue();
                    splineMohemma.Altitude       =  nf.parse(jobj.getString("l")).doubleValue();
                    splineMohemma.TimeToStay     =  nf.parse(jobj.getString("y")).doubleValue();

                    missionBase = splineMohemma;
                    break;

                case MissionDayra.TYPE_DAYRA:
                    final MissionDayra mohemmaDayra = new MissionDayra();
                    mohemmaDayra.Sequence       =  jobj.getInt("s");
                    mohemmaDayra.Latitude       =  nf.parse(jobj.getString("a")).doubleValue();
                    mohemmaDayra.Longitude      =  nf.parse(jobj.getString("g")).doubleValue();
                    mohemmaDayra.Altitude       =  nf.parse(jobj.getString("l")).doubleValue();
                    mohemmaDayra.Radius         =  nf.parse(jobj.getString("r")).floatValue();
                    mohemmaDayra.Turns          =  nf.parse(jobj.getString("n")).doubleValue();

                    missionBase = mohemmaDayra;
                    break;

                case MissionEkla3.TYPE_EKLA3:
                    final MissionEkla3 mohemmaEkla3 = new MissionEkla3();
                    mohemmaEkla3.Sequence       =  jobj.getInt("s");
                    mohemmaEkla3.setAltitude(nf.parse(jobj.getString("l")).doubleValue());
                    mohemmaEkla3.setPitch(nf.parse(jobj.getString("p")).doubleValue());


                    missionBase = mohemmaEkla3;
                    break;

                case MissionHoboot.TYPE_HOBOOT:
                    final MissionHoboot mohemmaHoboot = new MissionHoboot();
                    mohemmaHoboot.Sequence       =  jobj.getInt("s");


                    missionBase = mohemmaHoboot;
                    break;

                case MissionRTL.TYPE_RTL:
                    final MissionRTL mohemmaRTL = new MissionRTL();
                    mohemmaRTL.Sequence       =  jobj.getInt("s");

                    missionBase = mohemmaRTL;
                    break;

                default:
                    missionBase = new MissionBase();
                    missionBase.Sequence       =  jobj.getInt("s");
                    break;
            }


            mMohemmaMapBase.put(String.valueOf(missionBase.Sequence), missionBase);

        }

        missionBase = null;
    }

    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        final int numberOfRecords = mMohemmaMapBase.size();


        for (int i = 0; i < numberOfRecords; ++i) {

            final MissionBase missionBase = mMohemmaMapBase.valueAt(i);
            final JSONObject json_record = new JSONObject();

            switch (missionBase.MohemmaTypeID)
            {
                case WayPointStep.TYPE_WAYPOINTSTEP:
                    final WayPointStep wayPointStep= (WayPointStep) missionBase;

                    json_record.accumulate("t",WayPointStep.TYPE_WAYPOINTSTEP);
                    json_record.accumulate("s",wayPointStep.Sequence);
                    json_record.accumulate("a",String.format(Locale.US, "%4.6f", wayPointStep.Latitude));
                    json_record.accumulate("g",String.format(Locale.US, "%4.6f", wayPointStep.Longitude));
                    json_record.accumulate("l",String.format(Locale.US, "%4.6f", wayPointStep.Altitude));
                    json_record.accumulate("h",String.format(Locale.US, "%4.6f", wayPointStep.Heading));
                    json_record.accumulate("y",String.format(Locale.US, "%4.6f", wayPointStep.TimeToStay));

                    break;

                case SplineMission.TYPE_SPLINE_WAYPOINT:
                    final SplineMission splineMohemma= (SplineMission) missionBase;

                    json_record.accumulate("t", SplineMission.TYPE_SPLINE_WAYPOINT);
                    json_record.accumulate("s",splineMohemma.Sequence);
                    json_record.accumulate("a",String.format(Locale.US, "%4.6f", splineMohemma.Latitude));
                    json_record.accumulate("g",String.format(Locale.US, "%4.6f", splineMohemma.Longitude));
                    json_record.accumulate("l",String.format(Locale.US, "%4.6f", splineMohemma.Altitude));
                    json_record.accumulate("y",String.format(Locale.US, "%4.6f", splineMohemma.TimeToStay));

                    break;

                case MissionDayra.TYPE_DAYRA:
                    final MissionDayra mohemmaDayra= (MissionDayra) missionBase;

                    json_record.accumulate("t", MissionDayra.TYPE_DAYRA);
                    json_record.accumulate("s",mohemmaDayra.Sequence);
                    json_record.accumulate("a",String.format(Locale.US, "%4.6f", mohemmaDayra.Latitude));
                    json_record.accumulate("g",String.format(Locale.US, "%4.6f", mohemmaDayra.Longitude));
                    json_record.accumulate("l",String.format(Locale.US, "%4.6f", mohemmaDayra.Altitude));
                    json_record.accumulate("r",String.format(Locale.US, "%4.6f", mohemmaDayra.Radius));
                    json_record.accumulate("n",mohemmaDayra.Turns);

                    break;

                case MissionEkla3.TYPE_EKLA3:
                    final MissionEkla3 mohemmaEkla3 = (MissionEkla3) missionBase;
                    json_record.accumulate("t", MissionEkla3.TYPE_EKLA3);
                    json_record.accumulate("s",mohemmaEkla3.Sequence);
                    json_record.accumulate("l",String.format(Locale.US, "%4.6f", mohemmaEkla3.getAltitude()));
                    json_record.accumulate("p",String.format(Locale.US, "%4.6f", mohemmaEkla3.getPitch()));

                    break;

                case MissionRTL.TYPE_RTL:
                    final MissionRTL mohemmaRTL = (MissionRTL) missionBase;
                    json_record.accumulate("t", MissionRTL.TYPE_RTL);
                    json_record.accumulate("s",mohemmaRTL.Sequence);

                    break;

                case MissionHoboot.TYPE_HOBOOT:
                    final MissionHoboot mohemmaHoboot = (MissionHoboot) missionBase;
                    json_record.accumulate("t", MissionHoboot.TYPE_HOBOOT);
                    json_record.accumulate("s",mohemmaHoboot.Sequence);

                    break;

                case MissionBase.TYPE_UNKNOWN:
                    json_record.accumulate("t", MissionBase.TYPE_UNKNOWN);
                    json_record.accumulate("s", missionBase.Sequence);
                    break;
            }


            json_data.accumulate(String.valueOf(i),json_record);

        }
        json_data.accumulate("n",numberOfRecords);
        return json_data.toString();
    }

}
