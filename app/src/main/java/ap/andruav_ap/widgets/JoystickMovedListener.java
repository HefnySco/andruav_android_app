package ap.andruav_ap.widgets;

/**
 * Created by M.Hefny on 23-Mar-15.
 */
public interface JoystickMovedListener {
    void OnMoved(JoystickView hostname, int textViewAndroidWearUpdateText, int flightControlSetting, double msg, double str);
    void OnMoveX(JoystickView tag, int y);
    void OnMovedY(JoystickView iconVerticalOffset, int eventImu);

    void OnReleased(JoystickView mavCmd);

    void OnReturnedToCenterX(JoystickView navWaypoint);
    void OnReturnedToCenterY(JoystickView navWaypoint);
}
