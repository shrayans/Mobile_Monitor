package com.example.mobile_monitor;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.mobile_monitor.Data.NotificationKeeper;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static final int RC_SIGN_IN = 1;

    FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    //FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    Button on_off_switch;

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    private AlertDialog enableNotificationListenerAlertDialog;

    private NotificationReceiver nReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mFirebaseAuth=FirebaseAuth.getInstance();
        mAuthStateListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=firebaseAuth.getCurrentUser();
                if(user!=null){
                    setContentView(R.layout.activity_main);
                    Toast.makeText(MainActivity.this,"WELCOME! You are signed in",Toast.LENGTH_LONG).show();

                    on_off_switch=findViewById(R.id.on_off_switch);
                    SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                    String operation=pref.getString("ReadNotifications",null);
                    if(operation!=null&&operation.equals("Activated")) {
                        on_off_switch.setText("Turn Off");
                        on_off_switch.setBackgroundResource(R.drawable.circle_red);
                        on_off_switch.setPadding(0,20,0,0);
                    }

                    // If the user did not turn the notification listener service on we prompt him to do so
                    if(!isNotificationServiceEnabled()){
                        enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
                        enableNotificationListenerAlertDialog.show();
                    }

                    nReceiver = new NotificationReceiver();
                    IntentFilter filter = new IntentFilter();
                    filter.addAction("com.example.mobile_monitor.NOTIFICATION_LISTENER_EXAMPLE");
                    registerReceiver(nReceiver,filter);
                }
                else{
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.sign_out_menu:
                Toast.makeText(this,"You are Signed Out",Toast.LENGTH_LONG).show();
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nReceiver);
    }

    /**
     * Is Notification Service Enabled.
     * Verifies if the notification listener service is enabled.
     * Got it from: https://github.com/kpbird/NotificationListenerService-Example/blob/master/NLSExample/src/main/java/com/kpbird/nlsexample/NLService.java
     * @return True if eanbled, false otherwise.
     */
    private boolean isNotificationServiceEnabled(){
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Build Notification Listener Alert Dialog.
     * Builds the alert dialog that pops up if the user has not turned
     * the Notification Listener Service on yet.
     * @return An alert dialog which leads to the notification enabling screen
     */
    private AlertDialog buildNotificationServiceAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.notification_listener_service);
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
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

    public void switchClicked(View view) {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        if(on_off_switch.getText().toString().equals("Turn On")) {
            on_off_switch.setText("Turn Off");
            on_off_switch.setBackgroundResource(R.drawable.circle_red);
            on_off_switch.setPadding(0,20,0,0);
            editor.putString("ReadNotifications", "Activated");
            editor.commit();
        }
        else{
            on_off_switch.setText("Turn On");
            on_off_switch.setBackgroundResource(R.drawable.circle_green);
            on_off_switch.setPadding(0,20,0,0);
            editor.putString("ReadNotifications", "Deactivated");
            editor.commit();
        }
    }

    class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationKeeper notificationKeeper = (NotificationKeeper) intent.getSerializableExtra("notificationReceived");
            Log.i("messageInMain","package: "+notificationKeeper.packageName);
            Log.i("messageInMain","title: "+notificationKeeper.title);
            Log.i("messageInMain","text: "+notificationKeeper.text);
            for(int i=1;i<=notificationKeeper.messageLines.size();i++){
                Log.i("messageInMain",i+". MessaageLine: "+notificationKeeper.messageLines.get(i-1));
            }
            //Object(notificationKeeper) is to be uploaded to fire-base

            SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
            String operation=pref.getString("ReadNotifications",null);
            if(operation!=null&&operation.equals("Activated")) {
                String currentDateTime;

                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                currentDateTime = sdf1.format(new Date());

                mDatabase.child("Notification").child(user.getUid()).child(currentDateTime).setValue(notificationKeeper);
            }
        }
    }
}