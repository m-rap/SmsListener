package com.mrap.smslistener;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.mrap.smslistener.model.Callback;
import com.mrap.smslistener.model.MergedSmsSqliteHandler;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncService extends Service {
    private final static String TAG = "SyncService";

    public class LocalBinder extends Binder {
        public SyncService getService() {
            return SyncService.this;
        }
    }

    private Binder binder = new LocalBinder();
    private ExecutorService service = Executors.newSingleThreadExecutor();
    public final ArrayList<Callback> syncListeners = new ArrayList<>();
    private boolean isDestroyed = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        Log.d(TAG, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sync(null);
        return START_NOT_STICKY;
    }

    public void sync(Callback callback) {
        service.submit(() -> {
            Log.d(TAG, "syncing (isDestroyed " + isDestroyed + ")");
            MergedSmsSqliteHandler.syncContentProvider(this);
            Log.d(TAG, "notify to sync listeners: " + syncListeners.size());
            if (callback != null) {
                callback.onCallback(null);
            }
            for (Callback listener : syncListeners) {
                listener.onCallback(null);
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
