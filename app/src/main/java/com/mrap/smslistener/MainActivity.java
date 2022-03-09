package com.mrap.smslistener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQCODE_REQPERM = 0;
    public static final int ROW_PER_PAGE = 50;
    public static SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

    private ArrayList<Sms> lastSmss = null;
    private final HashMap<String, ArrayList<Sms>> smssMap = new HashMap<>();
    public int lastSmsCurrPage = 0;
    private HashMap<String, Integer> smsMapCurrPage = new HashMap<>();
    private HashMap<String, String> contactNames = new HashMap<>();
    private SyncService syncService = null;
    private boolean receiverIsRegistered = false;
    private boolean navigatedToMain = false;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public final ArrayList<Callback> onSmssUpdatedListeners = new ArrayList<>();
    public final ArrayList<Callback> onContactUpdatedListeners = new ArrayList<>();

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
    private ServiceConnection syncServiceConnection = null;

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
                PackageManager.PERMISSION_GRANTED) {
            permErrorMsg = "App needs receive sms permission.";
            permsToReq = new String[] {Manifest.permission.READ_CONTACTS};
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

        Callback syncCallback = arg -> {
            Log.d(TAG, "got sync callback. notify to sms updated listeners: " +
                    onSmssUpdatedListeners.size());
            for (Callback listener : onSmssUpdatedListeners) {
                listener.onCallback(null);
            }
        };

        Log.d(TAG, "binding sync service");
        syncServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "sync service connected");
                syncService = ((SyncService.LocalBinder) service).getService();
                Log.d(TAG, "sync listener: " + syncService.syncListeners.size());
                if (!syncService.syncListeners.contains(syncCallback)) {
                    Log.d(TAG, "adding sync listener");
                    syncService.syncListeners.add(syncCallback);
                }

                if (navigatedToMain) {
                    return;
                }

                Callback navigateToMain = arg -> {
                    executorService.submit(() -> {
                        loadContacts();
                        for (Callback listener : onContactUpdatedListeners) {
                            listener.onCallback(null);
                        }
                    });
                    Log.d(TAG, "navigating to main");
                    navigatedToMain = true;
                    MainPage mainPage = new MainPage();
                    getSupportFragmentManager().
                            beginTransaction().
                            replace(R.id.actmain_framelayout, mainPage, null).
                            commit();
                };

                if (!MergedSmsSqliteHandler.isDbExists(syncService)) {
                    Log.d(TAG, "db not exists, performing sync then navigate to main");
                    syncService.sync(navigateToMain);
                } else {
                    navigateToMain.onCallback(null);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "sync service disconnected");
                syncService.syncListeners.remove(syncCallback);
                syncService = null;
            }
        };
        bindService(new Intent(this, SyncService.class), syncServiceConnection, BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("smsUIReceiver");
        registerReceiver(smsUIReceiver, intentFilter);
        receiverIsRegistered = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (syncServiceConnection != null) {
            Log.d(TAG, "unbinding sync service");
            unbindService(syncServiceConnection);
        }
        if (receiverIsRegistered) {
            unregisterReceiver(smsUIReceiver);
        }
        startService(new Intent(this, SyncService.class));
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

    public String getContactName(final String phoneNumber) {
        String res = contactNames.get(phoneNumber);
        if (res == null) {
            res = phoneNumber;
        }
        return res;
    }

    private void loadContacts() {
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

        String[] projection = new String[] {
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        };
        Cursor cursor = getContentResolver().query(uri, projection,
                null, null, null);

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                do {
                    contactNames.put(cursor.getString(0),
                            cursor.getString(1));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    public ArrayList<MergedSmsSqliteHandler.SearchResult> searchSms(
            String keyword, boolean[] abortSearch,
            Callback<MergedSmsSqliteHandler.SearchResult> onEach) {
        ArrayList<MergedSmsSqliteHandler.SearchResult> res = new ArrayList<>();
        for (String addr : smssMap.keySet()) {
            ArrayList<Sms> smss = smssMap.get(addr);
            for (int i = 0; i < smss.size(); i++) {
                Sms currSms = smss.get(i);
                int i_ = i;
                if (currSms.body.matches("(?i).*" + keyword + ".*")) {
                    MergedSmsSqliteHandler.SearchResult searchResult = new
                            MergedSmsSqliteHandler.SearchResult() {{
                                sms = currSms;
                                rowNum = i_;
                            }};
                    if (onEach != null) {
                        onEach.onCallback(searchResult);
                    }
                    res.add(searchResult);
                }
                if (abortSearch[0]) {
                    break;
                }
            }
            if (abortSearch[0]) {
                break;
            }
        }

        SQLiteDatabase db = MergedSmsSqliteHandler.openDb(this);
        res.addAll(MergedSmsSqliteHandler.searchSms(db, keyword, abortSearch, onEach)); // will raise duplicated data
        db.close();

        return res;
    }
}