package com.example.levelup.Fragments;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;

public class CreateProfile extends Fragment {

    private AutoCompleteTextView gamesAutoComplete;
    private TextView gamesSelectedTextView;

    private EditText emailField;
    private EditText passwordField;
    private ImageButton deleteButton;
    private EditText retypePasswordField;
    private Button registerButton;
    private FirebaseAuth mAuth;

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
        deleteButton = view.findViewById(R.id.DeleteButton);
        retypePasswordField = view.findViewById(R.id.retypePasswordField);
        registerButton = view.findViewById(R.id.registerButton);

        mAuth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(v -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();
            String retypePassword = retypePasswordField.getText().toString();

            if (password.equals(retypePassword)) {
                registerUser(email, password);
            } else {
                Toast.makeText(getActivity(), "Passwords do not match.", Toast.LENGTH_SHORT).show();
            }
        });

        String[] games = {"Minecraft", "Fortnite", "Call of Duty", "League of Legends", "Overwatch", "Call of Duty 2"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, games);
        gamesAutoComplete.setAdapter(adapter);

        gamesAutoComplete.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedGame = (String) parent.getItemAtPosition(position);
            if (!selectedGames.contains(selectedGame)) {
                selectedGames.add(selectedGame);
                updateSelectedGamesTextView();
            }
            gamesAutoComplete.setText("");


        });

        deleteButton.setOnClickListener(v -> {
            if (!selectedGames.isEmpty()) {
                selectedGames.remove(selectedGames.size() - 1);
                updateSelectedGamesTextView();
            }
        });

        return view;
    }

    private void registerUser(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter email and password.", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(getActivity(), "Registration successful.", Toast.LENGTH_SHORT).show();
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
}