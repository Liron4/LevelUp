package com.example.levelup.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.levelup.services.MessageListenerService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.example.levelup.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FragmentLogin extends Fragment {

    private EditText emailField;
    private EditText passwordField;
    private Button loginButton;
    private Button createProfileButton;

    private FirebaseAuth mAuth;

    public FragmentLogin() {
        // Required empty public constructor
    }

    public static FragmentLogin newInstance(String param1, String param2) {
        FragmentLogin fragment = new FragmentLogin();
        Bundle args = new Bundle();
      //  args.putString(ARG_PARAM1, param1);
      //  args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        //    mParam1 = getArguments().getString(ARG_PARAM1);
        //    mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is already logged in, go to main screen
            Log.d("LoginFragment", "Moving on to SearchEngine Fragment, since User is already authenticated: " + currentUser.getUid());
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_fragmentLogin_to_searchEngine);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        emailField = view.findViewById(R.id.emailField);
        passwordField = view.findViewById(R.id.passwordField);
        loginButton = view.findViewById(R.id.loginButton);
        createProfileButton = view.findViewById(R.id.createProfileButton);

        // Retrieve login details from SharedPreferences
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String savedEmail = sharedPreferences.getString("email", "");
        String savedPassword = sharedPreferences.getString("password", "");

        // Pre-fill the email and password fields
        emailField.setText(savedEmail);
        passwordField.setText(savedPassword);

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();
            loginUser(email, password, v);
        });

        createProfileButton.setOnClickListener(v -> {
            // Navigate to the CreateProfile fragment
            Navigation.findNavController(v).navigate(R.id.action_login_to_createProfileFragment);
        });

        return view;
    }




    private void loginUser(String email, String password, View view) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();

                            // Save login details and UID in SharedPreferences
                            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("email", email);
                            editor.putString("password", password);
                            editor.putString("uid", uid);
                            editor.apply();

                            // Send update to MessageListenerService
                            Intent serviceIntent = new Intent(getContext(), MessageListenerService.class);
                            serviceIntent.putExtra("newUid", uid);
                            getContext().startService(serviceIntent);

                            // Navigate to the SearchEngine fragment
                            Navigation.findNavController(view).navigate(R.id.action_fragmentLogin_to_searchEngine);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}