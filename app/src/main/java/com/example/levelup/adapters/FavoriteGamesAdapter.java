package com.example.levelup.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.levelup.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class FavoriteGamesAdapter extends RecyclerView.Adapter<FavoriteGamesAdapter.FavoriteGameViewHolder> {

    private List<String> favoriteGames;

    private OnGameDeleteListener onGameDeleteListener;

    public FavoriteGamesAdapter(List<String> favoriteGames, OnGameDeleteListener listener) {
        this.favoriteGames = favoriteGames;
        this.onGameDeleteListener = listener;
    }

    @NonNull
    @Override
    public FavoriteGameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_game, parent, false);
        return new FavoriteGameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteGameViewHolder holder, int position) {
        String gameName = favoriteGames.get(position);
        holder.gameNameTextView.setText(gameName);

        holder.deleteTextView.setOnClickListener(v -> {
            if (onGameDeleteListener != null) {
                onGameDeleteListener.onDeleteGame(position, gameName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoriteGames.size();
    }

    public static class FavoriteGameViewHolder extends RecyclerView.ViewHolder {
        TextView gameNameTextView;
        TextView deleteTextView;

        public FavoriteGameViewHolder(@NonNull View itemView) {
            super(itemView);
            gameNameTextView = itemView.findViewById(R.id.gameNameTextView);
            deleteTextView = itemView.findViewById(R.id.deleteTextView);
        }
    }

    public interface OnGameDeleteListener {
        void onDeleteGame(int position, String gameName);
    }
}