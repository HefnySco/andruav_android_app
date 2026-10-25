package ap.andruav.denative;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.EnumMap;

/** Debug-only control screen for the native DE modules (Plan A6). */
public class DeNativeDebugActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final EnumMap<DeUnitSupervisor.Module, TextView> stateViews =
            new EnumMap<>(DeUnitSupervisor.Module.class);
    private final Runnable refresher = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DeUnitSupervisor.get().init(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("DE native modules");
        title.setTextSize(20);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        addModuleRow(root, DeUnitSupervisor.Module.DE_COMM, "de_comm");
        addModuleRow(root, DeUnitSupervisor.Module.DE_MAVLINK, "de_mavlink");

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refresher);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresher);
        super.onPause();
    }

    private void addModuleRow(LinearLayout root, final DeUnitSupervisor.Module module, String label) {
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(16);
        name.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        name.setLayoutParams(lp);
        root.addView(name);

        TextView state = new TextView(this);
        state.setTextSize(14);
        stateViews.put(module, state);
        root.addView(state);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button start = new Button(this);
        start.setText("Start");
        start.setOnClickListener(v -> DeUnitSupervisor.get().start(module));
        Button stop = new Button(this);
        stop.setText("Stop");
        stop.setOnClickListener(v -> DeUnitSupervisor.get().stop(module));
        buttons.addView(start);
        buttons.addView(stop);
        root.addView(buttons);
    }

    private void refresh() {
        DeUnitSupervisor supervisor = DeUnitSupervisor.get();
        for (DeUnitSupervisor.Module module : DeUnitSupervisor.Module.values()) {
            TextView view = stateViews.get(module);
            if (view == null) {
                continue;
            }
            int pid = supervisor.getPid(module);
            String pidText = pid > 0 ? String.valueOf(pid) : "-";
            view.setText("state: " + supervisor.getState(module)
                    + "   pid: " + pidText
                    + "   restarts: " + supervisor.getRestartCount(module));
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
