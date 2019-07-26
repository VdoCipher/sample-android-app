package com.vdocipher.sampleapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.vdocipher.aegis.offline.DownloadStatus;
import com.vdocipher.aegis.offline.VdoDownloadManager;

import java.util.List;

public class DownloadNotificationService extends Service implements VdoDownloadManager.EventListener {

    private static final String TAG = "DownloadNotification";
    private static final String CHANNEL_ID = "downloads_notification_channel_name";
    private static final int DOWNLOAD_NOTIFICATION_ID = 100;

    private VdoDownloadManager vdoDownloadManager;
    private final Handler mainHandler;

    public DownloadNotificationService() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Runnable checkIfNeeded = new Runnable() {
        @Override
        public void run() {
            // check if there is at least one active download
            VdoDownloadManager.Query query = new VdoDownloadManager.Query().setFilterByStatus(
                    VdoDownloadManager.STATUS_PENDING, VdoDownloadManager.STATUS_DOWNLOADING);
            vdoDownloadManager.query(query, new VdoDownloadManager.QueryResultListener() {
                @Override
                public void onQueryResult(List<DownloadStatus> list) {
                    if (list.isEmpty()) {
                        // it's time to close
                        closeService();
                    } else {
                        // reschedule
                        mainHandler.postDelayed(checkIfNeeded, 10000);
                    }
                }
            });
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        if (Build.VERSION.SDK_INT < 21) {
            stopSelf();
        } else {
            createNotificationChannel();
            vdoDownloadManager = VdoDownloadManager.getInstance(this);
            vdoDownloadManager.addEventListener(this);
            startForeground(DOWNLOAD_NOTIFICATION_ID, getDownloadNotification("Downloads", "Tap to see downloads"));
            mainHandler.postDelayed(checkIfNeeded, 10000);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (vdoDownloadManager != null) {
            vdoDownloadManager.removeEventListener(this);
        }
        super.onDestroy();
    }

    private void closeService() {
        Log.d(TAG, "closeService");
        stopForeground(false);
        stopSelf();
    }

    private void updateNotification(DownloadStatus downloadStatus) {
        Notification notification = getDownloadNotification(makeTitle(downloadStatus), makeDescription(downloadStatus));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, notification);
    }

    private Notification getDownloadNotification(String title, String description) {
        Intent notificationIntent = new Intent(this, DownloadsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_file_download_black_18dp)
                .setContentTitle(title)
                .setContentText(description)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel only on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Download notifications";
            String description = "Download notifications";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createDownloadsIntent() {
        //
    }

    private String makeTitle(DownloadStatus downloadStatus) {
        final String title = downloadStatus.mediaInfo.title;
        switch (downloadStatus.status) {
            case VdoDownloadManager.STATUS_DOWNLOADING:
                return "Downloading " + title;
            case VdoDownloadManager.STATUS_COMPLETED:
                return "Downloaded " + title;
            case VdoDownloadManager.STATUS_FAILED:
                return "Error downloading " + title;
            default:
                return title;
        }
    }

    private String makeDescription(DownloadStatus downloadStatus) {
        switch (downloadStatus.status) {
            case VdoDownloadManager.STATUS_DOWNLOADING:
                return downloadStatus.downloadPercent + "%";
            case VdoDownloadManager.STATUS_COMPLETED:
                return "100%";
            case VdoDownloadManager.STATUS_FAILED:
                return "Error code " + downloadStatus.reason;
            default:
                return "";
        }
    }

    // VdoDownloadManager.EventListener implementation

    @Override
    public void onQueued(String mediaId, DownloadStatus downloadStatus) {
        Log.d(TAG, "Download queued : " + mediaId);
    }

    @Override
    public void onChanged(String mediaId, DownloadStatus downloadStatus) {
        Log.d(TAG, "Download status changed: " + mediaId + " " + downloadStatus.downloadPercent + "%");
        updateNotification(downloadStatus);
    }

    @Override
    public void onCompleted(String mediaId, DownloadStatus downloadStatus) {
        Log.d(TAG, "Download complete: " + mediaId);
        updateNotification(downloadStatus);
    }

    @Override
    public void onFailed(String mediaId, DownloadStatus downloadStatus) {
        Log.e(TAG, mediaId + " download error: " + downloadStatus.reason);
        updateNotification(downloadStatus);
    }

    @Override
    public void onDeleted(String mediaId) {
        Log.d(TAG, "Deleted " + mediaId);
    }
}
