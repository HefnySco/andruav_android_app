package rcmobile.FPV.activities.drone;


import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import rcmobile.FPV.App;
import rcmobile.FPV.R;
import rcmobile.FPV.widgets.AlarmWidget;

public class FlashActivity extends AppCompatActivity {


    private FlashActivity Me;
    private AlarmWidget alarmWidget;


    private void initGUI ()
    {
        alarmWidget = findViewById(R.id.activity_flash_widget_alarm);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Me = this;
        App.activeActivity = this;
        App.ForceLanguage();
        //setScreenOrientation(false);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        setContentView(R.layout.activity_flash);

        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initGUI();
    }


    @Override
    public void onStart() {
        super.onStart();

        App.activeActivity = this;
    }

        @Override
    protected void onResume() {
        // The activity has become visible (it is now "resumed").
        super.onResume();

        alarmWidget.init();

    }

        @Override
    protected void onPause() {
        // Another activity is taking focus (this activity is about to be "paused").
        //EventBus.getDefault().unregister(this);
        //mhandle.removeCallbacksAndMessages(null);
            alarmWidget.unInit();
            super.onPause();
    }
}
