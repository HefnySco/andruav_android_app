package com.andruav.protocol.commands.textMessages;


import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.controlBoard.shared.geoFence.GeoCylinderFenceMapBase;
import com.andruav.controlBoard.shared.geoFence.GeoFencePoint;
import com.andruav.controlBoard.shared.geoFence.GeoFenceBase;
import com.andruav.controlBoard.shared.geoFence.GeoFenceCompositBase;
import com.andruav.controlBoard.shared.geoFence.GeoFencePointNodeCylinder;
import com.andruav.controlBoard.shared.geoFence.GeoFenceMapBaseMasna3;
import com.andruav.controlBoard.shared.geoFence.GeoLinearFenceCompositBase;
import com.andruav.controlBoard.shared.geoFence.GeoPolygonFenceCompositBase;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 *
 * Used to recieve GeoFencePoint from Unit. It can be requested uding {@link AndruavMessage_RemoteExecute}
 *
 * <br><b>Radius:</b> is optional and refer to minimum length to point on line segment.
 *
 * <br>
 * Created by mhefny on 6/17/16.
 */
public class AndruavMessage_GeoFence extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_GeoFence = 1023;

    private int fenceType;


    protected GeoFenceBase mGeoFenceMapBase;

    public GeoFenceBase getGeoFencePoints() {


        return mGeoFenceMapBase;
    }

    public void setWayPoints(GeoFenceBase geoLinearFenceMapBase) {
        mGeoFenceMapBase = geoLinearFenceMapBase;
    }


    public AndruavMessage_GeoFence() {
        super();
        messageTypeID = TYPE_AndruavMessage_GeoFence;

    }

    @Override
    public void setMessageText(String messageText) throws JSONException, ParseException {

        final JSONObject json_receive_data = new JSONObject(messageText);

        final NumberFormat nf = NumberFormat.getInstance(Locale.US);

        if (json_receive_data.has("t")) {
            fenceType = json_receive_data.getInt("t");
        }
        else
        {
            fenceType = GeoFenceMapBaseMasna3.LinearFence;
        }

        mGeoFenceMapBase = GeoFenceMapBaseMasna3.createGeoFenceMapBase(fenceType);



        mGeoFenceMapBase.fenceName = json_receive_data.getString("n");

        if (json_receive_data.has("r")) {
            // else default GeoLinearFenceCompositBase.MAX_DISTANCE
            // most related to Linear Fence
            mGeoFenceMapBase.maxDistance = Double.parseDouble(json_receive_data.getString("r"));
        }

        if (json_receive_data.has("o"))
        {
            mGeoFenceMapBase.shouldKeepOutside = (json_receive_data.getInt("o")==1);
        }


        if (json_receive_data.has("a"))
        {
            mGeoFenceMapBase.hardFenceAction = (json_receive_data.getInt("a"));
        }
        else {
            // backword compatibility
            if (json_receive_data.has("h")) {
                final boolean hf = json_receive_data.getBoolean("h");

                // backward compatibility
                int hardFenceAction = hf?FlightMode.CONST_FLIGHT_CONTROL_RTL:GeoFenceBase.ACTION_SOFT_FENCE;

                if (json_receive_data.has("i"))
                {
                    hardFenceAction = json_receive_data.getInt("i");
                }
                if (hf) mGeoFenceMapBase.hardFenceAction = hardFenceAction;
            }
        }




        if (fenceType == GeoFenceMapBaseMasna3.CylindersFence)
        {
            JSONObject jobj = json_receive_data.getJSONObject(String.valueOf(0));
            GeoFencePointNodeCylinder geoFence = new GeoFencePointNodeCylinder(nf.parse(jobj.getString("g")).doubleValue(), nf.parse(jobj.getString("a")).doubleValue());
            if (jobj.has("l")) // Altitude
            {
                ((GeoCylinderFenceMapBase)mGeoFenceMapBase).maxAltitude = jobj.getDouble("l");
            }

            ((GeoCylinderFenceMapBase)mGeoFenceMapBase).setGeoFence(geoFence);

        }else {

            int size = json_receive_data.getInt("c");


            for (int i = 0; i < size; ++i) {

                JSONObject jobj = json_receive_data.getJSONObject(String.valueOf(i));
                GeoFencePoint geoFencePoint = new GeoFencePoint(nf.parse(jobj.getString("g")).doubleValue(), nf.parse(jobj.getString("a")).doubleValue());
                ((GeoFenceCompositBase)mGeoFenceMapBase).Put(String.valueOf(i), geoFencePoint);

            }
        }

    }


    @Override
    public String getJsonMessage () throws JSONException
    {
        final JSONObject json_data= new JSONObject();

        if (mGeoFenceMapBase instanceof GeoLinearFenceCompositBase) {
            fenceType = GeoFenceMapBaseMasna3.LinearFence;
        }else
        if (mGeoFenceMapBase instanceof GeoPolygonFenceCompositBase) {
            fenceType = GeoFenceMapBaseMasna3.PolygonFence;
        }else
        if (mGeoFenceMapBase instanceof GeoCylinderFenceMapBase) {
        fenceType = GeoFenceMapBaseMasna3.CylindersFence;
    }

        json_data.accumulate("t",fenceType);

        json_data.accumulate("n", mGeoFenceMapBase.fenceName);              // n
        json_data.accumulate("r", mGeoFenceMapBase.maxDistance);            // r
        json_data.accumulate("o", mGeoFenceMapBase.shouldKeepOutside?1:0);  // o
        json_data.accumulate("h", (mGeoFenceMapBase.hardFenceAction != 0));             // h
        json_data.accumulate("i", (mGeoFenceMapBase.hardFenceAction));                  // h

        if (fenceType == GeoFenceMapBaseMasna3.CylindersFence)
        {
            final JSONObject json_record = new JSONObject();

            final GeoFencePointNodeCylinder geoFence = ((GeoCylinderFenceMapBase)mGeoFenceMapBase).getGeoFence();

            json_record.accumulate("a", String.format(Locale.US, "%4.6f", geoFence.Latitude));
            json_record.accumulate("g", String.format(Locale.US, "%4.6f", geoFence.Longitude));
            json_record.accumulate("l", String.format(Locale.US, "%4.6f", ((GeoCylinderFenceMapBase)mGeoFenceMapBase).maxAltitude));
            json_data.accumulate("0", json_record);
        }
        else {

            final int size = mGeoFenceMapBase.size();
            json_data.accumulate("c",size);


            for (int i = 0; i < size; ++i) {

                final JSONObject json_record = new JSONObject();

                final GeoFencePoint geoFencePoint = ((GeoFenceCompositBase)mGeoFenceMapBase).valueAt(i);

                json_record.accumulate("a", String.format(Locale.US, "%4.6f", geoFencePoint.Latitude));
                json_record.accumulate("g", String.format(Locale.US, "%4.6f", geoFencePoint.Longitude));
                json_data.accumulate(String.valueOf(i), json_record);
            }

        }
        return json_data.toString();
    }

}