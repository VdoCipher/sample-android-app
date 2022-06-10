package com.vdocipher.sampleapp;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
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
import com.vdocipher.aegis.player.PlayerHost;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoInitParams;
import com.vdocipher.aegis.player.VdoPlayerSupportFragment;

import org.json.JSONException;

import java.io.IOException;

public class PlayerActivity extends AppCompatActivity implements PlayerHost.InitializationListener {

    private static final String TAG = "PlayerActivity";
    public static final String EXTRA_VDO_PARAMS = "vdo_params";

    private VdoPlayerSupportFragment playerFragment;
    private VdoPlayerControlView playerControlView;
    private TextView eventLog;

    private String eventLogString = "";
    private int currentOrientation;
    private VdoInitParams vdoParams;
    private AudioManager audioManager;

    private MediaSessionCompat mSession;

    public static final long MEDIA_ACTIONS_PLAY_PAUSE =
            PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_PAUSE;

    public static final long MEDIA_ACTIONS_ALL =
            MEDIA_ACTIONS_PLAY_PAUSE;
                    /*| PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;*/

    private VdoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate called");
        setContentView(R.layout.activity_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(uiVisibilityListener);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (savedInstanceState != null) {
            vdoParams = savedInstanceState.getParcelable("initParams");
        }

        if (vdoParams == null) {
            vdoParams = getIntent().getParcelableExtra(EXTRA_VDO_PARAMS);
        }

        playerFragment = (VdoPlayerSupportFragment)getSupportFragmentManager().findFragmentById(R.id.vdo_player_fragment);
        playerControlView = findViewById(R.id.player_control_view);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        ((TextView)findViewById(R.id.library_version))
                .setText(String.format("sdk version: %s", com.vdocipher.aegis.BuildConfig.VDO_VERSION_NAME));

        eventLog = findViewById(R.id.event_log);
        eventLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        showControls(false);

        currentOrientation = getResources().getConfiguration().orientation;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        initializePlayer();
    }

    private boolean requestAudioFocus(AudioManager audioManager) {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // when switched out of PiP mode, handle the new video, stopping any existing video playback if needed.
    }

    private void initializeMediaSession() {
        mSession = new MediaSessionCompat(this, TAG);
        mSession.setActive(true);
        MediaControllerCompat.setMediaController(this, mSession.getController());

        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Sample playback")
                .build();
        mSession.setMetadata(metadata);

        MediaSessionCallback mMediaSessionCallback = new MediaSessionCallback(player);
        mSession.setCallback(mMediaSessionCallback);

        boolean playing = player != null && player.getPlaybackState() != VdoPlayer.STATE_IDLE && player.getPlaybackState() != VdoPlayer.STATE_ENDED && player.getPlayWhenReady();

        int state = playing ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        if (player != null) {
            updatePlaybackState(state, MEDIA_ACTIONS_ALL, player.getCurrentTime(), player.getPlaybackSpeed());
        }
    }

    /**
     * Overloaded method that persists previously set media actions.
     *
     * @param state    The state of the video, e.g. playing, paused, etc.
     * @param position The position of playback in the video.
     */
    private void updatePlaybackState(
            @PlaybackStateCompat.State int state, long position, float playBackSpeed) {
        long actions = mSession.getController().getPlaybackState().getActions();
        updatePlaybackState(state, actions, position, playBackSpeed);
    }

    private void updatePlaybackState(
            @PlaybackStateCompat.State int state, long playbackActions, long position, float playBackSpeed) {
        PlaybackStateCompat.Builder builder =
                new PlaybackStateCompat.Builder()
                        .setActions(playbackActions)
                        .setState(state, position, playBackSpeed);
        mSession.setPlaybackState(builder.build());
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        //switch to PiP mode if the user presses the home or recent button,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            enterPictureInPictureMode();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
        showControls(isInPictureInPictureMode);
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
        if (vdoParams != null) {
            outState.putParcelable("initParams", vdoParams);
        }
    }

