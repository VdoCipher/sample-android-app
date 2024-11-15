package com.vdocipher.sampleapp.castdemo;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.vdocipher.aegis.cast.CastSessionAvailabilityListener;
import com.vdocipher.aegis.cast.CastVdoPlayer;
import com.vdocipher.aegis.media.ErrorDescription;
import com.vdocipher.aegis.media.PlayerOption;
import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.player.PlayerHost;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoInitParams;
import com.vdocipher.aegis.player.VdoTimeLine;
import com.vdocipher.aegis.ui.view.VdoPlayerUIFragment;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class CastVdoPlayerActivity extends AppCompatActivity
        implements PlayerHost.InitializationListener, CastSessionAvailabilityListener {

    private final String TAG = "CastVdoPlayerActivity";

    private VdoPlayerUIFragment playerFragment;
    private VdoPlayerControlView playerControlView;
    private TextView eventLog;
    private CharSequence eventLogString = "";

    private boolean playWhenReady = false;
    private long playPositionMs = 0;
    private int currentOrientation;

    private VdoInitParams initParams;

    private long initTimeMs = 0;

    private @Nullable VdoPlayer localPlayer;
    private @Nullable CastVdoPlayer castPlayer;
    private @Nullable VdoPlayer currentPlayer;

    private CastContext castContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate called");
        super.onCreate(savedInstanceState);

        castContext = CastContext.getSharedInstance(this);

        setContentView(R.layout.activity_cast_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(uiVisibilityListener);

        initParams = getIntent().getParcelableExtra("initParams");

        playerFragment = (VdoPlayerUIFragment)getSupportFragmentManager().findFragmentById(R.id.online_vdo_player_fragment);
        playerControlView = findViewById(R.id.player_control_view);
        eventLog = findViewById(R.id.event_log);
        eventLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        showControls(false);

        currentOrientation = getResources().getConfiguration().orientation;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        initializeLocalPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        castPlayer = new CastVdoPlayer(castContext);
        castPlayer.setSessionAvailabilityListenerWeakRef(this);
        setCurrentPlayer(castPlayer.isCastSessionAvailable() ? castPlayer : localPlayer);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (castPlayer != null) {
            castPlayer.setSessionAvailabilityListenerWeakRef(null);
            castPlayer.release();
        }
        castPlayer = null;
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

    // CastSessionAvailabilityListener impl

    @Override
    public void onCastSessionAvailable() {
        log("Cast session AVAILABLE");
        setCurrentPlayer(castPlayer);
    }

    @Override
    public void onCastSessionUnavailable() {
        log("Cast session LOST");
        setCurrentPlayer(localPlayer);
    }

    // VdoPlayer.InitializationListener impl

    @Override
    public void onInitializationSuccess(PlayerHost playerHost, VdoPlayer player, boolean wasRestored) {
        log("onInitializationSuccess");
        localPlayer = player;
        player.addPlaybackEventListener(localPlaybackListener);
        showControls(true);

        playerControlView.setFullscreenActionListener(fullscreenToggleListener);
        playerControlView.setControllerVisibilityListener(visibilityListener);

        if (castPlayer != null && !castPlayer.isCastSessionAvailable()) {
            setCurrentPlayer(player);
        }
    }

    @Override
    public void onInitializationFailure(PlayerHost playerHost, ErrorDescription errorDescription) {
        String msg = "onInitializationFailure: errorCode = " + errorDescription.errorCode + ": " + errorDescription.errorMsg;
        log(errorText(msg));
        Log.e(TAG, msg);
        Toast.makeText(CastVdoPlayerActivity.this, "initialization failure: " + errorDescription.errorMsg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDeInitializationSuccess() {

    }

    // Private

    private VdoPlayer.PlaybackEventListener localPlaybackListener = new VdoPlayer.PlaybackEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            log(Utils.playbackStateString(playWhenReady, playbackState));
            CastVdoPlayerActivity.this.playWhenReady = playWhenReady;
        }

        @Override
        public void onTracksChanged(Track[] tracks, Track[] tracks1) {
            log("onTracksChanged");
        }

        @Override
        public void onMetaDataLoaded(PlayerOption playerOption) {

        }

        @Override
        public void onTimelineChanged(VdoTimeLine vdoTimeLine, int i) {

        }

        @Override
        public void onBufferUpdate(long bufferTime) {}

        @Override
        public void onSeekTo(long millis) {
            Log.i(TAG, "onSeekTo: " + millis);
        }

        @Override
        public void onProgress(long millis) {}

        @Override
        public void onPlaybackSpeedChanged(float speed) {
            log("onPlaybackSpeedChanged " + speed);
        }

        @Override
        public void onLoading(VdoInitParams vdoInitParams) {
            log("onLoading");
        }

        @Override
        public void onLoadError(VdoInitParams vdoInitParams, ErrorDescription errorDescription) {
            String err = "onLoadError code: " + errorDescription.errorCode + ": " + errorDescription.errorMsg;
            Log.e(TAG, err);
            log(errorText(err));
        }

        @Override
        public void onLoaded(VdoInitParams vdoInitParams) {
            log("onLoaded");
        }

        @Override
        public void onError(VdoInitParams vdoParams, ErrorDescription errorDescription) {
            String err = "onError code " + errorDescription.errorCode + ": " + errorDescription.errorMsg;
            Log.e(TAG, err);
            log(errorText(err));
        }

        @Override
        public void onMediaEnded(VdoInitParams vdoInitParams) {
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

    private void setCurrentPlayer(@Nullable VdoPlayer newPlayer) {
        if (currentPlayer == newPlayer) {
            return;
        }

        if (currentPlayer != null) {
            playWhenReady = currentPlayer.getPlayWhenReady();
            playPositionMs = currentPlayer.getCurrentTime();

            initParams = initParams.getBuilder()
                    .setResumeTime((int)playPositionMs)
                    .setAutoplay(playWhenReady)
                    .build();
        }

        if (newPlayer == null) {
            // stop all playback
            if (currentPlayer != null) currentPlayer.stop();
        } else if (newPlayer == castPlayer) {
            // join/load cast player, stop local player
            // DO NOT stop previous instance of castPlayer by referencing currentPlayer
            if (localPlayer != null) localPlayer.stop();
        } else {
            // load local player, stop cast player
            if (castPlayer != null) castPlayer.stop();
        }

        playerControlView.setPlayer(newPlayer);
        currentPlayer = newPlayer;
        maybeLoadInitParams(newPlayer);
    }

    private void maybeLoadInitParams(@Nullable VdoPlayer player) {
        if (player != null && initParams != null) {
            if (player == castPlayer) {
                // join cast session, else load new params
                if (!castPlayer.joinSession(initParams)) {
                    castPlayer.load(initParams);
                    log("loaded init params to cast player");
                } else {
                    log("joined cast media session");
                }
            } else {
                player.load(initParams);
                log("loaded init params to local player");
            }
        }
    }

    private void initializeLocalPlayer() {
        if (initParams != null) {
            // initialize the playerFragment; a VdoPlayer instance will be received
            // in onInitializationSuccess() callback
            playerFragment.initialize(CastVdoPlayerActivity.this);
            log("initializing player fragment");
        } else {
            // get otp and playbackInfo before creating the player
            Toast.makeText(this, "No video parameters", Toast.LENGTH_SHORT).show();
        }
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CastVdoPlayerActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void log(CharSequence msg) {
        Log.i(TAG, msg.toString());

        if (initTimeMs == 0) initTimeMs = System.currentTimeMillis();

        double elapsedTime = (System.currentTimeMillis() - initTimeMs) / 1000.0;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        String timeStamp = "[" + df.format(elapsedTime) +"s]";

        SpannableString spannableTs = new SpannableString(timeStamp);
        ForegroundColorSpan foregroundSpan = new ForegroundColorSpan(Color.DKGRAY);
        spannableTs.setSpan(foregroundSpan, 0, spannableTs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        eventLogString = TextUtils.concat(eventLogString, msg, "  ", spannableTs, "\n");
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

    private static CharSequence errorText(String msg) {
        SpannableString spannable = new SpannableString(msg);
        ForegroundColorSpan foregroundSpan = new ForegroundColorSpan(Color.RED);
        spannable.setSpan(foregroundSpan, 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }
}
