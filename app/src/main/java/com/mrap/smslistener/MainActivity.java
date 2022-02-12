package com.mrap.smslistener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mrap.smslistener.model.Sms;
import com.mrap.smslistener.model.SmsModel;
import com.mrap.smslistener.model.SmsModel_v1;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int REQCODE_REQPERM = 0;

    private ArrayList<Sms> lastSmss = null;
    private HashMap<String, ArrayList<Sms>> smssMap = new HashMap<>();
    public final int ROW_PER_PAGE = 100;
    private int lastSmsCurrPage = 0;
    private HashMap<String, Integer> smsMapCurrPage = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        Sms.migrateToLatestVersion(this);

        SQLiteDatabase smsDb = SmsModel_v1.openDb(this);
        SmsModel_v1.cleanupDbAlreadyInContentResolver(smsDb, this);
        smsDb.close();

        MainPage mainPage = new MainPage();
        getSupportFragmentManager().
                beginTransaction().
                replace(R.id.actmain_framelayout, mainPage, null).
                commit();
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