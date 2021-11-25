package ap.andruav_ap.widgets.sliding;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;

import com.andruav.andruavUnit.AndruavUnitShadow;

import ap.andruav_ap.helpers.GUI;
import ap.andruav_ap.R;

/**
 * Created by mhefny on 3/24/16.
 */
public class SlidingAndruavUnitMapPlanningItem extends ListUnitItemBase {

    protected boolean bItIsMe = false;

    private Button mMain;
    private Button mHome;
    private Button mLoadMission;
    private Button mSaveMission;
    private Button mFollow;
    private Button mClearMission;


    @Override
    public void setUnit(AndruavUnitShadow andruavUnit)
    {
        mAndruavUnit = andruavUnit;
        if (mAndruavUnit != null)
        {
            bItIsMe = andruavUnit.IsMe();


        }
        setMainButton();
        setHomeButton();
        setLoadMissionButton();
        setSaveMissionButton();
        setFollowButton();
        setClearButton();
    }


    public void setOnClickListener(OnClickListener onClickListener)
    {
        this.mMain.setOnClickListener(onClickListener);
    }

    public void setOnHomeClickListener(OnClickListener onClickListener)
    {
        this.mHome.setOnClickListener(onClickListener);
    }

    public void setOnLoadMissionClickListener(OnClickListener onClickListener)
    {
        this.mLoadMission.setOnClickListener(onClickListener);
    }

    public void setOnSaveMissionClickListener(OnClickListener onClickListener)
    {
        this.mSaveMission.setOnClickListener(onClickListener);
    }

    public void setOnFollowMissionClickListener(OnClickListener onClickListener)
    {
        this.mFollow.setOnClickListener(onClickListener);
    }

    public void setOnClearMissionClickListener(OnClickListener onClickListener)
    {
        this.mClearMission.setOnClickListener(onClickListener);
    }


    private void initGUI (Context context)
    {
        mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.slidingitem_andruavunit_mapediting, this, true);

        mMain                   = findViewById(R.id.slidingitem_andruavunit_btnUnit);
        mHome                   = findViewById(R.id.slidingitem_andruavunit_mapplanning_btnHome);
        mLoadMission            = findViewById(R.id.slidingitem_andruavunit_mapplanning_btnLoad);
        mSaveMission            = findViewById(R.id.slidingitem_andruavunit_mapplanning_btnSave);
        mFollow                 = findViewById(R.id.slidingitem_andruavunit_mapplanning_btnFollow);
        mClearMission           = findViewById(R.id.slidingitem_andruavunit_mapplanning_btnClear);


        if (Build.VERSION.SDK_INT >Build.VERSION_CODES.KITKAT) {
            // a Lollypop Issue http://stackoverflow.com/questions/26958909/why-is-my-button-text-coerced-to-all-caps-on-lollipop
            mMain.setTransformationMethod(null);
        }
    }

    public SlidingAndruavUnitMapPlanningItem(Context context, AndruavUnitShadow andruavUnit) {
        super(context);

        initGUI(context);
        setUnit(andruavUnit);
    }

    public SlidingAndruavUnitMapPlanningItem(Context context) {
        super(context);
    }

    public SlidingAndruavUnitMapPlanningItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initGUI(context);
    }

    public SlidingAndruavUnitMapPlanningItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initGUI(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SlidingAndruavUnitMapPlanningItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initGUI(context);
    }


    private void setMainButton(){

        mMain.setText(GUI.getButtonText(mAndruavUnit));
    }

    private void setHomeButton(){

        int color;
        if ((mAndruavUnit.getIsCGS()) || (mAndruavUnit.getIsShutdown()))
        {

            color = getResources().getColor(R.color.btn_TXT_GREY_DARK);
        }
        else
        {
            color = getResources().getColor(R.color.btn_TXT_BLUE_DARKER);
        }
        mHome.setTextColor(color);
    }

    private void setLoadMissionButton(){

        int color;
        if ((mAndruavUnit.getIsCGS()) || (mAndruavUnit.getIsShutdown()))
        {

            color = getResources().getColor(R.color.btn_TXT_GREY_DARK);
        }
        else
        {
            color = getResources().getColor(R.color.btn_TXT_BLUE_DARKER);
        }
        mLoadMission.setTextColor(color);

    }

    private void setSaveMissionButton(){

        int color;
        if ((mAndruavUnit.getIsCGS()) || (mAndruavUnit.getIsShutdown()))
        {

            color = getResources().getColor(R.color.btn_TXT_GREY_DARK);
        }
        else
        {
            color = getResources().getColor(R.color.btn_TXT_BLUE_DARKER);
        }
        mSaveMission.setTextColor(color);
    }

    private void setFollowButton(){

        int color;
        if ((mAndruavUnit.getIsCGS()) || (mAndruavUnit.getIsShutdown()))
        {

            color = getResources().getColor(R.color.btn_TXT_GREY_DARK);
        }
        else
        {
            color = getResources().getColor(R.color.btn_TXT_BLUE_DARKER);
        }
        mFollow.setTextColor(color);
    }

    private void setClearButton(){

        int color;
        if ((mAndruavUnit.getIsCGS()) || (mAndruavUnit.getIsShutdown()))
        {

            color = getResources().getColor(R.color.btn_TXT_GREY_DARK);
        }
        else
        {
            color = getResources().getColor(R.color.btn_TXT_BLUE_DARKER);
        }
        mClearMission.setTextColor(color);
    }




}
