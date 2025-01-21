package com.example.levelup.adapters;

import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.levelup.R;
import com.example.levelup.models.Message;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private String currentNickname;

    public MessageAdapter(List<Message> messageList, String currentNickname) {
        this.messageList = messageList;
        this.currentNickname = currentNickname;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getUsername().equals(currentNickname)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatmessage, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatmessage2, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageContentTextView;
        public TextView usernamemsg;
        public TextView messageTimeTextView2;
        public LinearLayout msgarea;

        public SentMessageViewHolder(View itemView) {
            super(itemView);
            messageContentTextView = itemView.findViewById(R.id.messageContentTextView);
            usernamemsg = itemView.findViewById(R.id.usernamemsg);
            messageTimeTextView2 = itemView.findViewById(R.id.messageTimeTextView2);
            msgarea = itemView.findViewById(R.id.msgarea);
        }

        public void bind(Message message) {
            messageContentTextView.setText(message.getContent());
            usernamemsg.setText(message.getUsername());
            messageTimeTextView2.setText(new SimpleDateFormat("HH:mm dd/MM/yy", Locale.getDefault()).format(new Date(message.getTimestamp())));
        }
    }

    public static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageContentTextView;
        public TextView usernamemsg;
        public TextView messageTimeTextView2;
        public LinearLayout msgarea;

        public ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            messageContentTextView = itemView.findViewById(R.id.messageContentTextView);
            usernamemsg = itemView.findViewById(R.id.usernamemsg);
            messageTimeTextView2 = itemView.findViewById(R.id.messageTimeTextView2);
            msgarea = itemView.findViewById(R.id.msgarea);
        }

        public void bind(Message message) {
            messageContentTextView.setText(message.getContent());
            usernamemsg.setText(message.getUsername());
            messageTimeTextView2.setText(new SimpleDateFormat("HH:mm dd/MM/yy", Locale.getDefault()).format(new Date(message.getTimestamp())));
        }
    }
}