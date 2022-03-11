package com.mrap.smslistener;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mrap.smslistener.model.MergedSmsSqliteHandler;
import com.mrap.smslistener.model.Sms;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchResultAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private static final String TAG = "SearchResultAdptr";

    private final ArrayList<MergedSmsSqliteHandler.SearchResult> searchResults = new ArrayList<>();
    private final MainActivity activity;

    private int bodyTargetWidth = 0;

    ExecutorService logExecutor = Executors.newSingleThreadExecutor();

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

    @Override
    public void onBindViewHolder(@NonNull ConversationAdapter.ConversationViewHolder holder, int position) {
        synchronized (searchResults) {
            MergedSmsSqliteHandler.SearchResult searchResult = searchResults.get(position);
            Sms sms = searchResult.sms;

            String niceDate = activity.niceDate(sms.date, true);
            holder.txtNum.setText(activity.getContactName(sms.addr) + ", " +
                    niceDate);

            Paint paint = holder.txtMsg.getPaint();

            String bodySingleLine = sms.body.replace("\n", " ");
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(bodySingleLine);
            spannableStringBuilder.setSpan(new StyleSpan(Typeface.BOLD), searchResult.charStartPos,
                    searchResult.charEndPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            int targetWidth = bodyTargetWidth;

            EllipsizeUtil ellipsizeUtil = new EllipsizeUtil(spannableStringBuilder,
                    searchResult.charStartPos, searchResult.charEndPos, paint, targetWidth);
            SpannableStringBuilder content = (SpannableStringBuilder) ellipsizeUtil.processEllipsize();

            logExecutor.submit(() -> {
                Log.d(TAG, "ellipsize " + targetWidth + " " + niceDate + " "
                        + bodySingleLine.substring(0, 20) + " -> " + content);
            });

            holder.txtMsg.setText(content);

            holder.itemView.setOnClickListener(v -> {
                ConversationPage conversationPage = new ConversationPage();
                Bundle args = new Bundle();
                args.putString("addr", sms.addr);
                args.putInt("jumpToPos", searchResult.rowNum);
                conversationPage.setArguments(args);
                activity.getSupportFragmentManager().
                        beginTransaction().
                        replace(R.id.actmain_framelayout, conversationPage, null).
                        addToBackStack(null).
                        commit();
            });
        }
    }

    @Override
    public int getItemCount() {
        synchronized (searchResults) {
            return searchResults.size();
        }
    }

    public void appendResult(MergedSmsSqliteHandler.SearchResult searchResult) {

        synchronized (searchResults) {
            boolean exists = false;
            for (MergedSmsSqliteHandler.SearchResult result : searchResults) {
                if (result.sms.date == searchResult.sms.date &&
                        result.sms.addr.equals(searchResult.sms.addr) &&
                        result.sms.body.equals(searchResult.sms.body)) {
                    exists = true;
                    break;
                }
            }
            logExecutor.submit(() -> {
                String bodySingleLine = searchResult.sms.body.replace("\n", " ");
                Log.d(TAG, "append result " + bodySingleLine.substring(0, Math.min(
                        bodySingleLine.length(), 20)));
            });
            if (exists) {
                return;
            }

            activity.runOnUiThread(() -> {
                synchronized (searchResults) {
                    searchResults.add(searchResult);
                    notifyDataSetChanged();
                }
            });
        }
    }

    public void clearResults() {
        activity.runOnUiThread(() -> {
            synchronized (searchResults) {
                Log.d(TAG, "clearResults");
                searchResults.clear();
                notifyDataSetChanged();
            }
        });
    }
}
