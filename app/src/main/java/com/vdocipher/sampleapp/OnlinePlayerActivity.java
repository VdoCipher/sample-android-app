package com.vdocipher.sampleapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vdocipher.aegis.media.ErrorDescription;
import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerFragment;

import org.json.JSONException;

import java.io.IOException;

public class OnlinePlayerActivity extends AppCompatActivity implements VdoPlayer.InitializationListener {

    private final String TAG = "OnlinePlayerActivity";

    private VdoPlayer player;
    private VdoPlayerFragment playerFragment;
    private ImageButton playPauseButton, replayButton, errorButton;
    private TextView currTime, duration;
    private SeekBar seekBar;
    private ProgressBar bufferingIcon;
    private Button speedControlButton;
    private TextView eventLog;
    private String eventLogString = "";

    private boolean playWhenReady = false;
    private boolean controlsShowing = false;
    private boolean isLandscape = false;
    private int mLastSystemUiVis;

    private volatile String mOtp;
    private volatile String mPlaybackInfo;

    private static final float allowedSpeedList[] = new float[]{0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f};
    private static final CharSequence allowedSpeedStrList[] =
            new CharSequence[]{"0.5x", "0.75x", "1x", "1.25x", "1.5x", "1.75x", "2x"};
    private int chosenSpeedIndex = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate called");
        setContentView(R.layout.activity_online_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(uiVisibilityListener);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (savedInstanceState != null) {
            mOtp = savedInstanceState.getString("otp");
            mPlaybackInfo = savedInstanceState.getString("playbackInfo");
        }

        seekBar = (SeekBar)findViewById(R.id.seekbar);
        seekBar.setEnabled(false);
        currTime = (TextView)findViewById(R.id.current_time);
        duration = (TextView)findViewById(R.id.duration);
        playerFragment = (VdoPlayerFragment)getFragmentManager().findFragmentById(R.id.online_vdo_player_fragment);
        playPauseButton = (ImageButton)findViewById(R.id.play_pause_button);
        replayButton = (ImageButton)findViewById(R.id.replay_button);
        replayButton.setVisibility(View.INVISIBLE);
        errorButton = (ImageButton)findViewById(R.id.error_icon);
        errorButton.setEnabled(false);
        errorButton.setVisibility(View.INVISIBLE);
        bufferingIcon = (ProgressBar) findViewById(R.id.loading_icon);
        speedControlButton = (Button) findViewById(R.id.speed_control_button);
        eventLog = (TextView)findViewById(R.id.event_log);
        eventLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        showLoadingIcon(false);
        showControls(false);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        initializePlayer();
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart called");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop called");
        disablePlayerUI();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "onSaveInstanceState called");
        super.onSaveInstanceState(outState);
        if (mOtp != null && mPlaybackInfo != null) {
            outState.putString("otp", mOtp);
            outState.putString("playbackInfo", mPlaybackInfo);
        }
    }

    private void initializePlayer() {
        if (mOtp != null && mPlaybackInfo != null) {
            // initialize the playerFragment; a VdoPlayer instance will be received
            // in onInitializationSuccess() callback
            playerFragment.initialize(OnlinePlayerActivity.this);
            log("initializing player fragment");
        } else {
            // lets get otp and playbackInfo before creating the player
            obtainOtpAndPlaybackInfo();
        }
    }

    /**
     * Fetch (otp + playbackInfo) and initialize VdoPlayer
     * here we're fetching a sample (otp + playbackInfo)
     * TODO you need to generate/fetch (otp + playbackInfo) OR (signature + playbackInfo) for the
     * video you wish to play
     */
    private void obtainOtpAndPlaybackInfo() {
        // todo use asynctask
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Pair<String, String> pair = Utils.getSampleOtpAndPlaybackInfo();
                    mOtp = pair.first;
                    mPlaybackInfo = pair.second;
                    Log.i(TAG, "obtained new otp and playbackInfo");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            initializePlayer();
                        }
                    });
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    showToast("Error fetching otp and playbackInfo: " + e.getClass().getSimpleName());
                    log("error fetching otp and playbackInfo");
                }
            }
        }).start();
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(OnlinePlayerActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void log(String msg) {
        eventLogString += (msg + "\n");
        eventLog.setText(eventLogString);
    }

    private View.OnClickListener playPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (player == null) return;
            if (playWhenReady) {
                player.setPlayWhenReady(false);
            } else {
                player.setPlayWhenReady(true);
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
        Log.v(TAG, (show ? "show " : "hide ") + "controls");
        int visibility = show ? View.VISIBLE : View.INVISIBLE;
        playPauseButton.setVisibility(visibility);
        (findViewById(R.id.bottom_panel)).setVisibility(visibility);
        controlsShowing = show;
    }

    private void showSpeedControlDialog() {
        new AlertDialog.Builder(this)
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

    private void disablePlayerUI() {
        showControls(false);
        showLoadingIcon(false);
        playPauseButton.setEnabled(false);
        currTime.setEnabled(false);
        seekBar.setEnabled(false);
        replayButton.setEnabled(false);
        replayButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onInitializationSuccess(VdoPlayer.PlayerHost playerHost, VdoPlayer player, boolean wasRestored) {
        Log.i(TAG, "onInitializationSuccess");
        log("onInitializationSuccess");
        this.player = player;
        player.addPlaybackEventListener(playbackListener);
        showLoadingIcon(false);

        (findViewById(R.id.player_region)).setOnClickListener(playerTapListener);
        (findViewById(R.id.fullscreen_toggle_button)).setOnClickListener(fullscreenToggleListener);
        showControls(true);

        // load a media to the player
        VdoPlayer.VdoInitParams vdoParams = VdoPlayer.VdoInitParams.createParamsWithOtp(mOtp, mPlaybackInfo);
        player.load(vdoParams);
        log("loaded init params to player");
    }

    @Override
    public void onInitializationFailure(VdoPlayer.PlayerHost playerHost, ErrorDescription errorDescription) {
        String msg = "onInitializationFailure: errorCode = " + errorDescription.errorCode + ": " + errorDescription.errorMsg;
        log(msg);
        Log.e(TAG, msg);
        Toast.makeText(OnlinePlayerActivity.this, "initialization failure: " + errorDescription.errorMsg, Toast.LENGTH_LONG).show();
        showLoadingIcon(false);
        errorButton.setVisibility(View.VISIBLE);
    }

    private VdoPlayer.PlaybackEventListener playbackListener = new VdoPlayer.PlaybackEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            log(Utils.playbackStateString(playWhenReady, playbackState));
            OnlinePlayerActivity.this.playWhenReady = playWhenReady;
            if (playWhenReady) {
                playPauseButton.setImageResource(R.drawable.ic_pause_white_48dp);
            } else {
                playPauseButton.setImageResource(R.drawable.ic_play_arrow_white_48dp);
            }
            switch (playbackState) {
                case VdoPlayer.STATE_READY: {
                    showLoadingIcon(false);
                    break;
                }
                case VdoPlayer.STATE_BUFFERING: {
                    showLoadingIcon(true);
                    break;
                }
                case VdoPlayer.STATE_ENDED: {
                    playPauseButton.setEnabled(false);
                    playPauseButton.setVisibility(View.INVISIBLE);
                    showLoadingIcon(false);
                    replayButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            replayButton.setVisibility(View.INVISIBLE);
                            if (OnlinePlayerActivity.this.player != null) {
                                OnlinePlayerActivity.this.player.seekTo(0);
                                playPauseButton.setEnabled(true);
                            }
                        }
                    });
                    replayButton.setEnabled(true);
                    replayButton.setVisibility(View.VISIBLE);
                    break;
                }
                default:
                    showLoadingIcon(false);
                    break;
            }
        }

        @Override
        public void onTracksChanged(Track[] tracks, Track[] tracks1) {
            Log.i(TAG, "onTracksChanged");
            log("onTracksChanged");
        }

        @Override
        public void onBufferUpdate(long bufferTime) {
            seekBar.setSecondaryProgress((int)bufferTime);
        }

        @Override
        public void onSeekTo(long millis) {
            Log.i(TAG, "onSeekTo: " + String.valueOf(millis));
        }

        @Override
        public void onProgress(long millis) {
            seekBar.setProgress((int)millis);
            currTime.setText(Utils.digitalClockTime((int)millis));
        }

        @Override
        public void onPlaybackSpeedChanged(float speed) {
            Log.i(TAG, "onPlaybackSpeedChanged " + speed);
            log("onPlaybackSpeedChanged " + speed);
            chosenSpeedIndex = Utils.getClosestFloatIndex(allowedSpeedList, speed);
            ((TextView)(findViewById(R.id.speed_control_button))).setText(allowedSpeedStrList[chosenSpeedIndex]);
        }

        @Override
        public void onLoading(VdoPlayer.VdoInitParams vdoInitParams) {
            Log.i(TAG, "onLoading");
            log("onLoading");
            showLoadingIcon(true);
        }

        @Override
        public void onLoadError(VdoPlayer.VdoInitParams vdoInitParams, ErrorDescription errorDescription) {
            Log.i(TAG, "onLoadError");
            log("onLoadError");
            showLoadingIcon(false);
        }

        @Override
        public void onLoaded(VdoPlayer.VdoInitParams vdoInitParams) {
            Log.i(TAG, "onLoaded");
            log("onLoaded");
            showLoadingIcon(false);
            duration.setText(Utils.digitalClockTime((int)player.getDuration()));
            seekBar.setMax((int)player.getDuration());
            seekBar.setEnabled(true);
            seekBar.setOnSeekBarChangeListener(seekbarChangeListener);
            playPauseButton.setEnabled(true);
            playPauseButton.setOnClickListener(playPauseListener);
            speedControlButton.setOnClickListener(speedButtonListener);
        }

        @Override
        public void onError(VdoPlayer.VdoInitParams vdoParams, ErrorDescription errorDescription) {
            String err = "onError code " + errorDescription.errorCode + ": " + errorDescription.errorMsg;
            Log.e(TAG, err);
            log(err);
            showLoadingIcon(false);
        }

        @Override
        public void onMediaEnded(VdoPlayer.VdoInitParams vdoInitParams) {
            Log.i(TAG, "onMediaEnded");
            log("onMediaEnded");
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

    private View.OnClickListener speedButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showSpeedControlDialog();
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
                (findViewById(R.id.log_container)).setVisibility(View.GONE);
                (findViewById(R.id.online_vdo_player_fragment)).setLayoutParams(new RelativeLayout.LayoutParams(
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
                (findViewById(R.id.log_container)).setVisibility(View.VISIBLE);
                (findViewById(R.id.online_vdo_player_fragment)).setLayoutParams(new RelativeLayout.LayoutParams(
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
