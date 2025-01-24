package com.example.levelup.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.levelup.R;
import com.example.levelup.adapters.MessageAdapter;
import com.example.levelup.models.Message;
import com.example.levelup.services.MessageListenerService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

        if (recieverNickname != null) {
            msgrecievername.setText(recieverNickname);
            findUserUidByNickname(recieverNickname);
        }

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList, currentNickname);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messagesRecyclerView.setAdapter(messageAdapter);

        sendButton.setOnClickListener(v -> sendMessage());


        return view;
    }



    @Override
    public void onResume() {
        super.onResume();
        Log.d("ChatFragment", "onResume called");
        if (chatPath != null) {
            Log.d("ChatFragment", "Re-attaching chat listener");
            loadChatHistory();
            addChatListener();
        } else {
            Log.d("ChatFragment", "chatPath is null, listener not re-attached");
        }
        // Update the service to re-ignore the chat
        Intent serviceIntent = new Intent(getContext(), MessageListenerService.class);
        serviceIntent.putExtra("receiverUid", receiverUid);
        getContext().startService(serviceIntent);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("ChatFragment", "onPause called");
        if (chatListener != null) {
            Log.d("ChatFragment", "Removing chat listener");
            databaseReference.child("chats").child(chatPath).removeEventListener(chatListener);
            chatListener = null;
        }
        Log.d("ChatFragment", "Clearing message list");
        messageList.clear();
        messageAdapter.notifyDataSetChanged();

        // Update the service to ignore no one
        Intent serviceIntent = new Intent(getContext(), MessageListenerService.class);
        serviceIntent.putExtra("receiverUid", (String) null);
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
                                setupChatPath(receiverUid);
                                // Ignore notifications from current chat
                                Intent serviceIntent = new Intent(getContext(), MessageListenerService.class);
                                serviceIntent.putExtra("receiverUid", receiverUid);
                                getContext().startService(serviceIntent);
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
        databaseReference.child("chats").child(chatPath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    messageList.add(message);
                }
                messageAdapter.notifyDataSetChanged();
                messagesRecyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getActivity(), "Error loading chat history: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addChatListener() {
        Log.d("ChatFragment", "addChatListener called");
        Log.d("ChatFragment", "Initializing new chatListener");
        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d("ChatFragment", "onChildAdded called");
                Message message = dataSnapshot.getValue(Message.class);
                if (message != null && !message.getUsername().equals(currentNickname)) {
                    messageList.add(message);
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    messagesRecyclerView.scrollToPosition(messageList.size() - 1);
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
            long currentTime = System.currentTimeMillis();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String currentUserUid = currentUser.getUid();
                Message message = new Message(currentNickname, messageContent, currentTime, currentUserUid, receiverUid, false);
                databaseReference.child("chats").child(chatPath).push().setValue(message);
                databaseReference.child("notifications").child(receiverUid).push().setValue(message); // to add future TTL mechanism
                messageList.add(message);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                messagesRecyclerView.scrollToPosition(messageList.size() - 1);
                messageEditText.setText("");
            }
        }
    }
}