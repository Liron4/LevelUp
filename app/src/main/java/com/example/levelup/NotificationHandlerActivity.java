package com.example.levelup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.levelup.Fragments.ChatFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NotificationHandlerActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

        // Retrieve sender username and UID from the intent
        Intent intent = getIntent();
        String senderUsername = intent.getStringExtra("senderUsername");
        String senderUid = intent.getStringExtra("senderUid");

        // Retrieve login details from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        String uid = sharedPreferences.getString("uid", "");

        if (uid.isEmpty()) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Find current nickname
        databaseReference.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String currentNickname = dataSnapshot.child("nickname").getValue(String.class);

                    // Pass data to ChatFragment
                    Bundle bundle = new Bundle();
                    bundle.putString("recieverNickname", senderUsername);
                    bundle.putString("currentNickname", currentNickname);
                    bundle.putString("receiverUid", senderUid);

                    ChatFragment chatFragment = new ChatFragment();
                    chatFragment.setArguments(bundle);

                    getSupportFragmentManager().beginTransaction()
                            .replace(android.R.id.content, chatFragment)
                            .commit();
                } else {
                    Toast.makeText(NotificationHandlerActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(NotificationHandlerActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}