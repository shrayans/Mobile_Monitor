package com.example.mobile_monitor.Data;

import java.io.Serializable;
import java.util.ArrayList;

public class NotificationKeeper implements Serializable {
    public String packageName;//Notification from package
    public String title;//Title of the notification
    public String text;//text present in the notification
    public ArrayList<String> messageLines;//notification's extra lines
    public NotificationKeeper(){
        messageLines=new ArrayList<>();
    }
}
