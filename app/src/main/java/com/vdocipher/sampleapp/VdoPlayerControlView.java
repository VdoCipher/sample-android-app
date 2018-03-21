package com.vdocipher.sampleapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.vdocipher.aegis.media.ErrorDescription;
import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.player.VdoPlayer;

/**
 * A view for controlling playback via a VdoPlayer.
 */
public class VdoPlayerControlView extends FrameLayout {
    public interface ControllerVisibilityListener {
        /**
         * Called when the visibility of the controller ui changes.
         *
         * @param visibility new visibility of controller ui. Either {@link View#VISIBLE} or
         * {@link View#GONE}.
         */
        void onControllerVisibilityChange(int visibility);
    }

    public interface FullscreenActionListener {
        /**
         * @return if enter or exit fullscreen action was handled
         */
        boolean onFullscreenAction(boolean enterFullscreen);
    }
    
    public static final int DEFAULT_FAST_FORWARD_MS = 10000;
    public static final int DEFAULT_REWIND_MS = 10000;
    public static final int DEFAULT_SHOW_TIMEOUT_MS = 3000;

    private final View playButton;
    private final View pauseButton;
    private final View fastForwardButton;
    private final View rewindButton;
    private final TextView durationView;
    private final TextView positionView;
    private final SeekBar seekBar;
    private final Button speedControlButton;
    private final ImageButton captionsButton;
    private final ImageButton enterFullscreenButton;
    private final ImageButton exitFullscreenButton;
    private final ProgressBar loaderView;
    private final ImageButton errorView;
    private final TextView errorTextView;
    private final View controlPanel;
    private final View controllerBackground;

    private int ffwdMs;
    private int rewindMs;
    private int showTimeoutMs;

    private boolean scrubbing;
    private boolean isAttachedToWindow;
    private boolean fullscreen;

    private VdoPlayer player;
    private UiListener uiListener;
    private VdoPlayer.VdoInitParams lastErrorParams;
    private FullscreenActionListener fullscreenActionListener;
    private ControllerVisibilityListener visibilityListener;

    private static final float allowedSpeedList[] = new float[]{0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f};
    private static final CharSequence allowedSpeedStrList[] =
            new CharSequence[]{"0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x"};
    private int chosenSpeedIndex = 2;

    private Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public VdoPlayerControlView(Context context) {
        this(context, null);
    }

    public VdoPlayerControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VdoPlayerControlView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        ffwdMs = DEFAULT_FAST_FORWARD_MS;
        rewindMs = DEFAULT_REWIND_MS;
        showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS;

        uiListener = new UiListener();

        LayoutInflater.from(context).inflate(R.layout.vdo_control_view, this);

