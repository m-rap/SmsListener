package com.mrap.smslistener;

import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
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

        String content = processEllipsis(bodySingleLine, searchResult, paint, targetWidth);

//        Log.d(TAG, "ellipsis " + targetWidth + " " + niceDate + " "
//                + bodySingleLine.substring(0, 20) + " -> " + content);

        holder.txtMsg.setText(content);
    }

    private String processEllipsis(
            String bodySingleLine, MergedSmsSqliteHandler.SearchResult searchResult,
            Paint paint, int targetWidth) {

        String content;
        if (bodySingleLine.length() - searchResult.charEndPos < searchResult.charStartPos) {
            content = processEllipsisFromBack(bodySingleLine, searchResult, paint, targetWidth);
        } else {
            content = processEllipsisFromFront(bodySingleLine, searchResult, paint, targetWidth);
        }

        return content;
    }

    private String processEllipsisFromBack(
            String bodySingleLine, MergedSmsSqliteHandler.SearchResult searchResult,
            Paint paint, int targetWidth) {
        String content;
        int ellipsisMode = 0x0;
        int subEnd = searchResult.charEndPos + 10;
        if (bodySingleLine.length() < subEnd) {
            subEnd = bodySingleLine.length();
        } else {
            ellipsisMode |= 0x2;
        }

        content = bodySingleLine.substring(0, subEnd);

        Rect bound = new Rect();

        int subStart = 0;
        paint.getTextBounds(content, subStart, content.length(), bound);
        int textWidth = bound.width();

        String ellipsisStr = "...";
        paint.getTextBounds(ellipsisStr, 0, 3, bound);
        int ellipsisWidth = bound.width();

        int ellipsisCount = 0;
        if ((ellipsisMode & 0x2) > 0) {
            ellipsisCount++;
        }

        if (textWidth + ellipsisWidth * ellipsisCount > targetWidth) {
            ellipsisMode |= 0x1;
        }

        while (true) {
            paint.getTextBounds(content, subStart, content.length(), bound);
            textWidth = bound.width();

            ellipsisCount = 0;
            if ((ellipsisMode & 0x2) > 0) {
                ellipsisCount++;
            }
            if ((ellipsisMode & 0x1) > 0) {
                ellipsisCount++;
            }

            if (textWidth + ellipsisWidth * ellipsisCount <= targetWidth) {
                break;
            }
            subStart++;
            if (subStart >= content.length() - 1) {
                break;
            }
        }

        content = content.substring(subStart);
        if ((ellipsisMode & 0x2) > 0) {
            content += ellipsisStr;
        }
        if ((ellipsisMode & 0x1) > 0) {
            content = ellipsisStr + content;
        }

        return content;
    }

    private String processEllipsisFromFront(
            String bodySingleLine, MergedSmsSqliteHandler.SearchResult searchResult,
            Paint paint, int targetWidth) {
        String content;
        int ellipsisMode = 0;
        int subStart = searchResult.charStartPos - 10;
        if (subStart < 0) {
            subStart = 0;
        } else {
            ellipsisMode |= 0x1;
        }

        content = bodySingleLine.substring(subStart);

        Rect bound = new Rect();

        int subEnd = content.length();
        paint.getTextBounds(content, 0, subEnd, bound);
        int textWidth = bound.width();

        String ellipsisStr = "...";
        paint.getTextBounds(ellipsisStr, 0, 3, bound);
        int ellipsisWidth = bound.width();

        int ellipsisCount = 0;
        if ((ellipsisMode & 0x1) > 0) {
            ellipsisCount++;
        }

        if (textWidth + ellipsisWidth * ellipsisCount > targetWidth) {
            ellipsisMode |= 0x2;
        }

        while (true) {
            paint.getTextBounds(content, 0, subEnd, bound);
            textWidth = bound.width();

            ellipsisCount = 0;
            if ((ellipsisMode & 0x1) > 0) {
                ellipsisCount++;
            }
            if ((ellipsisMode & 0x2) > 0) {
                ellipsisCount++;
            }

            if (textWidth + ellipsisWidth * ellipsisCount <= targetWidth) {
                break;
            }

            subEnd--;
            if (subEnd <= 1) {
                break;
            }
        }

        content = content.substring(0, subEnd);
        if ((ellipsisMode & 0x2) > 0) {
            content += ellipsisStr;
        }
        if ((ellipsisMode & 0x1) > 0) {
            content = ellipsisStr + content;
        }

        return content;
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
