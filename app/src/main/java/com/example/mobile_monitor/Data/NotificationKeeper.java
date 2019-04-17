package com.example.mobile_monitor.Data;

import java.io.Serializable;
import java.util.ArrayList;

public class NotificationKeeper implements Serializable {
    public String packageName;//Notification from package
    public String title;//Title of the notification
    public String text;//text present in the notification
    public ArrayList<String> messageLines;//notification's extra lines
    public String dateAndTime;//date and time of the notification, when it got posted on device

    public NotificationKeeper(){
        messageLines=new ArrayList<>();
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ArrayList<String> getMessageLines() {
        return messageLines;
    }

    public void setMessageLines(ArrayList<String> messageLines) {
        this.messageLines = messageLines;
    }



}
