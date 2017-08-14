package com.vdocipher.sampleapp;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OnlinePlayerActivity extends AppCompatActivity implements VdoPlayer.InitializationListener {

    private final String TAG = "OnlinePlayerActivity";

    private VdoPlayer player;
    private VdoPlayerFragment playerFragment;
    private ImageButton playPauseButton, replayButton, errorButton;
    private TextView currTime, duration;
    private SeekBar seekBar;
    private ProgressBar bufferingIcon;

    private boolean playWhenReady = false;
    private boolean controlsShowing = false;
    private boolean isLandscape = false;
    private int mLastSystemUiVis;

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
        showLoadingIcon(false);
        showControls(false);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //getSampleOtpAndStartPlayer(null);
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop called");
        super.onStop();
        disablePlayerUI();
        player = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

    private void disablePlayerUI() {
        showControls(false);
        showLoadingIcon(false);
        playPauseButton.setEnabled(false);
        currTime.setEnabled(false);
        seekBar.setEnabled(false);
        replayButton.setEnabled(false);
        replayButton.setVisibility(View.INVISIBLE);
        (findViewById(R.id.player_region)).setOnClickListener(null);
    }

    @Override
    public void onInitializationSuccess(VdoPlayer.PlayerHost playerHost, VdoPlayer player, boolean wasRestored) {
        Log.i(TAG, "onInitializationSuccess");
        this.player = player;
        player.addPlaybackEventListener(playbackListener);
        if (wasRestored) player.setPlayWhenReady(true);
        Log.v(TAG, "player duration = " + player.getDuration());
        duration.setText(Utils.digitalClockTime(player.getDuration()));
        seekBar.setMax((int)player.getDuration());
        seekBar.setEnabled(true);
        seekBar.setOnSeekBarChangeListener(seekbarChangeListener);
        playPauseButton.setOnClickListener(playPauseListener);
        playPauseButton.setEnabled(true);
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
        showLoadingIcon(false);

        (findViewById(R.id.player_region)).setOnClickListener(playerTapListener);
        (findViewById(R.id.fullscreen_toggle_button)).setOnClickListener(fullscreenToggleListener);
        showControls(true);
    }

    @Override
    public void onInitializationFailure(VdoPlayer.PlayerHost playerHost, int errorCode, String msg) {
        Log.e(TAG, "onInitializationFailure: errorCode = " + errorCode + ": " + msg);
        Toast.makeText(OnlinePlayerActivity.this, "initialization failure: " + msg, Toast.LENGTH_LONG).show();
        showLoadingIcon(false);
        errorButton.setVisibility(View.VISIBLE);
    }

    private VdoPlayer.PlaybackEventListener playbackListener = new VdoPlayer.PlaybackEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
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
                    replayButton.setEnabled(true);
                    replayButton.setVisibility(View.VISIBLE);
                    break;
                }
                default:
                    break;
            }
        }

        @Override
        public void onTracksChanged(Track[] tracks, Track[] tracks1) {
            Log.i(TAG, "onTracksChanged");
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
            currTime.setText(Utils.digitalClockTime(millis));
        }

        @Override
        public void onLoading(VdoPlayer.VdoInitParams vdoInitParams) {
            Log.i(TAG, "onLoading");
        }

        @Override
        public void onLoadError(VdoPlayer.VdoInitParams vdoInitParams, int errorCode, String msg) {
            Log.i(TAG, "onLoadError");
        }

        @Override
        public void onLoaded(VdoPlayer.VdoInitParams vdoInitParams) {
            Log.i(TAG, "onLoaded");
        }

        @Override
        public void onError(VdoPlayer.VdoInitParams vdoParams, int errorCode, String msg) {
            Log.e(TAG, "onError code " + errorCode + ": " + msg);
        }

        @Override
        public void onVideoEnded(VdoPlayer.VdoInitParams vdoInitParams) {
            Log.i(TAG, "onVideoEnded");
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

    private Pair<String, String> getSampleOtpAndPlaybackInfo() throws IOException, JSONException {
        final String SAMPLE_OTP_PLAYBACK_INFO_URL = "https://dev.vdocipher.com/api/site/homepage_video";

        URL url = new URL(SAMPLE_OTP_PLAYBACK_INFO_URL);
        final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();

        if (responseCode == 200) {
            InputStream is = connection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inLine;
            StringBuffer responseBuffer = new StringBuffer();

            while ((inLine = br.readLine()) != null) {
                responseBuffer.append(inLine);
            }
            br.close();

            String response = responseBuffer.toString();
            //new String(Base64.decode(respEncoded, Base64.DEFAULT));
            Log.i(TAG, "response: " + response);

            JSONObject jObj = new JSONObject(response);
            String otp = jObj.getString("otp");
            String playbackInfo = jObj.getString("playbackInfo");
            return Pair.create(otp, playbackInfo);
        } else {
            Log.e(TAG, "error response code = " + responseCode);
            throw new IOException("Network error, code " + responseCode);
        }
    }

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
