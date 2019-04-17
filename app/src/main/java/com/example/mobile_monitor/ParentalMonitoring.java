package com.example.mobile_monitor;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;
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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ParentalMonitoring extends AppCompatActivity {

    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();


    UsageStatsManager mUsageStatsManager;

    View view;

    UsageListAdapter mUsageListAdapter;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;

    private AlertDialog enableAppUsageAlertDialog;

    long yesterdayInMills = System.currentTimeMillis() - 43200000;
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    Calendar cal = Calendar.getInstance();

    static final int APP_USAGE_REQUEST = 1;
    Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parental_monitoring);

        if(!isAccessGranted()) {
            enableAppUsageAlertDialog = buildAppUsageAlertDialog();
            enableAppUsageAlertDialog.show();
        }

        populateRecyclerView();

        mUsageStatsManager = (UsageStatsManager) this
                .getSystemService(Context.USAGE_STATS_SERVICE);

        timedUsageCheck();//is method me firebase krna hai.

    }

    private void timedUsageCheck() {
        timer = new Timer();
        TimerTask minuteCheck = new TimerTask() {
            @Override
            public void run() {

                String interval ="Daily";
                StatsUsageInterval statsUsageInterval = StatsUsageInterval
                        .getValue(interval);
                if (statsUsageInterval != null) {
                    List<UsageStats> usageStatsList =
                            getUsageStatistics(statsUsageInterval.mInterval);
                    Collections.sort(usageStatsList, new LastTimeLaunchedComparatorDesc());

                    //usageStatsList ko upload krna hai.

                    //getPackageName and getLastTimeUsed 2 methods hai jo String aur long return karri.
                    Log.i("&&&InsideTimer::", ""+usageStatsList.get(0).getPackageName());
                    long lastTime = usageStatsList.get(0).getLastTimeUsed();
                    formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
                    Log.i("&&&ITLastTime::", ""+formatter.format(lastTime));

                    //--->Firebase ka code yaha likhna<----- to wo timer ke andar rahega.
                }
            }
        };
        timer.schedule(minuteCheck, 01, 1000*30); //1000*60 - every minute.
    }

    public void showApps(View view){
        String interval ="Daily";
        StatsUsageInterval statsUsageInterval = StatsUsageInterval
                .getValue(interval);
        if (statsUsageInterval != null) {
            List<UsageStats> usageStatsList =
                    getUsageStatistics(statsUsageInterval.mInterval);
            Collections.sort(usageStatsList, new LastTimeLaunchedComparatorDesc());
            updateAppsList(usageStatsList);
        }
    }

    private void populateRecyclerView() {

        mUsageListAdapter = new UsageListAdapter();
        mRecyclerView = findViewById(R.id.recyclerview_app_usage);
        mLayoutManager = mRecyclerView.getLayoutManager();
        mRecyclerView.scrollToPosition(0);
        mRecyclerView.setAdapter(mUsageListAdapter);
    }

    public List<UsageStats> getUsageStatistics(int intervalType) {
        // Get the app statistics since one year ago from the current time.
//        cal.add(Calendar.DAY_OF_MONTH, -4);

        List<UsageStats> queryUsageStats = mUsageStatsManager
                .queryUsageStats(intervalType, yesterdayInMills,
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

            long timeInMills = usageStatsList.get(i).getLastTimeUsed();
            cal.setTimeInMillis(timeInMills);
            Log.i("##Day::", formatter.format(cal.getTime())+"");

            if(timeInMills>=yesterdayInMills) {
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
                Log.i("***CustomUsageStats::: ", "");
                mUsageListAdapter.setCustomUsageStatsList(customUsageStatsList);
                mUsageListAdapter.notifyDataSetChanged();
                mRecyclerView.scrollToPosition(0);
            }
            else
                break;
//            mDatabase.child("Test01").setValue("");
        }
    }

    private static class LastTimeLaunchedComparatorDesc implements Comparator<UsageStats> {

        @Override
        public int compare(UsageStats left, UsageStats right) {
            return Long.compare(right.getLastTimeUsed(), left.getLastTimeUsed());
        }
    }

    enum StatsUsageInterval {
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

    private AlertDialog buildAppUsageAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.app_usage_permission);
        alertDialogBuilder.setMessage(R.string.app_usage_permission_explanation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), APP_USAGE_REQUEST);
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return(alertDialogBuilder.create());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == APP_USAGE_REQUEST) {
            // Make sure the request was successful
            if (isAccessGranted()) {
                showApps(view);
            }
        }
    }
}
