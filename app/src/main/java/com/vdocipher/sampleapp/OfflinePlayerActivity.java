package com.vdocipher.sampleapp;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerFragment;

public class OfflinePlayerActivity extends AppCompatActivity implements VdoPlayer.OnInitializationListener {

    private final String TAG = "OfflinePlayerActivity";

    private VdoPlayer player;
    private VdoPlayerFragment playerFragment;
    private ImageButton playPauseButton, replayButton;
    private TextView currTime, duration;
    private SeekBar seekBar;
    private ProgressBar bufferingIcon;

    private boolean isPlaying = false;
    private boolean controlsShowing = false;
    private boolean isLandscape = false;
    private int mLastSystemUiVis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(uiVisibilityListener);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        seekBar = (SeekBar)findViewById(R.id.seekbar);
        seekBar.setEnabled(false);
        currTime = (TextView)findViewById(R.id.current_time);
        duration = (TextView)findViewById(R.id.duration);
        playerFragment = (VdoPlayerFragment)getFragmentManager().findFragmentById(R.id.offline_vdo_player_fragment);
        playPauseButton = (ImageButton)findViewById(R.id.play_pause_button);
        replayButton = (ImageButton)findViewById(R.id.replay_button);
        replayButton.setVisibility(View.INVISIBLE);
        bufferingIcon = (ProgressBar) findViewById(R.id.loading_icon);
        showLoadingIcon(false);
        showControls(false);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

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
        VdoPlayer.VdoInitParams vdoParams = new VdoPlayer.VdoInitParams(null, true, localFolder, videoId);
        // initialize vdoPlayerFragment with initParams and a VdoPlayer.OnInitializationListener
        playerFragment.initialize(vdoParams, OfflinePlayerActivity.this);
        showLoadingIcon(true);
    }

    private View.OnClickListener playPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (player == null) return;
            if (isPlaying) {
                player.pause();
            } else {
                player.play();
            }
        }
    };

    private View.OnClickListener playerTapListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showControls(!controlsShowing); // controlsShowing changed in this method
            if (isLandscape) {
                // show/hide system ui as well
                showSystemUi(controlsShowing);
            }
        }
    };

    private void showControls(boolean show) {
        int visibility = show ? View.VISIBLE : View.INVISIBLE;
        playPauseButton.setVisibility(visibility);
        (findViewById(R.id.bottom_panel)).setVisibility(visibility);
        controlsShowing = show;
    }

    @Override
    public void onInitializationSuccess(VdoPlayer player, boolean wasRestored) {
        Log.v(TAG, "onInitializationSuccess");
        this.player = player;
        player.setOnPlaybackEventListener(playbackListener);
        Log.v(TAG, "player duration = " + player.getDuration());
        duration.setText(Utils.digitalClockTime(player.getDuration()));
        seekBar.setMax(player.getDuration());
        seekBar.setEnabled(true);
        seekBar.setOnSeekBarChangeListener(seekbarChangeListener);
        playPauseButton.setOnClickListener(playPauseListener);
        replayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replayButton.setVisibility(View.INVISIBLE);
                if (OfflinePlayerActivity.this.player != null) {
                    OfflinePlayerActivity.this.player.restart();
                    playPauseButton.setEnabled(true);
                }
            }
        });
        showLoadingIcon(false);

        (findViewById(R.id.player_region)).setOnClickListener(playerTapListener);
        (findViewById(R.id.fullscreen_toggle_button)).setOnClickListener(fullscreenToggleListener);
        showControls(true);
    }

    @Override
    public void onInitializationFailure(VdoPlayer.InitializationResult result) {
        Log.v(TAG, "onInitializationFailure: " + result.name());
        Toast.makeText(OfflinePlayerActivity.this, "initialization failure: " + result.name(), Toast.LENGTH_LONG).show();
        showLoadingIcon(false);
    }

    private VdoPlayer.OnPlaybackEventListener playbackListener = new VdoPlayer.OnPlaybackEventListener() {
        @Override
        public void onPlaying() {
            Log.v(TAG, "onPlaying");
            isPlaying = true;
            playPauseButton.setImageResource(R.drawable.ic_pause_white_48dp);
        }

        @Override
        public void onPaused() {
            Log.v(TAG, "onPaused");
            isPlaying = false;
            playPauseButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        }

        @Override
        public void onStopped() {
            Log.v(TAG, "onStopped");
            playPauseButton.setEnabled(false);
            playPauseButton.setVisibility(View.INVISIBLE);
            replayButton.setVisibility(View.VISIBLE);
        }

        @Override
        public void onBuffering(boolean isBuffering) {
            Log.v(TAG, isBuffering ? "buffering started" : "buffering stopped");
            showLoadingIcon(isBuffering);
            playPauseButton.setVisibility(View.INVISIBLE);
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
            seekBar.setProgress(millis);
            currTime.setText(Utils.digitalClockTime(millis));
        }

        @Override
        public void onError(VdoPlayer.PlaybackErrorReason playbackErrorReason) {
            Log.e(TAG, playbackErrorReason.name());
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

    private View.OnClickListener fullscreenToggleListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showFullScreen(!isLandscape);
            isLandscape = !isLandscape;
            int fsButtonResId = isLandscape ? R.drawable.ic_action_return_from_full_screen : R.drawable.ic_action_full_screen;
            ((ImageButton)(findViewById(R.id.fullscreen_toggle_button))).setImageResource(fsButtonResId);
        }
    };

    private void showFullScreen(boolean show) {
        Log.v(TAG, show ? "go fullscreen" : "return from fullscreen");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (show) {
                // go to landscape orientation
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                // hide other views
                (findViewById(R.id.title_text)).setVisibility(View.GONE);
                (findViewById(R.id.offline_vdo_player_fragment)).setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
                findViewById(R.id.player_region).setFitsSystemWindows(true);
                // hide system windows
                showSystemUi(false);
                showControls(false);
            } else {
                // go to portrait orientation
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                // show other views
                (findViewById(R.id.title_text)).setVisibility(View.VISIBLE);
                (findViewById(R.id.offline_vdo_player_fragment)).setLayoutParams(new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                findViewById(R.id.player_region).setFitsSystemWindows(false);
                findViewById(R.id.player_region).setPadding(0,0,0,0);
                // show system windows
                showSystemUi(true);
            }
        }
    }

    private void showSystemUi(boolean show) {
        Log.v(TAG, (show ? "show " : "hide ") + "system ui");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (!show) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        }
    }

    private View.OnSystemUiVisibilityChangeListener uiVisibilityListener = new View.OnSystemUiVisibilityChangeListener() {
        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            Log.v(TAG, "onSystemUiVisibilityChange");
            // show player controls when system ui is showing
            int diff = mLastSystemUiVis ^ visibility;
            mLastSystemUiVis = visibility;
            if ((diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                    && (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                Log.v(TAG, "system ui visible, making controls visible");
                showControls(true);
            }
        }
    };
}
