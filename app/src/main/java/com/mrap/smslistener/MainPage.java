package com.mrap.smslistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

        refresh();
    }

    private void renderSmss(String[][] smss, View[] views) {
        MainActivity activity = (MainActivity) getActivity();
        activity.runOnUiThread(() -> {
            View view = getView();
            LinearLayout listSms = view.findViewById(R.id.main_listSms);

            listSms.removeAllViews();

//            Context themedCtx = new ContextThemeWrapper(activity, R.style.Theme_SmsListener);

            for (int i = 0; i < smss.length; i++) {
                String[] sms = smss[i];

                View viewSms = views[i];
                viewSms.setOnClickListener(v -> {
                    ChatPage chatPage = new ChatPage();
                    Bundle args = new Bundle();
                    args.putString("number", sms[0]);
                    chatPage.setArguments(args);
                    activity.getSupportFragmentManager().
                            beginTransaction().
                            replace(R.id.actmain_framelayout, chatPage, null).
                            addToBackStack(null).
                            commit();
                });
//                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//                layoutParams.topMargin = layoutParams.rightMargin = layoutParams.leftMargin =
//                        (int)activity.convertDipToPix(activity, 10);
//                if (i == smss.length - 1) {
//                    layoutParams.bottomMargin = (int)activity.convertDipToPix(activity, 10);
//                }
//                viewSms.setLayoutParams(layoutParams);

                TextView numTv = viewSms.findViewById(R.id.idxrow_address);
                numTv.setText(sms[0] + ", " + sms[2]);

                TextView msgTv = viewSms.findViewById(R.id.idxrow_content);
                msgTv.setText(sms[1]);

//            Log.d(TAG, sms[0] + ":::" + sms[1]);

//                Log.d(TAG, "add view " + i + " to list");
                listSms.addView(viewSms);
            }
        });
    }

    private void refresh() {
        executorService.submit(() -> {
            MainActivity activity = (MainActivity) getActivity();
            SmsDb smsDb = new SmsDb();
            smsDb.openDb(activity);
            String[][] smss = smsDb.getSmss();
            smsDb.close();

            Log.d(TAG, "loaded sms " + smss.length);

            View[] views = new View[smss.length];
            for (int i = 0; i < smss.length; i++) {
                views[i] = activity.getLayoutInflater().
                        inflate(R.layout.view_sms_index_row, null);
            }

            renderSmss(smss, views);
        });
    }
}
