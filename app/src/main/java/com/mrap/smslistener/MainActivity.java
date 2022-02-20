package com.mrap.smslistener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mrap.smslistener.model.Callback;
import com.mrap.smslistener.model.MergedSmsSqliteHandler;
import com.mrap.smslistener.model.Sms;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQCODE_REQPERM = 0;

    private ArrayList<Sms> lastSmss = null;
    private final HashMap<String, ArrayList<Sms>> smssMap = new HashMap<>();
    public final int ROW_PER_PAGE = 100;
    private int lastSmsCurrPage = 0;
    private HashMap<String, Integer> smsMapCurrPage = new HashMap<>();

    private final ArrayList<Callback> onSmssUpdatedListeners = new ArrayList<>();

    private final BroadcastReceiver smsUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (smssMap) {
                lastSmss = null;
                smssMap.clear();
                for (Callback listener : onSmssUpdatedListeners) {
                    listener.onCallback(null);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "sync is running " + MergedSmsSqliteHandler.isSyncRunning());

        String permErrorMsg = null;
        String[] permsToReq = null;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) !=
                PackageManager.PERMISSION_GRANTED) {
            permErrorMsg = "App needs read sms permission.";
            permsToReq = new String[] {Manifest.permission.READ_SMS};
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) !=
                PackageManager.PERMISSION_GRANTED) {
            permErrorMsg = "App needs receive sms permission.";
            permsToReq = new String[] {Manifest.permission.RECEIVE_SMS};
        }

        if (permErrorMsg != null) {
            TextView textView = new TextView(new ContextThemeWrapper(this,
                    R.style.Theme_SmsListener));
            textView.setText(permErrorMsg);
            textView.setGravity(Gravity.CENTER);
            ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            textView.setLayoutParams(lp);
            setContentView(textView);

            ActivityCompat.requestPermissions(this, permsToReq,
                    REQCODE_REQPERM);
            return;
        }

        setContentView(R.layout.activity_main);

        View view = findViewById(R.id.actmain_cover);
        view.setOnClickListener(v -> {});

//        Sms.migrateToLatestVersion(this);
//
//        SQLiteDatabase smsDb = SmsSqliteHandler_v1.openDb(this);
//        SmsSqliteHandler_v1.cleanupDbAlreadyInContentResolver(smsDb, this);
//        smsDb.close();

        if (!MergedSmsSqliteHandler.isDbExists(this)) {
//            MergedSmsSqliteHandler.migrateToMergedSms(this);
            MergedSmsSqliteHandler.syncContentProvider(this);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("smsUIReceiver");
        registerReceiver(smsUIReceiver, intentFilter);

        MainPage mainPage = new MainPage();
        getSupportFragmentManager().
                beginTransaction().
                replace(R.id.actmain_framelayout, mainPage, null).
                commit();
    }

    public static class SyncService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            MergedSmsSqliteHandler.syncContentProvider(this);
            return START_NOT_STICKY;
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

//    @Override
//    public void onBackPressed() {
//        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
//            super.onBackPressed();
//            return;
//        }
//
//        View view = findViewById(R.id.actmain_cover);
//        view.setVisibility(View.VISIBLE);
//
//        new Thread(() -> {
//            MergedSmsSqliteHandler.syncContentProvider(this);
//            runOnUiThread(() -> {
//                finish();
//            });
//        }).start();
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        unregisterReceiver(smsUIReceiver);
        startService(new Intent(this, SyncService.class));
    }

    public ArrayList<Callback> getOnSmssUpdatedListeners() {
        return onSmssUpdatedListeners;
    }

    public ArrayList<Sms> getLastSmss() {
        return lastSmss;
    }

    public void setLastSmss(ArrayList<Sms> lastSmss) {
        this.lastSmss = lastSmss;
    }

    public HashMap<String, ArrayList<Sms>> getSmssMap() {
        return smssMap;
    }

    public HashMap<String, Integer> getSmsMapCurrPage() {
        return smsMapCurrPage;
    }

    public float convertDipToPix(Context context, int dip){
        float scale = context.getResources().getDisplayMetrics().density;
        return (float)dip * scale;
    }

    private SimpleDateFormat sdfSameDay = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat sdfSameYear = new SimpleDateFormat("MMM d");
    private SimpleDateFormat sdfDiffYear = new SimpleDateFormat("MMM d, yyyy");

    public String niceDate(long date) {
        Date now = new Date();

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        int nowDayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int nowYear = cal.get(Calendar.YEAR);

        cal.setTimeInMillis(date);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int year = cal.get(Calendar.YEAR);

        if (dayOfMonth == nowDayOfMonth) {
            return sdfSameDay.format(date);
        } else if (year == nowYear) {
            return sdfSameYear.format(date);
        } else {
            return sdfDiffYear.format(date);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQCODE_REQPERM) {
            if (grantResults.length == 0) {
                return;
            }
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            recreate();
        }
    }
}