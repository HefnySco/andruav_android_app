package rcmobile.FPV.widgets.flightControlWidgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by M.Hefny on 17-Sep-14.
 */

public class CompassView extends View {

    private final Paint paint1 = new Paint();
    private final Paint paint2 = new Paint();
    private final Paint paint3 = new Paint();
    private final Path mPath = new Path();
    RectF rectF;
    public float kier;
    int color = Color.GREEN;
    int textColor = Color.YELLOW;
    int hh = 80, ww = 80;
    String text = "";
    float scaledDensity = 0;

    public void SetHeading(float h) {
        kier = -h;
        invalidate();
    }

    public CompassView(Context context) {
        super(context);

        init();
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    private void init() {
        // Construct a wedge-shaped path
        mPath.moveTo(0, -20 * scaledDensity);
        mPath.lineTo(-10 * scaledDensity, 30 * scaledDensity);
        mPath.lineTo(0, 20 * scaledDensity);
        mPath.lineTo(10 * scaledDensity, 30 * scaledDensity);
        mPath.close();

        SetColor(color, textColor);

        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
    }

    public void SetText(String Text) {
        text = Text;
    }

    public void SetColor(int c, int text_color) {
        color = c;
        textColor = text_color;

        paint2.setAntiAlias(true);
        paint2.setColor(color);
        paint2.setStyle(Paint.Style.FILL);

        paint1.setAntiAlias(true);
        paint1.setColor(color);
        paint1.setStyle(Paint.Style.STROKE);
        paint1.setStrokeWidth(2 * scaledDensity);

        paint3.setAntiAlias(true);
        paint3.setColor(textColor);
        paint3.setStyle(Paint.Style.STROKE);
        paint3.setStrokeWidth(1 * scaledDensity);
        paint3.setTextSize(10 * scaledDensity);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        ww = w;
        hh = h;
        rectF = new RectF(-hh / 2, -hh / 2, hh / 2, hh / 2);
        init();
        super.onSizeChanged(w, h, oldw, oldh);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawColor(Color.TRANSPARENT);

        int cx = ww / 2;
        int cy = hh / 2;

        canvas.translate(cx, cy);
        canvas.rotate(kier);

        canvas.drawPath(mPath, paint2);
        if (rectF != null) {
            canvas.drawOval(rectF, paint1);
        }
        if (text.length() > 0)
            canvas.drawText(text, 0 - paint3.measureText(text) / 2, -20 * scaledDensity, paint3);
    }

    @Override
    protected void onAttachedToWindow() {

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {

        super.onDetachedFromWindow();
    }
}
