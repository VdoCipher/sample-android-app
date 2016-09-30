package com.vdocipher.sampleapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerFragment;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class OnlinePlayerActivity extends AppCompatActivity implements VdoPlayer.OnInitializationListener {

    private final String TAG = "OnlinePlayerActivity";

    private VdoPlayer player;
    private VdoPlayerFragment playerFragment;
    private Button playButton,pauseButton;
    private TextView seekStart, seekEnd;
    private SeekBar seekBar;
    private ProgressBar bufferingIcon;

    private AsyncHttpClient client = new AsyncHttpClient();
    private String otp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate called");
        setContentView(R.layout.activity_online_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        seekBar = (SeekBar)findViewById(R.id.online_activity_seek);
        seekBar.setEnabled(false);
        seekStart = (TextView)findViewById(R.id.online_activity_seek_indicator);
        seekEnd = (TextView)findViewById(R.id.online_activity_seek_end);
        playerFragment = (VdoPlayerFragment)getFragmentManager().findFragmentById(R.id.online_vdo_player_fragment);
        playButton = (Button)findViewById(R.id.online_play_button);
        playButton.setEnabled(false);
        pauseButton = (Button)findViewById(R.id.online_pause_button);
        pauseButton.setEnabled(false);
        bufferingIcon = (ProgressBar) findViewById(R.id.loading_icon);

        if (savedInstanceState != null) {
            otp = savedInstanceState.getString("otp", null);
            Log.v(TAG, "otp: " + otp);
        }
        getSampleOtpAndStartPlayer();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop called");
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (otp != null) outState.putString("otp", otp);
    }

    private void getSampleOtpAndStartPlayer() {
        final String videoId = "********";
        final String OTP_URL = "https://api.vdocipher.com/v2/otp/?video=" + videoId;
        RequestParams params = new RequestParams();
        params.put("clientSecretKey", "********");

        if (otp == null) {
            client.post(OTP_URL, params, new TextHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    try {
                        JSONObject jObject = new JSONObject(responseString);
                        otp = jObject.getString("otp");
                        Log.v(TAG, "otp: " + otp);
                        // create vdoInitParams
                        VdoPlayerFragment.VdoInitParams vdoParams1 = new VdoPlayerFragment.VdoInitParams(otp, false, null, null);
                        // set vdoInitParams to vdoPlayerFragment
                        playerFragment.setInitParams(vdoParams1);
                        // initialize vdoPlayerFragment with otp and a VdoPlayer.OnInitializationListener
                        playerFragment.initialize(otp, OnlinePlayerActivity.this);
                    } catch (JSONException e) {
                        Log.v(TAG, Log.getStackTraceString(e));
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.v(TAG, "status code: " + responseString);
                }
            });
        } else {
            // create vdoInitParams
            VdoPlayerFragment.VdoInitParams vdoParams1 = new VdoPlayerFragment.VdoInitParams(otp, false, null, null);
            // set vdoInitParams to vdoPlayerFragment
            playerFragment.setInitParams(vdoParams1);
            // initialize vdoPlayerFragment with otp and a VdoPlayer.OnInitializationListener
            playerFragment.initialize(otp, OnlinePlayerActivity.this);
        }
    }

    View.OnClickListener playListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            player.play();
        }
    };

    View.OnClickListener pauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            player.pause();
        }
    };

    @Override
    public void onInitializationSuccess(VdoPlayer player, boolean wasRestored) {
        Log.v(TAG, "onInitializationSuccess");
        this.player = player;
        player.setOnPlaybackEventListener(playbackListener);
        Log.v(TAG, "player duration = " + player.getDuration());
        seekEnd.setText(String.valueOf(player.getDuration()));
        seekBar.setMax(player.getDuration());
        seekBar.setEnabled(true);
        seekBar.setOnSeekBarChangeListener(seekbarChangeListener);
        playButton.setOnClickListener(playListener);
        pauseButton.setOnClickListener(pauseListener);
        playButton.setEnabled(true);
        pauseButton.setEnabled(true);
    }

    @Override
    public void onInitializationFailure(String reason) {
        Log.v(TAG, "onInitializationFailure: " + reason);
    }

    private VdoPlayer.OnPlaybackEventListener playbackListener = new VdoPlayer.OnPlaybackEventListener() {
        @Override
        public void onPlaying() {
            Log.v(TAG, "onPlaying");
        }

        @Override
        public void onPaused() {
            Log.v(TAG, "onPaused");
        }

        @Override
        public void onStopped() {
            Log.v(TAG, "onStopped");
            pauseButton.setEnabled(false);
            playButton.setText("REPLAY");
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (player.isStopped()) {
                        player.restart();
                    }
                    player.play();
                    ((Button)v).setText("PLAY");
                    v.setOnClickListener(playListener);
                    pauseButton.setEnabled(true);
                }
            });
        }

        @Override
        public void onBuffering(boolean isBuffering) {
            Log.v(TAG, isBuffering ? "buffering started" : "buffering stopped");
            showLoadingIcon(isBuffering);
        }

        @Override
        public void onBufferUpdate(int bufferTime) {
            seekBar.setSecondaryProgress(bufferTime);
        }

        @Override
        public void onSeekTo(int millis) {
            Log.v(TAG, "onSeekTo: " + String.valueOf(millis));
        }

        @Override
        public void onProgress(int millis) {
            String currTimeStr = String.valueOf(millis);
            //Log.v(TAG, "onProgress: " + currTimeStr);
            seekBar.setProgress(millis);
            seekStart.setText(currTimeStr);
        }
    };

    private void showLoadingIcon(final boolean showIcon) {
        if (showIcon) {
            bufferingIcon.setVisibility(View.VISIBLE);
            bufferingIcon.bringToFront();
        } else {
            bufferingIcon.setVisibility(View.INVISIBLE);
            bufferingIcon.requestLayout();
        }
    }

    private SeekBar.OnSeekBarChangeListener seekbarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
            // nothing much to do here
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // nothing much to do here
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            player.seekTo(seekBar.getProgress());
        }
    };
}
