package com.vdocipher.sampleapp;

import android.app.Notification;
import android.content.Context;

import com.vdocipher.aegis.offline.DownloadStatus;
import com.vdocipher.aegis.offline.exoplayer.VdoDownloadNotificationHelper;

import java.util.List;

/**
 * Custom implementation for in-progress, completed and failed notifications.
 * This class will be instantiated by aegis.
 */
public class CustomDownloadNotificationHelper extends VdoDownloadNotificationHelper {

    /**
     * @param context   A context.
     * @param channelId The id of the notification channel to use.
     */
    public CustomDownloadNotificationHelper(Context context, String channelId) {
        //Specify custom channel id if needed
        super(context, channelId);
    }

    /**
     * Returns a progress notification for the given download.
     *
     * @param context          A context.
     * @param downloadStatuses The downloadStatuses.
     * @return The notification.
     */
    @Override
    public Notification buildProgressNotification(Context context, List<DownloadStatus> downloadStatuses) {
        //Make changes here to suit your app needs
        return super.buildProgressNotification(context, downloadStatuses);
    }

    /**
     * Returns a notification for a completed download.
     *
     * @param context        A context.
     * @param downloadStatus Download status information corresponding to a media download.
     * @return The notification.
     */
    @Override
    public Notification buildDownloadCompletedNotification(Context context, DownloadStatus downloadStatus) {
        //Make changes here to suit your app needs
        return super.buildDownloadCompletedNotification(context, downloadStatus);
    }

    /**
     * Returns a notification for a failed download.
     *
     * @param context        A context.
     * @param downloadStatus Download status information corresponding to a media download.
     * @return The notification.
     */
    @Override
    public Notification buildDownloadFailedNotification(Context context, DownloadStatus downloadStatus) {
        //Make changes here to suit your app needs
        return super.buildDownloadFailedNotification(context, downloadStatus);
    }
}
