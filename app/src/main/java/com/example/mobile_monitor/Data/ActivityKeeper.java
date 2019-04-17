package com.example.mobile_monitor.Data;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ActivityKeeper implements Serializable {
    public String appPackage;
    public String time;
    public static String changeDateFormat(long lastTimeUsed){
        DateFormat df=new SimpleDateFormat();
        return df.format(new Date(lastTimeUsed));
    }
    public static boolean toStore(long lastTimeUsed){
        boolean flag=false;
        //long today= System.currentTimeMillis();
        Calendar today = Calendar.getInstance();
        today.set(Calendar.MILLISECOND, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.HOUR_OF_DAY, 0);
        long todayTime=today.getTimeInMillis();
        if(todayTime<lastTimeUsed) flag=true;
        return flag;
    }
}
