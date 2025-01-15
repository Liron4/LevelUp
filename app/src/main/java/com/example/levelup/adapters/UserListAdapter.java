package com.example.levelup.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.levelup.R;
import com.example.levelup.Fragments.CreateProfile.UserProfile;
import java.util.List;

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
        UserProfile user = userList.get(position);
        holder.nicknameTextView.setText(user.nickname);
        holder.favGamesTextView.setText(String.join(", ", user.favoriteGames));
        holder.chatUserButton.setOnClickListener(v -> onChatUserClickListener.onChatUserClick(user));
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