package com.andruav.protocol.commands.textMessages;

import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecute;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by M.Hefny on 13-May-15.
 * <br>cmd: <b>1020</b>
 * This is a generic command that is used in reporting a status from a Drone.
 * Could be seens as the opposite of {@link AndruavMessage_RemoteExecute} command.
 */
public class AndruavMessage_DroneReport extends AndruavMessageBase {

    public final static int TYPE_AndruavMessage_DroneReport = 1020;




    public int mReportType;
    public int mParameter1;


    public AndruavMessage_DroneReport()
    {
        super();
        messageTypeID = TYPE_AndruavMessage_DroneReport;

    }

    public AndruavMessage_DroneReport(int reportType, int parameter1) {
        this();
        mReportType = reportType;
        mParameter1 = parameter1;
    }


    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);
        mReportType = json_receive_data.getInt("R");
        mParameter1 = json_receive_data.getInt("P");

    }

    /***
     * You can fill the data using direct throttle variable of using setData
     * that is why variables are used to fill data so it is valid all time.
     *
     * @return
     * @throws JSONException
     */
    @Override
    public String getJsonMessage() throws JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("R", mReportType);
        json_data.accumulate("P", mParameter1);

        return json_data.toString();
    }


}
