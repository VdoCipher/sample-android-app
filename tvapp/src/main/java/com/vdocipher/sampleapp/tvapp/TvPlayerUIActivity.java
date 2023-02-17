package com.vdocipher.sampleapp.tvapp;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.vdocipher.aegis.media.ErrorDescription;
import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.player.PlayerHost;
import com.vdocipher.aegis.player.VdoInitParams;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.ui.view.VdoPlayerUIFragment;

public class TvPlayerUIActivity extends AppCompatActivity implements PlayerHost.InitializationListener {

    private static final String TAG = "TvPlayerUIActivity";
    public static final String VIDEO = "video";

    private VdoPlayerUIFragment vdoPlayerUIFragment;

    private VdoInitParams vdoParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_player_ui);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (vdoParams == null) {
            Video video = getIntent().getParcelableExtra(VIDEO);
            vdoParams = new VdoInitParams.Builder()
                    .setOtp(video.getVideoOtp())
                    .setPlaybackInfo(video.getVideoPlaybackInfo())
                    .setPreferredCaptionsLanguage("en")
                    .setForceHighestSupportedBitrate(true)
                    .build();
        }

        vdoPlayerUIFragment = (VdoPlayerUIFragment) getSupportFragmentManager().findFragmentById(R.id.vdo_player_fragment);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        initializePlayer();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (vdoParams != null) {
            outState.putParcelable("initParams", vdoParams);
        }
    }

    private void initializePlayer() {
        if (vdoParams != null) {
            // initialize the playerFragment; a VdoPlayer instance will be received
            // in onInitializationSuccess() callback
            vdoPlayerUIFragment.initialize(TvPlayerUIActivity.this);
        }
    }

    @Override
    public void onInitializationSuccess(PlayerHost playerHost, VdoPlayer player, boolean wasRestored) {
        player.addPlaybackEventListener(playbackListener);
        // load a media to the player
        player.load(vdoParams);
    }

    @Override
    public void onInitializationFailure(PlayerHost playerHost, ErrorDescription errorDescription) {
        String msg = "onInitializationFailure: errorCode = " + errorDescription.errorCode + ": " + errorDescription.errorMsg;
        Log.e(TAG, msg);
        Toast.makeText(TvPlayerUIActivity.this, "initialization failure: " + errorDescription.errorMsg, Toast.LENGTH_LONG).show();
    }

    private final VdoPlayer.PlaybackEventListener playbackListener = new VdoPlayer.PlaybackEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        }

        @Override
        public void onTracksChanged(Track[] tracks, Track[] tracks1) {

        }

        @Override
        public void onBufferUpdate(long bufferTime) {
        }

        @Override
        public void onSeekTo(long millis) {
            Log.i(TAG, "onSeekTo: " + millis);
        }

        @Override
        public void onProgress(long millis) {
        }

        @Override
        public void onPlaybackSpeedChanged(float speed) {

        }

        @Override
        public void onLoading(VdoInitParams vdoInitParams) {

        }

        @Override
        public void onLoadError(VdoInitParams vdoInitParams, ErrorDescription errorDescription) {

        }

        @Override
        public void onLoaded(VdoInitParams vdoInitParams) {

        }

        @Override
        public void onError(VdoInitParams vdoParams, ErrorDescription errorDescription) {
            Log.d("error", errorDescription.toString());
        }

        @Override
        public void onMediaEnded(VdoInitParams vdoInitParams) {

        }
    };
}