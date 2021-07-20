package com.mrap.smslistener;

import androidx.appcompat.app.AppCompatActivity;

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

        BroadcastReceiver smsUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }

                refresh();
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("smsUIReceiver");
        registerReceiver(smsUIReceiver, intentFilter);

        refresh();
    }

    public float convertDipToPix(Context context, int dip){
        float scale = context.getResources().getDisplayMetrics().density;
        return (float)dip * scale;
    }

    public void refresh() {
        LinearLayout listSms = findViewById(R.id.main_listSms);

        SmsDb smsDb = new SmsDb();
        smsDb.openDb(this);
        String[][] smss = smsDb.getSmss();
        smsDb.close();

        listSms.removeAllViews();
        
        Context themedCtx = new ContextThemeWrapper(this, R.style.Theme_SmsListener);

        for (int i = 0; i < smss.length; i++) {
            String[] sms = smss[i];

            LinearLayout viewSms = new LinearLayout(themedCtx);
            viewSms.setOrientation(LinearLayout.VERTICAL);
            viewSms.setBackgroundResource(R.drawable.box);
            viewSms.setClickable(true);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = layoutParams.rightMargin = layoutParams.leftMargin = (int)convertDipToPix(this, 10);
            if (i == smss.length - 1) {
                layoutParams.bottomMargin = (int)convertDipToPix(this, 10);
            }
            viewSms.setLayoutParams(layoutParams);

            TextView numTv = new TextView(themedCtx);
            LinearLayout.LayoutParams numTvPar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            numTvPar.topMargin = numTvPar.rightMargin = numTvPar.leftMargin = (int)convertDipToPix(this, 10);
            numTv.setLayoutParams(numTvPar);
            numTv.setText(sms[0] + ", " + sms[2]);

            viewSms.addView(numTv);

            TextView msgTv = new TextView(themedCtx);
            LinearLayout.LayoutParams msgTvPar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            msgTvPar.topMargin = msgTvPar.rightMargin = msgTvPar.bottomMargin = msgTvPar.leftMargin = (int)convertDipToPix(this, 10);
            msgTv.setLayoutParams(msgTvPar);
            msgTv.setText(sms[1]);

//            Log.d(TAG, sms[0] + ":::" + sms[1]);

            viewSms.addView(msgTv);

            listSms.addView(viewSms);
        }
    }
}