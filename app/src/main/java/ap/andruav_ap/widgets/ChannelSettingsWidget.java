package ap.andruav_ap.widgets;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.util.Maths;
import com.appyvet.rangebar.RangeBar;

import ap.andruav_ap.R;


/**
 * Created by M.Hefny on 01-Apr-15.
 */
public class ChannelSettingsWidget extends RelativeLayout {

    private LayoutInflater flatter;

    private RangeBar rangeBar;
    private EditText edtDRRatio;
    private CheckBox chkReversed;
    private CheckBox chkReturnToCentre;
    private TextView txtChannelName;


    private int adjustRange (int value)
    {
        if (value< Constants.Default_RC_MIN_VALUE) return Constants.Default_RC_MIN_VALUE;
        if (value> Constants.Default_RC_MAX_VALUE) return Constants.Default_RC_MAX_VALUE;

        return value;
    }



    public void setChannelName (CharSequence rcAux2Format)
    {
        if (txtChannelName != null)
        {
            txtChannelName.setText(rcAux2Format);
        }
    }

    public void setMinValue (int value)
    {
        //maxMin = (adjustRange (value) - FeatureSwitch.Default_RC_MIN_VALUE) * ratio;
        value = adjustRange (value);


        if (rangeBar != null) {

            final double right = Maths.Constraint(Constants.Default_RC_MIN_VALUE,Float.parseFloat(rangeBar.getRightPinValue()), Constants.Default_RC_MAX_VALUE);
            final double left = Maths.Constraint(Constants.Default_RC_MIN_VALUE,value, Constants.Default_RC_MAX_VALUE);


            rangeBar.setRangePinsByValue((float)left, (float)right);
        }
    }

    public int getMinValue ()
    {
        if (rangeBar != null)
        {
            try {
                return (int)Float.parseFloat(rangeBar.getLeftPinValue());
            }
            catch (Exception genExit)
            {
                return 0;
            }

        }
        return Constants.Default_RC_MIN_VALUE;
    }


    public void setMaxValue (int value) {
       // maxMax = (adjustRange (value) - FeatureSwitch.Default_RC_MIN_VALUE)  * ratio;
        value = adjustRange (value);

        if (rangeBar != null) {
            //rangeBar.setTickEnd(value); << you may need this to solve:
            /*
            Caused by: java.lang.IllegalArgumentException: Pin value left 1148.0, or right 1850.0 is out of bounds. Check that it is greater than the minimum (1150.0) and less than the maximum value (1850.0)
        at com.appyvet.rangebar.RangeBar.setRangePinsByValue(RangeBar.java:927)
        at rcmobile.FPV.widgets.ChannelSettingsWidget.setMaxValue(ChannelSettingsWidget.java:86)
             */


            final double left = Maths.Constraint(Constants.Default_RC_MIN_VALUE,Float.parseFloat(rangeBar.getLeftPinValue()), Constants.Default_RC_MAX_VALUE);
            final double right = Maths.Constraint(Constants.Default_RC_MIN_VALUE,value, Constants.Default_RC_MAX_VALUE);


            rangeBar.setRangePinsByValue((float)left, (float)right);
        }
    }
    public int getMaxValue ()
    {
        if (rangeBar != null)
        {
            try {
                return (int)Float.parseFloat(rangeBar.getRightPinValue());
            }
            catch (Exception genExit)
            {
                return Constants.Default_RC_MAX_VALUE;
            }

        }
        return 0;
    }

    public void setDRRatioValue (int roll)
    {
        if (edtDRRatio != null)
        {
            edtDRRatio.setText(String.valueOf(roll));

        }
    }

    public int getDRRatioValue ()
    {
        if (edtDRRatio != null)
        {
            try {
                return Integer.parseInt(edtDRRatio.getText().toString());
            }
            catch (Exception genExit)
            {
                return 0;
            }

        }
        return 0;
    }


    public void setIsReverse (Boolean save)
    {
        chkReversed.setChecked(save);
    }

    public Boolean getIsReturnToCenter ()
    {
        return chkReturnToCentre.isChecked();
    }

