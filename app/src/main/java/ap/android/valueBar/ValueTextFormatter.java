package ap.android.valueBar;

/**
 * Created by mhefny on 5/27/16.
 */
public interface ValueTextFormatter {

    String getValueText(float value, float maxVal, float minVal);
    String getMinVal(float minVal);
    String getMaxVal(float maxVal);
}