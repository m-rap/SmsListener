package com.mrap.smslistener;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mrap.smslistener.model.Sms;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private static final String TAG = "ConversationAdapter";
    private Drawable defaultBg = null;

    public ConversationAdapter(Context context, ArrayList<Sms> smss) {
        this.context = context;
        this.smss = smss;
    }

    public static class ConversationViewHolder extends RecyclerView.ViewHolder {
        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private final Context context;
    private final ArrayList<Sms> smss;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm");

//    private int viewHolderCount = 0;

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        viewHolderCount++;
//        if (viewHolderCount % 10 == 0) {
//            Log.d(TAG, "viewHolderCount " + viewHolderCount);
//        }
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_conversation_item,
                parent, false);
        if (defaultBg == null) {
            defaultBg = itemView.getBackground();
        }
        return new ConversationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Sms sms = smss.get(position);

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

//        if (sms.source == Sms.SOURCE_SQLITE) {
//            viewSms.setBackground(new ColorDrawable(Color.parseColor("#FFDADA")));
//        } else {
//            if (defaultBg != null)
//                viewSms.setBackground(defaultBg);
//        }
    }

    @Override
    public int getItemCount() {
        return smss.size();
    }
}
