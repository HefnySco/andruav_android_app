package ap.andruav_ap.widgets.flytopoint_sliding;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.andruav.andruavUnit.AndruavUnitShadow;

import ap.andruav_ap.R;

import ap.andruav_ap.widgets.sliding.ListUnitItemBase;

/**
 * Created by mhefny on 12/27/16.
 */

public class DroneFlyToPointUnitItem extends ListUnitItemBase
{

    private TextView mDroneTitle;
    private ToggleButton mBtnGo;


    private boolean mGotoPoint;
    public boolean getGotoPoint ()
    {
        return mBtnGo.isChecked();
    }


    private void initGUI (Context context) {
        mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.listitem_drone_flytopoint, this, true);

        mDroneTitle = findViewById(R.id.listitem_flytopoint_txtDrone);
        if (mAndruavUnit.IsArmed())
        {
            mDroneTitle.setTextColor(getResources().getColor(R.color.btn_TXT_ERROR));
        }
        mBtnGo = findViewById(R.id.listitem_flytopoint_tglSelected);
        mBtnGo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }


    public AndruavUnitShadow getUnit()
    {
        return mAndruavUnit;
    }

    public void setUnit(AndruavUnitShadow andruavUnit) {
        mDroneTitle.setText(andruavUnit.UnitID);

    }


    public DroneFlyToPointUnitItem(Context context, AndruavUnitShadow andruavUnit) {
        super(context);
        mAndruavUnit = andruavUnit;
        initGUI(context);
        setUnit(andruavUnit);
    }

    public DroneFlyToPointUnitItem(Context context) {
        super(context);
    }

    public DroneFlyToPointUnitItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DroneFlyToPointUnitItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DroneFlyToPointUnitItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


}