    public void setIsReturnToCenter (Boolean context)
    {
        chkReturnToCentre.setChecked(context);
    }

    public Boolean getIsReverse ()
    {
        return chkReversed.isChecked();
    }

    private void initGUI (Context context)
    {
        if (isInEditMode()) return ;

        flatter = LayoutInflater.from(context);
        flatter.inflate(R.layout.widget_remote_channels_config, this, true);

        txtChannelName = findViewById(R.id.widget_channel_config_txtChannelName);
        edtDRRatio = findViewById(R.id.widget_channel_config_edtDualRate);
        rangeBar = findViewById(R.id.widget_channel_config_range);
        //edtMaxValue = (EditText) findViewById(R.id.widget_channel_config_edtMaxValue);
        //edtMinValue = (EditText) findViewById(R.id.widget_channel_config_edtMinValue);
        chkReversed = findViewById(R.id.widget_channel_config_chkReverse);
        chkReturnToCentre = findViewById(R.id.widget_channel_config_chkRTC);



        if (AndruavSettings.andruavWe7daBase.getIsCGS())
        {
            edtDRRatio.setVisibility(View.INVISIBLE);
            rangeBar.setVisibility(View.GONE);
            chkReversed.setVisibility(View.INVISIBLE);
            chkReturnToCentre.setVisibility(View.VISIBLE);


        }
        else
        {

            edtDRRatio.setVisibility(View.VISIBLE);
            rangeBar.setVisibility(View.VISIBLE);
            chkReversed.setVisibility(View.VISIBLE);
            chkReturnToCentre.setVisibility(View.VISIBLE);

        }

try {
        if (!this.isInEditMode()) {
            rangeBar.setTicks(Constants.Default_RC_MIN_VALUE, Constants.Default_RC_MAX_VALUE);
            rangeBar.setRangePinsByValue(Constants.Default_RC_MIN_VALUE, Constants.Default_RC_MAX_VALUE);

            rangeBar.setTickInterval(1.0f);
            rangeBar.setBarWeight(2.0f);
            rangeBar.setBarColor(context.getResources().getColor(R.color.btn_TXT_WHITE));
            rangeBar.setConnectingLineWeight(3.0f);
            rangeBar.setTickColor(context.getResources().getColor(R.color.btn_TXT_BLUE));
            rangeBar.setConnectingLineColor(context.getResources().getColor(R.color.btn_TXT_BLUE_DARKEST));
            rangeBar.setSelectorColor(context.getResources().getColor(R.color.btn_TXT_GREEN_DARKER));
            rangeBar.setPinColor(context.getResources().getColor(R.color.btn_TXT_GREEN));
            //rangeBar.setPinTextColor(context.getResources().getColor(R.color.btn_TXT_GREEN));
            rangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
                @Override
                public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {

                }
            });
        }

    } catch (IllegalArgumentException e) {
    Log.e("s",e.getMessage());
    }
        edtDRRatio.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence mavLinkPacket, int timestamp, int actionBar, int soundPoolMapAttrs) {

            }

            @Override
            public void beforeTextChanged(CharSequence inputEnabled, int mapActivityChooserViewAutoCompleteTextAppearanceAppCompatLightBaseListPressedDark, int imgData, int remoteModeSelectAllCaps) {
            }

            @Override
            public void afterTextChanged(Editable end) {
            }

        });
    }



    public ChannelSettingsWidget(Context context) {
        super(context);

        initGUI(context);
    }

    public ChannelSettingsWidget(Context context, AttributeSet abcSpinnerMode) {
        super(context, abcSpinnerMode);

        initGUI(context);
    }

    public ChannelSettingsWidget(Context context, AttributeSet abcListLongpressedHoloDark, int coord) {
        super(context, abcListLongpressedHoloDark, coord);

        initGUI(context);
    }

    @Override
    public void onSizeChanged (final int w, final int h, final int oldw, final int oldh)
    {
        if (this.isInEditMode()) return;
        super.onSizeChanged(w, h, oldw, oldh);
    }


}
