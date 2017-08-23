package com.vdocipher.sampleapp.tv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.vdocipher.sampleapp.R;
import com.vdocipher.sampleapp.Utils;

import org.json.JSONException;

import java.io.IOException;

public class TvLandingActivity extends Activity {
    private static final String TAG = "TvLandingActivity";

    private Button launchPlayerButton;

    private String mOtp, mPlaybackInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_landing);

        launchPlayerButton = (Button)findViewById(R.id.tv_launch_player);
    }

    @Override
    protected void onStart() {
        super.onStart();
        launchPlayerButton.setEnabled(false);
        launchPlayerButton.setText("Loading params...");
        // todo use asynctask
        // fetch otp and playbackInfo and initialize VdoPlayer
        // here we're fetching a sample (otp + playbackInfo)
        // TODO you need to generate/fetch (otp + playbackInfo) OR (signature + playbackInfo) for the video you wish to play
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Pair<String, String> pair = Utils.getSampleOtpAndPlaybackInfo();
                    mOtp = pair.first;
                    mPlaybackInfo = pair.second;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            launchPlayerButton.setText("START PLAYBACK");
                            launchPlayerButton.setEnabled(true);
                            launchPlayerButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    startPlayback();
                                }
                            });
                        }
                    });
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    // todo show error fragment
                    showToast("Error fetching otp and playbackInfo: " + e.getClass().getSimpleName());
                }
            }
        }).start();
    }

    private void startPlayback() {
        Intent intent = new Intent(this, TvPlayerActivity.class);
        intent.putExtra(TvPlayerActivity.OTP, mOtp);
        intent.putExtra(TvPlayerActivity.PLAYBACK_INFO, mPlaybackInfo);
        startActivity(intent);
    }

    private void showToast(final String message) {
        Log.i(TAG, message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TvLandingActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
