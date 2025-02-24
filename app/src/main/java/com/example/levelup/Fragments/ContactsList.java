package com.example.levelup.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.levelup.R;
import com.example.levelup.adapters.UserListAdapter;
import com.example.levelup.models.Message;
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
import java.util.Collections;
import java.util.List;

public class ContactsList extends Fragment {

    private RecyclerView recyclerView;
    private UserListAdapter userListAdapter;
    private List<UserProfile> userList;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String CurrentUsernickname;

    private BroadcastReceiver messageReceiver;

    private ImageView overlayImage;



    public ContactsList() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userList = new ArrayList<>();
        databaseReference = FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        mAuth = FirebaseAuth.getInstance(); // Ensure mAuth is initialized here



// Initialize the BroadcastReceiver
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.levelup.NEW_MESSAGE".equals(intent.getAction())) {
                    String username = intent.getStringExtra("username");
                    String content = intent.getStringExtra("content");
                    long timestamp = intent.getLongExtra("timestamp", 0); // Default value is 0 if TIMESTAMP=NULL
                    Log.d("ContactsList", "Broadcast received: " + content);
                    updateRecyclerViewWithNewMessage(username, content, timestamp);

                }
            }
        };

    }


    // חלק א' של הפרגמנט - להביא את הרשימת אנשי קשר!

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("ContactsList", "onCreateView started");
        View view = inflater.inflate(R.layout.fragment_contacts_list, container, false);
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fetchUserNicknameAndSetTitle(titleTextView);
        userListAdapter = new UserListAdapter(userList, user -> {
            Bundle bundle = new Bundle();
            bundle.putString("recieverNickname", user.nickname);
            bundle.putString("currentNickname", CurrentUsernickname);
            Navigation.findNavController(getView()).navigate(R.id.action_contactsList_to_chatFragment, bundle);
        });
        recyclerView.setAdapter(userListAdapter);

        fetchFavoriteList();
        Log.d("ContactsList", "onCreateView finished");

        overlayImage = view.findViewById(R.id.overlayImage);

        return view;
    }

    private void fetchUserNicknameAndSetTitle(TextView titleTextView) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("FirebaseAuth", "User is not logged in! Aborting fetchUserNicknameAndSetTitle.");
            return;
        }

        String currentUserUid = currentUser.getUid();
        DatabaseReference userRef = databaseReference.child("users").child(currentUserUid).child("nickname");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                CurrentUsernickname = dataSnapshot.getValue(String.class);
                if (CurrentUsernickname != null) {
                    titleTextView.setText(CurrentUsernickname + "'s Contact List");
                } else {
                    Log.w("ContactsList", "Nickname not found for user: " + currentUserUid);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ContactsList", "Failed to fetch nickname for user: " + currentUserUid + ". Error: " + databaseError.getMessage());
            }
        });
    }




    private void fetchFavoriteList() {
        Log.d("ContactsList", "fetchFavoriteList started");

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.e("FirebaseAuth", "User is not logged in! Aborting fetchFavoriteList.");
            return;
        }

        String currentUserUid = currentUser.getUid();
        Log.d("FirebaseAuth", "User ID: " + currentUserUid);

        DatabaseReference favListRef = databaseReference.child("users").child(currentUserUid).child("favList"); // תביא קישור לרשימת המועדפים של המשתמש הנוכחי

        favListRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> favList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String favoriteNickname = snapshot.getValue(String.class);
                    if (favoriteNickname != null && !favoriteNickname.isEmpty()) {
                        favList.add(favoriteNickname); // תוסיף את השם לרשימת המועדפים באפליקציה
                    } else {
                        Log.w("ContactsList", "empty favorite nickname.");
                    }
                }

                Log.d("ContactsList", "Fetched favorite list: " + favList);

                if (!favList.isEmpty()) { // אם יש מועדפים
                    fetchLatestMessages(favList);
                } else {
                    Log.w("ContactsList", "Favorite list is empty, skipping message fetch.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ContactsList", "Failed to fetch favorite list: " + databaseError.getMessage());
            }
        });

        Log.d("ContactsList", "fetchFavoriteList finished");
    }

    private void fetchLatestMessages(List<String> favList) { // קיבלת את הרשימה כסטרינג ולא כפרופיל שמכיל עוד דברים משם
        Log.d("ContactsList", "fetchLatestMessages started with " + favList.size() + " favorite users.");

        List<UserProfile> tempUserList = new ArrayList<>(); // To transfer the names to the RecyclerView as profiles

        for (String nickname : favList) { // נתחיל להביא את הפרופילים של כל המשתמשים שנמצאים ברשימת המועדפים
            Log.d("ContactsList", "Fetching user profile for nickname: " + nickname);

            Query usersQuery = databaseReference.child("users").orderByChild("nickname").equalTo(nickname); // שמרתי קישור לפרופיל של קורל

            usersQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) { // עובר על כל המשתמשים שנמצאו עם אותו השם
                        String userUid = snapshot.getKey(); // קישור לת.ז של המשתמש, נמצא תמיד משתמש בודד
                        Log.d("ContactsList", "Found user UID: " + userUid + " for nickname: " + nickname);
                        fetchLatestMessageForUser(userUid, nickname, tempUserList, favList.size()); // נביא את ההודעה האחרונה למשתמש הספציפי שמצאנו!
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("ContactsList", "Failed to fetch user data for " + nickname + ": " + databaseError.getMessage());
                }

            });
        }
    }

    private void fetchLatestMessageForUser(String userUid, String favNickname, List<UserProfile> tempUserList, int favListSize) {
        String currentUserUid = mAuth.getCurrentUser().getUid();
        String chatPath = (currentUserUid.compareTo(userUid) < 0)
                ? currentUserUid + "_" + userUid
                : userUid + "_" + currentUserUid;

        DatabaseReference chatPathRef = databaseReference
                .child("chats")
                .child(chatPath)
                .child("messages"); // Use the 'messages' child

        chatPathRef.limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    addUserProfileWithNoMessages(favNickname, tempUserList);
                    checkIfFinished();
                    return;
                }
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null) {
                        UserProfile userProfile = new UserProfile();
                        userProfile.nickname = favNickname;
                        if (message.getUsername().equals(CurrentUsernickname)) {
                            userProfile.latestMessage = "You: " + message.getContent();
                        } else {
                            userProfile.latestMessage = message.getContent();
                        }
                        userProfile.timestamp = message.getTimestamp();
                        tempUserList.add(userProfile);
                    }
                }
                checkIfFinished();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ContactsList", "Failed to fetch messages: " + databaseError.getMessage());
            }

            private void checkIfFinished() {
                if (tempUserList.size() == favListSize) {
                    sortAndDisplayUsers(tempUserList);
                }
            }
        });
    }

    private void addUserProfileWithNoMessages(String favNickname, List<UserProfile> tempUserList) {
        UserProfile userProfile = new UserProfile();
        userProfile.nickname = favNickname;
        userProfile.latestMessage = "No recent messages";
        userProfile.timestamp = 0;
        tempUserList.add(userProfile); // הגדלנו את הרשימה עם משתמש חסר הודעה
        Log.d("ContactsList", "Added user with no messages: " + favNickname);
    }

    private void sortAndDisplayUsers(List<UserProfile> tempUserList) {
        Collections.sort(tempUserList, (u1, u2) -> Long.compare(u2.timestamp, u1.timestamp)); // מיון לפי הזמן ההודעה מפונקציית ההשוואה של ג'אווה
        userList.clear();
        userList.addAll(tempUserList);
        userListAdapter.notifyDataSetChanged();
    }

    // חלק ב' - לעדכן עם המשדר את הרשימת אנשי קשר בזמן שאנחנו בפרגמנט


    private void updateRecyclerViewWithNewMessage(String nickname, String latestMessage, long timestamp) {
        for (UserProfile userProfile : userList) { // נעבור על כל המשתמשים ברשימה
            if (userProfile.nickname.equals(nickname)) { // אם מצאנו את המשתמש שהגיעה ממנו ההודעה
                // אז נעשה לו עדכונים
                userProfile.latestMessage = latestMessage + " 🟣";
                userProfile.timestamp = timestamp;
                userList.remove(userProfile);
                userList.add(0, userProfile);
                userListAdapter.notifyDataSetChanged();
                return;
            }
        }
    }

    // PAUSE & RESUME - כדי להפעיל ולכבות את קבלת ההודעות בזמן שאנחנו בפרגמנט

    @Override
    public void onPause() {
        super.onPause();
        Log.d("ContactsList", "onPause called");
        Context context = getContext();
        if (context != null) {
            context.unregisterReceiver(messageReceiver);
        } else {
            Log.e("ContactsList", "Context is null, cannot unregister receiver");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("ContactsList", "onResume called");
        fetchFavoriteList();
        // Register the BroadcastReceiver
        Context context = getContext();
        if (context != null) {
            IntentFilter filter = new IntentFilter("com.example.levelup.NEW_MESSAGE");
            context.registerReceiver(messageReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            Log.e("ContactsList", "Context is null, cannot register receiver");
        }

        overlayImage.animate()
                .alpha(0.0f)
                .setDuration(1500) // Duration of the fade-out animation in milliseconds
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        overlayImage.setVisibility(View.GONE);
                    }
                });
    }
}