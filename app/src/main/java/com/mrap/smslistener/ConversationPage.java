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
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.mrap.smslistener.model.MergedSmsSqliteHandler;
import com.mrap.smslistener.model.Sms;
import com.mrap.smslistener.model.SmsSqliteHandler_v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationPage extends Fragment {
    private static final String TAG = "ChatPage";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String addr;
    private BroadcastReceiver smsUIReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_conversation, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        addr = getArguments().getString("addr");

        Toolbar toolbar = view.findViewById(R.id.conv_toolbar);
        toolbar.setTitle(addr);
        activity.setSupportActionBar(toolbar);

        smsUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }

                executorService.submit(() -> {
                    ArrayList<Sms> smss = refresh();
                    renderMsgs(smss);
                });
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("smsUIReceiver");
        activity.registerReceiver(smsUIReceiver, intentFilter);

        RecyclerView listMsg = view.findViewById(R.id.conv_listChat);
        listMsg.setAdapter(new MessageAdapter(getContext(), new ArrayList<>()));

        executorService.submit(() -> {
            checkOrRefresh();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(smsUIReceiver);
    }

    private void renderMsgs(ArrayList<Sms> smss) {
        MainActivity activity = (MainActivity) getActivity();
        activity.runOnUiThread(() -> {
            View view = getView();
            LinearLayout container = view.findViewById(R.id.conv_container);

            RecyclerView listMsg = view.findViewById(R.id.conv_listChat);
            container.setVisibility(View.INVISIBLE);
            MessageAdapter adapter = new MessageAdapter(activity, smss);
            listMsg.setAdapter(adapter);

            listMsg.post(() -> {
                LinearLayout.LayoutParams listMsgLp = (LinearLayout.LayoutParams) listMsg.getLayoutParams();
                int scrollViewBotMargin = listMsgLp.bottomMargin;
                int listMsgHeight = listMsg.getHeight();
                int contianerHeight = container.getHeight() - scrollViewBotMargin;
                if (listMsgHeight < contianerHeight) {
                    listMsgLp.height = listMsgHeight;
                    listMsg.setLayoutParams(listMsgLp);
                } else {
                    listMsg.scrollToPosition(adapter.getItemCount() - 1);
                }
                container.setVisibility(View.VISIBLE);
            });
        });
    }

    private void checkOrRefresh() {
        MainActivity activity = (MainActivity) getActivity();

        HashMap<String, ArrayList<Sms>> smssMap = activity.getSmssMap();

        ArrayList<Sms> smss = smssMap.get(addr);

        if (smss == null) {
            Log.d(TAG, "smss null, refreshing");
            smss = refresh();
            smssMap.put(addr, smss);
        }

//            long startMs = System.currentTimeMillis();
//            View[] views = new View[smss.size()];
//            for (int i = 0; i < smss.size(); i++) {
//                views[i] = activity.getLayoutInflater().inflate(R.layout.view_message, null);
//            }
//            Log.d(TAG, "inflated views for " + (System.currentTimeMillis() - startMs) + " ms");

        Log.d(TAG, "rendering " + smss.size() + " smss");
        renderMsgs(smss);
    }

    private ArrayList<Sms> refresh() {
        Log.d(TAG, "refresh");
        MainActivity activity = (MainActivity) getActivity();

//        int currPage = activity.getSmsMapCurrPage().get(addr);

//        SQLiteDatabase smsDb = SmsSqliteHandler_v1.openDb(activity);
////            ArrayList<SmsSqliteHandler.Sms> smss = SmsSqliteHandler.getSmss(smsDb,
////                    getArguments().getString("addr"), 0, 1000);
////            ArrayList<SmsSqliteHandler.Sms> smss = SmsSqliteHandler.getSmssFromContentResolver(activity,
////                    getArguments().getString("addr"), 0, 10);
//        ArrayList<Sms> smss = Sms.getSmssFromBoth(smsDb, activity,
//                addr, 0, 1000);
//        smsDb.close();

        SQLiteDatabase smsDb = MergedSmsSqliteHandler.openDb(activity);
        ArrayList<Sms> smss = MergedSmsSqliteHandler.getSmss(smsDb, "sms_addr = '" +
                        addr + "'", "sms_timems DESC", 0, 1000, null);
        smsDb.close();

        Log.d(TAG, "loaded " + smss.size() + " smss");

        return smss;
    }

//    private void loadMore() {
//        MainActivity activity = (MainActivity) getActivity();
//
//        int currPage = activity.getSmsMapCurrPage().get(addr);
//        HashMap<String, ArrayList<SmsSqliteHandler.Sms>> smssMap = activity.getSmssMap();
//        ArrayList<SmsSqliteHandler.Sms> smss = smssMap.get(addr);
//        if (smss == null) {
//            checkOrRefresh();
//            return;
//        }
//
//        SQLiteDatabase smsDb = SmsSqliteHandler.openDb(activity);
//        smss.addAll(SmsSqliteHandler.getSmssFromBoth(smsDb))
//    }
}
