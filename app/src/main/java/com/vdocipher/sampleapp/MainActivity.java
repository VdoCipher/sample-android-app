package com.vdocipher.sampleapp;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.vdocipher.aegis.player.VdoDownloader;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerView;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends ActionBarActivity implements VdoPlayer.OnInitializationListener {

    private VdoPlayer player;
    private VdoPlayerView playerView;

	private AsyncHttpClient client = new AsyncHttpClient();
    private String otp = "";
    static final String TAG = "vdociphersampleapp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        playerView = (VdoPlayerView)findViewById(R.id.vdo_player_view);

        getSampleOtpAndStartPlayer();
    }

    @Override
    protected void onStop() {
        if (player != null) {
            player.stop();
        }
        super.onStop();
    }

    private void getSampleOtpAndStartPlayer() {
    	final String OTP_EXTRA_INTENT = "com.vdocipher.aegis.otp";
    	final String OTP_URL = "https://api.vdocipher.com/v2/otp/?video=*****";
        RequestParams params = new RequestParams();
        // add client secret key to request params
        params.put("clientSecretKey", "********");
        // receive otp
        client.post(OTP_URL, params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                try {
                    JSONObject jObject = new JSONObject(responseString);
                    otp = jObject.getString("otp");
                    Log.v(TAG, "otp: " + otp);
                    String localFolder = getExternalFilesDir(null).getPath();

                    // Online playback
                    VdoPlayer.VdoInitParams vdoParams = new VdoPlayer.VdoInitParams(otp, false, null, null);

                    // Offline playback		: VdoPlayer.VdoInitParams vdoParams = new VdoPlayer.VdoInitParams(null, true, localFolder, videoId);

                    //player = new VdoPlayer(getApplicationContext(), vdoParams, playerView);

                    //initialize the VdoPlayer with a listener
                    //player.initialize(MainActivity.this);

                    // For Offline Download :
                    VdoDownloader.InitParams initParams = new VdoDownloader.InitParams(otp, localFolder);
                    VdoDownloader downloader = new VdoDownloader(MainActivity.this, initParams);
                    downloader.initialize(new VdoDownloader.OnInitializationListener() {
                        @Override
                        public void onInitializationSuccess() {
                            Log.v(TAG, "download initialize success");
                        }

                        @Override
                        public void onInitializationFailure(String reason) {
                            Log.v(TAG, "download initialize failure, reason: " + reason);
                        }
                    });

                } catch (Exception e) {
                    Log.v(TAG, Log.getStackTraceString(e));
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.v(TAG, "status code: " + responseString);
            }
        });
    }

    @Override
    public void onInitializationSuccess() {
        Log.v(TAG, "success");
        player.setOnPlaybackEventListener(playbackListener);
    }

    @Override
    public void onInitializationFailure(String reason) {
        Log.v(TAG, "initialization failure, reason: " + reason);
    }

    private VdoPlayer.OnPlaybackEventListener playbackListener = new VdoPlayer.OnPlaybackEventListener() {
        @Override
        public void onPlaying() {
            Log.v(TAG, "playing");
        }

        @Override
        public void onPaused() {
            Log.v(TAG, "paused");
        }

        @Override
        public void onStopped() {
            Log.v(TAG, "stopped");
        }

        @Override
        public void onBuffering(boolean isBuffering) {
            Log.v(TAG, isBuffering ? "buffering started" : "buffering stopped");
        }

        @Override
        public void onSeekTo(int millis) {
            Log.v(TAG, "seeked to " + String.valueOf(millis));
        }

        @Override
        public void onProgress(int millis) {
            Log.v(TAG, "current time: " + String.valueOf(millis));
        }
    };
}
