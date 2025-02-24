package com.example.levelup.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.levelup.activities.MainActivity;
import com.example.levelup.R;
import com.example.levelup.activities.StopNotificationsActivity;
import com.example.levelup.models.Message;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;

public class MessageListenerService extends Service {
    private static final String CHANNEL_ID = "MessageNotificationChannel";
    private String currentUid;
    private String receiverUid;
    private ChildEventListener messageListener;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MessageListenerService", "Service created");
        createNotificationChannel(); // this must be created first of all. StackOverflow
        startForegroundService();

        // Retrieve UID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        currentUid = sharedPreferences.getString("uid", null);

        if (currentUid != null) {
            Log.d("MessageListenerService", "UID retrieved: " + currentUid);
            listenForMessages(currentUid, null);
        } else {
            Log.e("MessageListenerService", "UID not found. Service will not function properly.");
        }
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, StopNotificationsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build the notification using the same importance as the channel
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Levelup Notification Service")
                .setContentText("Listening for new messages")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)  // Match channel importance
                .setSilent(true)  // Remove silent flag so the notification is fully visible
                .setSmallIcon(R.drawable.bellicon) // Ensure this icon meets Android guidelines (white on transparent background)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)  // Do not show on lock screen
                .setCategory(NotificationCompat.CATEGORY_SERVICE);  // Must be set to CATEGORY_SERVICE for foreground services

        startForeground(1, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String newUid = intent.getStringExtra("newUid"); // listen to this user notifications
        String newReceiverUid = intent.getStringExtra("receiverUid"); //ignore notifications from this user

        Log.d("MessageListenerService", "Listener updated with current UID: " + currentUid + " and receiver UID: " + receiverUid);
        if (newUid != null) {
            currentUid = newUid;
        }
            receiverUid = newReceiverUid; // ignore notifications from this user, if its null -> listen to all notifications

        if (currentUid != null) {
            listenForMessages(currentUid, receiverUid);
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void listenForMessages(String uid, String chatIgnoreUid) {
        Log.d("MessageListenerService", "Listening for notifications for UID: " + uid + " ignoring: " + chatIgnoreUid);

        // מסיר מאזין קודם אם קיים כדי למנוע כפילויות
        if (messageListener != null) {
            FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference("notifications")
                    .child(uid)
                    .removeEventListener(messageListener);
        }

        // מאזין חדש להודעות
        messageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d("MessageListenerService", "Notification added: " + dataSnapshot.getKey());
                Message message = dataSnapshot.getValue(Message.class);

                if (message == null || message.getUsername() == null) {
                    Log.d("MessageListenerService", "Invalid message or missing username.");
                    return;
                }

                boolean isFromCurrentChat = chatIgnoreUid != null && message.getFrom().equals(chatIgnoreUid);

                // מעדכן שההתראה נשלחה (גם אם זו שיחה פעילה)
                dataSnapshot.getRef().child("notificationSent").setValue(true);

                if (!isFromCurrentChat && !message.isNotificationSent()) {
                    Log.d("MessageListenerService", "New notification from: " + message.getUsername());
                    sendNotification(message);
                    broadcastMessageToContactsList(message);
                } else {
                    Log.d("MessageListenerService", "Ignoring notification from current chat user: " + message.getUsername());
                }

                // מוחק את ההודעה מהמסד
                dataSnapshot.getRef().removeValue();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        // מחבר את המאזין למסד הנתונים
        FirebaseDatabase.getInstance("https://levelup-3bc20-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("notifications")
                .child(uid)
                .addChildEventListener(messageListener);
    }

    // פונקציה לשליחת ברודקאסט ל-ContactsList
    private void broadcastMessageToContactsList(Message message) {
        Intent broadcastIntent = new Intent("com.example.levelup.NEW_MESSAGE");
        broadcastIntent.setPackage(getPackageName());
        broadcastIntent.putExtra("username", message.getUsername());
        broadcastIntent.putExtra("content", message.getContent());
        broadcastIntent.putExtra("timestamp", message.getTimestamp());
        Log.d("MessageListenerService", "Broadcasting message: " + message.getContent());
        sendBroadcast(broadcastIntent);
    }


    private void sendNotification(Message message) {
        String groupKey = "com.example.levelup.NOTIFICATIONS_" + message.getFrom();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("senderUsername", message.getUsername());
        intent.putExtra("senderUid", message.getFrom());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notificationicon) // Ensure this icon exists
                .setContentTitle("New Message from " + message.getUsername())
                .setContentText(message.getContent())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setGroup(groupKey)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // Show on lock screen

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());

        // Create a summary notification to group all notifications
        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notificationicon) // Ensure this icon exists
                .setContentTitle("New Messages from " + message.getUsername())
                .setContentText("You have new messages")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // Show on lock screen

        notificationManager.notify(message.getFrom().hashCode(), summaryBuilder.build());
        Log.d("MessageListenerService", "Notification sent for message from: " + message.getUsername());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Message Notifications";
            String description = "Channel for message notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d("MessageListenerService", "Notification channel created");
            } else {
                Log.e("MessageListenerService", "NotificationManager is null, cannot create notification channel");
            }
        }
    }
}
