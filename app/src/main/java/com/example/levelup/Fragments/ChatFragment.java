package com.example.levelup.Fragments;

import android.content.Intent;
import android.net.Uri;
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
    private ImageButton sendButton;
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

    private ImageView overlayImage;

    private ImageView returnButton;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            recieverNickname = "";
        currentNickname = getArguments() != null ? getArguments().getString("currentNickname", "") : "";

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

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentNickname);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesRecyclerView.setAdapter(messageAdapter);

        sendButton.setOnClickListener(v -> sendMessage());

        favPersonButton.setOnClickListener(v -> toggleFavoriteStatus());

        blockButton.setOnClickListener(v -> showBlockConfirmationDialog());

        overlayImage = view.findViewById(R.id.overlayImage);

        returnButton = view.findViewById(R.id.returnButton);

        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //hit back key
               requireActivity().onBackPressed();
            }
        });



        return view;
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d("ChatFragment", "onPause called - Cleaning area");

        // ניקוי האזנה להודעות צ'אט
        if (chatListener != null) {
            Log.d("ChatFragment", "Removing chat listener");
            databaseReference.child("chats").child(chatPath).removeEventListener(chatListener);
            chatListener = null;
        }


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

            if (newCurrentNickname != null && !newCurrentNickname.equals(currentNickname)) { // incase of an account swap
                currentNickname = newCurrentNickname;
            }

            if (!newRecieverNickname.equals(recieverNickname)) {
                recieverNickname = newRecieverNickname;
                msgrecievername.setText(recieverNickname);
                findUserUidByNickname(recieverNickname); // updates chat fragment completely
            } else { // we came back to the same chat
                Log.d("ChatFragment", "Optimization mode");
                addChatListener(); // Add the chat listener to listen again
                startMessageListenerService(); // Start the service with the old receiverUid
            }
        }

        overlayImage.animate() //maybe move it to the top
                .alpha(0.0f)
                .setDuration(1300) // Duration of the fade-out animation in milliseconds
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        overlayImage.setVisibility(View.GONE);
                    }
                });
    }




    private void findUserUidByNickname(String nickname) {
        databaseReference.child("users").orderByChild("nickname").equalTo(nickname)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                receiverUid = userSnapshot.getKey(); // Set global variable
                                startMessageListenerService(); // Start the service with the updated receiverUid
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

    private void startMessageListenerService() {
        if (receiverUid != null) {
            Intent serviceIntent = new Intent(getContext(), MessageListenerService.class);
            serviceIntent.putExtra("receiverUid", receiverUid);
            getContext().startService(serviceIntent);
        } else {
            Log.e("ChatFragment", "receiverUid is null, cannot start MessageListenerService");
        }
    }

    private void setupChatPath(String receiverUid) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserUid = currentUser.getUid();
            if (currentUserUid.compareTo(receiverUid) < 0) {
                chatPath = currentUserUid + "_" + receiverUid;
            } else {
                chatPath = receiverUid + "_" + currentUserUid;
            }
            Log.d("ChatPath", "Chat path: " + chatPath);

            // Add both UIDs to 'members' child
            DatabaseReference membersRef = databaseReference.child("chats")
                    .child(chatPath)
                    .child("members");
            membersRef.child(currentUserUid).setValue(true);
            membersRef.child(receiverUid).setValue(true);

            loadChatHistory();
        }
    }

    private void loadChatHistory() {
        Query query = databaseReference.child("chats")
                .child(chatPath)
                .child("messages")
                .limitToLast(25);
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
                addChatListener();
                // Check if the person is in the favorites list and update the button image
                checkIfFavorite();

                // Add scroll listener to load older messages
                messagesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        if (layoutManager != null && layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                            loadExtraMessages();
                        }
                    }
                });
            }



            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error loading chat history: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadExtraMessages() {
        if (messageList.isEmpty()) return;

        long firstMessageTimestamp = messageList.get(0).getTimestamp();
        Query query = databaseReference.child("chats")
                .child(chatPath)
                .child("messages")
                .orderByChild("timestamp")
                .endBefore(firstMessageTimestamp)
                .limitToLast(25);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Message> olderMessages = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null) {
                        olderMessages.add(message);
                    }
                }
                if (!olderMessages.isEmpty()) {
                    messageList.addAll(0, olderMessages);
                    messageAdapter.notifyItemRangeInserted(0, olderMessages.size());
                    messagesRecyclerView.scrollToPosition(olderMessages.size() - 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error loading older messages: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
        databaseReference.child("chats")
                .child(chatPath)
                .child("messages")
                .addChildEventListener(chatListener);    }

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
                                        DatabaseReference msgRef = databaseReference.child("chats")
                                                .child(chatPath)
                                                .child("messages");
                                        msgRef.push().setValue(message);
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
                            Toast.makeText(getContext(), recieverNickname + " removed from favorites.", Toast.LENGTH_SHORT).show();
                        } else {
                            userProfile.favList.add(recieverNickname);
                            favPersonButton.setImageResource(android.R.drawable.star_big_on);
                            Toast.makeText(getContext(), recieverNickname + " added to favorites.", Toast.LENGTH_SHORT).show();
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
                            showReportConfirmationDialog(currentUserUid, currentNickname, receiverUid, recieverNickname);
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

    private void showReportConfirmationDialog(String reporterUid, String reporterNickname, String reportedUid, String reportedNickname) {
        new AlertDialog.Builder(getContext())
                .setTitle("Report User")
                .setMessage("Do you want to report this user?")
                .setPositiveButton("Yes", (dialog, which) -> uploadReportToDatabase(reporterUid, reporterNickname, reportedUid, reportedNickname))
                .setNegativeButton("No", null)
                .show();
    }

    private void uploadReportToDatabase(String reporterUid, String reporterNickname, String reportedUid, String reportedNickname) {
        DatabaseReference messagesRef = databaseReference.child("chats").child(chatPath).child("messages");
        messagesRef.limitToLast(50).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                StringBuilder messagesContent = new StringBuilder();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null) {
                        messagesContent.append(message.getUsername()).append(": ").append(message.getContent()).append(", \n");
                    }
                }

                String reportId = databaseReference.child("reports").child(reporterUid).push().getKey();
                if (reportId != null) {
                    DatabaseReference reportRef = databaseReference.child("reports").child(reporterUid).child(reportId);
                    reportRef.child("reporterNickname").setValue(reporterNickname);
                    reportRef.child("reporterUid").setValue(reporterUid);
                    reportRef.child("reportedNickname").setValue(reportedNickname);
                    reportRef.child("reportedUid").setValue(reportedUid);
                    reportRef.child("chatPath").setValue(chatPath);
                    reportRef.child("messagesContent").setValue(messagesContent.toString());
                    reportRef.child("handled").setValue(false);
                    Toast.makeText(getContext(), "Report submitted successfully.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Error: Could not generate report ID.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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