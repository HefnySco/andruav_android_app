package rcmobile.FPV.activities.remote;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import java.util.Locale;

import rcmobile.FPV.R;
import rcmobile.andruavmiddlelibrary.factory.util.GMail;

/**
 * Created by mhefny on 5/26/16.
 */
public class RemoteControlSettingGCSActivityTab  extends AppCompatActivity implements ActionBar.TabListener, RemoteControlGamePadTestFragment.OnFragmentInteractionListener, RemoteModeFragment.OnFragmentInteractionListener,RemoteControlTestFragment.OnFragmentInteractionListener{

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    Fragment[] mFragment= new Fragment[2];


    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        final Fragment currentFragment = mFragment[mViewPager.getCurrentItem()];

        if (currentFragment instanceof RemoteControlGamePadTestFragment)
        {
            final boolean handled = ((RemoteControlGamePadTestFragment) currentFragment).onGenericMotionEvent(event);
            if (handled) return true;
        }



        return super.onGenericMotionEvent(event);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control_setting_activity_tab);

        if (savedInstanceState != null) {
            return ;
        }
        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_remote_control_setting_activity_tab, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case  R.id.action_save:
                if ((mViewPager!= null) && (mFragment[mViewPager.getCurrentItem()] != null))
                {
                   // ((IFragmentSave) mFragment[mViewPager.getCurrentItem()]).Save();
                }
                return true;
            case R.id.action_refresh:
                if ((mViewPager!= null) && (mFragment[mViewPager.getCurrentItem()] != null)) {
                   // ((IFragmentSave) mFragment[mViewPager.getCurrentItem()]).Refresh();
                }
                return true;
            case R.id.mi_Help:
                GMail.sendGMail(this, getString(R.string.email_title), getString(R.string.email_to), getString(R.string.email_subject), getString(R.string.email_body), null);
                return true;

            case R.id.mi_remotescreen:
                startActivity(new Intent(RemoteControlSettingGCSActivityTab.this, RemoteControlActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position)
            {
                case 0:
                    mFragment[position] =  RemoteModeFragment.newInstance("1","1");
                    break;
                case 1:
                    mFragment[position] =  RemoteControlGamePadTestFragment.newInstance("1","1");
                    break;
                default:
                    mFragment[position] =  RemoteModeFragment.newInstance("1","1");
                    break;
            }
            return mFragment[position];
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_activity_remote_control_setting_GCS_activity_tab_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_activity_remote_control_setting_GCS_activity_tab_section3).toUpperCase(l);


            }
            return null;
        }



    }

}
