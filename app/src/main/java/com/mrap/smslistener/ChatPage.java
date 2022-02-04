package com.mrap.smslistener;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private void renderMsgs(ArrayList<SmsModel.Sms> smss, View[] views) {
        MainActivity activity = (MainActivity) getActivity();
        activity.runOnUiThread(() -> {
            View view = getView();
            LinearLayout listMsg = view.findViewById(R.id.cht_listChat);
            listMsg.removeAllViews();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm");

            for (int i = 0; i < smss.size(); i++) {
                SmsModel.Sms sms = smss.get(i);
                View viewSms = views[i];

//                registerForContextMenu(viewSms);
                viewSms.setOnLongClickListener(v -> {
//                    activity.openContextMenu(viewSms);
                    ClipboardManager clipboardManager = (ClipboardManager) activity.
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, sms.body));
                    Toast.makeText(activity, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                    return true;
                });

                TextView textView = viewSms.findViewById(R.id.msg_txtMsg);
                textView.setText(sms.body);

                textView = viewSms.findViewById(R.id.msg_txtDate);
                textView.setText(sdf.format(sms.date));

                listMsg.addView(viewSms);
            }
        });
    }

    private void refresh() {
        executorService.submit(() -> {
            MainActivity activity = (MainActivity) getActivity();

            SQLiteDatabase smsDb = SmsModel.openDb(activity);
            ArrayList<SmsModel.Sms> smss = SmsModel.getSmss(smsDb,
                    getArguments().getString("addr"), 0, 1000);
            smsDb.close();

            View[] views = new View[smss.size()];
            for (int i = 0; i < smss.size(); i++) {
                views[i] = activity.getLayoutInflater().inflate(R.layout.view_message, null);
            }

            renderMsgs(smss, views);
        });
    }
}
