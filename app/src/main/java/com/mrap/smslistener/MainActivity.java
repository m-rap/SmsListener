package com.mrap.smslistener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainPage mainPage = new MainPage();
        getSupportFragmentManager().
                beginTransaction().
                replace(R.id.actmain_framelayout, mainPage, null).
                commit();
    }

    public float convertDipToPix(Context context, int dip){
        float scale = context.getResources().getDisplayMetrics().density;
        return (float)dip * scale;
    }
}