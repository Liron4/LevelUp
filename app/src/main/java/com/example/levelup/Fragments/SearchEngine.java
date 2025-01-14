package com.example.levelup.Fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.example.levelup.R;
import com.example.levelup.adapters.UserListAdapter;
import com.example.levelup.Fragments.CreateProfile.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class SearchEngine extends Fragment {

    private RecyclerView recyclerView;
    private UserListAdapter userListAdapter;
    private List<UserProfile> userList;
    private List<UserProfile> filteredList;
    private DatabaseReference databaseReference;
    private EditText searchEngine;
    private Spinner filterSpinner;
    private TextView nicknameHolder;

    public SearchEngine() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userList = new ArrayList<>();
        filteredList = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/").getReference("users");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_engine, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userListAdapter = new UserListAdapter(filteredList, user -> {
            // Handle add friend button click
        });
        recyclerView.setAdapter(userListAdapter);

        searchEngine = view.findViewById(R.id.searchEngine);
        filterSpinner = view.findViewById(R.id.filterSpinner);
        nicknameHolder = view.findViewById(R.id.nicknameHolder);

        searchEngine.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fetchUsersFromDatabase();
        fetchCurrentUserNickname();
        return view;
    }

    private void fetchUsersFromDatabase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    UserProfile user = snapshot.getValue(UserProfile.class);
                    userList.add(user);
                }
                filterUsers(searchEngine.getText().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors.
            }
        });
    }

    private void fetchCurrentUserNickname() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            databaseReference.child(uid).child("nickname").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String nickname = dataSnapshot.getValue(String.class);
                    if (nickname != null) {
                        nicknameHolder.setText("Welcome " + nickname + "!");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle possible errors.
                }
            });
        }
    }

    private void filterUsers(String query) {
        filteredList.clear();
        String filterOption = filterSpinner.getSelectedItem().toString();
        for (UserProfile user : userList) {
            if (filterOption.equals("Nicknames") && user.nickname.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(user);
            } else if (filterOption.equals("Games")) {
                for (String game : user.favoriteGames) {
                    if (game.toLowerCase().contains(query.toLowerCase())) {
                        filteredList.add(user);
                        break;
                    }
                }
            }
        }
        userListAdapter.notifyDataSetChanged();
    }
}