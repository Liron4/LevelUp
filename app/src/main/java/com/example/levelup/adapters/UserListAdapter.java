package com.example.levelup.adapters;

import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.levelup.R;
import com.example.levelup.models.UserProfile;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {

    private List<UserProfile> userList;
    private OnChatUserClickListener onChatUserClickListener;

    public UserListAdapter(List<UserProfile> userList, OnChatUserClickListener onChatUserClickListener) {
        this.userList = userList;
        this.onChatUserClickListener = onChatUserClickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.usercard, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserProfile userProfile = userList.get(position);
        if (userProfile != null) {
            Log.d("UserListAdapter", "Binding user: " + userProfile.nickname);
            holder.nicknameTextView.setText(userProfile.nickname);
            if (userProfile.favoriteGames != null) {
                holder.favGamesTextView.setText(String.join(", ", userProfile.favoriteGames));
            }
            else {
                if (userProfile.timestamp == 0) {
                    holder.favGamesTextView.setText("No messages yet");
                }
                else {
                String formattedTimestamp = new SimpleDateFormat("HH:mm dd/MM/yy", Locale.getDefault()).format(new Date(userProfile.timestamp));
                holder.favGamesTextView.setText(userProfile.latestMessage + " | " + formattedTimestamp);
                }
            }
            holder.chatUserButton.setOnClickListener(v -> onChatUserClickListener.onChatUserClick(userProfile));
        } else {
            Log.e("UserListAdapter", "UserProfile at position " + position + " is null");
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nicknameTextView;
        TextView favGamesTextView;
        ImageButton chatUserButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nicknameTextView = itemView.findViewById(R.id.nicknameTextView);
            favGamesTextView = itemView.findViewById(R.id.favGamesTextView);
            chatUserButton = itemView.findViewById(R.id.chatUser);
        }
    }

    public interface OnChatUserClickListener {
        void onChatUserClick(UserProfile user);
    }
}