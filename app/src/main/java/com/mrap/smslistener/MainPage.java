package com.mrap.smslistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.mrap.smslistener.model.MergedSmsSqliteHandler;
import com.mrap.smslistener.model.Sms;
import com.mrap.smslistener.model.SmsSqliteHandler_v1;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainPage extends Fragment {

    private static final String TAG = "MainPage";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private BroadcastReceiver smsUIReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_main, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        Toolbar toolbar = view.findViewById(R.id.main_toolbar);
        toolbar.setTitle("Sms Listener");
        activity.setSupportActionBar(toolbar);
        smsUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }

                executorService.submit(() -> {
                    ArrayList<Sms> smss = refresh();
                    renderSmss(smss);
                });
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("smsUIReceiver");
        activity.registerReceiver(smsUIReceiver, intentFilter);

        RecyclerView recyclerView = view.findViewById(R.id.main_listConversation);
        recyclerView.setAdapter(new ConversationAdapter(activity, new ArrayList<>()));

        executorService.submit(() -> {
            checkOrRefresh();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(smsUIReceiver);
    }

    private void renderSmss(ArrayList<Sms> smss) {
        MainActivity activity = (MainActivity) getActivity();
        activity.runOnUiThread(() -> {
            long startMs = System.currentTimeMillis();

            View view = getView();

            RecyclerView recyclerView = view.findViewById(R.id.main_listConversation);
            recyclerView.setAdapter(new ConversationAdapter(activity, smss));

            Log.d(TAG, "rendering recycler view for " + (System.currentTimeMillis() - startMs) + " ms");
        });
    }

    private void checkOrRefresh() {
        MainActivity activity = (MainActivity) getActivity();

        ArrayList<Sms> lastSmss = activity.getLastSmss();
        if (lastSmss == null) {
            lastSmss = refresh();
        }

        renderSmss(lastSmss);
    }

    private ArrayList<Sms> refresh() {
        MainActivity activity = (MainActivity) getActivity();
        try {
//            SQLiteDatabase smsDb = SmsSqliteHandler_v1.openDb(activity);
////            ArrayList<SmsSqliteHandler.Sms> lastSmss = SmsSqliteHandler.getLastSmss(smsDb, 0, 1000);
////                ArrayList<SmsSqliteHandler.Sms> lastSmss = SmsSqliteHandler.getLastSmssFromContentResolver(activity,
////                        0, 1000);
//            ArrayList<Sms> lastSmss = Sms.getLastSmssFromBoth(smsDb, activity,
//                    0, 1000);
//            smsDb.close();

            SQLiteDatabase smsDb = MergedSmsSqliteHandler.openDb(activity);
            ArrayList<Sms> lastSmss = MergedSmsSqliteHandler.getLastSmss(smsDb, 0, 1000,
                    null);
            smsDb.close();

            Log.d(TAG, "loaded sms " + lastSmss.size());

            activity.setLastSmss(lastSmss);

            return lastSmss;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}
