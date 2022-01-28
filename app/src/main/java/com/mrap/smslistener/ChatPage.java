package com.mrap.smslistener;

import android.os.Bundle;
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

public class ChatPage extends Fragment {
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_chat, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        activity.getSupportActionBar().setTitle(getArguments().getString("number"));
        refresh();
    }

    private void renderMsgs(String[][] smss, View[] views) {
        MainActivity activity = (MainActivity) getActivity();
        activity.runOnUiThread(() -> {
            View view = getView();
            LinearLayout listMsg = view.findViewById(R.id.cht_listChat);
            listMsg.removeAllViews();
            for (int i = 0; i < smss.length; i++) {
                View viewSms = views[i];
                String[] sms = smss[i];

                TextView textView = viewSms.findViewById(R.id.msg_txtMsg);
                textView.setText(sms[1]);

                textView = viewSms.findViewById(R.id.msg_txtDate);
                textView.setText(sms[2]);

                listMsg.addView(viewSms);
            }
        });
    }

    private void refresh() {
        executorService.submit(() -> {
            MainActivity activity = (MainActivity) getActivity();

            SmsDb smsDb = new SmsDb();
            smsDb.openDb(activity);
            String[][] smss = smsDb.getSmss(getArguments().getString("number"));

            View[] views = new View[smss.length];
            for (int i = 0; i < smss.length; i++) {
                views[i] = activity.getLayoutInflater().inflate(R.layout.view_message, null);
            }

            renderMsgs(smss, views);
        });
    }
}
