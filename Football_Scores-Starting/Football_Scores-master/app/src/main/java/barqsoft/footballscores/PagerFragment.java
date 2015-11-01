package barqsoft.footballscores;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.service.myFetchService;

/**
 * Created by yehya khaled on 2/27/2015.
 */
public class PagerFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private final String LOG_TAG = PagerFragment.class.getSimpleName();
    public static final int NUM_PAGES = 5;
    public ViewPager mPagerHandler;
    private myPageAdapter mPagerAdapter;
    private MainScreenFragment[] viewFragments = new MainScreenFragment[5];
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.pager_fragment, container, false);
        mPagerHandler = (ViewPager) rootView.findViewById(R.id.pager);
        mPagerAdapter = new myPageAdapter(getChildFragmentManager());
        for (int i = 0;i < NUM_PAGES;i++)
        {
            Date fragmentdate = new Date(System.currentTimeMillis()+((i-2)*86400000));
            SimpleDateFormat mformat = new SimpleDateFormat("yyyy-MM-dd");
            viewFragments[i] = new MainScreenFragment();
            viewFragments[i].setFragmentDate(mformat.format(fragmentdate));
        }
        mPagerHandler.setAdapter(mPagerAdapter);
        mPagerHandler.setCurrentItem(MainActivity.current_fragment);
        //Check network status before starting service
        if(Utility.isNetworkAvailable(getActivity())){
            update_scores();
        }
        else{
            Toast.makeText(getActivity(), R.string.error_no_internet,Toast.LENGTH_SHORT).show();
        }
        return rootView;
    }

    private void update_scores()
    {
        Intent service_start = new Intent(getActivity(), myFetchService.class);
        getActivity().startService(service_start);
    }

    private class myPageAdapter extends FragmentStatePagerAdapter
    {
        @Override
        public Fragment getItem(int i)
        {
            return viewFragments[i];
        }

        @Override
        public int getCount()
        {
            return NUM_PAGES;
        }

        public myPageAdapter(FragmentManager fm)
        {
            super(fm);
        }
        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position)
        {
            return getDayName(getActivity(),System.currentTimeMillis()+((position-2)*86400000));
        }
        public String getDayName(Context context, long dateInMillis) {
            // If the date is today, return the localized version of "Today" instead of the actual
            // day name.

            Time t = new Time();
            t.setToNow();
            int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
            int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
            if (julianDay == currentJulianDay) {
                return context.getString(R.string.today);
            } else if ( julianDay == currentJulianDay +1 ) {
                return context.getString(R.string.tomorrow);
            }
             else if ( julianDay == currentJulianDay -1)
            {
                return context.getString(R.string.yesterday);
            }
            else
            {
                Time time = new Time();
                time.setToNow();
                // Otherwise, the format is just the day of the week (e.g "Wednesday".
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
                return dayFormat.format(dateInMillis);
            }
        }
    }

    @Override
    public void onResume() {
        Log.v(LOG_TAG, Integer.toString(Utility.getScoreStatus(getActivity())));
        Utility.resetScoreStatus(getActivity());
        Log.v(LOG_TAG, Integer.toString(Utility.getScoreStatus(getActivity())));
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.v("Preference Change", key + " " + Utility.getScoreStatus(getActivity()));
        if (key.equals(getString(R.string.pref_score_status_key))) {
            int message = 0;
            Log.v("Preference Change", key + " inside " + Utility.getScoreStatus(getActivity()));
            @myFetchService.ScoreStatus int scoreStatus = Utility.getScoreStatus(getActivity());
            switch (scoreStatus) {
                case myFetchService.SCORE_STATUS_OK:
                    return;
                case myFetchService.SCORE_STATUS_SERVER_DOWN:
                    message = R.string.error_score_list_server_down;
                    break;
                case myFetchService.SCORE_STATUS_SERVER_INVALID:
                    message = R.string.error_score_list_server_error;
                    break;
                case myFetchService.SCORE_STATUS_INVALID:
                    message = R.string.error_score_list_server_error;
                    break;
                default:
                    if (!Utility.isNetworkAvailable(getActivity())) {
                        message = R.string.error_no_internet;
                    }
            }
            if(message != 0){
                Log.v("Preference Change", key + " " + message + " " + Utility.getScoreStatus(getActivity()));
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
