package com.vdocipher.sampleapp;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
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
    private VdoPlayerControlView playerControlView;
    private TextView eventLog;
    private String eventLogString = "";

    private boolean playWhenReady = false;
    private int currentOrientation;

    private volatile String mOtp;
    private volatile String mPlaybackInfo;

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

        playerFragment = (VdoPlayerFragment)getFragmentManager().findFragmentById(R.id.online_vdo_player_fragment);
        playerControlView = (VdoPlayerControlView)findViewById(R.id.player_control_view);
        eventLog = (TextView)findViewById(R.id.event_log);
        eventLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        showControls(false);

        currentOrientation = getResources().getConfiguration().orientation;
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
        log("fetching params...");
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToast("Error fetching otp and playbackInfo: " + e.getClass().getSimpleName());
                            log("error fetching otp and playbackInfo");
                        }
                    });
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

    private void showControls(boolean show) {
        Log.v(TAG, (show ? "show" : "hide") + " controls");
        if (show) {
            playerControlView.show();
        } else {
            playerControlView.hide();
        }
    }

    private void disablePlayerUI() {
//        showControls(false);
    }

    @Override
    public void onInitializationSuccess(VdoPlayer.PlayerHost playerHost, VdoPlayer player, boolean wasRestored) {
        Log.i(TAG, "onInitializationSuccess");
        log("onInitializationSuccess");
        this.player = player;
        player.addPlaybackEventListener(playbackListener);
        playerControlView.setPlayer(player);
        showControls(true);

        playerControlView.setFullscreenActionListener(fullscreenToggleListener);
        playerControlView.setControllerVisibilityListener(visibilityListener);

        // load a media to the player
        VdoPlayer.VdoInitParams vdoParams = new VdoPlayer.VdoInitParams.Builder()
                .setOtp(mOtp)
                .setPlaybackInfo(mPlaybackInfo)
                .setPreferredCaptionsLanguage("en")
                .build();
        player.load(vdoParams);
        log("loaded init params to player");
    }

    @Override
    public void onInitializationFailure(VdoPlayer.PlayerHost playerHost, ErrorDescription errorDescription) {
        String msg = "onInitializationFailure: errorCode = " + errorDescription.errorCode + ": " + errorDescription.errorMsg;
        log(msg);
        Log.e(TAG, msg);
        Toast.makeText(OnlinePlayerActivity.this, "initialization failure: " + errorDescription.errorMsg, Toast.LENGTH_LONG).show();
    }

    private VdoPlayer.PlaybackEventListener playbackListener = new VdoPlayer.PlaybackEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            log(Utils.playbackStateString(playWhenReady, playbackState));
            OnlinePlayerActivity.this.playWhenReady = playWhenReady;
        }

        @Override
        public void onTracksChanged(Track[] tracks, Track[] tracks1) {
            Log.i(TAG, "onTracksChanged");
            log("onTracksChanged");
        }

        @Override
        public void onBufferUpdate(long bufferTime) {}

        @Override
        public void onSeekTo(long millis) {
            Log.i(TAG, "onSeekTo: " + String.valueOf(millis));
        }

        @Override
        public void onProgress(long millis) {}

        @Override
        public void onPlaybackSpeedChanged(float speed) {
            Log.i(TAG, "onPlaybackSpeedChanged " + speed);
            log("onPlaybackSpeedChanged " + speed);
        }

        @Override
        public void onLoading(VdoPlayer.VdoInitParams vdoInitParams) {
            Log.i(TAG, "onLoading");
            log("onLoading");
        }

        @Override
        public void onLoadError(VdoPlayer.VdoInitParams vdoInitParams, ErrorDescription errorDescription) {
            String err = "onLoadError code: " + errorDescription.errorCode;
            Log.e(TAG, err);
            log(err);
        }

        @Override
        public void onLoaded(VdoPlayer.VdoInitParams vdoInitParams) {
            Log.i(TAG, "onLoaded");
            log("onLoaded");
        }

        @Override
        public void onError(VdoPlayer.VdoInitParams vdoParams, ErrorDescription errorDescription) {
            String err = "onError code " + errorDescription.errorCode + ": " + errorDescription.errorMsg;
            Log.e(TAG, err);
            log(err);
        }

        @Override
        public void onMediaEnded(VdoPlayer.VdoInitParams vdoInitParams) {
            Log.i(TAG, "onMediaEnded");
            log("onMediaEnded");
        }
    };

    private VdoPlayerControlView.FullscreenActionListener fullscreenToggleListener = new VdoPlayerControlView.FullscreenActionListener() {
        @Override
        public boolean onFullscreenAction(boolean enterFullscreen) {
            showFullScreen(enterFullscreen);
            return true;
        }
    };

    private VdoPlayerControlView.ControllerVisibilityListener visibilityListener = new VdoPlayerControlView.ControllerVisibilityListener() {
        @Override
        public void onControllerVisibilityChange(int visibility) {
            Log.i(TAG, "controller visibility " + visibility);
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (visibility != View.VISIBLE) {
                    showSystemUi(false);
                }
            }
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        final int newOrientation = newConfig.orientation;
        final int oldOrientation = currentOrientation;
        currentOrientation = newOrientation;
        Log.i(TAG, "new orientation " +
                (newOrientation == Configuration.ORIENTATION_PORTRAIT ? "PORTRAIT" :
                        newOrientation == Configuration.ORIENTATION_LANDSCAPE ? "LANDSCAPE" : "UNKNOWN"));
        super.onConfigurationChanged(newConfig);
        if (newOrientation == oldOrientation) {
            Log.i(TAG, "orientation unchanged");
        } else if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            // hide other views
            (findViewById(R.id.title_text)).setVisibility(View.GONE);
            (findViewById(R.id.log_container)).setVisibility(View.GONE);
            (findViewById(R.id.online_vdo_player_fragment)).setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            playerControlView.setFitsSystemWindows(true);
            // hide system windows
            showSystemUi(false);
            showControls(false);
        } else {
            // show other views
            (findViewById(R.id.title_text)).setVisibility(View.VISIBLE);
            (findViewById(R.id.log_container)).setVisibility(View.VISIBLE);
            (findViewById(R.id.online_vdo_player_fragment)).setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            playerControlView.setFitsSystemWindows(false);
            playerControlView.setPadding(0,0,0,0);
            // show system windows
            showSystemUi(true);
        }
    }

    @Override
    public void onBackPressed() {
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            showFullScreen(false);
            playerControlView.setFullscreenState(false);
        } else {
            super.onBackPressed();
        }
    }

    private void showFullScreen(boolean show) {
        Log.v(TAG, (show ? "enter" : "exit") + " fullscreen");
        if (show) {
            // go to landscape orientation for fullscreen mode
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            // go to portrait orientation
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    private void showSystemUi(boolean show) {
        Log.v(TAG, (show ? "show" : "hide") + " system ui");
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
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                Log.v(TAG, "system ui visible, making controls visible");
                showControls(true);
            }
        }
    };
}
