package com.mrap.smslistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SmsContentProviderChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, SyncService.class));
    }
}
