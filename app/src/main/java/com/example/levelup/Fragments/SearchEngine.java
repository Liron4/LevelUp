package com.example.levelup.Fragments;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.levelup.R;
import com.example.levelup.adapters.UserListAdapter;
import com.example.levelup.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
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
    ImageView settingsButton;
    ImageView signOutButton;

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
        userListAdapter = new UserListAdapter(filteredList, user -> {
            Bundle bundle = new Bundle();
            bundle.putString("recieverNickname", user.nickname);
            bundle.putString("currentNickname", nicknameHolder.getText().toString().replace("Welcome ", "").replace("!", ""));
            Navigation.findNavController(getView()).navigate(R.id.action_searchEngine_to_chatFragment, bundle);
        });
        recyclerView.setAdapter(userListAdapter);

        searchEngine = view.findViewById(R.id.searchEngine);
        filterSpinner = view.findViewById(R.id.filterSpinner);
        nicknameHolder = view.findViewById(R.id.nicknameHolder);

        loadLatestProfiles();

        searchEngine.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchEngine.getText().toString();
                if (query.isEmpty()) {
                    loadLatestProfiles();
                } else {
                    recyclerView.clearOnScrollListeners();
                    performSearch(query);
                }
                return true;
            }
            return false;
        });

        signOutButton = view.findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });

        moveToContactsButton = view.findViewById(R.id.moveToContactsButton);
        moveToContactsButton.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_searchEngine_to_contactsList);
        });

        settingsButton = view.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.action_searchEngine_to_settingsFragment);
        });



        searchEngine.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    loadLatestProfiles();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed
            }
        });

        return view;
    }


    private void loadLatestProfiles() {
        DatabaseReference usersRef = databaseReference;
        Query latestUsersQuery = usersRef.orderByKey().limitToFirst(10); // ✅ Changed from limitToLast

        latestUsersQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<UserProfile> profiles = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    UserProfile user = snapshot.getValue(UserProfile.class);
                    if (user != null && !snapshot.getKey().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        profiles.add(user);
                    }
                }
                filteredList.clear();
                filteredList.addAll(profiles);
                userListAdapter.notifyDataSetChanged();
                setupScrollListener();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors.
            }
        });
    }

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == filteredList.size() - 1) {
                    GetlatestUserProfilesUID();
                }
            }
        });
    }

    private void GetlatestUserProfilesUID() {
    String lastUserNickname = filteredList.get(filteredList.size() - 1).nickname;
    Query queryByNickname = databaseReference.orderByChild("nickname").equalTo(lastUserNickname);
queryByNickname.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot.exists()) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String lastUserId = snapshot.getKey();
                    loadMoreProfilesByUid(lastUserId);
                    break;
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Handle possible errors.
        }
    });
}


    private void loadMoreProfilesByUid(String lastUserId) {
        Log.d("SearchEngine", "loadMoreProfilesByUid called with lastUserId: " + lastUserId);
        DatabaseReference usersRef = databaseReference;
        Query moreUsersQuery = usersRef.orderByKey().startAfter(lastUserId).limitToFirst(10); // ✅ Use limitToFirst

        moreUsersQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("SearchEngine", "onDataChange called with dataSnapshot: " + dataSnapshot);
                List<UserProfile> profiles = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    UserProfile user = snapshot.getValue(UserProfile.class);
                    if (user != null && !snapshot.getKey().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        profiles.add(user);
                    }
                }
                if (profiles.isEmpty()) {
                    Log.d("SearchEngine", "No more profiles found");
                    Toast.makeText(getContext(), "All users loaded", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d("SearchEngine", "Profiles loaded: " + profiles.size());
                    Toast.makeText(getContext(), "Loading extra users", Toast.LENGTH_SHORT).show();
                    filteredList.addAll(profiles);
                    userListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("SearchEngine", "onCancelled called with error: " + databaseError.getMessage());
                // Handle possible errors.
            }
        });
    }

    private void performSearch(String query) {
        String filterOption = filterSpinner.getSelectedItem().toString();
        if (filterOption.equals("Nickname")) {
            searchByNickname(query);
        } else if (filterOption.equals("Game")) {
            searchByGame(query);
        }
    }

    private void searchByNickname(String query) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Query queryByNickname = databaseReference.orderByChild("nickname").startAt(query).endAt(query + "\uf8ff");
        queryByNickname.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                filteredList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    UserProfile user = snapshot.getValue(UserProfile.class);
                    if (user != null && !snapshot.getKey().equals(currentUserId) && user.nickname.toLowerCase().contains(query.toLowerCase())) {
                        filteredList.add(user);
                    }
                }
                userListAdapter.notifyDataSetChanged();
                if (filteredList.isEmpty()) {
                    Toast.makeText(getContext(), "No users found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors.
            }
        });
    }


    private void searchByGame(String query) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                filteredList.clear();
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    UserProfile user = snapshot.getValue(UserProfile.class);
                    if (user != null && !snapshot.getKey().equals(currentUserId) && user.favoriteGames != null) {
                        for (String game : user.favoriteGames) {
                            if (game.toLowerCase().contains(query.toLowerCase())) {
                                filteredList.add(user);
                                break;
                            }
                        }
                    }
                }
                userListAdapter.notifyDataSetChanged();
                if (filteredList.isEmpty()) {
                    Toast.makeText(getContext(), "No users found", Toast.LENGTH_SHORT).show();
                }
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
                        nicknameHolder.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM); // CRTICIAL



                        Log.d("SearchEngine", "Current user nickname fetched: " + mynickname);
                    } else {
                        Log.d("SearchEngine", "Current user nickname is null");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle possible errors.
                }
            });
        }
    }

    private boolean isXiaomiDevice() {
        return android.os.Build.MANUFACTURER.equalsIgnoreCase("Xiaomi");
    }

}