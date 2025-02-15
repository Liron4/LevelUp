package com.example.levelup.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.levelup.R;
import com.example.levelup.adapters.FavoriteGamesAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private RecyclerView gamesRecyclerView;
    private FavoriteGamesAdapter adapter;
    private List<String> favoriteGames;
    private ImageButton bellButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        gamesRecyclerView = view.findViewById(R.id.gamesRecyclerView);
        gamesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        favoriteGames = new ArrayList<>();
        adapter = new FavoriteGamesAdapter(favoriteGames);
        gamesRecyclerView.setAdapter(adapter);

        bellButton = view.findViewById(R.id.bellButton);
        setupBellButton();

        fetchFavoriteGames();

        return view;
    }

    private void fetchFavoriteGames() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users").child(userId).child("favoriteGames");

            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    favoriteGames.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String gameName = snapshot.getValue(String.class);
                        favoriteGames.add(gameName);
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle possible errors.
                }
            });
        }
    }

    private void setupBellButton() {
        applyColorMatrixForDarkMode();
        loadBellGif();
    }

    private void applyColorMatrixForDarkMode() {
        int nightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            android.graphics.ColorMatrix matrix = new android.graphics.ColorMatrix(new float[]{
                    -1, 0, 0, 0, 255,
                    0, -1, 0, 0, 255,
                    0, 0, -1, 0, 255,
                    0, 0, 0, 1, 0
            });
            bellButton.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
        } else {
            bellButton.clearColorFilter();
        }
    }

    private void loadBellGif() {
        Glide.with(this)
                .asGif()
                .load(R.drawable.bellgif)
                .into(bellButton);
    }
}