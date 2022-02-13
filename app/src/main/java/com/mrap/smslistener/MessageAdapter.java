package com.mrap.smslistener;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mrap.smslistener.model.Sms;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int TYPE_SPACER = 0;
    private static final int TYPE_MSG = 1;
    private static final String TAG = "MessageAdapter";

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm");

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView txtMsg;
        private TextView txtDate;
        public MessageViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            if (viewType == TYPE_MSG) {
                txtMsg = itemView.findViewById(R.id.msg_txtMsg);
                txtDate = itemView.findViewById(R.id.msg_txtDate);
            }
        }
    }

    private final Context context;
    private final ArrayList<Sms> smss;

    public MessageAdapter(Context context, ArrayList<Sms> smss) {
        this.context = context;
        this.smss = smss;
    }

    private Drawable defaultBg = null;

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == TYPE_SPACER) {
            MainActivity activity = (MainActivity) context;
            itemView = new View(activity);
            int spacerHeight = (int)activity.convertDipToPix(activity, 30);
            Log.d(TAG, "spacer height " + spacerHeight);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    spacerHeight);
            itemView.setLayoutParams(lp);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.view_message, parent,
                            false);
            defaultBg = itemView.getBackground();
        }
        return new MessageViewHolder(itemView, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        return position == smss.size() ? TYPE_SPACER : TYPE_MSG;
    }

    @Override
    public int getItemCount() {
        return smss.size() + 1;
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        if (position == smss.size()) {
            return;
        }

        Sms sms = smss.get(smss.size() - 1 - position);

        MainActivity activity = (MainActivity) context;
        View viewSms = holder.itemView;

        viewSms.setOnLongClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) activity.
                    getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, sms.body));
            Toast.makeText(activity, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            return true;
        });

        holder.txtMsg.setText(sms.body);
        holder.txtDate.setText(sdf.format(sms.date));

        if (sms.source == Sms.SOURCE_SQLITE) {
//            viewSms.setBackground(new ColorDrawable(Color.parseColor("#1C8F8F")));
            viewSms.setBackground(new ColorDrawable(Color.parseColor("#FFDADA")));
        } else {
            if (defaultBg != null) {
                viewSms.setBackground(defaultBg);
            }
        }
    }
}
