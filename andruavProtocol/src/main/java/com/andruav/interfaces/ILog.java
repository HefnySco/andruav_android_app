package com.andruav.interfaces;

/**
 * Created by M.Hefny on 08-Feb-15.
 */
public interface ILog {

    void logException(final String tag, final Exception exception);
    void logException(Exception exception);
    void logException(String userName, String tag, Throwable exception);
    void logException(final String userName, final String tag, final Exception exception);
    void logException(final String userName, final String tag, final java.lang.VirtualMachineError error);
    void log(final String userName, final String tag, final String text);
    void log2(final String userName, final String tag, final String text);
    void LogDeviceInfo(final String userName, final String tag);
}
