package ap.andruav_ap.helpers;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Created by mhefny on 11/18/16.
 */

public class TouchListener implements View.OnTouchListener {


    private float mPrevX, mOffsetX;
    private float mPrevY, mOffsetY;
    private boolean mFirst = true;
    ViewGroup.LayoutParams initLayoutParams;
    public boolean onTouch(View view, MotionEvent motionEvent) {

        float currX, currY;
        int action = motionEvent.getAction();


        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //ClipData data = ClipData.newPlainText("", "");
                //View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(
                //        view);
                //view.startDrag(data, shadowBuilder, view, 0);
                view.setVisibility(View.INVISIBLE);

                mPrevX = motionEvent.getRawX() ;
                mPrevY = motionEvent.getRawY() ;

                mOffsetX = mPrevX - view.getX();
                mOffsetY = mPrevY - view.getY();
                view.bringToFront();

                break;

            case MotionEvent.ACTION_MOVE: {

                if (mFirst)
                {
                    view.bringToFront();
                    mPrevX = motionEvent.getRawX() ;
                    mPrevY = motionEvent.getRawY() ;

                    mFirst = false;
                    break;
                }

                mOffsetX =  motionEvent.getRawX() - mPrevX;
                mOffsetY =  motionEvent.getRawY() - mPrevY;
                currX = view.getX() + mOffsetX;
                currY = view.getY() + mOffsetY;

                final ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(
                        view.getLayoutParams());
                marginParams.setMargins((int) (currX),
                        (int) (currY), 0, 0);
                final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                        marginParams);
                view.setLayoutParams(layoutParams);

                mPrevX = motionEvent.getRawX() ;
                mPrevY = motionEvent.getRawY() ;

                break;
            }

            case MotionEvent.ACTION_UP:
                view.setOnTouchListener(null);
                mFirst = true;
                break;

            default:
                break;

        }
        return true;

    }
}
