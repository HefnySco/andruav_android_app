package com.andruav.protocol.commands.textMessages;

import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.ENUM_TelemetryProtocol;
import com.andruav.TelemetryProtocol;
import com.andruav.controlBoard.shared.common.VehicleTypes;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by M.Hefny on 29-Oct-14.
 * <br>cmd: <b>1004</b>
 * <br>Sends Self Identification message to other units.
 */
public class AndruavMessage_ID extends AndruavMessageBase {

    /***
     * request from an individual andruav to sendMessageToModule complete ID info.
     */
    public final static int TYPE_AndruavMessage_ID = 1004;


    /***
     * Video recording status:
     * <br> 0 for no recording
     * <br> 1 for recording
     */
    public int VideoRecording = 0;
    /***
     * Vechicle Type
     */
    public int VehicleType;
    /***
     * True if GCS and false if Drone
     */
    public boolean IsCGS;

    public boolean IsArmed;
    public boolean IsReadyToArm;

    public boolean IsFlashing = false;
    public boolean IsWhisling = false;





    public boolean IsFlying;
    public int  FlyingMode;
    public int  GPSMode;

    public long FlyingLastStartTime = 0;
    public long FlyingTotalDuration = 0;

    public String  Permissions;

    /***
     * True if FCB is connected and active.
     */
    public boolean useFCBIMU;
    public boolean isGCSBlocked;

    public int manualTXBlockedMode =0;


    /***
     * True to notify that the Unit will disconnect peacefully.
     */
    public boolean IsShutdown;
    /***
     * Supported Telemetry Protocol
     * <br>{@link ENUM_TelemetryProtocol}
     */
    public int telemetry_protocol;


    /***
     * Unit Name
     */
    public String UnitID;

    /***
     * Unit Description
     */
    public String Description;


    /***
     * This field is used by ANdruav and NOT DE and it means I am Andruav & My Version is x.y.z
     */
    public String Version_of_Andruav;

    protected  int m_arming_status = 0;
    public AndruavMessage_ID() {
        super();

        messageTypeID = TYPE_AndruavMessage_ID;
        defaultInit();
    }

    public AndruavMessage_ID(AndruavUnitBase andruavUnitBase) {
        super();

        messageTypeID = TYPE_AndruavMessage_ID;
        if (andruavUnitBase == null) {
            defaultInit();
            return;
        }

        Description         = AndruavSettings.andruavWe7daBase.Description;
        FlyingMode          = AndruavSettings.andruavWe7daBase.getFlightModeFromBoard();
        GPSMode             = AndruavSettings.andruavWe7daBase.getGPSMode();
        IsCGS               = AndruavSettings.andruavWe7daBase.getIsCGS();
        IsShutdown          = AndruavSettings.andruavWe7daBase.getIsShutdown();
        IsFlying            = AndruavSettings.andruavWe7daBase.IsFlying();
        FlyingLastStartTime = AndruavSettings.andruavWe7daBase.getFlyingStartTime();
        FlyingTotalDuration = AndruavSettings.andruavWe7daBase.getFlyingTotalDuration();
        isGCSBlocked        = AndruavSettings.andruavWe7daBase.getisGCSBlockedFromBoard();
        manualTXBlockedMode = AndruavSettings.andruavWe7daBase.getManualTXBlockedSubAction();
        IsArmed             = AndruavSettings.andruavWe7daBase.IsArmed();
        IsReadyToArm        = AndruavSettings.andruavWe7daBase.isReadyToArm();
        telemetry_protocol  = AndruavSettings.andruavWe7daBase.getTelemetry_protocol();
        UnitID              = AndruavSettings.andruavWe7daBase.UnitID;
        useFCBIMU           = AndruavSettings.andruavWe7daBase.useFCBIMU();
        VideoRecording      = AndruavSettings.andruavWe7daBase.VideoRecording;
        VehicleType         = AndruavSettings.andruavWe7daBase.getVehicleType();
        IsFlashing          = AndruavSettings.andruavWe7daBase.getIsFlashing();
        IsWhisling          = AndruavSettings.andruavWe7daBase.getIsWhisling();
        Permissions         = AndruavSettings.andruavWe7daBase.getPermissions();
        Version_of_Andruav  = AndruavSettings.andruavWe7daBase.getVersionOfAndruav();
    }

