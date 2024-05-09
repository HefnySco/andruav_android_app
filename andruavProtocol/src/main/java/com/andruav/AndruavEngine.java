package com.andruav;

import android.content.Context;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitMapBase;
import com.andruav.andruavUnit.AndruavUnitFactoryBase;
import com.andruav.dummyclasses.DummyLog;
import com.andruav.dummyclasses.DummyNotification;
import com.andruav.dummyclasses.Dummy_EventBus;
import com.andruav.interfaces.IAndruavWe7daMasna3;
import com.andruav.interfaces.IEventBus;
import com.andruav.interfaces.IControlBoardFactory;
import com.andruav.interfaces.ILog;
import com.andruav.interfaces.INotification;
import com.andruav.interfaces.IPreference;
import com.andruav.protocol.commands.Andruav_Parser;
import com.andruav.protocol.communication.sms.AndruavSMSClientParserBase;
import com.andruav.protocol.communication.uavos.AndruavUDPBase;
import com.andruav.protocol.communication.uavos.AndruavUDPServerBase;
import com.andruav.protocol.communication.udpproxy.UDPProxy;
import com.andruav.protocol.communication.udpserver.UDPServerBase;
import com.andruav.protocol.communication.websocket.AndruavWSClientBase;
import com.andruav.uavos.modules.UAVOSException;
import com.andruav.uavos.modules.UAVOSMapBase;

/**
 * <br>This class has members for notification, logging, AndruavParsers, AndruavUnitFactory & AndruavWe7daMapBase.
 * <br>This class is the core of everything, as it has a default implementation of each module,and it is where you can inherit a class for a given module and expand it then register
 * <br>it in the Engine to use it to extend Andruav ModuleFeatures.
 * <br><br>Created by M.Hefny on 08-Feb-15.
 */
public class AndruavEngine {
    //AndruavEngine


    /**
     * App context given from App class at initialization.
     */
    public static Context AppContext;
    private static IPreference mPreference;
    private static IEventBus mEventBus = new Dummy_EventBus();
    /**
     * Interface to be called for logging.
     */
    private static ILog iLog;


    /***
     * Interface called for database storage.
     */
    private static AndruavDatabase database;

    private static EmergencyBase emergencyBase;

    /**
     * Interface to be called for Notifcation.
     * This is mainly responsible for toolbar notification icons.
     * <br><b>Should</b> be initialized before usin any Andruav ModuleFeatures.
     */
    private static INotification iNotification;
    /**
     * This is used to create AndruavUnit instance, to provide a way to have updated version of {@link AndruavUnitBase} class.
     * <br><b>Should</b> be initialized before usin any Andruav ModuleFeatures.
     */
    private static IAndruavWe7daMasna3 mIAndruavWe7daMasna3;
    private static IControlBoardFactory mIControlBoardFactory;
    /**
     * This holds a collection of all neighbor {@link AndruavUnitBase} <b>NOT including</b> me "the running andruav unit".
     * <br><b>Should</b> be initialized before usin any Andruav ModuleFeatures.
     */
    private static AndruavUnitMapBase andruavUnitMapBase = new AndruavUnitMapBase();
    private static UAVOSMapBase uavosMapBase = new UAVOSMapBase();
    /**
     * This represents {@link Andruav_Parser} or an updated version of it.
     * <br><b>Should</b> be initialized before usin any Andruav ModuleFeatures.
     */
    private static Andruav_Parser andruavParser = new Andruav_Parser();
    /**
     * This represents {@link AndruavWSClientBase} or an updated version of it.
     * <br><b>Should</b> be initialized before usin any Andruav ModuleFeatures.
     */
    private static AndruavWSClientBase andruavWSClientBase_autoBohn;

    private static AndruavSMSClientParserBase andruavSMSClientParserBase;

    /**
     * This represents {@link AndruavUDPServerBase} or updated version of it.
     */
    private static AndruavUDPBase andruavAndruavUDPServerBase;

    private static UDPProxy udpProxy;

    public static IPreference getPreference() {
        return mPreference;
    }

    public static void setPreference(IPreference iPreference) {
        mPreference = iPreference;
    }

    /**
     * Returns the active instance of {@link AndruavWSClientBase}
     *
     * @return
     */
    public static AndruavWSClientBase getAndruavWS() {
        return andruavWSClientBase_autoBohn;
    }


    /**
     * Set active instance for {@link #andruavWSClientBase_autoBohn}
     *
     * @param AndruavWebsocket active instance of {@link AndruavWSClientBase}
     *                         <br>should be called first to initialize {@link #andruavWSClientBase_autoBohn}
     */
    public static void setAndruavWS(AndruavWSClientBase AndruavWebsocket) {
        andruavWSClientBase_autoBohn = AndruavWebsocket;
    }

    public static AndruavSMSClientParserBase getAndruavSMSClientParserBase ()
    {
        return andruavSMSClientParserBase;
    }

