package com.example.levelup.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.levelup.R;

import java.util.List;

public class FavoriteGamesAdapter extends RecyclerView.Adapter<FavoriteGamesAdapter.FavoriteGameViewHolder> {

    private List<String> favoriteGames;

    public FavoriteGamesAdapter(List<String> favoriteGames) {
        this.favoriteGames = favoriteGames;
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
}