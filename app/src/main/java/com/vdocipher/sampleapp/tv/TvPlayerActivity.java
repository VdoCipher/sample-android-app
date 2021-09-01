package com.vdocipher.sampleapp.tv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.vdocipher.aegis.media.ErrorDescription;
import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.player.PlayerHost;
import com.vdocipher.aegis.player.VdoInitParams;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerFragment;
import com.vdocipher.sampleapp.R;

/**
 * This class serves as a basic example of integrating playback in android TV.
 *
 * It uses a class extending {@link androidx.leanback.app.PlaybackFragment} to provide a
 * standard playback control ui for android TV. However, you can make your own or use a different
 * playback controller layout.
 *
 * The {@link PlaybackOverlayFragment} handles user interaction and interacts with the {@link VdoPlayer}
 * held by this class.
 */
public class TvPlayerActivity extends Activity implements PlayerHost.InitializationListener,
        VdoPlayer.PlaybackEventListener {
    private static final String TAG = "TvPlayerActivity";
    public static final String EXTRA_OTP = "otp";
    public static final String EXTRA_PLAYBACK_INFO = "playbackInfo";

    private VdoPlayerFragment playerFragment;
    private PlaybackOverlayFragment overlayFragment;
    private VdoPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_player);

        overlayFragment = (PlaybackOverlayFragment)getFragmentManager().findFragmentById(R.id.tv_player_overlay_fragment);
        playerFragment = (VdoPlayerFragment)getFragmentManager().findFragmentById(R.id.tv_player_fragment);
        playerFragment.initialize(TvPlayerActivity.this);
    }

    @Override
    public void onInitializationSuccess(PlayerHost playerHost, VdoPlayer vdoPlayer, boolean restored) {
        Log.i(TAG, "init success");
        mPlayer = vdoPlayer;
        vdoPlayer.addPlaybackEventListener(TvPlayerActivity.this);
        loadParams();
    }

    @Override
    public void onInitializationFailure(PlayerHost playerHost, ErrorDescription errorDescription) {
        Log.e(TAG, "init failure");
        showToast("Initialization failure. Reason: " + errorDescription.errorCode + ", " + errorDescription.errorMsg);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        overlayFragment.playbackStateChanged(playWhenReady, state);
    }

    @Override
    public void onSeekTo(long timeMs) {}

    @Override
    public void onProgress(long timeMs) {
        overlayFragment.playbackPositionChanged(timeMs);
    }

    @Override
    public void onBufferUpdate(long timeMs) {}

    @Override
    public void onPlaybackSpeedChanged(float speed) {}

    @Override
    public void onLoading(VdoInitParams vdoInitParams) {
        Log.i(TAG, "onLoading");
    }

    @Override
    public void onLoaded(VdoInitParams vdoInitParams) {
        Log.i(TAG, "onLoaded");
        mPlayer.setPlayWhenReady(true);
        overlayFragment.playbackDurationChanged(mPlayer.getDuration());
    }

    @Override
    public void onLoadError(VdoInitParams vdoInitParams, ErrorDescription errorDescription) {
        showToast("onLoadError " + errorDescription.errorCode + ": " + errorDescription.errorMsg);
    }

    @Override
    public void onMediaEnded(VdoInitParams vdoInitParams) {
        Log.i(TAG, "onMediaEnded");
    }

    @Override
    public void onError(VdoInitParams vdoInitParams, ErrorDescription errorDescription) {
        showToast("onError code " + errorDescription.errorCode + ": " + errorDescription.errorMsg);
    }

    @Override
    public void onTracksChanged(Track[] tracks, Track[] tracks1) {}

    private void loadParams() {
        Intent intent = getIntent();
        String otp = intent.getStringExtra(EXTRA_OTP);
        String playbackInfo = intent.getStringExtra(EXTRA_PLAYBACK_INFO);
        VdoInitParams vdoParams = VdoInitParams.createParamsWithOtp(otp, playbackInfo);
        mPlayer.load(vdoParams);
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        if (mPlayer != null) {
            mPlayer.setPlayWhenReady(playWhenReady);
        }
    }

    public void fastForward() {
        if (mPlayer != null) {
            long target = Math.min(mPlayer.getDuration(), mPlayer.getCurrentTime() + 5000);
            mPlayer.seekTo(target);
        }
    }

    public void rewind() {
        if (mPlayer != null) {
            long target = Math.max(0, mPlayer.getCurrentTime() - 5000);
            mPlayer.seekTo(target);
        }
    }

    private void showToast(String msg) {
        Log.i(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
