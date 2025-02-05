package com.example.levelup.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.levelup.R;
import com.example.levelup.adapters.MessageAdapter;
import com.example.levelup.models.Message;
import com.example.levelup.models.UserProfile;
import com.example.levelup.services.MessageListenerService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatFragment extends Fragment {

    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText messageEditText;
    private Button sendButton;
    private ImageButton favPersonButton;
    private ImageButton blockButton;
    private TextView msgrecievername;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private String chatPath;
    private String recieverNickname;
    private String currentNickname;
    private String receiverUid; // Global variable for receiver UID
    private ChildEventListener chatListener;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            recieverNickname = getArguments().getString("recieverNickname");
            currentNickname = getArguments().getString("currentNickname");
        }
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView);
        messageEditText = view.findViewById(R.id.messageEditText);
        sendButton = view.findViewById(R.id.sendButton);
        msgrecievername = view.findViewById(R.id.nicknameTextView);
        favPersonButton = view.findViewById(R.id.favpersonbutton);
        blockButton = view.findViewById(R.id.blockbutton);

     /*   if (recieverNickname != null) {
            msgrecievername.setText(recieverNickname);
            findUserUidByNickname(recieverNickname);
        } */

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentNickname);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesRecyclerView.setAdapter(messageAdapter);

        checkIfFavorite();

        sendButton.setOnClickListener(v -> sendMessage());

        favPersonButton.setOnClickListener(v -> toggleFavoriteStatus());

        blockButton.setOnClickListener(v -> showBlockConfirmationDialog());

        return view;
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d("ChatFragment", "onPause called - delegating to onDestroyView()");
        Log.d("ChatFragment", "Ensuring cleanup");

        // ניקוי האזנה להודעות צ'אט
        if (chatListener != null) {
            Log.d("ChatFragment", "Removing chat listener");
            databaseReference.child("chats").child(chatPath).removeEventListener(chatListener);
            chatListener = null;
        }

        // ניקוי רשימת ההודעות
        Log.d("ChatFragment", "Clearing message list");
        messageList.clear();
        messageAdapter.notifyDataSetChanged();

        // עדכון השירות שלא להאזין יותר
        Intent serviceIntent = new Intent(getContext(), MessageListenerService.class);
        serviceIntent.putExtra("receiverUid", (String) null);
        getContext().startService(serviceIntent);

    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d("ChatFragment", "onResume called");

        // Check if the given items for the fragment are updated
        if (getArguments() != null) {
            String newRecieverNickname = getArguments().getString("recieverNickname");
            String newCurrentNickname = getArguments().getString("currentNickname");

            if (newCurrentNickname != null && !newCurrentNickname.equals(currentNickname)) { // incase i change account
                currentNickname = newCurrentNickname;
            }

            if (newRecieverNickname != null && !newRecieverNickname.equals(recieverNickname)) {}

                recieverNickname = newRecieverNickname;
                msgrecievername.setText(recieverNickname);
                findUserUidByNickname(recieverNickname);

            //else if (receiverUid != null) {
              //  setupChatPath(receiverUid);
           // }


        }

        // Check if the person is in the favorites list and update the button image
        checkIfFavorite();

        // Restart the MessageListenerService with the updated receiverUid
        Intent serviceIntent = new Intent(getContext(), MessageListenerService.class);
        serviceIntent.putExtra("receiverUid", receiverUid);
        getContext().startService(serviceIntent);
    }




    private void findUserUidByNickname(String nickname) {
        databaseReference.child("users").orderByChild("nickname").equalTo(nickname)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                receiverUid = userSnapshot.getKey(); // Set global variable
                                // Set up chat path
                                setupChatPath(receiverUid);

                                break;
                            }
                        } else {
                            Toast.makeText(getActivity(), "User not found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(getActivity(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupChatPath(String receiverUid) {

        // Ignore notifications from current chat
        Intent serviceIntent = new Intent(getContext(), MessageListenerService.class);
        serviceIntent.putExtra("receiverUid", receiverUid);
        getContext().startService(serviceIntent);


        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserUid = currentUser.getUid();
            // Sort UIDs lexicographically
            if (currentUserUid.compareTo(receiverUid) < 0) {
                chatPath = currentUserUid + "_" + receiverUid;
            } else {
                chatPath = receiverUid + "_" + currentUserUid;
            }
            Log.d("ChatPath", "Chat path: " + chatPath);
            loadChatHistory();
            addChatListener();
        }
    }

    private void loadChatHistory() {
        Query query = databaseReference.child("chats").child(chatPath).limitToLast(200);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null) {
                        message.setNotificationSent(true); // Mark as notification sent
                        snapshot.getRef().child("notificationSent").setValue(true); // Update in database
                        messageList.add(message);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                messagesRecyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error loading chat history: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addChatListener() {
        Log.d("ChatFragment", "addChatListener called");
        Log.d("ChatFragment", "Initializing new chatListener");
        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Message message = dataSnapshot.getValue(Message.class);
                if (message != null && !message.getUsername().equals(currentNickname) && !message.isNotificationSent()) {
                    messageList.add(message);
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                    dataSnapshot.getRef().child("notificationSent").setValue(true); // Mark as notification sent
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle child changed if needed
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Handle child removed if needed
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle child moved if needed
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("ChatFragment", "onCancelled called: " + databaseError.getMessage());
                Toast.makeText(getActivity(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        Log.d("ChatFragment", "Adding chatListener to databaseReference");
        databaseReference.child("chats").child(chatPath).addChildEventListener(chatListener);
    }

    private void sendMessage() {
        String messageContent = messageEditText.getText().toString().trim();
        if (!messageContent.isEmpty() && chatPath != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String currentUserUid = currentUser.getUid();
                DatabaseReference currentUserRef = databaseReference.child("users").child(currentUserUid);
                DatabaseReference receiverUserRef = databaseReference.child("users").child(receiverUid);

                currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        UserProfile currentUserProfile = dataSnapshot.getValue(UserProfile.class);
                        if (currentUserProfile != null && currentUserProfile.blockedList != null && currentUserProfile.blockedList.contains(recieverNickname)) {
                            Toast.makeText(getContext(), "You have blocked this user. Cannot send message.", Toast.LENGTH_SHORT).show();
                        } else {
                            receiverUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    UserProfile receiverUserProfile = dataSnapshot.getValue(UserProfile.class);
                                    if (receiverUserProfile != null && receiverUserProfile.blockedList != null && receiverUserProfile.blockedList.contains(currentNickname)) {
                                        Toast.makeText(getContext(), "This user has blocked you. Cannot send message.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        long currentTime = System.currentTimeMillis();
                                        Message message = new Message(currentNickname, messageContent, currentTime, currentUserUid, receiverUid, false);
                                        databaseReference.child("chats").child(chatPath).push().setValue(message);
                                        databaseReference.child("notifications").child(receiverUid).push().setValue(message);
                                        messageList.add(message);
                                        messageAdapter.notifyItemInserted(messageList.size() - 1);
                                        messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                                        messageEditText.setText("");
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void toggleFavoriteStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserUid = currentUser.getUid();
            DatabaseReference userRef = databaseReference.child("users").child(currentUserUid);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                    if (userProfile != null) {
                        if (userProfile.favList == null) {
                            userProfile.favList = new ArrayList<>();
                        }

                        if (userProfile.favList.contains(recieverNickname)) {
                            userProfile.favList.remove(recieverNickname);
                            favPersonButton.setImageResource(android.R.drawable.star_big_off);
                        } else {
                            userProfile.favList.add(recieverNickname);
                            favPersonButton.setImageResource(android.R.drawable.star_big_on);
                        }

                        userRef.setValue(userProfile);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("ChatFragment", "Failed to update user data: " + databaseError.getMessage());
                }
            });
        }
    }





    private void checkIfFavorite() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserUid = currentUser.getUid();
            DatabaseReference userRef = databaseReference.child("users").child(currentUserUid);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                    if (userProfile != null && userProfile.favList != null && userProfile.favList.contains(recieverNickname)) {
                        favPersonButton.setImageResource(android.R.drawable.star_big_on);
                    } else {
                        favPersonButton.setImageResource(android.R.drawable.star_big_off);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("ChatFragment", "Failed to fetch user data: " + databaseError.getMessage());
                }
            });
        }
    }

    //block btn logic:

    private void showBlockConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Block User")
                .setMessage("Do you want to block or unblock this user?")
                .setPositiveButton("Block", (dialog, which) -> handleBlockUser())
                .setNegativeButton("Unblock", (dialog, which) -> handleUnblockUser())
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    private void handleBlockUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserUid = currentUser.getUid();
            DatabaseReference userRef = databaseReference.child("users").child(currentUserUid);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                    if (userProfile != null) {
                        List<String> blockedList = userProfile.blockedList != null ? userProfile.blockedList : new ArrayList<>();
                        if (!blockedList.contains(recieverNickname)) {
                            blockedList.add(recieverNickname);
                            userRef.child("blockedList").setValue(blockedList);
                            Toast.makeText(getContext(), recieverNickname + " has been blocked.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), recieverNickname + " is already blocked.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handleUnblockUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserUid = currentUser.getUid();
            DatabaseReference userRef = databaseReference.child("users").child(currentUserUid);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
                    if (userProfile != null) {
                        List<String> blockedList = userProfile.blockedList != null ? userProfile.blockedList : new ArrayList<>();
                        if (blockedList.contains(recieverNickname)) {
                            blockedList.remove(recieverNickname);
                            userRef.child("blockedList").setValue(blockedList);
                            Toast.makeText(getContext(), recieverNickname + " has been unblocked.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), recieverNickname + " is not blocked.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }




}