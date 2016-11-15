package com.vdocipher.sampleapp;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.vdocipher.aegis.player.PlayerControllerWrapper;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerFragment;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class FullScreenDemo extends AppCompatActivity implements VdoPlayer.OnInitializationListener {

    private final String TAG = "FullscreenDemo";

    private VdoPlayer player;
    private VdoPlayerFragment playerFragment;
    private MediaController controller;
    private PlayerControllerWrapper wrapper;
    private ProgressBar loader;
    private LinearLayout others;

    private AsyncHttpClient client = new AsyncHttpClient();
    private String otp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate called");
        setContentView(R.layout.activity_full_screen_demo);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        playerFragment = (VdoPlayerFragment)getFragmentManager().findFragmentById(R.id.fullscreen_demo_fragment);

        loader = (ProgressBar)findViewById(R.id.loading_icon);
        others = (LinearLayout) findViewById(R.id.others);

        controller = new MediaController(this);

        if (savedInstanceState != null) {
            otp = savedInstanceState.getString("otp");
        }
        getSampleOtpAndStartPlayer();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        setLayout();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop called");
        if (player != null) {
            player.stop();
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (otp != null) outState.putString("otp", otp);
    }

    private void getSampleOtpAndStartPlayer() {
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
                        otp = jObject.getString("otp");
                        Log.v(TAG, "otp: " + otp);

                        VdoPlayer.VdoInitParams vdoParams1 = new VdoPlayer.VdoInitParams(otp, false, null, null);

                        playerFragment.initialize(vdoParams1, FullScreenDemo.this);
                    } catch (JSONException e) {
                        Log.v(TAG, Log.getStackTraceString(e));
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.v(TAG, "getting otp failed. status code: " + statusCode + " " + responseString);
                }
            });
        } else {
            VdoPlayer.VdoInitParams vdoParams1 = new VdoPlayer.VdoInitParams(otp, false, null, null);

            playerFragment.initialize(vdoParams1, FullScreenDemo.this);
        }
    }

    private void showLoadingIcon(final boolean showIcon) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (showIcon) {
                    loader.setVisibility(View.VISIBLE);
                    loader.bringToFront();
                } else {
                    loader.setVisibility(View.INVISIBLE);
                    loader.requestLayout();
                }
            }
        });
    }

    @Override
    public void onInitializationSuccess(VdoPlayer player, boolean wasRestored) {
        Log.v(TAG, "onInitializationSuccess");
        this.player = player;
        player.setOnPlaybackEventListener(playbackListener);
        Log.v(TAG, "player duration = " + player.getDuration());
        wrapper = new PlayerControllerWrapper(playerFragment);
        controller.setMediaPlayer(wrapper);
        playerFragment.setMediaController(controller);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    public void onInitializationFailure(VdoPlayer.InitializationResult result) {
        Log.v(TAG, "onInitializationFailure: " + result.name());
    }

    private VdoPlayer.OnPlaybackEventListener playbackListener = new VdoPlayer.OnPlaybackEventListener() {
        @Override
        public void onPlaying() {}

        @Override
        public void onPaused() {}

        @Override
        public void onStopped() {}

        @Override
        public void onBuffering(boolean isBuffering) {
            Log.v(TAG, isBuffering ? "buffering started" : "buffering stopped");
            showLoadingIcon(isBuffering);
        }

        @Override
        public void onSeekTo(int millis) {}

        @Override
        public void onProgress(int millis) {}

        @Override
        public void onBufferUpdate(int bufferTime) {}

        @Override
        public void onError(VdoPlayer.PlaybackErrorReason playbackErrorReason) {
            Log.e(TAG, playbackErrorReason.name());
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "config change callback");
        setLayout();
    }

    private void setLayout() {
        RelativeLayout.LayoutParams playerLp = (RelativeLayout.LayoutParams)playerFragment.getView().getLayoutParams();
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.v(TAG, "going to landscape");
            showFullScreen(true);
            playerLp.width = LinearLayout.LayoutParams.MATCH_PARENT;
            playerLp.height = LinearLayout.LayoutParams.MATCH_PARENT;
            others.setVisibility(View.GONE);
        } else {
            Log.v(TAG, "going back from landscape");
            showFullScreen(false);
            others.setVisibility(View.VISIBLE);
            playerLp.width = LinearLayout.LayoutParams.MATCH_PARENT;
            playerLp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        }
    }

    private void showFullScreen(boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (show) {
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
}
