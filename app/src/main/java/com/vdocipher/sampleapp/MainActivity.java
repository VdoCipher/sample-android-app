package com.vdocipher.sampleapp;

import android.content.Intent;
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
import com.vdocipher.aegis.PlayerActivity;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity {

    public final static String TAG = "vdociphersampleapp";
    private final String OTP_URL = "https://api.vdocipher.com/v2/otp/?video=********";
    private final String OTP_EXTRA_INTENT = "com.vdocipher.aegis.otp";

    private Button playButton;
    private String otp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        playButton = (Button)findViewById(R.id.play_button);

        AsyncHttpClient client = new AsyncHttpClient();
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

                    playButton.setText("Play video");
                    playButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // start the video player
                            startPlayerActivity(otp);
                        }
                    });
                } catch (JSONException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e(TAG, responseString);
                playButton.setText("Network error. Please try again.");
            }
        });
    }

    private void startPlayerActivity(String otp) {
        if (otp == null) {
            return;
        }
        Intent intent = new Intent(this, PlayerActivity.class);
        Bundle extras = new Bundle();
        extras.putString(OTP_EXTRA_INTENT, otp);
        intent.putExtras(extras);
        startActivity(intent);
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
