package com.mrap.smslistener;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationPage extends Fragment {
    private static final String TAG = "ChatPage";
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_conversation, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        activity.getSupportActionBar().setTitle(getArguments().getString("addr"));

        RecyclerView listMsg = view.findViewById(R.id.cht_listChat);
        listMsg.setAdapter(new MessageAdapter(getContext(), new ArrayList<>()));

        refresh();
    }

    private void renderMsgs(ArrayList<SmsModel.Sms> smss) {
        MainActivity activity = (MainActivity) getActivity();
        activity.runOnUiThread(() -> {
            View view = getView();
            LinearLayout container = view.findViewById(R.id.cht_container);

            RecyclerView listMsg = view.findViewById(R.id.cht_listChat);
            container.setVisibility(View.INVISIBLE);
            listMsg.setAdapter(new MessageAdapter(activity, smss));

//            listMsg.removeAllViews();
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm");
//
//            for (int i = smss.size() - 1; i >= 0; i--) {
//                SmsModel.Sms sms = smss.get(i);
//                View viewSms = views[i];
//
////                registerForContextMenu(viewSms);
//                viewSms.setOnLongClickListener(v -> {
////                    activity.openContextMenu(viewSms);
//                    ClipboardManager clipboardManager = (ClipboardManager) activity.
//                            getSystemService(Context.CLIPBOARD_SERVICE);
//                    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, sms.body));
//                    Toast.makeText(activity, "Copied to clipboard", Toast.LENGTH_SHORT).show();
//                    return true;
//                });
//
//                TextView textView = viewSms.findViewById(R.id.msg_txtMsg);
//                textView.setText(sms.body);
//
//                textView = viewSms.findViewById(R.id.msg_txtDate);
//                textView.setText(sdf.format(sms.date));
//
//                listMsg.addView(viewSms);
//            }

            listMsg.post(() -> {
                LinearLayout.LayoutParams listMsgLp = (LinearLayout.LayoutParams) listMsg.getLayoutParams();
                int scrollViewBotMargin = listMsgLp.bottomMargin;
                int listMsgHeight = listMsg.getHeight();
                int contianerHeight = container.getHeight() - scrollViewBotMargin;
                if (listMsgHeight < contianerHeight) {
                    listMsgLp.height = listMsgHeight;
                    listMsg.setLayoutParams(listMsgLp);
                } else {
//                    listMsg.scrollTo(0, listMsgHeight);
                    listMsg.scrollToPosition(smss.size() - 1);
                }
                container.setVisibility(View.VISIBLE);
            });
        });
    }

    private void refresh() {
        executorService.submit(() -> {
            MainActivity activity = (MainActivity) getActivity();

            SQLiteDatabase smsDb = SmsModel.openDb(activity);
//            ArrayList<SmsModel.Sms> smss = SmsModel.getSmss(smsDb,
//                    getArguments().getString("addr"), 0, 1000);
//            ArrayList<SmsModel.Sms> smss = SmsModel.getSmssFromContentResolver(activity,
//                    getArguments().getString("addr"), 0, 10);
            ArrayList<SmsModel.Sms> smss = SmsModel.getSmssFromBoth(smsDb, activity,
                    getArguments().getString("addr"), 0, 1000);
            smsDb.close();

//            long startMs = System.currentTimeMillis();
//            View[] views = new View[smss.size()];
//            for (int i = 0; i < smss.size(); i++) {
//                views[i] = activity.getLayoutInflater().inflate(R.layout.view_message, null);
//            }
//            Log.d(TAG, "inflated views for " + (System.currentTimeMillis() - startMs) + " ms");

            renderMsgs(smss);
        });
    }
}
