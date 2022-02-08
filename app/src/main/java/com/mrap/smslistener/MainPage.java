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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainPage extends Fragment {

    private static final String TAG = "MainPage";
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_main, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        activity.getSupportActionBar().setTitle("Sms Listener");
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
        getActivity().registerReceiver(smsUIReceiver, intentFilter);

        RecyclerView recyclerView = view.findViewById(R.id.main_listConversation);
        recyclerView.setAdapter(new ConversationAdapter(activity, new ArrayList<>()));

        refresh();

//        executorService.submit(() -> {
//            SmsModel smsDb = new SmsModel();
//            smsDb.openDb(activity);
//            ArrayList<SmsModel.Sms> dbSmss = smsDb.getSmss();
//            ArrayList<SmsModel.Sms> matchedSmss = smsDb.getSmssFromContentResolver(dbSmss);
//
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
//
//            String info = "Db smss (" + dbSmss.size() + "): \n";
//            for (SmsModel.Sms sms : dbSmss) {
//                info += sms.addr + ":" + sms.date + ": ==" + sms.body + "==\n";
//            }
//
//            Log.d(TAG, info);
//
//            info = "Matched smss (" + matchedSmss.size() + "): \n";
//            for (SmsModel.Sms sms : matchedSmss) {
//                info += sms.addr + ":" + sms.date + ": ==" + sms.body + "==\n";
//            }
//
//            Log.d(TAG, info);
//        });
    }

    private void renderSmss(ArrayList<SmsModel.Sms> smss) {
        MainActivity activity = (MainActivity) getActivity();
        activity.runOnUiThread(() -> {
            long startMs = System.currentTimeMillis();

            View view = getView();

            RecyclerView recyclerView = view.findViewById(R.id.main_listConversation);
            recyclerView.setAdapter(new ConversationAdapter(activity, smss));

            Log.d(TAG, "rendering recycler view for " + (System.currentTimeMillis() - startMs) + " ms");

//            LinearLayout listSms = view.findViewById(R.id.main_listConversation);
//
//            listSms.removeAllViews();
//
////            Context themedCtx = new ContextThemeWrapper(activity, R.style.Theme_SmsListener);
//
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm");
//
//            for (int i = 0; i < smss.size(); i++) {
//                SmsModel.Sms sms = smss.get(i);
//
//                View viewSms = views[i];
//                viewSms.setOnClickListener(v -> {
//                    ConversationPage conversationPage = new ConversationPage();
//                    Bundle args = new Bundle();
//                    args.putString("addr", sms.addr);
//                    conversationPage.setArguments(args);
//                    activity.getSupportFragmentManager().
//                            beginTransaction().
//                            replace(R.id.actmain_framelayout, conversationPage, null).
//                            addToBackStack(null).
//                            commit();
//                });
////                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
////                layoutParams.topMargin = layoutParams.rightMargin = layoutParams.leftMargin =
////                        (int)activity.convertDipToPix(activity, 10);
////                if (i == smss.length - 1) {
////                    layoutParams.bottomMargin = (int)activity.convertDipToPix(activity, 10);
////                }
////                viewSms.setLayoutParams(layoutParams);
//
//                TextView numTv = viewSms.findViewById(R.id.idxrow_address);
//                numTv.setText(sms.addr + ", " + sdf.format(sms.date));
//
//                TextView msgTv = viewSms.findViewById(R.id.idxrow_content);
//                msgTv.setText(sms.body);
//
////            Log.d(TAG, sms[0] + ":::" + sms[1]);
//
////                Log.d(TAG, "add view " + i + " to list");
//                listSms.addView(viewSms);
//            }
        });
    }

    private void refresh() {
        executorService.submit(() -> {
            MainActivity activity = (MainActivity) getActivity();

            try {
                SQLiteDatabase smsDb = SmsModel.openDb(activity);
//            ArrayList<SmsModel.Sms> smss = SmsModel.getLastSmss(smsDb, 0, 1000);
//                ArrayList<SmsModel.Sms> smss = SmsModel.getLastSmssFromContentResolver(activity,
//                        0, 1000);
                ArrayList<SmsModel.Sms> smss = SmsModel.getLastSmssFromBoth(smsDb, activity,
                        0, 1000);
                smsDb.close();

                Log.d(TAG, "loaded sms " + smss.size());

//                long startMs = System.currentTimeMillis();
//                View[] views = new View[smss.size()];
//                for (int i = 0; i < smss.size(); i++) {
//                    views[i] = activity.getLayoutInflater().
//                            inflate(R.layout.view_conversation_item, null);
//                }
//                Log.d(TAG, "inflated views for " + (System.currentTimeMillis() - startMs) + " ms");

                renderSmss(smss);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
