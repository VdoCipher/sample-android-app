package com.vdocipher.sampleapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;

public class MainActivity extends AppCompatActivity implements ProviderInstaller.ProviderInstallListener {

    private static final String TAG = "MainActivity";
    private static final int ERROR_DIALOG_REQUEST_CODE = 1;

    private boolean retryProviderInstall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        Log.i(TAG, "version name = " + com.vdocipher.aegis.BuildConfig.VDO_VERSION_NAME);
        ((TextView)findViewById(R.id.library_version))
                .setText(String.format("VdoCipher sdk version: %s", com.vdocipher.aegis.BuildConfig.VDO_VERSION_NAME));

    }

    /**
     * On resume, check to see if we flagged that we need to reinstall the
     * provider.
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (retryProviderInstall) {
            // We can now safely retry installation.
            ProviderInstaller.installIfNeededAsync(this, this);
        }
        retryProviderInstall = false;
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop called");
        super.onStop();
    }

    /**
     * This method is only called if the provider is successfully updated
     * (or is already up-to-date).
     */
    @Override
    public void onProviderInstalled() {
        // Provider is up-to-date, app can make secure network calls.
        allowUserInteraction(true);
    }

    /**
     * This method is called if updating fails; the error code indicates
     * whether the error is recoverable.
     */
    @Override
    public void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        if (availability.isUserResolvableError(errorCode)) {
            // Recoverable error. Show a dialog prompting the user to
            // install/update/enable Google Play services.
            availability.showErrorDialogFragment(
                    this,
                    errorCode,
                    ERROR_DIALOG_REQUEST_CODE,
                    dialog -> {
                        // The user chose not to take the recovery action
                        onProviderInstallerNotAvailable();
                    });
        } else {
            // Google Play services is not available.
            onProviderInstallerNotAvailable();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
            // Adding a fragment via GoogleApiAvailability.showErrorDialogFragment
            // before the instance state is restored throws an error. So instead,
            // set a flag here, which will cause the fragment to delay until
            // onPostResume.
            retryProviderInstall = true;
        }
    }

    private void onProviderInstallerNotAvailable() {
        // This is reached if the provider cannot be updated for some reason.
        // App should consider all HTTP communication to be vulnerable, and take
        // appropriate action.
        final String msg = "Network security provider not updated. Playback may fail.";
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        allowUserInteraction(true);
    }

    private void allowUserInteraction(boolean allow) {
        findViewById(R.id.progress_modal).setVisibility(allow ? View.GONE : View.VISIBLE);
    }

    public void onlinePlayback(View v) {
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }

    public void onlinePlaybackWithoutWaterMark(View v) {
        Intent intent = new Intent(this, VdoPlayerUIActivity.class);
        intent.putExtra("watermark",false);
        startActivity(intent);
    }

    public void onlinePlaybackWithUI(View v) {
        Intent intent = new Intent(this, VdoPlayerUIActivity.class);
        startActivity(intent);
    }

    public void showDownloads(View v) {
        Intent intent = new Intent(this, DownloadsActivity.class);
        startActivity(intent);
    }
    public void openPlaylistVideo(View v) {
        Intent intent = new Intent(this, VdoPlaylistActivity.class);
        startActivity(intent);
    }

    public void openPlaylistAudio(View v) {
        Intent intent = new Intent(this, VdoPlaylistActivityAudio.class);
        startActivity(intent);
    }
}
