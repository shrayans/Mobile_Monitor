package com.example.mobile_monitor;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.SettingInjectorService;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ParentalMonitoring extends AppCompatActivity {

    UsageStatsManager mUsageStatsManager;

    ListView intervalListView;

    UsageListAdapter mUsageListAdapter;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parental_monitoring);

        if(!isAccessGranted()) {
            Toast.makeText(this,
                    getString(R.string.explanation_access_to_appusage_is_not_enabled),
                    Toast.LENGTH_LONG).show();
            Intent usageIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(usageIntent);
        }

        populateListView();

        populateRecyclerView();

        mUsageStatsManager = (UsageStatsManager) this
                .getSystemService(Context.USAGE_STATS_SERVICE);

        intervalListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String interval =(intervalListView.getItemAtPosition(position).toString());
                StatsUsageInterval statsUsageInterval = StatsUsageInterval
                        .getValue(interval);
                if (statsUsageInterval != null) {
                    List<UsageStats> usageStatsList =
                            getUsageStatistics(statsUsageInterval.mInterval);
                    Collections.sort(usageStatsList, new LastTimeLaunchedComparatorDesc());
                    updateAppsList(usageStatsList);
                }
            }
        });


    }

    private void populateRecyclerView() {

        mUsageListAdapter = new UsageListAdapter();
        mRecyclerView = findViewById(R.id.recyclerview_app_usage);
        mLayoutManager = mRecyclerView.getLayoutManager();
        mRecyclerView.scrollToPosition(0);
        mRecyclerView.setAdapter(mUsageListAdapter);
    }

    private void populateListView() {
        intervalListView = findViewById(R.id.list_view);
        List<String> interval_arrayList = new ArrayList<String>();
        interval_arrayList.add("Daily");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                interval_arrayList );

        intervalListView.setAdapter(arrayAdapter);
    }

    public List<UsageStats> getUsageStatistics(int intervalType) {
        // Get the app statistics since one year ago from the current time.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);

        List<UsageStats> queryUsageStats = mUsageStatsManager
                .queryUsageStats(intervalType, cal.getTimeInMillis(),
                        System.currentTimeMillis());

        if (queryUsageStats.size() == 0) {
            Toast.makeText(this,
                    getString(R.string.explanation_access_to_appusage_is_not_enabled),
                    Toast.LENGTH_LONG).show();

        }
        return queryUsageStats;
    }

    private boolean isAccessGranted() {
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = 0;
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.KITKAT) {
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        applicationInfo.uid, applicationInfo.packageName);
            }
            return (mode == AppOpsManager.MODE_ALLOWED);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    void updateAppsList(List<UsageStats> usageStatsList) {
        List<CustomUsageStats> customUsageStatsList = new ArrayList<>();
        for (int i = 0; i < usageStatsList.size(); i++) {
            CustomUsageStats customUsageStats = new CustomUsageStats();
            customUsageStats.usageStats = usageStatsList.get(i);
            try {
                Drawable appIcon = this.getPackageManager()
                        .getApplicationIcon(customUsageStats.usageStats.getPackageName());
                customUsageStats.appIcon = appIcon;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("App Icon :: ", String.format("App Icon is not found for %s",
                        customUsageStats.usageStats.getPackageName()));
                customUsageStats.appIcon = this
                        .getDrawable(R.drawable.ic_default_app_launcher);
            }
            customUsageStatsList.add(customUsageStats);
            Log.i("***CustomUsageStats::: ", customUsageStatsList.toString());
            mUsageListAdapter.setCustomUsageStatsList(customUsageStatsList);
            mUsageListAdapter.notifyDataSetChanged();
            mRecyclerView.scrollToPosition(0);
        }
    }

    private static class LastTimeLaunchedComparatorDesc implements Comparator<UsageStats> {

        @Override
        public int compare(UsageStats left, UsageStats right) {
            return Long.compare(right.getLastTimeUsed(), left.getLastTimeUsed());
        }
    }

    static enum StatsUsageInterval {
        DAILY("Daily", UsageStatsManager.INTERVAL_DAILY),
        WEEKLY("Weekly", UsageStatsManager.INTERVAL_WEEKLY),
        MONTHLY("Monthly", UsageStatsManager.INTERVAL_MONTHLY),
        YEARLY("Yearly", UsageStatsManager.INTERVAL_YEARLY);

        private int mInterval;
        private String mStringRepresentation;

        StatsUsageInterval(String stringRepresentation, int interval) {
            mStringRepresentation = stringRepresentation;
            mInterval = interval;
        }

        static StatsUsageInterval getValue(String stringRepresentation) {
            for (StatsUsageInterval statsUsageInterval : values()) {
                if (statsUsageInterval.mStringRepresentation.equals(stringRepresentation)) {
                    return statsUsageInterval;
                }
            }
            return null;
        }
    }
}
