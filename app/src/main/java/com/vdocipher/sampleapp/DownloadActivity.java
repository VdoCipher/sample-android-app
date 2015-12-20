package com.vdocipher.sampleapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.vdocipher.aegis.player.VdoDownloader;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class DownloadActivity extends AppCompatActivity {

    private final String TAG = "DownloadActivity";

    private VdoDownloader downloader;
    private Button startDownload;
    private TextView progress;
    private ProgressBar progressBar;

    private AsyncHttpClient client = new AsyncHttpClient();
    private String otp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        startDownload = (Button)findViewById(R.id.download_start_button);
        progress = (TextView)findViewById(R.id.download_progress_view);
        progressBar = (ProgressBar)findViewById(R.id.download_progress_bar);

        startDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getOtpAndStartDownload();
            }
        });
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop called");
        if (downloader != null) {
            downloader.disconnect();
        }
        super.onStop();
    }

    private void getOtpAndStartDownload() {
        //TODO set videoId and clientSecretKey
        final String videoId = "********";
        final String OTP_URL = "https://api.vdocipher.com/v2/otp/?video=" + videoId;
        RequestParams params = new RequestParams();
        params.put("clientSecretKey", "********");

        client.post(OTP_URL, params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                try {
                    JSONObject jObject = new JSONObject(responseString);
                    otp = jObject.getString("otp");
                    Log.v(TAG, "otp: " + otp);
                    String localFolder = getExternalFilesDir(null).getPath();
                    // create InitParams
                    VdoDownloader.InitParams initParams = new VdoDownloader.InitParams(otp, localFolder);
                    //TODO set resource id for drawable to be used for showing download progress notification
                    initParams.setNotificationResId(R.drawable.abc_btn_rating_star_on_mtrl_alpha);
                    downloader = new VdoDownloader(DownloadActivity.this, initParams);
                    downloader.download(new VdoDownloader.OnDownloadInitializationListener() {
                        @Override
                        public void onDownloadInitializationSuccess() {
                            Log.v(TAG, "download initialize success");
                            downloader.setOnDownloadEventListener(mDownloadListener);
                            startDownload.setEnabled(false);
                        }

                        @Override
                        public void onDownloadInitializationFailure(String reason) {
                            Log.v(TAG, "download initialize failure, reason: " + reason);
                        }
                    });

                } catch (JSONException e) {
                    Log.v(TAG, Log.getStackTraceString(e));
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.v(TAG, "status code: " + responseString);
            }
        });
    }

    private VdoDownloader.OnDownloadEventListener mDownloadListener = new VdoDownloader.OnDownloadEventListener() {
        @Override
        public void onDownloadStart() {
            Log.v(TAG, "download start");
            Toast.makeText(DownloadActivity.this, "Download started", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDownloadProgress(final int percentage) {
            Log.v(TAG, "download progress: " + percentage);
            progress.setText(String.valueOf(percentage) + "%");
            progressBar.setProgress(percentage);
        }

        @Override
        public void onDownloadComplete() {
            Log.v(TAG, "download complete");
            Toast.makeText(DownloadActivity.this, "Download completed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDownloadError(String reason) {
            Log.v(TAG, "download error: " + reason);
        }
    };
}