    private void defaultInit() {
        IsShutdown = false;
        telemetry_protocol = TelemetryProtocol.TelemetryProtocol_No_Telemetry;
        VehicleType = VehicleTypes.VEHICLE_UNKNOWN;
        VideoRecording = AndruavUnitBase.VIDEORECORDING_OFF;
    }

    @Override
    public void setMessageText(String messageText) throws JSONException {
        JSONObject json_receive_data = new JSONObject(messageText);

        VehicleType = json_receive_data.getInt("VT");
        IsCGS = json_receive_data.getBoolean("GS");
        if (json_receive_data.has("VR")) VideoRecording = json_receive_data.getInt("VR");
        if (json_receive_data.has("FI")) useFCBIMU = json_receive_data.getBoolean("FI");
        if (json_receive_data.has("SD")) IsShutdown = json_receive_data.getBoolean("SD");
        if (json_receive_data.has("GM")) GPSMode = json_receive_data.getInt("GM");
        if (json_receive_data.has("AR"))
        {
            m_arming_status = json_receive_data.getInt("AR");
            IsArmed = (m_arming_status & 0x1) != 0;
            IsReadyToArm = (m_arming_status & 0x2) != 0;
        }
        if (json_receive_data.has("FL")) IsFlying = json_receive_data.getBoolean("FL"); //backward compatibility
        if (json_receive_data.has("FM")) FlyingMode = json_receive_data.getInt("FM"); //backward compatibility
        if (json_receive_data.has("B"))  isGCSBlocked = json_receive_data.getBoolean("B");
        if (json_receive_data.has("C"))  manualTXBlockedMode = json_receive_data.getInt("C");
        if (json_receive_data.has("x"))  IsFlashing = json_receive_data.getBoolean("x");
        if (json_receive_data.has("y"))  IsWhisling = json_receive_data.getBoolean("y");
        if (json_receive_data.has("z"))  FlyingLastStartTime = json_receive_data.getLong("z");
        if (json_receive_data.has("a"))  FlyingTotalDuration = json_receive_data.getLong("a");
        if (json_receive_data.has("p"))  Permissions = json_receive_data.getString("p");
        //TODO: backword compatibility
        if (json_receive_data.has("TP")) {
            telemetry_protocol = json_receive_data.getInt("TP");
        }
        UnitID = json_receive_data.getString("UD");
        Description = json_receive_data.getString("DS");
    }


    @Override
    public String getJsonMessage() throws org.json.JSONException {
        JSONObject json_data = new JSONObject();
        json_data.accumulate("VT", VehicleType);
        json_data.accumulate("GS", IsCGS);
        json_data.accumulate("VR", VideoRecording);
        json_data.accumulate("B",  isGCSBlocked);
        json_data.accumulate("FM", FlyingMode);
        json_data.accumulate("GM", GPSMode);
        json_data.accumulate("TP", telemetry_protocol);
        json_data.accumulate("UD", UnitID);
        json_data.accumulate("DS", Description);
        json_data.accumulate("p", Permissions);
        json_data.accumulate("av", Version_of_Andruav);

        m_arming_status = 0;
        if (IsReadyToArm)
        {
            m_arming_status  |= (1 << 0);
        }
        if (IsArmed)
        {
            m_arming_status  |= (1 << 1);
        }
        json_data.accumulate("AR", m_arming_status);

        // dont add unless it is TRUE to save packet size
        if (useFCBIMU) json_data.accumulate("FI", useFCBIMU);
        if (IsShutdown) json_data.accumulate("SD", IsShutdown);
        if (IsFlying) json_data.accumulate("FL", IsFlying);
        if (IsFlashing) json_data.accumulate("x", IsFlashing);
        if (IsWhisling) json_data.accumulate("y", IsWhisling);
        if (FlyingLastStartTime != 0) json_data.accumulate("z", FlyingLastStartTime);
        if (FlyingTotalDuration != 0) json_data.accumulate("a", FlyingTotalDuration);

        if (!IsCGS && useFCBIMU) {
            json_data.accumulate("C", manualTXBlockedMode);
        }

        return json_data.toString();
    }

}
