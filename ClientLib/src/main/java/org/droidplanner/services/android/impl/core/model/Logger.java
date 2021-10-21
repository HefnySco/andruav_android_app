package org.droidplanner.services.android.impl.core.model;

/**
 * Defines a set of essential logging utilities.
 */
public interface Logger {

	void logVerbose(String logTag, String verbose);

	void logDebug(String logTag, String debug);

	void logInfo(String logTag, String info);

	void logWarning(String logTag, String warning);

	void logWarning(String logTag, Exception exception);

	void logWarning(String logTag, String warning, Exception exception);

	void logErr(String logTag, String err);

	void logErr(String logTag, Exception exception);

	void logErr(String logTag, String err, Exception exception);
}
