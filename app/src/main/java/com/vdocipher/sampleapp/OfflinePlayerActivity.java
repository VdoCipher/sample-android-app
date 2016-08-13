package com.vdocipher.sampleapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerFragment;

public class OfflinePlayerActivity extends AppCompatActivity implements VdoPlayer.OnInitializationListener {

    private final String TAG = "OfflinePlayerActivity";

    private VdoPlayer player;
    private VdoPlayerFragment playerFragment;
    private Button playButton, pauseButton, onBuffering;
    private TextView seekStart, seekEnd;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_player);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        seekBar = (SeekBar)findViewById(R.id.offline_activity_seek);
        seekBar.setEnabled(false);
        seekStart = (TextView)findViewById(R.id.offline_activity_seek_indicator);
        seekEnd = (TextView)findViewById(R.id.offline_activity_seek_end);
        onBuffering = (Button)findViewById(R.id.offline_on_buffering);
        playerFragment = (VdoPlayerFragment)getFragmentManager().findFragmentById(R.id.offline_vdo_player_fragment);
        playButton = (Button)findViewById(R.id.offline_play_button);
        playButton.setEnabled(false);
        pauseButton = (Button)findViewById(R.id.offline_pause_button);
        pauseButton.setEnabled(false);

        startPlayer();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop called");
        super.onStop();
    }

    private void startPlayer() {
        final String videoId = "********";
        String localFolder = getExternalFilesDir(null).getPath();
        // create vdoInitParams
        VdoPlayerFragment.VdoInitParams vdoParams = new VdoPlayerFragment.VdoInitParams(null, true, localFolder, videoId);
        // set vdoInitParams to vdoPlayerFragment
        playerFragment.setInitParams(vdoParams);
        // initialize vdoPlayerFragment with otp(null) and a VdoPlayer.OnInitializationListener
        playerFragment.initialize(null, OfflinePlayerActivity.this);
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
        }

        @Override
        public void onBuffering(boolean isBuffering) {
            Log.v(TAG, isBuffering ? "buffering started" : "buffering stopped");
            onBuffering.setText("on_buffering: " + (isBuffering ? "YES" : "NO"));
        }

        @Override
        public void onBufferUpdate(int bufferTime) {
        }

        @Override
        public void onSeekTo(int millis) {
            Log.v(TAG, "onSeekTo " + String.valueOf(millis));
        }

        @Override
        public void onProgress(int millis) {
            Log.v(TAG, "onProgress: " + String.valueOf(millis));
            seekBar.setProgress(millis);
            seekStart.setText(String.valueOf(millis));
        }
    };

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
