package com.example.levelup.Fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.levelup.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.levelup.models.UserProfile;

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

public class CreateProfile extends Fragment {

    private AutoCompleteTextView gamesAutoComplete;
    private TextView gamesSelectedTextView;
    private EditText emailField;
    private EditText passwordField;
    private EditText retypePasswordField;
    private EditText nicknameField;
    private ImageButton deleteButton;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    private List<String> selectedGames = new ArrayList<>();

    public CreateProfile() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_profile, container, false);

        gamesAutoComplete = view.findViewById(R.id.gamesAutoComplete);
        gamesSelectedTextView = view.findViewById(R.id.GamesSelected);
        emailField = view.findViewById(R.id.emailField);
        passwordField = view.findViewById(R.id.passwordField);
        retypePasswordField = view.findViewById(R.id.retypePasswordField);
        nicknameField = view.findViewById(R.id.nicknameField);
        deleteButton = view.findViewById(R.id.DeleteButton);
        registerButton = view.findViewById(R.id.registerButton);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users");

        registerButton.setOnClickListener(v -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();
            String retypePassword = retypePasswordField.getText().toString();
            String nickname = nicknameField.getText().toString();

            if (password.equals(retypePassword)) {
                checkNicknameUniqueAndRegister(email, password, nickname);
            } else {
                Toast.makeText(getActivity(), "Passwords do not match.", Toast.LENGTH_SHORT).show();
            }
        });

        deleteButton.setOnClickListener(v -> {
            if (!selectedGames.isEmpty()) {
                selectedGames.remove(selectedGames.size() - 1);
                updateSelectedGamesTextView();
            }
        });

        // 1. Set up the search field to trigger the API call when the user presses Enter
        gamesAutoComplete.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                    String query = gamesAutoComplete.getText().toString().trim();
                    if (!query.isEmpty()) {
                        searchGames(query);
                    }
                    return true;
                }
                return false;
            }
        });

        return view;
    }

    private void checkNicknameUniqueAndRegister(String email, String password, String nickname) {
        databaseReference.orderByChild("nickname").equalTo(nickname).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().getChildrenCount() == 0) {
                    registerUser(email, password, nickname);
                } else {
                    Toast.makeText(getActivity(), "Nickname already exists.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), "Error checking nickname: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(String email, String password, String nickname) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter email and password.", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            UserProfile userProfile = new UserProfile(nickname, selectedGames, null, null);
                            databaseReference.child(userId).setValue(userProfile);
                            Toast.makeText(getActivity(), "Registration successful.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Registration failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateSelectedGamesTextView() {
        StringBuilder gamesText = new StringBuilder();
        for (int i = 0; i < selectedGames.size(); i++) {
            gamesText.append(selectedGames.get(i));
            if (i < selectedGames.size() - 1) {
                gamesText.append(", ");
            }
        }

        SpannableString spannableString = new SpannableString(gamesText.toString());
        spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, gamesText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        gamesSelectedTextView.setText(spannableString);
    }

    // 2. Method to perform the RAWG API search using OkHttp
    private void searchGames(String query) {
        List<String> localResults = loadLocalGames(query);
        if (!localResults.isEmpty()) {
            showGameResults(localResults);
        } else {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String url = "https://api.rawg.io/api/games?key=ff52dd671c9045c0a820c272cc243062"
                        + "&search=" + encodedQuery
                        + "&page_size=5";

                OkHttpClient client = new OkHttpClient(); // ספרייה לעשות קריאות API
                Request request = new Request.Builder().url(url).build(); // אובייקש בקשה מהספרייה

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "API error", Toast.LENGTH_SHORT).show();
                        }); // תקלה מסוג אין לי אינטרנט
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            onFailure(call, new IOException("Unexpected response " + response));
                            return; // יש לי אינטרנט, אבל הAPI לא בא לו לתת לי תשובות
                        }
                        String responseData = response.body().string();
                        List<String> gameResults = parseGameResults(responseData);
                        getActivity().runOnUiThread(() -> {
                            if (gameResults.isEmpty()) { // מה עם הכל עבד אבל קיבלתי רשימה ריקה?
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

    // 4. Fallback: Filter the local games resource
    private List<String> loadLocalGames(String query) {
        String[] localGames = getResources().getStringArray(R.array.popular_multiplayer_games);
        List<String> filteredGames = new ArrayList<>();
        for (String game : localGames) {
            if (game.toLowerCase().contains(query.toLowerCase())) {
                filteredGames.add(game);
            }
        }
        // Limit results to 5 entries if necessary
        if (filteredGames.size() > 5) {
            filteredGames = filteredGames.subList(0, 5);
        }
        return filteredGames;
    }

    // 3. Parse the JSON response from RAWG to extract game names
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



    // 5. Display the list of games in a dialog for the user to select one
// Modify the showGameResults function
    private void showGameResults(List<String> gameResults) {
        if (gameResults.isEmpty()) {
            Toast.makeText(getContext(), "No games found", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select a Game")
                .setItems(gameResults.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedGame = gameResults.get(which);
                        if (!selectedGames.contains(selectedGame)) {
                            selectedGames.add(selectedGame);
                            updateSelectedGamesTextView();
                        }
                        gamesAutoComplete.setText(""); // Clear only when a game is selected
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // Do nothing, so the text remains for correction
                    }
                })
                .show();
    }

}