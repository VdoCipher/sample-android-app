package com.vdocipher.sampleapp;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerFragment;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class OnlinePlayerActivity extends AppCompatActivity implements VdoPlayer.OnInitializationListener {

    private final String TAG = "OnlinePlayerActivity";

    private VdoPlayer player;
    private VdoPlayerFragment playerFragment;
    private ImageButton playPauseButton, replayButton, errorButton;
    private TextView currTime, duration;
    private SeekBar seekBar;
    private ProgressBar bufferingIcon;

    private AsyncHttpClient client = new AsyncHttpClient();
    private boolean isPlaying = false;
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
        getSampleOtpAndStartPlayer(null);
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

    /**
     * Use provided otp or generate a new one if null, and initialize player.
     */
    private void getSampleOtpAndStartPlayer(String otp) {
        final String videoId = "********";
        final String OTP_URL = "https://api.vdocipher.com/v2/otp/?video=" + videoId;
        RequestParams params = new RequestParams();
        params.put("clientSecretKey", "********");

        if (otp == null) {
            client.post(OTP_URL, params, new TextHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    try {
                        JSONObject jObject = new JSONObject(responseString);
                        String newOtp = jObject.getString("otp");
                        Log.v(TAG, "new otp: " + newOtp);
                        // create vdoInitParams
                        VdoPlayer.VdoInitParams vdoParams1 = new VdoPlayer.VdoInitParams(newOtp, false, null, null);
                        // initialize vdoPlayerFragment with otp and a VdoPlayer.OnInitializationListener
                        playerFragment.initialize(vdoParams1, OnlinePlayerActivity.this);
                        showLoadingIcon(true);
                    } catch (JSONException e) {
                        Log.v(TAG, Log.getStackTraceString(e));
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.v(TAG, "status code: " + responseString);
                }
            });
        } else {
            // create vdoInitParams
            VdoPlayer.VdoInitParams vdoParams1 = new VdoPlayer.VdoInitParams(otp, false, null, null);
            // initialize vdoPlayerFragment with initParams and a VdoPlayer.OnInitializationListener
            playerFragment.initialize(vdoParams1, OnlinePlayerActivity.this);
            showLoadingIcon(true);
        }
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
    public void onInitializationSuccess(VdoPlayer player, boolean wasRestored) {
        Log.v(TAG, "onInitializationSuccess");
        this.player = player;
        player.setOnPlaybackEventListener(playbackListener);
        if (wasRestored) player.play();
        Log.v(TAG, "player duration = " + player.getDuration());
        duration.setText(Utils.digitalClockTime(player.getDuration()));
        seekBar.setMax(player.getDuration());
        seekBar.setEnabled(true);
        seekBar.setOnSeekBarChangeListener(seekbarChangeListener);
        playPauseButton.setOnClickListener(playPauseListener);
        playPauseButton.setEnabled(true);
        replayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replayButton.setVisibility(View.INVISIBLE);
                if (OnlinePlayerActivity.this.player != null) {
                    OnlinePlayerActivity.this.player.restart();
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
        Toast.makeText(OnlinePlayerActivity.this, "initialization failure: " + result.name(), Toast.LENGTH_LONG).show();
        showLoadingIcon(false);
        errorButton.setVisibility(View.VISIBLE);
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
            replayButton.setEnabled(true);
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
            seekBar.setSecondaryProgress(bufferTime);
        }

        @Override
        public void onSeekTo(int millis) {
            Log.v(TAG, "onSeekTo: " + String.valueOf(millis));
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
