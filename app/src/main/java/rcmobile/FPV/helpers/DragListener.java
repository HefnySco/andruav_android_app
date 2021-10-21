package rcmobile.FPV.helpers;

import android.graphics.drawable.Drawable;
import android.view.DragEvent;
import android.view.View;
import android.widget.RelativeLayout;

import rcmobile.FPV.App;
import rcmobile.FPV.R;

/**
 * Created by mhefny on 11/18/16.
 */

public class DragListener implements View.OnDragListener {
    Drawable enterShape = App.context.getResources().getDrawable(
            R.drawable.camera_b_32x32);
    Drawable normalShape = App.context.getResources().getDrawable(R.drawable.camera_w_32x32);

    private android.widget.RelativeLayout.LayoutParams layoutParams;
    int x_cord,y_cord;
    @Override
    public boolean onDrag(View v, DragEvent event) {
        int action = event.getAction();
        switch(event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                layoutParams = (RelativeLayout.LayoutParams)v.getLayoutParams();

                // Do nothing
                break;

            case DragEvent.ACTION_DRAG_ENTERED:
                x_cord = (int) event.getX();
                y_cord = (int) event.getY();
                break;

            case DragEvent.ACTION_DRAG_EXITED :
                x_cord = (int) event.getX();
                y_cord = (int) event.getY();
                layoutParams.leftMargin = x_cord;
                layoutParams.topMargin = y_cord;
                v.setLayoutParams(layoutParams);
                v.setVisibility(View.VISIBLE);
                v.bringToFront();
                break;

            case DragEvent.ACTION_DRAG_LOCATION  :
                x_cord = (int) event.getX();
                y_cord = (int) event.getY();
                break;

            case DragEvent.ACTION_DRAG_ENDED   :

                v.setVisibility(View.VISIBLE);
                v.bringToFront();
                break;

            case DragEvent.ACTION_DROP:

                // Do nothing
                break;
            default: break;
        }
        return true;
    }
}