    public static void  setAndruavSMSClientParserBase (AndruavSMSClientParserBase smsClientParserBase)
    {
        andruavSMSClientParserBase = smsClientParserBase;
    }

    public static void setEventBus (IEventBus iEventBus)
    {
        mEventBus = iEventBus;
    }

    public static IEventBus getEventBus()
    {
        return mEventBus;
    }



    /**
     * Returns the active instance of {@link #andruavAndruavUDPServerBase}
     *
     * @return
     */
    public static AndruavUDPBase getAndruavUDP() {

        return andruavAndruavUDPServerBase;
    }

    public static UDPProxy getUDPProxy() {

        return udpProxy;
    }

    /**
     * Set active instance for {@link #andruavAndruavUDPServerBase}
     *
     * @param AndruavUDP
     */
    public static void setAndruavUDP(AndruavUDPBase AndruavUDP) {
        andruavAndruavUDPServerBase = AndruavUDP;
    }


    public static void setUDPProxy(UDPProxy udpproxy)
    {
        udpProxy = udpproxy;
    }


    public static Andruav_Parser getAndruavParser() {
        return andruavParser;
    }

    /**
     * @param parser
     */
    public static void setAndruavParser(Andruav_Parser parser) {
        andruavParser = parser;
    }

    public static AndruavUnitMapBase getAndruavWe7daMapBase() {
        return andruavUnitMapBase;
    }

    public static void setAndruavWe7daMapBase(AndruavUnitMapBase andruavUnitMap) {
        andruavUnitMapBase = andruavUnitMap;
    }

    public static UAVOSMapBase getUAVOSMapBase() throws UAVOSException {
        return uavosMapBase;
    }



    public static void setUAVOSMapBase(UAVOSMapBase uavosMap) {
        uavosMapBase = uavosMap;
    }

    public static void setLogHandler(ILog ilogHandler) {
        iLog = ilogHandler;
    }

    /**
     * use {@link #iLog} to log events, errors...etc.
     *
     * @return
     */
    public static ILog log() {
        if (iLog == null) {
            iLog = new DummyLog();
        }

        return iLog;

    }


    public static EmergencyBase getEmergency() {

        return emergencyBase;

    }


    public static void setEmergency (final EmergencyBase em) {

        emergencyBase = em;
    }

    public static AndruavDatabase getDabase () {

        return database;

    }


    public static void setDabase (AndruavDatabase db) {

        database = db;
    }


    public static void setNotificationHandler(INotification iNotificationHandler) {
        iNotification = iNotificationHandler;
    }

    /**
     * use {@link #iNotification} to make a notification.
     *
     * @return
     */
    public static INotification notification() {
        if (iNotification == null) {
            iNotification = new DummyNotification();
        }

        return iNotification;

    }

    // BUG: Use this interface for all creation of AndruavWe7da
    public static void setAndruavWe7daMasna3(final IAndruavWe7daMasna3 iAndruavWe7daMasna3) {
        mIAndruavWe7daMasna3 = iAndruavWe7daMasna3;
    }

    public static IAndruavWe7daMasna3 getAndruavWe7daMasna3() {
        return mIAndruavWe7daMasna3;
    }


    public static void setLo7etTa7akomMasna(IControlBoardFactory iAndruavWe7daMasna3) {
        mIControlBoardFactory = iAndruavWe7daMasna3;
    }

    public static IControlBoardFactory getLo7etTa7akomMasna3() {
        return mIControlBoardFactory;
    }


    /***
     * used to get active {@link #andruavUnitFactory()} that is used to create {@link AndruavUnitBase} based instances.
     *
     * @return
     */
    public static IAndruavWe7daMasna3 andruavUnitFactory() {
        if (mIAndruavWe7daMasna3 == null) {
            mIAndruavWe7daMasna3 = new AndruavUnitFactoryBase();
        }

        return mIAndruavWe7daMasna3;

    }


    /**
     * Returns TRUE if gievn status equals to socketState in AndruavWSClient.
     * if AndruavWSClient is null the return is always false.
     * @param State
     * @return
     */
    public static boolean isAndruavWSStatus (int State) {
        return AndruavEngine.getAndruavWS() != null && (AndruavEngine.getAndruavWS().getSocketState() == State);
    }

    /***
     * Returns current socket conneciton status
     * @return
     */
    public static int getAndruavWSStatus () {
        if (AndruavEngine.getAndruavWS()==null) return AndruavWSClientBase.SOCKETSTATE_FREASH;

        return AndruavEngine.getAndruavWS().getSocketState();
    }

    /***
     * Returns current socket conneciton status
     * @return
     */
    public static int getAndruavWSAction () {
        if (AndruavEngine.getAndruavWS()==null) return AndruavWSClientBase.SOCKETACTION_NONE;

        return AndruavEngine.getAndruavWS().getSocketAction();
    }
}