    private void initializePlayer() {
        if (vdoParams != null) {
            // initialize the playerFragment; a VdoPlayer instance will be received
            // in onInitializationSuccess() callback
            playerFragment.initialize(PlayerActivity.this);
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
        Pair<String, String> pair = Utils.getSampleOtpAndPlaybackInfo();
        VdoInitParams vdoParams = new VdoInitParams.Builder()
                .setOtp(pair.first)
                .setPlaybackInfo(pair.second)
                .setPreferredCaptionsLanguage("en")
                .build();
        Log.i(TAG, "obtained new otp and playbackInfo");
        return vdoParams;
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(PlayerActivity.this, message, Toast.LENGTH_SHORT).show());
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
    public void onInitializationSuccess(PlayerHost playerHost, VdoPlayer player, boolean wasRestored) {
        Log.i(TAG, "onInitializationSuccess");
        log("onInitializationSuccess");
        this.player = player;
        player.addPlaybackEventListener(playbackListener);
        playerControlView.setPlayer(player);
        showControls(true);
        initializeMediaSession();
        playerControlView.setFullscreenActionListener(fullscreenToggleListener);
        playerControlView.setControllerVisibilityListener(visibilityListener);
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
        Toast.makeText(PlayerActivity.this, "initialization failure: " + errorDescription.errorMsg, Toast.LENGTH_LONG).show();
    }

    private final VdoPlayer.PlaybackEventListener playbackListener = new VdoPlayer.PlaybackEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            log(Utils.playbackStateString(playWhenReady, playbackState));

            boolean playing = playbackState != VdoPlayer.STATE_IDLE && playbackState != VdoPlayer.STATE_ENDED && playWhenReady;

            // We are playing the video now. Update the media session state and the PiP
            // window will update the actions.

            updatePlaybackState(playing ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, player.getCurrentTime(), player.getPlaybackSpeed());

            if (playing) {
                //Request audio focus and notify other players to stop playback.
                if (!requestAudioFocus(audioManager)) {
                    Log.i(TAG, "Audio focus not granted");
                }
            } else if (playbackState == VdoPlayer.STATE_ENDED) {
                // Abandon audio focus when playback complete
                audioManager.abandonAudioFocus(audioFocusChangeListener);
            }
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
            Log.i(TAG, "onSeekTo: " + millis);
        }

        @Override
        public void onProgress(long millis) {}

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
            playerControlView.verifyAndUpdateCaptionsButton();
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

    private final VdoPlayerControlView.FullscreenActionListener fullscreenToggleListener = enterFullscreen -> {
        showFullScreen(enterFullscreen);
        return true;
    };

    private final VdoPlayerControlView.ControllerVisibilityListener visibilityListener = new VdoPlayerControlView.ControllerVisibilityListener() {
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

    private final VdoPlayerControlView.VdoParamsGenerator vdoParamsGenerator = () -> {
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
            (findViewById(R.id.vdo_player_fragment)).setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            playerControlView.setFitsSystemWindows(true);
            // hide system windows
            showSystemUi(false);
            showControls(false);
        } else {
            // show other views
            (findViewById(R.id.title_text)).setVisibility(View.VISIBLE);
            (findViewById(R.id.library_version)).setVisibility(View.VISIBLE);
            (findViewById(R.id.log_container)).setVisibility(View.VISIBLE);
            (findViewById(R.id.vdo_player_fragment)).setLayoutParams(new RelativeLayout.LayoutParams(
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

    private final View.OnSystemUiVisibilityChangeListener uiVisibilityListener = visibility -> {
        Log.v(TAG, "onSystemUiVisibilityChange");
        // show player controls when system ui is showing
        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
            Log.v(TAG, "system ui visible, making controls visible");
            showControls(true);
        }
    };

    private static class MediaSessionCallback extends MediaSessionCompat.Callback {

        private final VdoPlayer player;

        public MediaSessionCallback(VdoPlayer player) {
            this.player = player;
        }

        @Override
        public void onPlay() {
            super.onPlay();
            //resume playing
            player.setPlayWhenReady(true);
        }

        @Override
        public void onPause() {
            super.onPause();
            //pause playing
            player.setPlayWhenReady(false);
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            //Implement this according to your app needs
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            //Implement this according to your app needs
        }
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    //resume playing
                    if (player != null)
                        player.setPlayWhenReady(true);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    //depending on your app reduce audio to a minimum if you want to continue playing or pause
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    //pause playing
                    if (player != null)
                        player.setPlayWhenReady(false);
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                default:
                    //stop playing
                    break;
            }
        }
    };

}
