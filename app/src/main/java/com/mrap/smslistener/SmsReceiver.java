package com.mrap.smslistener;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.mrap.smslistener.model.MergedSmsSqliteHandler;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        Bundle bundle = intent.getExtras();
        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");

        SmsMessage[] smss = new SmsMessage[pdus.length];

        SQLiteDatabase mergedSmsDb = MergedSmsSqliteHandler.openDb(context);

        for (int i = 0; i < smss.length; i++) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                smss[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
            } else {
                smss[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
            }

            String number = smss[i].getOriginatingAddress();
            String message = smss[i].getMessageBody();
            long timestamp = smss[i].getTimestampMillis();

            Log.d(TAG, "got sms from: " + number);

            MergedSmsSqliteHandler.insertSms(mergedSmsDb, number, message, timestamp, false);

            Notification notification = NotificationFactory.createNotification(context, MainActivity.class, 0, number, message);
            NotificationManager notifManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            notifManager.notify(0, notification);
        }

        mergedSmsDb.close();

        Intent broadcastLocal = new Intent();
        broadcastLocal.setAction("smsUIReceiver");
        context.sendBroadcast(broadcastLocal);
    }
}
