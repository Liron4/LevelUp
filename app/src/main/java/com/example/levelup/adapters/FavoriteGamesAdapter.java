package com.example.levelup.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
    private String userId;

    public FavoriteGamesAdapter(List<String> favoriteGames, String userId) {
        this.favoriteGames = favoriteGames;
        this.userId = userId;
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
            // Remove the game from the list
            favoriteGames.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, favoriteGames.size());

            // Remove the game from the Firebase Realtime Database
            DatabaseReference userRef = FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference("users").child(userId).child("favoriteGames");

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String game = snapshot.getValue(String.class);
                        if (game != null && game.equals(gameName)) {
                            snapshot.getRef().removeValue().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("FavoriteGamesAdapter", "Successfully removed game: " + gameName);
                                } else {
                                    Log.e("FavoriteGamesAdapter", "Failed to remove game: " + gameName, task.getException());
                                }
                            });
                            break;
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("FavoriteGamesAdapter", "Database error: " + databaseError.getMessage());
                }
            });
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
}