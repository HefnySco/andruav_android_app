package ap.andruav_ap.activities.phone;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import ap.andruav_ap.R;
import ap.andruav_ap.activities.ThemeColors;
import ap.andruav_ap.activities.baseview.BaseAndruavShasha;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.preference.Preference;

/***
 * AN-0134 (TASK-113): PHONE screen - the renamed entry point of the old IMU
 * tile. Tactical home-style shell: back chevron, PHONE title, overflow kebab
 * and a three-tab strip (IMU | GPS | SMS) with an amber underline on the
 * active tab. The IMU tab holds the logic that used to live in IMUShasha
 * (it stays in the codebase unreferenced); GPS and SMS are placeholders for
 * TASK-115 and TASK-117. The SMS tab dot mirrors the key_sms_rx_chk state.
 */
public class PhoneActivity extends BaseAndruavShasha {

    private static final int TAB_IMU = 0;
    private static final int TAB_GPS = 1;
    private static final int TAB_SMS = 2;

    private View[] mTabRows;
    private TextView[] mTabLabels;
    private View[] mTabIndicators;
    private View mSmsDot;

    private final Fragment[] mFragments = new Fragment[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_phone);
        initGUI();
    }

    private void initGUI() {
        if (isInEditMode) return;

        findViewById(R.id.phone_btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.phone_btn_menu).setOnClickListener(this::showMenuPopup);

        mTabRows = new View[]{
                findViewById(R.id.phone_tab_imu),
                findViewById(R.id.phone_tab_gps),
                findViewById(R.id.phone_tab_sms)};
        mTabLabels = new TextView[]{
                findViewById(R.id.phone_tab_imu_label),
                findViewById(R.id.phone_tab_gps_label),
                findViewById(R.id.phone_tab_sms_label)};
        mTabIndicators = new View[]{
                findViewById(R.id.phone_tab_imu_indicator),
                findViewById(R.id.phone_tab_gps_indicator),
                findViewById(R.id.phone_tab_sms_indicator)};
        for (int i = 0; i < mTabRows.length; i++) {
            final int index = i;
            mTabRows[i].setOnClickListener(v -> selectTab(index));
        }

        mSmsDot = findViewById(R.id.phone_tab_sms_dot);
        final Drawable dotBg = mSmsDot.getBackground();
        if (dotBg instanceof GradientDrawable) {
            ((GradientDrawable) dotBg.mutate()).setColor(
                    ThemeColors.get(this, R.attr.deGood));
        }

        mFragments[TAB_IMU] = new PhoneImuFragment();
        mFragments[TAB_GPS] = new PhoneGpsFragment();
        mFragments[TAB_SMS] = new PhoneSmsFragment();
        selectTab(TAB_IMU);
    }

    private void selectTab(final int index) {
        final int active = ThemeColors.get(this, R.attr.deAccentText);
        final int dim = ThemeColors.get(this, R.attr.deTextDim);
        for (int i = 0; i < mTabLabels.length; i++) {
            mTabLabels[i].setTextColor(i == index ? active : dim);
            mTabIndicators[i].setVisibility(i == index ? View.VISIBLE : View.INVISIBLE);
        }
        final FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        for (int i = 0; i < mFragments.length; i++) {
            final Fragment f = mFragments[i];
            if (i == index) {
                if (f.isAdded()) tx.show(f); else tx.add(R.id.phone_content, f);
            } else if (f.isAdded()) {
                tx.hide(f);
            }
        }
        tx.commit();
    }

    /***
     * The green dot on the SMS tab - SMS command receiving on/off follows the
     * same key_sms_rx_chk preference Settings writes.
     */
    private void updateSmsDot() {
        if (mSmsDot == null) return;
        mSmsDot.setVisibility(Preference.isSMSRXEnabled(null) ? View.VISIBLE : View.GONE);
    }

    /***
     * Overflow kebab - same Help action the IMU screen carried on its menu.
     * Built in code: the home popup has no layout resource of its own here.
     */
    private void showMenuPopup(final View anchor) {
        final float density = getResources().getDisplayMetrics().density;
        final TextView item = new TextView(this);
        item.setText(R.string.phone_menu_help);
        item.setTextColor(ThemeColors.get(this, R.attr.deTextPrimary));
        item.setTextSize(14);
        item.setPadding((int) (16 * density), (int) (12 * density),
                (int) (16 * density), (int) (12 * density));

        final PopupWindow popup = new PopupWindow(item,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_home_popup));
        popup.setElevation(12 * density);
        item.setOnClickListener(v -> {
            popup.dismiss();
            GMail.sendGMail(this,
                    getString(ap.andruavmiddlelibrary.R.string.email_title),
                    getString(ap.andruavmiddlelibrary.R.string.email_to),
                    getString(ap.andruavmiddlelibrary.R.string.email_subject),
                    getString(ap.andruavmiddlelibrary.R.string.email_body), null);
        });
        popup.showAsDropDown(anchor);
    }

    @Override
    public void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        updateSmsDot();
    }
}
