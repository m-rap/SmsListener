package com.mrap.smslistener;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private final Context context;
    private final ArrayList<SmsModel.Sms> smss;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm");

    public ConversationAdapter(Context context, ArrayList<SmsModel.Sms> smss) {
        this.context = context;
        this.smss = smss;
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {
        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversationViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.view_conversation_item,
                        parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        SmsModel.Sms sms = smss.get(position);

        View viewSms = holder.itemView;
        MainActivity activity = (MainActivity) context;
        viewSms.setOnClickListener(v -> {
            ConversationPage conversationPage = new ConversationPage();
            Bundle args = new Bundle();
            args.putString("addr", sms.addr);
            conversationPage.setArguments(args);
            activity.getSupportFragmentManager().
                    beginTransaction().
                    replace(R.id.actmain_framelayout, conversationPage, null).
                    addToBackStack(null).
                    commit();
        });
//                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//                layoutParams.topMargin = layoutParams.rightMargin = layoutParams.leftMargin =
//                        (int)activity.convertDipToPix(activity, 10);
//                if (i == smss.length - 1) {
//                    layoutParams.bottomMargin = (int)activity.convertDipToPix(activity, 10);
//                }
//                viewSms.setLayoutParams(layoutParams);

        TextView numTv = viewSms.findViewById(R.id.idxrow_address);
        numTv.setText(sms.addr + ", " + sdf.format(sms.date));

        TextView msgTv = viewSms.findViewById(R.id.idxrow_content);
        msgTv.setText(sms.body);
    }

    @Override
    public int getItemCount() {
        return smss.size();
    }
}