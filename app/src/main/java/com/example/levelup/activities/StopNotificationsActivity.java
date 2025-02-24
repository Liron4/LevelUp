package com.example.levelup.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.levelup.services.MessageListenerService;

public class StopNotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new AlertDialog.Builder(this)
                .setTitle("Stop Notifications")
                .setMessage("Are you sure you want to stop the notification service?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Stop the service
                        Intent stopIntent = new Intent(StopNotificationsActivity.this, MessageListenerService.class);
                        stopService(stopIntent);

                        // Inform the user that the service will be re-enabled next time the app launches
                        new AlertDialog.Builder(StopNotificationsActivity.this)
                                .setTitle("Service Stopped")
                                .setMessage("The notification service has been stopped. It will be re-enabled the next time the app launches.")
                                .setPositiveButton(android.R.string.ok, (dialog1, which1) -> finish())
                                .show();
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> finish())
                .show();
    }
}