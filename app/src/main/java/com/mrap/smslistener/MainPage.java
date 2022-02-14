package com.mrap.smslistener;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.mrap.smslistener.model.Callback;
import com.mrap.smslistener.model.MergedSmsSqliteHandler;
import com.mrap.smslistener.model.Sms;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainPage extends Fragment {

    private static final String TAG = "MainPage";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Callback onSmssUpdated;

    private Parcelable recyclerViewState = null;

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
        setHasOptionsMenu(true);

        onSmssUpdated = new Callback() {
            @Override
            public void onCallback(Object arg) {
                executorService.submit(() -> {
                    ArrayList<Sms> smss = refresh();
                    renderSmss(smss);
                });
            }
        };
        activity.getOnSmssUpdatedListeners().add(onSmssUpdated);

        RecyclerView recyclerView = view.findViewById(R.id.main_listConversation);
//        if (recyclerView.getAdapter() == null) {
//            Log.d(TAG, "adapter is null, assigning");
//            recyclerView.setAdapter(new ConversationAdapter(activity, new ArrayList<>()));
//        }
        if (recyclerViewState != null) {
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }

        executorService.submit(() -> {
            checkOrRefresh();
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.main_sync) {
            executorService.submit(() -> {
                MainActivity activity = (MainActivity) getActivity();
                int res = MergedSmsSqliteHandler.syncContentProvider(activity);
                if (res == 0) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Sync done", Toast.LENGTH_SHORT).show();
                    });
                    ArrayList<Sms> smss = refresh();
                    renderSmss(smss);
                } else if (res == -2) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Sync error", Toast.LENGTH_SHORT).show();
                    });
                }
            });
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((MainActivity) getActivity()).getOnSmssUpdatedListeners().remove(onSmssUpdated);
        RecyclerView recyclerView = getView().findViewById(R.id.main_listConversation);
        recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
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
