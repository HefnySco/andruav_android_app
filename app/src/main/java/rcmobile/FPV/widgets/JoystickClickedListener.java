package rcmobile.FPV.widgets;

/**
 * Created by M.Hefny on 23-Mar-15.
 */
public interface JoystickClickedListener {

    void OnClicked(JoystickView vAccCorrection);

    void OnReleased(JoystickView totalBinaryTelemetry);
}
