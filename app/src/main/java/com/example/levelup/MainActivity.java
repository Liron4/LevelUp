package com.example.levelup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import com.example.levelup.services.MessageListenerService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Start the msg service
        Intent serviceIntent = new Intent(this, MessageListenerService.class);
        startService(serviceIntent);
        Log.d("MainActivity", "MessageService started");




    }


}