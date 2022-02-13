package com.mrap.smslistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mrap.smslistener.model.MergedSmsSqliteHandler;

public class SmsContentProviderChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MergedSmsSqliteHandler.syncContentProvider(context);
        Intent broadcastLocal = new Intent();
        broadcastLocal.setAction("smsUIReceiver");
        context.sendBroadcast(broadcastLocal);
    }
}
