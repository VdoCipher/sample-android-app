package com.vdocipher.sampleapp;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import com.vdocipher.aegis.media.ErrorDescription;
import com.vdocipher.aegis.media.PlayerOption;
import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.player.PlayerHost;
import com.vdocipher.aegis.player.VdoInitParams;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.ui.view.VdoParamsGenerator;
import com.vdocipher.aegis.ui.view.VdoPlayerControlView;
import com.vdocipher.aegis.ui.view.VdoPlayerUIFragment;
import com.vdocipher.sampleapp.utils.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class VdoPlayerUIActivity extends AppCompatActivity implements PlayerHost.InitializationListener {

    private static final String TAG = "VdoPlayerUIActivity";
    public static final String EXTRA_VDO_PARAMS = "vdo_params";

    private VdoPlayerUIFragment vdoPlayerUIFragment;
    private VdoPlayerControlView playerControlView;
    private TextView eventLog;

    private String eventLogString = "";
    private int currentOrientation;
    private VdoInitParams vdoParams;

    private VdoPlayer player;
    private boolean isWaterMark = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate called");
        setContentView(R.layout.activity_vdo_player_ui);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (savedInstanceState != null) {
            vdoParams = savedInstanceState.getParcelable("initParams");
        }

        if (vdoParams == null) {
            vdoParams = getIntent().getParcelableExtra(EXTRA_VDO_PARAMS);
        }

        isWaterMark = getIntent().getBooleanExtra("watermark", true);

        vdoPlayerUIFragment = (VdoPlayerUIFragment) getSupportFragmentManager().findFragmentById(R.id.vdo_player_fragment);
        playerControlView = Objects.requireNonNull(vdoPlayerUIFragment).requireView().findViewById(R.id.vdo_player_control_view);

        ((TextView) findViewById(R.id.library_version))
                .setText(String.format("sdk version: %s", com.vdocipher.aegis.BuildConfig.VDO_VERSION_NAME));

        eventLog = findViewById(R.id.event_log);
        eventLog.setMovementMethod(ScrollingMovementMethod.getInstance());

        currentOrientation = getResources().getConfiguration().orientation;


        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.rg);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {

                try {
                    if (player == null)
                        return;

                    if (id == R.id.rbEnglish) {
                        player.addPlaybackEventListener(playbackListener);
                        playerControlView.setVdoParamsGenerator(vdoParamsGenerator);

                        // load a media to the player
                        player.load(obtainNewVdoParamsVideo(true, (int) player.getCurrentTime()));
                    } else {
                        player.addPlaybackEventListener(playbackListener);
                        playerControlView.setVdoParamsGenerator(vdoParamsGenerator);
                        // load a media to the player
                        player.load(obtainNewVdoParamsVideo(true, (int) player.getCurrentTime()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        initializePlayer();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // when switched out of PiP mode, handle the new video, stopping any existing video playback if needed.
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart called");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop called");
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "onSaveInstanceState called");
        super.onSaveInstanceState(outState);
        if (vdoParams != null) {
            outState.putParcelable("initParams", vdoParams);
        }
    }

    private void initializePlayer() {
        if (vdoParams != null) {
            // initialize the playerFragment; a VdoPlayer instance will be received
            // in onInitializationSuccess() callback
            vdoPlayerUIFragment.initialize(VdoPlayerUIActivity.this);
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
        new Thread(() -> {
            try {
                vdoParams = obtainNewVdoParams();
                runOnUiThread(this::initializePlayer);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showToast("Error fetching otp and playbackInfo: " + e.getClass().getSimpleName());
                    log("error fetching otp and playbackInfo");
                });
            }
        }).start();
    }

    @WorkerThread
    private VdoInitParams obtainNewVdoParams() throws IOException, JSONException {
        Pair<String, String> pair = Utils.getSampleMediaItem(isWaterMark);//Utils.getSampleOtpAndPlaybackInfo();
        HashMap<String, Object> map = new HashMap<>();
        map.put("skipDuration", 20);
        map.put("enableChromecast", true);
        map.put("enableSaveOffline", false);
        VdoInitParams vdoParams = new VdoInitParams.Builder()
                .setOtp(pair.first)
                .setPlaybackInfo(pair.second)
                .setPreferredCaptionsLanguage("en")
                .updateConfig(map)
                .enableAutoResume()
                .setForceHighestSupportedBitrate(true)
                .build();
        Log.i(TAG, "obtained new otp and playbackInfo");
        return vdoParams;
    }

    private VdoInitParams obtainNewVdoParamsVideo(boolean isEnglish, int startTime) {
        Pair<String, String> pair = isEnglish ? Utils.getSampleMediaItem(isWaterMark) : Utils.getSampleMediaItem2(isWaterMark);//Utils.getSampleOtpAndPlaybackInfo();
        HashMap<String, Object> map = new HashMap<>();
        map.put("skipDuration", 20);
        map.put("enableChromecast", true);
        map.put("enableSaveOffline", false);
        VdoInitParams vdoParams = new VdoInitParams.Builder()
                .setOtp(pair.first)
                .setPlaybackInfo(pair.second)
                .setPreferredCaptionsLanguage("en")
                .updateConfig(map)
                .setResumeTime(startTime)
                .enableAutoResume()
                .setForceHighestSupportedBitrate(true)
                .build();
        Log.i(TAG, "obtained new otp and playbackInfo");
        return vdoParams;
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(VdoPlayerUIActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void log(String msg) {
        eventLogString += (msg + "\n");
        eventLog.setText(eventLogString);
    }

    @Override
    public void onInitializationSuccess(PlayerHost playerHost, VdoPlayer player, boolean wasRestored) {
        Log.i(TAG, "onInitializationSuccess");
        log("onInitializationSuccess");
        this.player = player;
        player.addPlaybackEventListener(playbackListener);
        playerControlView.setVdoParamsGenerator(vdoParamsGenerator);

        // load a media to the player
        player.load(vdoParams);
        log("loaded init params to player");
    }

    @Override
    public void onInitializationFailure(PlayerHost playerHost, ErrorDescription errorDescription) {
        String msg = "onInitializationFailure: errorCode = " + errorDescription.errorCode + ": " + errorDescription.errorMsg;
        log(msg);
        Log.e(TAG, msg);
        Toast.makeText(VdoPlayerUIActivity.this, "initialization failure: " + errorDescription.errorMsg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDeInitializationSuccess() {

    }

    private final VdoPlayer.PlaybackEventListener playbackListener = new VdoPlayer.PlaybackEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            log(Utils.playbackStateString(playWhenReady, playbackState));
        }

        @Override
        public void onTracksChanged(Track[] tracks, Track[] tracks1) {
            Log.i(TAG, "onTracksChanged");
            log("onTracksChanged");
        }

        @Override
        public void onMetaDataLoaded(PlayerOption playerOption) {

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
            Log.i(TAG, "onPlaybackSpeedChanged " + speed);
            log("onPlaybackSpeedChanged " + speed);
        }

        @Override
        public void onLoading(VdoInitParams vdoInitParams) {
            Log.i(TAG, "onLoading");
            log("onLoading");
        }

        @Override
        public void onLoadError(VdoInitParams vdoInitParams, ErrorDescription errorDescription) {
            String err = "onLoadError code: " + errorDescription.errorCode + ": " + errorDescription.errorMsg;
            Log.e(TAG, err);
            log(err);
        }

        @Override
        public void onLoaded(VdoInitParams vdoInitParams) {
            Log.i(TAG, "onLoaded");
            log("onLoaded");
        }

        @Override
        public void onError(VdoInitParams vdoParams, ErrorDescription errorDescription) {
            String err = "onError code " + errorDescription.errorCode + ": " + errorDescription.errorMsg;
            Log.e(TAG, err);
            log(err);
        }

        @Override
        public void onMediaEnded(VdoInitParams vdoInitParams) {
            Log.i(TAG, "onMediaEnded");
            log("onMediaEnded");
        }
    };

    private final VdoParamsGenerator vdoParamsGenerator = () -> {
        try {
            return obtainNewVdoParams();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                showToast("Error generating new otp and playbackInfo: " + e.getClass().getSimpleName());
                log("Error generating new otp and playbackInfo");
            });
            return null;
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
            (findViewById(R.id.library_version)).setVisibility(View.GONE);
            (findViewById(R.id.log_container)).setVisibility(View.GONE);
        } else {
            // show other views
            (findViewById(R.id.title_text)).setVisibility(View.VISIBLE);
            (findViewById(R.id.library_version)).setVisibility(View.VISIBLE);
            (findViewById(R.id.log_container)).setVisibility(View.VISIBLE);
        }
    }

    private void showSystemUi(boolean show) {
        Log.v(TAG, (show ? "show" : "hide") + " system ui");
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

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Enter Picture-in-Picture mode if supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
        }
    }
}