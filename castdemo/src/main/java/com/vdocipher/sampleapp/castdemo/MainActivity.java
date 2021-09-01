package com.vdocipher.sampleapp.castdemo;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.vdocipher.aegis.player.VdoInitParams;

import org.json.JSONException;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView loadInfo;
    private Button playerButton;
    private VdoInitParams initParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CastContext.getSharedInstance(this);

        setContentView(R.layout.activity_main);

        loadInfo = findViewById(R.id.params_loader_info);
        loadInfo.setText(R.string.load_active_msg);
        playerButton = findViewById(R.id.player_btn);

        obtainOtpAndPlaybackInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.cast_menu, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),
                menu,
                R.id.media_route_menu_item);
        return true;
    }

    /**
     * Fetch (otp + playbackInfo) and initialize VdoPlayer
     * here we're fetching a sample (otp + playbackInfo)
     * TODO you need to generate/fetch (otp + playbackInfo) OR (signature + playbackInfo) for the
     * video you wish to play
     */
    private void obtainOtpAndPlaybackInfo() {
        // todo use asynctask
        Log.i(TAG, "fetching params...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Pair<String, String> pair = Utils.getSampleOtpAndPlaybackInfo();
                    String otp = pair.first;
                    String playbackInfo = pair.second;
                    initParams = new VdoInitParams.Builder()
                            .setOtp(otp)
                            .setPlaybackInfo(playbackInfo)
                            .setPreferredCaptionsLanguage("en")
                            .build();

                    Log.i(TAG, "obtained new otp and playbackInfo");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadInfo.setText(R.string.load_done_msg);
                            playerButton.setEnabled(true);
                            playerButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    loadPlayerActivity();
                                }
                            });
                        }
                    });
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Error fetching otp and playbackInfo: " + e.getClass().getSimpleName(),
                                    Toast.LENGTH_LONG).show();
                            Log.e(TAG, "error fetching otp and playbackInfo");
                            loadInfo.setText(R.string.load_error_msg);
                            loadInfo.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    obtainOtpAndPlaybackInfo();
                                    loadInfo.setOnClickListener(null);
                                }
                            });
                        }
                    });
                }
            }
        }).start();
    }

    private void loadPlayerActivity() {
        Intent intent = new Intent(this, CastVdoPlayerActivity.class);
        intent.putExtra("initParams", initParams);
        startActivity(intent);
    }
}
