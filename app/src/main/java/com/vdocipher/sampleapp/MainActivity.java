package com.vdocipher.sampleapp;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.player.VdoPlayerView;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity {

    private VdoPlayer player;
    private VdoPlayerView playerView;

	private AsyncHttpClient client = new AsyncHttpClient();
    private String otp = "";
    static final String TAG = "vdociphersampleapp";

    private Button playButton;


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
        params.put("clientSecretKey", "******");
        // receive otp
        client.post(OTP_URL, params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                try {
                    JSONObject jObject = new JSONObject(responseString);
                    otp = jObject.getString("otp");
                    Log.v(TAG, "otp: " + otp);
                    String localFolder = getExternalFilesDir(null).getPath();
                    VdoPlayer.VdoInitParams vdoParams = new VdoPlayer.VdoInitParams(otp, false, null, null);

					// Other constructors available :-
                    // Online playback	: VdoPlayer.VdoInitParams vdoParams = new VdoPlayer.VdoInitParams(otp, false, null, null);
                    // Offline play		: VdoPlayer.VdoInitParams vdoParams = new VdoPlayer.VdoInitParams(null, true, localFolder, "**********");
                    // Download setup	: VdoPlayer.VdoInitParams vdoParams = new VdoPlayer.VdoInitParams(otp, localFolder);

                    player = new VdoPlayer(getApplicationContext(), vdoParams, playerView);
					// Other uses :-
                    // Downloading player = new VdoPlayer(getApplicationContext(), vdoParams2, null);

                } catch (Exception e) {
                    Log.v(TAG, Log.getStackTraceString(e));
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.v(TAG, "status code: " + responseString);
                playButton.setText("Network error. Please try again.");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
