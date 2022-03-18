package com.mrap.smslistener;

import android.content.Intent;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
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
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    private Callback onSmssUpdated;

    private Parcelable recyclerViewState = null;
    private Callback onContactsUpdated;

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
        activity.onSmssUpdatedListeners.add(onSmssUpdated);

        onContactsUpdated = new Callback() {
            @Override
            public void onCallback(Object args) {
                executorService.submit(() -> {
                    checkOrRefresh();
                });
            }
        };
        activity.onContactUpdatedListeners.add(onContactsUpdated);

        RecyclerView listConversation = view.findViewById(R.id.main_listConversation);
        if (recyclerViewState != null) {
            listConversation.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        } else {
            listConversation.setAdapter(new ConversationAdapter(activity, new ArrayList<>()));
        }

        listConversation.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1) && newState==RecyclerView.SCROLL_STATE_IDLE) {
                    loadMore();
                }
            }
        });

        RecyclerView listSearchResult = view.findViewById(R.id.main_listSearchResult);
        listSearchResult.setAdapter(new SearchResultAdapter(activity));

        executorService.submit(() -> {
            checkOrRefresh();
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.main, menu);

        RecyclerView listSearchResult = getView().findViewById(R.id.main_listSearchResult);
        RecyclerView listConversation = getView().findViewById(R.id.main_listConversation);

        MenuItem searchMenuItem = menu.findItem(R.id.main_searchMenu);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                MainActivity activity = (MainActivity) getActivity();
                SearchResultAdapter searchResultAdapter = (SearchResultAdapter) listSearchResult.getAdapter();
                activity.abortSearch();
                searchExecutor.submit(() -> {
                    searchResultAdapter.clearResults();
                });
                if (!newText.isEmpty()) {
                    searchExecutor.submit(() -> {
                        activity.prepareSearch();
                        activity.searchSms(newText, result -> {
                            searchResultAdapter.appendResult(result);
                        });
                    });
                }
                return false;
            }
        });
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                listSearchResult.setVisibility(View.VISIBLE);
                listConversation.setVisibility(View.GONE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                listSearchResult.setVisibility(View.GONE);
                listConversation.setVisibility(View.VISIBLE);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.main_sync) {
            executorService.submit(() -> {
                MainActivity activity = (MainActivity) getActivity();
                activity.startService(new Intent(activity, SyncService.class));
            });
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity activity = (MainActivity) getActivity();
        activity.onSmssUpdatedListeners.remove(onSmssUpdated);
        activity.onContactUpdatedListeners.remove(onContactsUpdated);

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
        int limit = MainActivity.ROW_PER_PAGE;

        try {
//            SQLiteDatabase smsDb = SmsSqliteHandler_v1.openDb(activity);
////            ArrayList<SmsSqliteHandler.Sms> lastSmss = SmsSqliteHandler.getLastSmss(smsDb, 0, limit);
////                ArrayList<SmsSqliteHandler.Sms> lastSmss = SmsSqliteHandler.getLastSmssFromContentResolver(activity,
////                        0, limit);
//            ArrayList<Sms> lastSmss = Sms.getLastSmssFromBoth(smsDb, activity,
//                    0, limit);
//            smsDb.close();

            SQLiteDatabase smsDb = MergedSmsSqliteHandler.openDb(activity);
            ArrayList<Sms> lastSmss = MergedSmsSqliteHandler.getLastSmss(smsDb, 0,
                    (activity.lastSmsCurrPage + 1) * limit, null);
            smsDb.close();

//            activity.loadContacts(lastSmss);

            Log.d(TAG, "loaded sms " + lastSmss.size());

            activity.setLastSmss(lastSmss);

            return lastSmss;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    private void loadMore() {
        MainActivity activity = (MainActivity) getActivity();
        ArrayList<Sms> lastSmss = activity.getLastSmss();
        if (lastSmss == null) {
            lastSmss = refresh();
            renderSmss(lastSmss);
            return;
        }

        View view = getView();
        RecyclerView recyclerView = view.findViewById(R.id.main_listConversation);
        ConversationAdapter adapter = (ConversationAdapter) recyclerView.getAdapter();

        activity.runOnUiThread(() -> {
            recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
            recyclerView.setAdapter(new ConversationAdapter(activity, null));
        });

        activity.lastSmsCurrPage++;
        SQLiteDatabase smsDb = MergedSmsSqliteHandler.openDb(activity);
        int limit = MainActivity.ROW_PER_PAGE;
        ArrayList<Sms> moreSmss = MergedSmsSqliteHandler.getLastSmss(smsDb,
                activity.lastSmsCurrPage * limit, limit, null);
        lastSmss.addAll(moreSmss);
        smsDb.close();

//        activity.loadContacts(moreSmss);

        activity.runOnUiThread(() -> {
            recyclerView.setAdapter(adapter);
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        });
    }
}
