package com.example.levelup.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.levelup.R;
import com.example.levelup.services.MessageListenerService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Show Beta Phase Message
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean showBetaMessage = prefs.getBoolean("showBetaMessage", true);
        if (showBetaMessage) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_beta_message, null);
            builder.setView(dialogView);

            CheckBox dontShowAgain = dialogView.findViewById(R.id.dontShowAgainCheckBox);
            builder.setTitle("Beta Phase")
                    .setMessage("This app is in Beta Phase. Please report bugs to teamlevelup66@gmail.com. If you feel harassed, use the block button and the blocked user's actions will be reviewed. \n Also please make sure your time and date is set correctly to your relative timezone!")
                    .setPositiveButton("OK", (dialog, which) -> {
                        if (dontShowAgain.isChecked()) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("showBetaMessage", false);
                            editor.apply();
                        }
                        dialog.dismiss();
                    })
                    .show();
        }

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            }
        }



        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

        // Start the msg service
        Intent serviceIntent = new Intent(this, MessageListenerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Handle navigation action from notification
        handleIntent(getIntent());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Log.d("MainActivity", "Notification permission granted. Thank you!");
                Toast.makeText(this, "Notification permission granted. You can turn it off via settings if needed.", Toast.LENGTH_LONG).show();
            } else {
                // Permission denied
                Log.e("MainActivity", "Notification permission denied. This permission is crucial for the app to work.");
                Toast.makeText(this, "Notification permission denied. This permission is crucial for the app to work.", Toast.LENGTH_LONG).show();
            }
        }
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