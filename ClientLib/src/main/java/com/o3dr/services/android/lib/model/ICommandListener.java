package com.o3dr.services.android.lib.model;

/**
 * Notification of a command execution state.
 */
public interface ICommandListener {

    /**
     * Called when the command was executed successfully.
     */
    void onSuccess();

    /**
     * Called when the command execution failed.
     * @param executionError Defined by {@link com.o3dr.services.android.lib.drone.attribute.error.CommandExecutionError}
     */
    void onError(int executionError);

    /**
     * Called when the command execution times out.
     */
    void onTimeout();

}
