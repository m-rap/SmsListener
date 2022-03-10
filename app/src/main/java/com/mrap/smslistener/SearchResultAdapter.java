package com.mrap.smslistener;

import android.graphics.Paint;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mrap.smslistener.model.MergedSmsSqliteHandler;
import com.mrap.smslistener.model.Sms;

import java.util.ArrayList;

public class SearchResultAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private static final String TAG = "SearchResultAdptr";

    private final ArrayList<MergedSmsSqliteHandler.SearchResult> searchResults = new ArrayList<>();
    private final MainActivity activity;
    private String keyword = "";

    private int bodyTargetWidth = 0;

    public SearchResultAdapter(MainActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public ConversationAdapter.ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_conversation_item,
                parent, false);
        ConversationAdapter.ConversationViewHolder holder =
                new ConversationAdapter.ConversationViewHolder(itemView);
        holder.txtMsg.post(() -> {
            bodyTargetWidth = holder.txtMsg.getWidth();
        });
        return holder;
    }

    private void getTextWidth(String text, Paint paint) {
        Rect bound = new Rect();
        paint.getTextBounds(text, 0, text.length(), bound);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationAdapter.ConversationViewHolder holder, int position) {
        MergedSmsSqliteHandler.SearchResult searchResult = searchResults.get(position);
        Sms sms = searchResult.sms;

        String niceDate = activity.niceDate(sms.date, true);
        holder.txtNum.setText(activity.getContactName(sms.addr) + ", " +
                niceDate);

        Paint paint = holder.txtMsg.getPaint();
        String bodySingleLine = sms.body.replace("\n", " ");

        int targetWidth = bodyTargetWidth;

        EllipsizeUtil ellipsizeUtil = new EllipsizeUtil(bodySingleLine, searchResult, paint, targetWidth);
        String content = ellipsizeUtil.processEllipsize();

//        Log.d(TAG, "ellipsize " + targetWidth + " " + niceDate + " "
//                + bodySingleLine.substring(0, 20) + " -> " + content);

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
