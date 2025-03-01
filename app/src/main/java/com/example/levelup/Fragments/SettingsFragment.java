package com.example.levelup.Fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsFragment extends Fragment implements FavoriteGamesAdapter.OnGameDeleteListener {
    private RecyclerView gamesRecyclerView;
    private FavoriteGamesAdapter adapter;
    private List<String> favoriteGames;
    private ImageButton bellButton;
    private EditText searchBar;

    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        gamesRecyclerView = view.findViewById(R.id.gamesRecyclerView);
        gamesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        String myUID = currentUser.getUid();
        favoriteGames = new ArrayList<>();
        adapter = new FavoriteGamesAdapter(favoriteGames, this);
        gamesRecyclerView.setAdapter(adapter);

        bellButton = view.findViewById(R.id.bellButton);
        searchBar = view.findViewById(R.id.searchBar);
        setupBellButton();
        setupSearchBar();

        fetchFavoriteGames();

        return view;
    }

    @Override
    public void onDeleteGame(int position, String gameName) {
        if (favoriteGames.size() <= 1) {
            Toast.makeText(requireContext(), "You must have at least one favourite game", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove the game from the list
        favoriteGames.remove(position);
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, favoriteGames.size());

        // Remove the game from the Firebase Realtime Database
        DatabaseReference userRef = FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("users").child(currentUser.getUid()).child("favoriteGames");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String game = snapshot.getValue(String.class);
                    if (game != null && game.equals(gameName)) {
                        snapshot.getRef().removeValue().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("SettingsFragment", "Successfully removed game: " + gameName);
                            } else {
                                Log.e("SettingsFragment", "Failed to remove game: " + gameName, task.getException());
                            }
                        });
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("SettingsFragment", "Database error: " + databaseError.getMessage());
            }
        });
    }



    private void fetchFavoriteGames() {
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
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
        bellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNotificationImportanceDialog();
            }
        });
        applyColorMatrixForDarkMode();
        loadBellGif();
    }

    private void showNotificationImportanceDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Notification Importance")
                .setMessage("Notifications are important for the chat to function properly. Please ensure they are enabled.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
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

    private void setupSearchBar() {
        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchBar.getText().toString().trim();
                    searchGame(query);
                    return true;
                }
                return false;
            }
        });
    }

    private void searchGame(String query) {
        List<String> localResults = loadLocalGames(query);
        if (!localResults.isEmpty()) {
            showGameResults(localResults);
        } else {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String url = "https://api.rawg.io/api/games?key=ff52dd671c9045c0a820c272cc243062&search=" + encodedQuery + "&page_size=5";

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        getActivity().runOnUiThread(() -> {
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            onFailure(call, new IOException("Unexpected response " + response));
                            return;
                        }
                        String responseData = response.body().string();
                        List<String> gameResults = parseGameResults(responseData);
                        getActivity().runOnUiThread(() -> {
                            if (gameResults.isEmpty()) {
                                Toast.makeText(getContext(), "No games found", Toast.LENGTH_SHORT).show();
                            } else {
                                showGameResults(gameResults);
                            }
                        });
                    }
                });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> loadLocalGames(String query) {
        String[] localGames = getResources().getStringArray(R.array.popular_multiplayer_games);
        List<String> filteredGames = new ArrayList<>();
        for (String game : localGames) {
            if (game.toLowerCase().contains(query.toLowerCase())) {
                filteredGames.add(game);
            }
        }
        if (filteredGames.size() > 5) {
            filteredGames = filteredGames.subList(0, 5);
        }
        return filteredGames;
    }

    private List<String> parseGameResults(String json) {
        List<String> results = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray("results");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject gameObj = jsonArray.getJSONObject(i);
                String gameName = gameObj.getString("name");
                results.add(gameName);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return results;
    }

    private void showGameResults(List<String> gameResults) {
        if (gameResults.isEmpty()) {
            Toast.makeText(getContext(), "No games found", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select a Game")
                .setItems(gameResults.toArray(new String[0]), (dialog, which) -> {
                    String selectedGame = gameResults.get(which);
                    if (!favoriteGames.contains(selectedGame)) {
                        favoriteGames.add(selectedGame);
                        adapter.notifyDataSetChanged();
                        addGameToDatabase(selectedGame);
                        Toast.makeText(getContext(), "Game added to favorites", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Game already in favorites", Toast.LENGTH_SHORT).show();
                    }
                    searchBar.setText("");
                })
                .setOnCancelListener(dialog -> {
                    // Do nothing, so the text remains for correction
                })
                .show();
    }


    private void addGameToDatabase(String game) {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users").child(userId).child("favoriteGames");

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<String> gamesList = new ArrayList<>();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String existingGame = snapshot.getValue(String.class);
                            gamesList.add(existingGame);
                        }
                    }
                    gamesList.add(game);
                    userRef.setValue(gamesList);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle possible errors.
                }
            });
        }
    }
}