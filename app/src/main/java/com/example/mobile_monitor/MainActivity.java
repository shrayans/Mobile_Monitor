package com.example.mobile_monitor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobile_monitor.Data.NotificationKeeper;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static final int RC_SIGN_IN = 1;

    FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    //FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    String uid;

    Button on_off_switch;
    TextView status;

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
                    status=findViewById(R.id.mobileMonitoringView);
                    Toast.makeText(MainActivity.this,"WELCOME! You are signed in",Toast.LENGTH_LONG).show();
                    MainActivity.this.user = user;

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
            case R.id.parental_monitoring:
                Intent intent = new Intent(this, ParentalMonitoring.class);
                startActivity(intent);
                return true;
            case R.id.group_menu:
                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                String operation=pref.getString("GroupID",null);
                if(operation!=null&&!operation.equals("")) {
                    showGroupID(this,operation);
                }
                else showDialogForGroups(this,"Select Option","Create a group of devices or join a group of devices");
                return true;
            case R.id.sign_out_menu:
                SharedPreferences prefSignOut = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                SharedPreferences.Editor editor = prefSignOut.edit();
                editor.putString("GroupID","");
                editor.putString("ReadNotifications", "");
                editor.commit();
                Toast.makeText(this,"You are Signed Out",Toast.LENGTH_LONG).show();
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showGroupID(Activity activity, final String groupID) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Your Group ID");
        builder.setMessage("Copy Your Group ID\n"+groupID);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setNegativeButton("Copy ID", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("ClipBoard Label", groupID);
                clipboard.setPrimaryClip(clip);
            }
        });
        builder.show();
    }

    public void showDialogForGroups(final Activity activity, String title, CharSequence message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (title != null) builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("Create Group", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDatabase.child("Groups").child("group-"+user.getUid()).child("1").setValue(user.getUid());
                SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("GroupID", "group-"+user.getUid());
                editor.commit();
                showGroupID(activity,"group-"+user.getUid());
                dialog.cancel();
            }
        });
        builder.setNegativeButton("Join Group", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                takeGroupID(activity);
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void takeGroupID(final Activity activity){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Group ID");
        // Set up the input
        final EditText inputField = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        inputField.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(inputField);
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                if(inputField.getText().toString().trim().equalsIgnoreCase("")) {
                    Toast.makeText(activity, "This field can not be blank", Toast.LENGTH_SHORT).show();
                }
                else{
                    final String inputID=inputField.getText().toString();
                    if(!inputID.startsWith("group-")){
                        Toast.makeText(activity, "Enter a valid group ID", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("Groups");
                        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.hasChild(inputID)) {
                                    //add the current user(user.getUid()) to this node(inputID)
                                    Log.e("MyLog " ,"Count: "+snapshot.getChildrenCount());
                                    String oldValue="";
                                    for (DataSnapshot postSnapshot: snapshot.getChildren()) {
                                        String key=postSnapshot.getKey();
                                        if(key.equals(inputID)) {
                                            //oldValue = postSnapshot.getValue(String.class);
                                            //Log.e("MyLog", "key/value: " + key + "/" + oldValue);
                                            DatabaseReference newRef=FirebaseDatabase.getInstance().getReference().child("Groups").child(inputID);
                                            newRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    long count=dataSnapshot.getChildrenCount();
                                                    count++;
                                                    mDatabase.child("Groups").child(inputID).child(String.valueOf(count)).setValue(user.getUid());
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                                    Log.e("The read failed: " ,databaseError.getMessage());
                                                }
                                            });
                                        }
                                    }
                                    SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                                    SharedPreferences.Editor editor = pref.edit();
                                    editor.putString("GroupID",inputID);
                                    editor.commit();
                                    dialog.cancel();
                                    showGroupID(activity,inputID);
                                }
                                else{
                                    Toast.makeText(activity, "There is no group with this ID", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.e("The read failed: " ,databaseError.getMessage());
                            }
                        });
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
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
            status.setText("Mobile Monitoring is Activated");
            Toast.makeText(this, "Mobile Monitoring is ON now", Toast.LENGTH_SHORT).show();
            on_off_switch.setBackgroundResource(R.drawable.circle_red);
            on_off_switch.setPadding(0,20,0,0);
            editor.putString("ReadNotifications", "Activated");
            editor.commit();
        }
        else{
            on_off_switch.setText("Turn On");
            status.setText("Mobile Monitoring is Deactivated");
            Toast.makeText(this, "Mobile Monitoring is OFF now", Toast.LENGTH_SHORT).show();
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
                //String currentDateTime;

                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                notificationKeeper.dateAndTime = sdf1.format(new Date());

                mDatabase.child("Notification").child(user.getUid()).push().setValue(notificationKeeper);
            }
        }
    }
}