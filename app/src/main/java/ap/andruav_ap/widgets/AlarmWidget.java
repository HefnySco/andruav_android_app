package ap.andruav_ap.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import com.andruav.AndruavSettings;

import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.util.ActivityMosa3ed;

/**
 * Created by mhefny on 2/3/17.
 */

public class AlarmWidget extends View {


    private AlarmWidget Me;
    private Handler mhandler;

    public AlarmWidget(Context context) {
        super(context);
    }

    public AlarmWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AlarmWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AlarmWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void OnEvent ()
    {

    }

    private  boolean initcalled = false;
    private  boolean enableFlash = false;

    private int color =0;
    private final int[] colors ={getResources().getColor(R.color.btn_TXT_ERROR),getResources().getColor(R.color.btn_TXT_WHITE),getResources().getColor(R.color.COLOR_SAFE_FENCE)};

    private final Runnable doFlash = new Runnable() {
        @Override
        public void run() {
            Me.setBackgroundColor(colors[color % colors.length]);
            color = color + 1;
            if (enableFlash) {
                mhandler.postDelayed(this, 250);
            }
        }
    };

    public void init()
    {

        Me = this;
        if (initcalled) return;
        initcalled = true;

       // EventBus.getDefault().register(this);

        mhandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };


        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Flash(!enableFlash);
            }
        });


    }


    public void unInit ()
    {
        if (!initcalled) return;
        initcalled = false;

        mhandler.removeCallbacksAndMessages(null);

        //v user .EventBus.getDefault().unregister(this);
    }


    @Override
    public void onAttachedToWindow ()
    {
        super.onAttachedToWindow();
        init();
        Flash(true);

        AndruavSettings.andruavWe7daBase.setIsFlashing(true);
    }


    @Override
    public void onDetachedFromWindow()
    {
        unInit();
        AndruavSettings.andruavWe7daBase.setIsFlashing(false);
        super.onDetachedFromWindow();

    }


    public void Flash (boolean enable)
    {
       if (!enableFlash && enable)
        {
            mhandler.post(doFlash);
        }

        enableFlash = enable;

        if (!enable)
        {
            // remove me also
            ActivityMosa3ed.removeMeFromParentView(this);
        }
        else
        {
            AndruavSettings.andruavWe7daBase.setIsFlashing(true);
        }

    }
}
