package com.example.levelup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.levelup.Fragments.ChatFragment;
import com.example.levelup.services.MessageListenerService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

        // Start the msg service
        Intent serviceIntent = new Intent(this, MessageListenerService.class);
        startService(serviceIntent);
        Log.d("MainActivity", "MessageService started");

        // Handle navigation action from notification
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("senderUsername")) {
            String senderUsername = intent.getStringExtra("senderUsername");
            String senderUid = intent.getStringExtra("senderUid");

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String currentUserUid = currentUser.getUid();
                databaseReference.child("users").child(currentUserUid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String currentNickname = dataSnapshot.child("nickname").getValue(String.class);

                            // Create bundle for the new chat
                            Bundle bundle = new Bundle();
                            bundle.putString("recieverNickname", senderUsername);
                            bundle.putString("currentNickname", currentNickname);
                            bundle.putString("receiverUid", senderUid);

                            NavController navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment);

                            // Pop the existing ChatFragment if it's in the stack (inclusive)
                            navController.popBackStack(R.id.chatFragment, true);

                            // Navigate to a new ChatFragment with the correct arguments
                            navController.navigate(R.id.chatFragment, bundle);

                            Log.d("MainActivity", "Navigated to new ChatFragment, old one removed.");
                        } else {
                            Log.e("MainActivity", "User not found.");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("MainActivity", "Database error: " + databaseError.getMessage());
                    }
                });
            }
        }
    }
}