package com.example.levelup.adapters;

import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.levelup.R;
import com.example.levelup.models.Message;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chatmessage, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.messageContentTextView.setText(message.getContent());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(new Date(message.getTimestamp()));
        holder.messageTimeTextView.setText(formattedTime);
        holder.usernameTextView.setText(message.getUsername());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageContentTextView;
        TextView messageTimeTextView;
        TextView usernameTextView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContentTextView = itemView.findViewById(R.id.messageContentTextView);
            messageTimeTextView = itemView.findViewById(R.id.messageTimeTextView2);
            usernameTextView = itemView.findViewById(R.id.usernamemsg);
        }
    }
}