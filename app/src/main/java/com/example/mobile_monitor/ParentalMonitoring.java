package com.example.mobile_monitor;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.SettingInjectorService;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;

import com.example.mobile_monitor.Data.ActivityKeeper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ParentalMonitoring extends AppCompatActivity {

    UsageStatsManager mUsageStatsManager;

    //ListView intervalListView;

    //UsageListAdapter mUsageListAdapter;
    //RecyclerView mRecyclerView;
    //RecyclerView.LayoutManager mLayoutManager;
    Button switchButton;
    TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parental_monitoring);

        switchButton=findViewById(R.id.on_off_switch_parental);
        status=findViewById(R.id.parentalMonitoringView);

        if(!isAccessGranted()) {
            Toast.makeText(this,
                    getString(R.string.explanation_access_to_appusage_is_not_enabled),
                    Toast.LENGTH_LONG).show();
            Intent usageIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(usageIntent);
        }

        //populateListView();

        //populateRecyclerView();

        mUsageStatsManager = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);

        /*intervalListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // If we get killed, after returning from here, restart
                //return START_STICKY;

                String interval ="Daily";
                StatsUsageInterval statsUsageInterval = StatsUsageInterval
                        .getValue(interval);
                if (statsUsageInterval != null) {
                    List<UsageStats> usageStatsList =
                            getUsageStatistics(statsUsageInterval.mInterval);
                    Collections.sort(usageStatsList, new LastTimeLaunchedComparatorDesc());
                    updateAppsList(usageStatsList);
                }
                checkAndCall();
            }
        });*/


    }

    /*private void populateRecyclerView() {

        mUsageListAdapter = new UsageListAdapter();
        mRecyclerView = findViewById(R.id.recyclerview_app_usage);
        mLayoutManager = mRecyclerView.getLayoutManager();
        mRecyclerView.scrollToPosition(0);
        mRecyclerView.setAdapter(mUsageListAdapter);
    }*/

    /*private void populateListView() {
        intervalListView = findViewById(R.id.list_view);
        List<String> interval_arrayList = new ArrayList<String>();
        interval_arrayList.add("Daily");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                interval_arrayList );

        intervalListView.setAdapter(arrayAdapter);
    }*/

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
        //List<CustomUsageStats> customUsageStatsList = new ArrayList<>();
        ArrayList<ActivityKeeper> listOfApps=new ArrayList<>();
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
            //Log.i("***NOTLIST::: ", customUsageStats.usageStats.toString());
            ActivityKeeper appDetails=new ActivityKeeper();
            appDetails.appPackage=customUsageStats.usageStats.getPackageName();
            long lastTimeUsed=customUsageStats.usageStats.getLastTimeUsed();
            appDetails.time=ActivityKeeper.changeDateFormat(lastTimeUsed);
            Log.i("***NOTLIST::: ",appDetails.appPackage);
            Log.i("***NOTLIST::: ", ""+appDetails.time);
            if(ActivityKeeper.toStore(lastTimeUsed)) listOfApps.add(appDetails);
            //Log.i("***NOTLIST::: ", customUsageStats.appIcon.toString());
            //customUsageStatsList.add(customUsageStats);
            //Log.i("***CustomUsageStats::: ", customUsageStatsList.toString());
            //mUsageListAdapter.setCustomUsageStatsList(customUsageStatsList);
            //mUsageListAdapter.notifyDataSetChanged();
            //mRecyclerView.scrollToPosition(0);
        }
        FirebaseDatabase.getInstance().getReference().child("Parental Monitoring").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(listOfApps);
    }

    public void switchClicked(View view) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        if(switchButton.getText().toString().equals("Turn On")) {
            String interval ="Daily";
            StatsUsageInterval statsUsageInterval = StatsUsageInterval
                    .getValue(interval);
            if (statsUsageInterval != null) {
                List<UsageStats> usageStatsList =
                        getUsageStatistics(statsUsageInterval.mInterval);
                Collections.sort(usageStatsList, new LastTimeLaunchedComparatorDesc());
                updateAppsList(usageStatsList);
            }
            checkAndCall();
            switchButton.setText("Turn Off");
            status.setText("Parental Monitoring is Activated");
            switchButton.setBackgroundResource(R.drawable.circle_red);
            switchButton.setPadding(0,20,0,0);
            Toast.makeText(this, "Parental Monitoring is ON now", Toast.LENGTH_LONG).show();
            editor.putString("ParentalMonitoring", "Activated");
            editor.commit();
        }
        else{
            switchButton.setText("Turn On");
            status.setText("Parental Monitoring is Deactivated");
            Toast.makeText(this, "Parental Monitoring is OFF now", Toast.LENGTH_LONG).show();
            switchButton.setBackgroundResource(R.drawable.circle_green);
            switchButton.setPadding(0,20,0,0);
            editor.putString("ParentalMonitoring", "Deactivated");
            editor.commit();
        }
    }

    public void checkAndCall(){
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        String operation=pref.getString("ParentalMonitoring",null);
        if(operation!=null&&operation.equals("Activated")) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //performe the deskred task
                    Log.i("***Called", "Calling again");
                    String interval = "Daily";//(intervalListView.getItemAtPosition(position).toString());
                    StatsUsageInterval statsUsageInterval = StatsUsageInterval.getValue(interval);
                    if (statsUsageInterval != null) {
                        List<UsageStats> usageStatsList =
                                getUsageStatistics(statsUsageInterval.mInterval);
                        Collections.sort(usageStatsList, new LastTimeLaunchedComparatorDesc());
                        updateAppsList(usageStatsList);
                    }
                    SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                    String operation=pref.getString("ParentalMonitoring",null);
                    if(operation!=null&&operation.equals("Activated")) handler.postDelayed(this, 1000 * 60 * 1);
                }
            }, 1000 * 60 * 1);
        }
    }


    private static class LastTimeLaunchedComparatorDesc implements Comparator<UsageStats> {

        @Override
        public int compare(UsageStats left, UsageStats right) {
            return Long.compare(right.getLastTimeUsed(), left.getLastTimeUsed());
        }
    }

    public static enum StatsUsageInterval {
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