        playButton = findViewById(R.id.vdo_play);
        playButton.setOnClickListener(uiListener);
        pauseButton = findViewById(R.id.vdo_pause);
        pauseButton.setOnClickListener(uiListener);
        pauseButton.setVisibility(GONE);
        fastForwardButton = findViewById(R.id.vdo_ffwd);
        fastForwardButton.setOnClickListener(uiListener);
        rewindButton = findViewById(R.id.vdo_rewind);
        rewindButton.setOnClickListener(uiListener);
        durationView = (TextView)findViewById(R.id.vdo_duration);
        positionView = (TextView)findViewById(R.id.vdo_position);
        seekBar = (SeekBar)findViewById(R.id.vdo_seekbar);
        seekBar.setOnSeekBarChangeListener(uiListener);
        speedControlButton = (Button)findViewById(R.id.vdo_speed);
        speedControlButton.setOnClickListener(uiListener);
        captionsButton = (ImageButton)findViewById(R.id.vdo_captions);
        captionsButton.setOnClickListener(uiListener);
        enterFullscreenButton = (ImageButton)findViewById(R.id.vdo_enter_fullscreen);
        enterFullscreenButton.setOnClickListener(uiListener);
        exitFullscreenButton = (ImageButton)findViewById(R.id.vdo_exit_fullscreen);
        exitFullscreenButton.setOnClickListener(uiListener);
        exitFullscreenButton.setVisibility(GONE);
        loaderView = (ProgressBar)findViewById(R.id.vdo_loader);
        loaderView.setVisibility(GONE);
        errorView = (ImageButton)findViewById(R.id.vdo_error);
        errorView.setOnClickListener(uiListener);
        errorView.setVisibility(GONE);
        errorTextView = (TextView)findViewById(R.id.vdo_error_text);
        errorTextView.setOnClickListener(uiListener);
        errorTextView.setVisibility(GONE);
        controlPanel = findViewById(R.id.vdo_control_panel);
        controllerBackground = findViewById(R.id.vdo_controller_bg);
        setOnClickListener(uiListener);
    }

    public void setPlayer(VdoPlayer vdoPlayer) {
        if (player == vdoPlayer) return;

        if (player != null) {
            player.removePlaybackEventListener(uiListener);
        }
        player = vdoPlayer;
        if (player != null) {
            player.addPlaybackEventListener(uiListener);
        }
    }
    
    public void setFullscreenActionListener(FullscreenActionListener fullscreenActionListener) {
        this.fullscreenActionListener = fullscreenActionListener;
    }

    public void setControllerVisibilityListener(ControllerVisibilityListener visibilityListener) {
        this.visibilityListener = visibilityListener;
    }

    public void show() {
        if (!controllerVisible()) {
            controllerBackground.setVisibility(VISIBLE);
            updateAll();
            if (visibilityListener != null) {
                visibilityListener.onControllerVisibilityChange(controllerBackground.getVisibility());
            }
        }
        hideAfterTimeout();
    }

    public void hide() {
        if (controllerVisible()) {
            controllerBackground.setVisibility(GONE);
            removeCallbacks(hideAction);
            if (visibilityListener != null) {
                visibilityListener.onControllerVisibilityChange(controllerBackground.getVisibility());
            }
        }
    }

    /**
     * Call if fullscreen in entered/exited in response to external triggers such as orientation
     * change, back button etc.
     * @param fullscreen true if fullscreen in new state
     */
    public void setFullscreenState(boolean fullscreen) {
        this.fullscreen = fullscreen;
        updateFullscreenButtons();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
        removeCallbacks(hideAction);
    }

    /**
     * Call to know the visibility of the playback controls ui. VdoPlayerControlView itself doesn't
     * change visibility when hiding ui controls.
     * @return true if playback controls are visible
     */
    public boolean controllerVisible() {
        return controllerBackground.getVisibility() == VISIBLE;
    }

    private void hideAfterTimeout() {
        removeCallbacks(hideAction);
        boolean playing = player != null && player.getPlayWhenReady();
        if (showTimeoutMs > 0 && isAttachedToWindow && playing) {
            postDelayed(hideAction, showTimeoutMs);
        }
    }

    private void updateAll() {
        updatePlayPauseButtons();
    }

    private void updatePlayPauseButtons() {
        boolean playWhenReady = player != null && player.getPlayWhenReady();
        playButton.setVisibility(playWhenReady ? GONE : VISIBLE);
        pauseButton.setVisibility(playWhenReady ? VISIBLE : GONE);
    }

    private void rewind() {
        if (rewindMs > 0) {
            player.seekTo(Math.max(0, player.getCurrentTime() - rewindMs));
        }
    }

    private void fastForward() {
        if (ffwdMs > 0) {
            player.seekTo(Math.min(player.getDuration(), player.getCurrentTime() + ffwdMs));
        }
    }

    private void updateLoader(boolean loading) {
        loaderView.setVisibility(loading ? VISIBLE : GONE);
    }
    
    private void toggleFullscreen() {
        if (fullscreenActionListener != null) {
            boolean handled = fullscreenActionListener.onFullscreenAction(!fullscreen);
            if (handled) {
                fullscreen = !fullscreen;
                updateFullscreenButtons();
            }
        }
    }

    private void updateFullscreenButtons() {
        enterFullscreenButton.setVisibility(fullscreen ? GONE : VISIBLE);
        exitFullscreenButton.setVisibility(fullscreen ? VISIBLE : GONE);
    }

    private void showSpeedControlDialog() {
        new AlertDialog.Builder(getContext())
                .setSingleChoiceItems(allowedSpeedStrList, chosenSpeedIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (player != null) {
                            float speed = allowedSpeedList[which];
                            player.setPlaybackSpeed(speed);
                        }
                        dialog.dismiss();
                    }
                })
                .setTitle("Choose playback speed")
                .show();
    }

    private void showCaptionsDialog() {
        // todo
    }

    private void showError(ErrorDescription errorDescription) {
        // todo finish
        updateLoader(false);
        controlPanel.setVisibility(GONE);
        errorView.setVisibility(VISIBLE);
        errorTextView.setVisibility(VISIBLE);
        String errMsg = "An error occurred : " + errorDescription.errorCode + "\nTap to retry";
        errorTextView.setText(errMsg);
    }

    private void retryAfterError() {
        if (lastErrorParams != null) {
            errorView.setVisibility(GONE);
            errorTextView.setVisibility(GONE);
            player.load(lastErrorParams);
            lastErrorParams = null;
        }
    }

    private final class UiListener implements VdoPlayer.PlaybackEventListener,
            SeekBar.OnSeekBarChangeListener, OnClickListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            scrubbing = true;
            removeCallbacks(hideAction);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            scrubbing = false;
            int seekTarget = seekBar.getProgress();
            player.seekTo(seekTarget);
            hideAfterTimeout();
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            updatePlayPauseButtons();
            updateLoader(playbackState == VdoPlayer.STATE_BUFFERING);
        }

        @Override
        public void onClick(View v) {
            boolean hideAfterTimeout = true;
            if (player != null) {
                if (v == rewindButton) {
                    rewind();
                } else if (v == playButton) {
                    player.setPlayWhenReady(true);
                } else if (v == pauseButton) {
                    hideAfterTimeout = false;
                    player.setPlayWhenReady(false);
                } else if (v == fastForwardButton) {
                    fastForward();
                } else if (v == speedControlButton) {
                    hideAfterTimeout = false;
                    showSpeedControlDialog();
                } else if (v == captionsButton) {
                    hideAfterTimeout = false;
                    showCaptionsDialog();
                } else if (v == enterFullscreenButton || v == exitFullscreenButton) {
                    toggleFullscreen();
                } else if (v == errorView || v == errorTextView) {
                    retryAfterError();
                } else if (v == VdoPlayerControlView.this) {
                    hideAfterTimeout = false;
                    if (controllerVisible()) {
                        hide();
                    } else {
                        show();
                    }
                }
            }
            if (hideAfterTimeout) {
                hideAfterTimeout();
            }
        }

        @Override
        public void onSeekTo(long millis) {}

        @Override
        public void onProgress(long millis) {
            positionView.setText(Utils.digitalClockTime((int)millis));
            seekBar.setProgress((int)millis);
        }

        @Override
        public void onBufferUpdate(long bufferTime) {
            seekBar.setSecondaryProgress((int)bufferTime);
        }

        @Override
        public void onPlaybackSpeedChanged(float speed) {
            chosenSpeedIndex = Utils.getClosestFloatIndex(allowedSpeedList, speed);
            speedControlButton.setText(allowedSpeedStrList[chosenSpeedIndex]);
        }

        @Override
        public void onLoading(VdoPlayer.VdoInitParams vdoInitParams) {
            updateLoader(true);
        }

        @Override
        public void onLoaded(VdoPlayer.VdoInitParams vdoInitParams) {
            durationView.setText(String.valueOf(Utils.digitalClockTime((int)player.getDuration())));
            seekBar.setMax((int)player.getDuration());
            controlPanel.setEnabled(true);
        }

        @Override
        public void onLoadError(VdoPlayer.VdoInitParams vdoParams, ErrorDescription errorDescription) {
            lastErrorParams = vdoParams;
        }

        @Override
        public void onMediaEnded(VdoPlayer.VdoInitParams vdoInitParams) {
            // todo
        }

        @Override
        public void onError(VdoPlayer.VdoInitParams vdoParams, ErrorDescription errorDescription) {
            lastErrorParams = vdoParams;
        }

        @Override
        public void onTracksChanged(Track[] availableTracks, Track[] selectedTracks) {

        }
    }
}
