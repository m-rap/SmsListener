package com.mrap.smslistener;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mrap.smslistener.model.MergedSmsSqliteHandler;
import com.mrap.smslistener.model.Sms;

import java.util.ArrayList;

public class SearchResultAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private final ArrayList<MergedSmsSqliteHandler.SearchResult> searchResults = new ArrayList<>();
    private final MainActivity activity;
    private String keyword = "";

    public SearchResultAdapter(MainActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public ConversationAdapter.ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_conversation_item,
                parent, false);
        return new ConversationAdapter.ConversationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationAdapter.ConversationViewHolder holder, int position) {
        MergedSmsSqliteHandler.SearchResult searchResult = searchResults.get(position);
        Sms sms = searchResult.sms;
        holder.txtNum.setText(activity.getContactName(sms.addr) + ", " + activity.niceDate(sms.date));
        String content;
//        sms.body.indexOf(keyword);
        content = sms.body;
        holder.txtMsg.setText(content);
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void appendResult(MergedSmsSqliteHandler.SearchResult searchResult) {
        boolean exists = false;
        for (MergedSmsSqliteHandler.SearchResult result : searchResults) {
            if (result.sms.date == searchResult.sms.date &&
                    result.sms.addr.equals(searchResult.sms.addr) &&
                    result.sms.body.equals(searchResult.sms.body)) {
                exists = true;
                break;
            }
        }
        if (exists) {
            return;
        }
        searchResults.add(searchResult);
        activity.runOnUiThread(() -> {
            notifyDataSetChanged();
        });
    }

    public void clearResults() {
        searchResults.clear();
        activity.runOnUiThread(() -> {
            notifyDataSetChanged();
        });
    }
}
