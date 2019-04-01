package com.example.mobile_monitor;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import com.example.mobile_monitor.Data.NotificationKeeper;

public class NotificationListenerExampleService extends NotificationListenerService {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //start sticky means service will be explicity started and stopped
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn){
        NotificationKeeper notificationKeeper=new NotificationKeeper();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Bundle bundle = sbn.getNotification().extras;
            int count=1;
            notificationKeeper.packageName=sbn.getPackageName();
            Log.i("messageFrom",count+". Application:"+notificationKeeper.packageName);
            for (String key : bundle.keySet()) {
                count++;
                Log.i("messageFrom", count+". "+key +": " + bundle.get(key));
                if(key.equals("android.title")) notificationKeeper.title=(String)bundle.get(key);
                else if(key.equals("android.text")) notificationKeeper.text=(String)bundle.get(key);
            }
        }
        /*if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Bundle extras = sbn.getNotification().extras;
            Log.i("messageFrom","title: "+extras.getString("android.title"));
            Log.i("messageFrom","package: "+sbn.getPackageName());
            Log.i("messageFrom","text: "+extras.getCharSequence("android.text").toString());
            if(extras.getCharSequence(Notification.EXTRA_BIG_TEXT)!=null)
                Log.i("messageFrom","extra text: "+extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString());
            /*content_title = extras.get(Notification.EXTRA_TITLE).toString();
            a = extras.get(Notification.EXTRA_BIG_TEXT).toString();
            b= extras.get(Notification.EXTRA_TEXT).toString();
            c= extras.get(Notification.EXTRA_SUMMARY_TEXT).toString();
            icon = (Bitmap) extras.get(Notification.EXTRA_LARGE_ICON);
            lines = (CharSequence[]) extras.get(Notification.EXTRA_TEXT_LINES);
        }*/
        Bundle extras = sbn.getNotification().extras;
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if(lines != null && lines.length > 0) {
            Log.i("messageFromAppended",lines.length+" "+lines.toString());
            StringBuilder sb = new StringBuilder();
            for (CharSequence msg : lines)
                if (!TextUtils.isEmpty(msg)) {
                    notificationKeeper.messageLines.add(msg.toString());
                    sb.append(msg.toString());
                    sb.append('\n');
                }
            Log.i("messageFromAppended",sb.toString().trim());
        }
        Intent i = new  Intent("com.example.mobile_monitor.NOTIFICATION_LISTENER_EXAMPLE");
        i.putExtra("notificationReceived",notificationKeeper);
        sendBroadcast(i);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn){
        //NotificationRemoved
    }

}