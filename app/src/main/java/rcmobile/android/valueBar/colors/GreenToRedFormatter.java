package rcmobile.android.valueBar.colors;

import android.graphics.Color;

/**
 * Created by mhefny on 5/27/16.
 */
public class GreenToRedFormatter implements BarColorFormatter {

    @Override
    public int getColor(float value, float maxVal, float minVal) {

        float[] hsv = new float[]{((120f * ((maxVal - minVal) - (value - minVal))) / (maxVal - minVal)), 1f, 1f};

        return Color.HSVToColor(hsv);

//        return Color.rgb((int) ((255 * value) / maxVal), (int) ((255 * (maxVal - value)) / maxVal),
//                0);
    }
}