package com.example.levelup.Fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import com.example.levelup.R;
import com.example.levelup.adapters.UserListAdapter;
import com.example.levelup.models.UserProfile;
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

    ImageView moveToContactsButton;

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
        fetchCurrentUserNickname();
        userListAdapter = new UserListAdapter(filteredList, user -> { //implement click function for adapter
            Bundle bundle = new Bundle();
            bundle.putString("recieverNickname", user.nickname);
            bundle.putString("currentNickname", nicknameHolder.getText().toString().replace("Welcome ", "").replace("!", ""));
            Navigation.findNavController(getView()).navigate(R.id.action_searchEngine_to_chatFragment, bundle);
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

        moveToContactsButton = view.findViewById(R.id.moveToContactsButton);
        moveToContactsButton.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_searchEngine_to_contactsList);
        });

        fetchUsersFromDatabase();



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
                    String mynickname = dataSnapshot.getValue(String.class);
                    if (mynickname != null) {
                        nicknameHolder.setText("Welcome " + mynickname + "!");
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
            } else if (filterOption.equals("Game")) {
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