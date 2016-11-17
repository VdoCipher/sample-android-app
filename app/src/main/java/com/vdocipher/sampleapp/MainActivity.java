package com.vdocipher.sampleapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop called");
    }

    public void onlinePlayback(View v) {
        Intent intent = new Intent(this, OnlinePlayerActivity.class);
        startActivity(intent);
    }

    public void offlinePlayback(View v) {
        Intent intent = new Intent(this, OfflinePlayerActivity.class);
        startActivity(intent);
    }

    public void download(View v) {
        Intent intent = new Intent(this, DownloadActivity.class);
        startActivity(intent);
    }
}
