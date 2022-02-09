package com.mrap.smslistener;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm");

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private final Context context;
    private final ArrayList<SmsModel.Sms> smss;

    public MessageAdapter(Context context, ArrayList<SmsModel.Sms> smss) {
        this.context = context;
        this.smss = smss;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MessageViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.view_message, parent,
                        false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        SmsModel.Sms sms = smss.get(smss.size() - 1 - position);

        MainActivity activity = (MainActivity) context;
        View viewSms = holder.itemView;

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
    }

    @Override
    public int getItemCount() {
        return smss.size();
    }
}
