package com.vdocipher.sampleapp.tvapp;

import static com.vdocipher.sampleapp.tvapp.TvPlayerUIActivity.VIDEO;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.vdocipher.aegis.media.ErrorDescription;
import com.vdocipher.aegis.media.PlayerOption;
import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.player.PlayerHost;
import com.vdocipher.aegis.player.VdoInitParams;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerSupportFragment;

/**
 * This class serves as a basic example of integrating playback in android TV.
 * <p>
 * It uses a class extending {@link androidx.leanback.app.PlaybackSupportFragment} to provide a
 * standard playback control ui for android TV. However, you can make your own or use a different
 * playback controller layout.
 * <p>
 * The {@link PlaybackOverlayFragment} handles user interaction and interacts with the {@link VdoPlayer}
 * held by this class.
 */
public class TvPlayerActivity extends FragmentActivity implements PlayerHost.InitializationListener,
        VdoPlayer.PlaybackEventListener {
    private static final String TAG = "TvPlayerActivity";

    private PlaybackOverlayFragment overlayFragment;
    private VdoPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_player);

        overlayFragment = (PlaybackOverlayFragment) getSupportFragmentManager().findFragmentById(R.id.tv_player_overlay_fragment);
        VdoPlayerSupportFragment playerFragment = (VdoPlayerSupportFragment) getSupportFragmentManager().findFragmentById(R.id.tv_player_fragment);
        if (playerFragment != null) {
            playerFragment.initialize(TvPlayerActivity.this);
        }
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
    public void onDeInitializationSuccess() {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int state) {
        overlayFragment.playbackStateChanged(playWhenReady, state);
    }

    @Override
    public void onSeekTo(long timeMs) {
    }

    @Override
    public void onProgress(long timeMs) {
        overlayFragment.playbackPositionChanged(timeMs);
    }

    @Override
    public void onBufferUpdate(long timeMs) {
    }

    @Override
    public void onPlaybackSpeedChanged(float speed) {
    }

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
    public void onTracksChanged(Track[] tracks, Track[] tracks1) {
    }

    @Override
    public void onMetaDataLoaded(PlayerOption playerOption) {

    }

    private void loadParams() {
        Video video = getIntent().getParcelableExtra(VIDEO);
        VdoInitParams vdoParams = VdoInitParams.createParamsWithOtp(video.getVideoOtp(), video.getVideoPlaybackInfo());
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
