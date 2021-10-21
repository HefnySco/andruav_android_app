package rcmobile.FPV.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vi.swipenumberpicker.OnValueChangeListener;
import com.vi.swipenumberpicker.SwipeNumberPicker;

import rcmobile.FPV.R;

/**
 * Created by mhefny on 4/6/16.
 */
public class CardWheelWidget extends LinearLayout {

    private LayoutInflater mInflater;

    private TextView                attributeName;
    private SwipeNumberPicker       attributeValue;

    private OnValueChangeListener mOnValueChangeListener;



    private void initGUI (Context context)
    {
        mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.widget_cardwheel, this, true);

        if (isInEditMode()) return ;

        attributeName = findViewById(R.id.cardwheel_attribute_name);
        attributeValue = findViewById(R.id.cardwheel_attribute_value);
        attributeValue.setOnValueChangeListener(new OnValueChangeListener() {
            @Override
            public boolean onValueChange(SwipeNumberPicker view, int oldValue, int newValue) {
                if (mOnValueChangeListener != null)
                {
                    return mOnValueChangeListener.onValueChange(view, oldValue, newValue);
                }

                return false;
            }
        });
    }



    public void setOnValueChangeListener (OnValueChangeListener onValueChangeListener)
    {
        mOnValueChangeListener = onValueChangeListener;
    }

    public void setAttributeValue (int minValue, int maxValue, int value)
    {
        attributeValue.setMaxValue(maxValue);
        attributeValue.setMinValue(minValue);
        attributeValue.setValue(value, false);


    }

    public void setAttributeValue (int value)
    {
        attributeValue.setValue(value, false);
    }


    public void setAttributeName(CharSequence txt) {
        attributeName.setText(txt);
    }

    public void setAttributeName(int titleRes) {
        attributeName.setText(titleRes);
    }

    public CharSequence getText() {
        return attributeName.getText();
    }



    public CardWheelWidget(Context context) {
        super(context);

        initGUI(context);

    }

    public CardWheelWidget(Context context, AttributeSet attrs) {
        super(context, attrs);

        initGUI(context);

    }

    public CardWheelWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initGUI(context);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CardWheelWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        initGUI(context);

    }



}
