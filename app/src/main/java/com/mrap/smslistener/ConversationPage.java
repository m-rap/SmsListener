package com.mrap.smslistener;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcelable;
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

import com.mrap.smslistener.model.Callback;
import com.mrap.smslistener.model.MergedSmsSqliteHandler;
import com.mrap.smslistener.model.Sms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationPage extends Fragment {
    private static final String TAG = "ChatPage";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String addr;
    private int jumpToPos;
    private Callback onSmssUpdated;
    private Callback onContactsUpdated;
    private Parcelable recyclerViewState = null;
    private boolean willJump = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.page_conversation, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        addr = getArguments().getString("addr");
        jumpToPos = getArguments().getInt("jumpToPos", -1);
        if (jumpToPos > 0) {
            willJump = true;
        }

        Toolbar toolbar = view.findViewById(R.id.conv_toolbar);
        toolbar.setTitle(activity.getContactName(addr));
        activity.setSupportActionBar(toolbar);

        onSmssUpdated = new Callback() {
            @Override
            public void onCallback(Object arg) {
                executorService.submit(() -> {
                    ArrayList<Sms> smss = refresh();
                    renderMsgs(smss);
                });
            }
        };
        onContactsUpdated = new Callback() {
            @Override
            public void onCallback(Object args) {
                executorService.submit(() -> {
                    checkOrRefresh();
                });
            }
        };

        activity.onSmssUpdatedListeners.add(onSmssUpdated);
        activity.onContactUpdatedListeners.add(onContactsUpdated);

        RecyclerView listMsg = view.findViewById(R.id.conv_listChat);
        if (listMsg.getAdapter() == null) {
            listMsg.setAdapter(new MessageAdapter(getContext(), new ArrayList<>()));
        }

        listMsg.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(-1) && newState==RecyclerView.SCROLL_STATE_IDLE) {
                    loadMore();
                }
            }
        });

        executorService.submit(() -> {
            checkOrRefresh();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity activity = (MainActivity) getActivity();
        activity.onSmssUpdatedListeners.remove(onSmssUpdated);
        activity.onContactUpdatedListeners.remove(onContactsUpdated);
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

                if (willJump) {
                    listMsg.scrollToPosition(smss.size() - 1 - jumpToPos);
                    willJump = false;
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
////                    getArguments().getString("addr"), 0, limit);
////            ArrayList<SmsSqliteHandler.Sms> smss = SmsSqliteHandler.getSmssFromContentResolver(activity,
////                    getArguments().getString("addr"), 0, 10);
//        ArrayList<Sms> smss = Sms.getSmssFromBoth(smsDb, activity,
//                addr, 0, limit);
//        smsDb.close();

        HashMap<String, ArrayList<Sms>> smssMap = activity.getSmssMap();
        Integer currPage = activity.getSmsMapCurrPage().get(addr);
        if (currPage == null) {
            currPage = 0;
        }
        int limit = MainActivity.ROW_PER_PAGE;
        if (jumpToPos > 0) {
            int jumpPage = jumpToPos / limit;
            if (currPage < jumpPage) {
                currPage = jumpPage;
            }
        }

        synchronized (smssMap) {
            SQLiteDatabase smsDb = MergedSmsSqliteHandler.openDb(activity);
            ArrayList<Sms> smss = MergedSmsSqliteHandler.getSmss(smsDb, "sms_addr = '" +
                    addr + "'", "sms_timems DESC", 0,
                    (currPage + 1) * limit, null);
            smsDb.close();

            Log.d(TAG, "loaded " + smss.size() + " smss");

            smssMap.put(addr, smss);

            return smss;
        }
    }

    private void loadMore() {
        MainActivity activity = (MainActivity) getActivity();

        HashMap<String, ArrayList<Sms>> smssMap = activity.getSmssMap();
        ArrayList<Sms> smssTmp = smssMap.get(addr);
        if (smssTmp == null) {
            Log.d(TAG, "smss null, refreshing");
            smssTmp = refresh();
            renderMsgs(smssTmp);
            return;
        }

        ArrayList<Sms> smss = smssTmp;

        Integer currPageTmp = activity.getSmsMapCurrPage().get(addr);
        if (currPageTmp == null) {
            currPageTmp = 0;
        }
        currPageTmp++;
        int currPage = currPageTmp;
        activity.getSmsMapCurrPage().put(addr, currPage);
        int limit = MainActivity.ROW_PER_PAGE;

        View view = getView();
        RecyclerView listMsg = view.findViewById(R.id.conv_listChat);
        MessageAdapter adapter = (MessageAdapter) listMsg.getAdapter();

        activity.runOnUiThread(() -> {
//            recyclerViewState = listMsg.getLayoutManager().onSaveInstanceState();
            listMsg.setAdapter(new MessageAdapter(activity, new ArrayList<>()));
        });

        SQLiteDatabase smsDb = MergedSmsSqliteHandler.openDb(activity);
        smss.addAll(MergedSmsSqliteHandler.getSmss(smsDb, "sms_addr = '" +
                        addr + "'", "sms_timems DESC", currPage * limit,
                limit, null));

        activity.runOnUiThread(() -> {
            listMsg.setAdapter(adapter);
//            listMsg.getLayoutManager().onRestoreInstanceState(recyclerViewState);
            listMsg.scrollToPosition(smss.size() - ((currPage) * limit));
        });
    }
}
