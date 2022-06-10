package com.vdocipher.sampleapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.vdocipher.aegis.media.ErrorDescription;
import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.offline.DownloadOptions;
import com.vdocipher.aegis.offline.DownloadRequest;
import com.vdocipher.aegis.offline.DownloadSelections;
import com.vdocipher.aegis.offline.DownloadStatus;
import com.vdocipher.aegis.offline.OptionsDownloader;
import com.vdocipher.aegis.offline.VdoDownloadManager;
import com.vdocipher.aegis.player.VdoInitParams;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.vdocipher.sampleapp.Utils.getSizeString;

public class DownloadsActivity extends Activity implements VdoDownloadManager.EventListener {
    private static final String TAG = "DownloadsActivity";

    // some samples for download demo
    private static final String SAMPLE_NAME_1 = "VdoCipher product demo";
    private static final String MEDIA_ID_1 = "661f6861d521a24288d608923d2c73f9";
    private static final String PLAYBACK_INFO_1 = "eyJ2aWRlb0lkIjoiNjYxZjY4NjFkNTIxYTI0Mjg4ZDYwODkyM2QyYzczZjkifQ==";
    private static final String OTP_1 = "20160313versASE313WAGCdGbRSkojp0pMJpESFT9RVVrbGSnzwVOr2ANUxMrfZ5";

    public static final String SAMPLE_NAME_2 = "Home page video";
    public static final String MEDIA_ID_2 = "3f29b5434a5c615cda18b16a6232fd75";
    public static final String PLAYBACK_INFO_2 = "eyJ2aWRlb0lkIjoiM2YyOWI1NDM0YTVjNjE1Y2RhMThiMTZhNjIzMmZkNzUifQ==";
    public static final String OTP_2 = "20160313versASE313BlEe9YKEaDuju5J0XcX2Z03Hrvm5rzKScvuyojMSBZBxfZ";

    private static final String SAMPLE_NAME_3 = "Tears of steel";
    private static final String MEDIA_ID_3 = "015de7e97f434865aa155ca83b690f4b";
    private static final String PLAYBACK_INFO_3 = "eyJ2aWRlb0lkIjoiMDE1ZGU3ZTk3ZjQzNDg2NWFhMTU1Y2E4M2I2OTBmNGIifQ==";
    private static final String OTP_3 = "20160313versASE323jKE0G7M1XT3mORYiaD1qft79B9nufTVGgshAd6fJ5QLHpU";

    private Button download1, download2, download3;
    private AppCompatButton deleteAll, refreshList, resumeAll, pauseAll;
    private RecyclerView downloadsListView;

    // dataset which backs the adapter for downloads recyclerview
    private ArrayList<DownloadStatus> downloadStatusList;
    private DownloadsAdapter downloadsAdapter;

    private volatile VdoDownloadManager vdoDownloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        download1 = findViewById(R.id.download_btn_1);
        download2 = findViewById(R.id.download_btn_2);
        download3 = findViewById(R.id.download_btn_3);
        download1.setEnabled(false);
        download2.setEnabled(false);
        download3.setEnabled(false);
        downloadsListView = findViewById(R.id.downloads_list);

        deleteAll = findViewById(R.id.delete_all);
        resumeAll = findViewById(R.id.resume_all);
        pauseAll = findViewById(R.id.pause_all);
        deleteAll.setEnabled(false);
        refreshList = findViewById(R.id.refresh_list);

        downloadStatusList = new ArrayList<>();
        downloadsAdapter = new DownloadsAdapter(downloadStatusList);
        downloadsListView.setAdapter(downloadsAdapter);
        downloadsListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        refreshList.setOnClickListener(v -> {
            refreshDownloadsList();
        });

        deleteAll.setOnClickListener(v -> {
            deleteAllDownloads();
        });

        pauseAll.setOnClickListener(v -> {
            pauseAll();
        });

        resumeAll.setOnClickListener(v -> {
            resumeAll();
        });

        refreshDownloadsList();
    }

    private String[] getAllMediaIds() {
        ArrayList<String> mediaIdList = new ArrayList<>();
        for (DownloadStatus status : downloadStatusList) {
            mediaIdList.add(status.mediaInfo.mediaId);
        }
        return mediaIdList.toArray(new String[0]);
    }

    public void pauseAll() {
        if (!downloadStatusList.isEmpty()) {
            String[] mediaIds = getAllMediaIds();
            vdoDownloadManager.pauseAllDownloads(mediaIds);
        }
    }


    public void resumeAll() {
        if (!downloadStatusList.isEmpty()) {
            String[] mediaIds = getAllMediaIds();
            vdoDownloadManager.resumeAllDownloads(mediaIds);
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        maybeCreateManager();
        vdoDownloadManager.addEventListener(this);
    }

    @Override
    protected void onStop() {
        if (vdoDownloadManager != null) {
            vdoDownloadManager.removeEventListener(this);
        }
        startNotificationService();
        super.onStop();
    }

    // VdoDownloadManager.EventListener implementation

    @Override
    public void onQueued(String mediaId, DownloadStatus downloadStatus) {
        showToastAndLog("Download queued : " + mediaId, Toast.LENGTH_SHORT);
        addListItem(downloadStatus);
    }

    @Override
    public void onChanged(String mediaId, DownloadStatus downloadStatus) {
        Log.d(TAG, "Download status changed: " + mediaId + " " + downloadStatus.downloadPercent + "%");
        updateListItem(downloadStatus);
    }

    @Override
    public void onCompleted(String mediaId, DownloadStatus downloadStatus) {
        showToastAndLog("Download complete: " + mediaId, Toast.LENGTH_SHORT);
        updateListItem(downloadStatus);
    }

    @Override
    public void onFailed(String mediaId, DownloadStatus downloadStatus) {
        Log.e(TAG, mediaId + " download error: " + downloadStatus.reason + " " + downloadStatus.reasonDescription);
        Toast.makeText(this, " download error: " + downloadStatus.reason + " " + downloadStatus.reasonDescription,
                Toast.LENGTH_LONG).show();
        updateListItem(downloadStatus);
    }

    @Override
    public void onDeleted(String mediaId) {
        showToastAndLog("Deleted " + mediaId, Toast.LENGTH_SHORT);
        removeListItem(mediaId);
    }

    // Private

    private void startNotificationService() {
        startService(new Intent(this, DownloadNotificationService.class));
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void maybeCreateManager() {
        if (vdoDownloadManager == null) {
            String downloadLocation = getDownloadLocation();
            vdoDownloadManager = VdoDownloadManager.getInstance(this, downloadLocation);
            //Provide custom implementation if you want to customize notifications look and feel
            vdoDownloadManager.setDownloadNotificationHelper(CustomDownloadNotificationHelper.class);
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void refreshDownloadsList() {
        maybeCreateManager();
        vdoDownloadManager.query(new VdoDownloadManager.Query(), statusList -> {
            // enable sample download buttons for media not downloaded or queued
            if (!containsMediaId(statusList, MEDIA_ID_1))
                setDownloadListeners(download1, "sample 1", OTP_1, PLAYBACK_INFO_1);
            if (!containsMediaId(statusList, MEDIA_ID_2))
                setDownloadListeners(download2, "sample 2", OTP_2, PLAYBACK_INFO_2);
            if (!containsMediaId(statusList, MEDIA_ID_3))
                setDownloadListeners(download3, "sample 3", OTP_3, PLAYBACK_INFO_3);

            // notify recyclerview
            downloadStatusList.clear();
            downloadStatusList.addAll(statusList);
            updateDeleteAllButton();
            downloadsAdapter.notifyDataSetChanged();

            if (statusList.isEmpty()) {
                Log.w(TAG, "No query results found");
                Toast.makeText(DownloadsActivity.this, "No query results found", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.i(TAG, statusList.size() + " results found");

            StringBuilder builder = new StringBuilder();
            builder.append("query results:").append("\n");
            for (DownloadStatus status : statusList) {
                builder.append(statusString(status)).append(" : ")
                        .append(status.mediaInfo.mediaId).append(", ")
                        .append(status.mediaInfo.title).append("\n");
            }
            Log.i(TAG, builder.toString());
        });
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private void deleteAllDownloads() {
        if (!downloadStatusList.isEmpty()) {
            maybeCreateManager();
            ArrayList<String> mediaIdList = new ArrayList<>();
            for (DownloadStatus status : downloadStatusList) {
                mediaIdList.add(status.mediaInfo.mediaId);
            }
            String[] mediaIds = mediaIdList.toArray(new String[mediaIdList.size()]);
            vdoDownloadManager.remove(mediaIds);
        }
    }

    private boolean containsMediaId(List<DownloadStatus> statusList, String mediaId) {
        for (DownloadStatus status : statusList) {
            if (status.mediaInfo.mediaId.equals(mediaId)) return true;
        }
        return false;
    }

    private void setDownloadListeners(final Button downloadButton, final String mediaName,
                                      final String otp, final String playbackInfo) {
        runOnUiThread(() -> {
            downloadButton.setEnabled(true);
            downloadButton.setText(String.format("Download %s", mediaName));
            downloadButton.setOnClickListener(view -> getOptions(otp, playbackInfo));
        });
    }

    private void getOptions(final String otp, final String playbackInfo) {
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        new Handler(handlerThread.getLooper()).post(
                () -> new OptionsDownloader().downloadOptionsWithOtp(
                        otp, playbackInfo, new OptionsDownloader.Callback() {
                            @Override
                            public void onOptionsReceived(DownloadOptions options) {
                                Log.i(TAG, "onOptionsReceived");
                                showSelectionDialog(options, options.mediaInfo.duration);
                            }

                            @Override
                            public void onOptionsNotReceived(ErrorDescription errDesc) {
                                String errMsg = "onOptionsNotReceived : " + errDesc.toString();
                                Log.e(TAG, errMsg);
                                Toast.makeText(DownloadsActivity.this, errMsg, Toast.LENGTH_LONG).show();
                            }
        }));
    }

    public void showSelectionDialog(DownloadOptions downloadOptions, long durationMs) {
        new OptionSelector(downloadOptions, durationMs, optionsSelectedCallback, OptionSelector.OptionStyle.SHOW_HIGHEST_AND_LOWEST_QUALITY)
                .showSelectionDialog(this, "Download options");
    }

    private OptionSelector.OptionsSelectedCallback optionsSelectedCallback =
            new OptionSelector.OptionsSelectedCallback() {
                @Override
                public void onTracksSelected(DownloadOptions downloadOptions, int[] selectedTracks) {
                    Log.i(TAG, selectedTracks.length + " options selected: " + Arrays.toString(selectedTracks));
                    long durationMs = downloadOptions.mediaInfo.duration;
                    Log.i(TAG, "---- selected tracks ----");
                    for (int trackIndex : selectedTracks) {
                        Log.i(TAG, getDownloadItemName(downloadOptions.availableTracks[trackIndex], durationMs));
                    }
                    Log.i(TAG, "---- selected tracks ----");

                    // currently only (1 video + 1 audio) track supported
                    if (selectedTracks.length != 2) {
                        showToastAndLog("Invalid selection", Toast.LENGTH_LONG);
                        return;
                    }

                    downloadSelectedOptions(downloadOptions, selectedTracks);

                    // disable the corresponding download button
                    if (downloadOptions.mediaId.equals(MEDIA_ID_1)) download1.setEnabled(false);
                    if (downloadOptions.mediaId.equals(MEDIA_ID_2)) download2.setEnabled(false);
                    if (downloadOptions.mediaId.equals(MEDIA_ID_3)) download3.setEnabled(false);
                }
            };

    private String getDownloadLocation() {
        String downloadLocation = "";
        try {
            downloadLocation = getExternalFilesDir(null).getPath() + File.separator + "offlineVdos";
        } catch (NullPointerException npe) {
            Log.e(TAG, "external storage not available");
            Toast.makeText(this, "external storage not available", Toast.LENGTH_LONG).show();
            return null;
        }

        // ensure download directory is created
        File dlLocation = new File(downloadLocation);
        if (!(dlLocation.exists() && dlLocation.isDirectory())) {
            // directory not created yet; let's create it
            if (!dlLocation.mkdir()) {
                Log.e(TAG, "failed to create storage directory");
                Toast.makeText(this, "failed to create storage directory", Toast.LENGTH_LONG).show();
            }
        }

        return downloadLocation;
    }

    private void downloadSelectedOptions(DownloadOptions downloadOptions, int[] selectionIndices) {
        DownloadSelections selections = new DownloadSelections(downloadOptions, selectionIndices);

        // build a DownloadRequest
        DownloadRequest request = new DownloadRequest.Builder(selections).build();

        // enqueue request to VdoDownloadManager for download
        try {
            vdoDownloadManager.enqueueV2(request);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Log.e(TAG, "error enqueuing download request");
            Toast.makeText(this, "error enqueuing download request", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void showItemSelectedDialog(final DownloadStatus downloadStatus) {
        AlertDialog.Builder builder = new AlertDialog.Builder(DownloadsActivity.this);
        builder.setTitle(downloadStatus.mediaInfo.title)
                .setMessage("Status: " + statusString(downloadStatus).toUpperCase());

        if (downloadStatus.status == VdoDownloadManager.STATUS_COMPLETED) {
            builder.setPositiveButton("PLAY", (dialog, which) -> {
                startPlayback(downloadStatus);
                dialog.dismiss();
            });
        } else {
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        }
        builder.setNegativeButton("DELETE", (dialog, which) -> {
            vdoDownloadManager.remove(downloadStatus.mediaInfo.mediaId);
            dialog.dismiss();
        });

        builder.create().show();
    }

    private void updateListItem(DownloadStatus status) {
        // if media already in downloadStatusList, update it
        String mediaId = status.mediaInfo.mediaId;
        int position = -1;
        for (int i = 0; i < downloadStatusList.size(); i++) {
            if (downloadStatusList.get(i).mediaInfo.mediaId.equals(mediaId)) {
                position = i;
                break;
            }
        }
        if (position >= 0) {
            downloadStatusList.set(position, status);
            downloadsAdapter.notifyItemChanged(position);
        } else {
            Log.e(TAG, "item not found in adapter");
        }
        updateDeleteAllButton();
    }

    private void addListItem(DownloadStatus downloadStatus) {
        downloadStatusList.add(0, downloadStatus);
        updateDeleteAllButton();
        downloadsAdapter.notifyItemInserted(0);
    }

    private void removeListItem(DownloadStatus status) {
        // remove by comparing mediaId; status may change
        String mediaId = status.mediaInfo.mediaId;
        removeListItem(mediaId);
    }

    private void removeListItem(String mediaId) {
        int position = -1;
        for (int i = 0; i < downloadStatusList.size(); i++) {
            if (downloadStatusList.get(i).mediaInfo.mediaId.equals(mediaId)) {
                position = i;
                break;
            }
        }
        if (position >= 0) {
            downloadStatusList.remove(position);
            downloadsAdapter.notifyItemRemoved(position);
        }
        updateDeleteAllButton();
    }

    private void updateDeleteAllButton() {
        deleteAll.setEnabled(!downloadStatusList.isEmpty());
    }

    private void startPlayback(DownloadStatus downloadStatus) {
        if (downloadStatus.status != VdoDownloadManager.STATUS_COMPLETED) {
            showToastAndLog("Download not complete", Toast.LENGTH_SHORT);
            return;
        }
        Intent intent = new Intent(this, PlayerActivity.class);
        VdoInitParams vdoParams = VdoInitParams.createParamsForOffline(downloadStatus.mediaInfo.mediaId);
        intent.putExtra(PlayerActivity.EXTRA_VDO_PARAMS, vdoParams);
        startActivity(intent);
    }

    private void showToastAndLog(final String message, final int toastLength) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), message, toastLength).show());
        Log.i(TAG, message);
    }

    private static String getDownloadItemName(Track track, long durationMs) {
        String type = track.type == Track.TYPE_VIDEO ? "V" : track.type == Track.TYPE_AUDIO ? "A" : "?";
        return type + " " + (track.bitrate / 1024) + " kbps, " +
                getSizeString(track.bitrate, durationMs);
    }

    private static String statusString(DownloadStatus status) {
        switch (status.status) {
            case VdoDownloadManager.STATUS_COMPLETED:
                return "COMPLETED";
            case VdoDownloadManager.STATUS_DOWNLOADING:
                return "DOWNLOADING";
            case VdoDownloadManager.STATUS_FAILED:
                return "FAILED " + status.reason + " " + status.reasonDescription + "\n";
            case VdoDownloadManager.STATUS_NOT_FOUND:
                return "NOT FOUND";
            case VdoDownloadManager.STATUS_PAUSED:
                return "PAUSED";
            case VdoDownloadManager.STATUS_PENDING:
                return "PENDING";
            case VdoDownloadManager.STATUS_REMOVING:
                return "REMOVING";
            case VdoDownloadManager.STATUS_RESTARTING:
                return "RESTARTING";
            default:
                throw new IllegalArgumentException("invalid download status");
        }
    }

    private class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            public TextView title;
            public TextView status;
            public TextView downloadPercent;
            public AppCompatButton stop, resume, delete;

            public ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.vdo_title);
                status = itemView.findViewById(R.id.download_status);
                downloadPercent = itemView.findViewById(R.id.download_percentage);
                stop = itemView.findViewById(R.id.download_stop_btn);
                resume = itemView.findViewById(R.id.download_resume_btn);
                delete = itemView.findViewById(R.id.download_delete_btn);
                stop.setOnClickListener(this);
                resume.setOnClickListener(this);
                delete.setOnClickListener(this);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getBindingAdapterPosition();
                if (v.getId() == R.id.download_resume_btn) {
                    vdoDownloadManager.resumeDownload(downloadStatusList.get(position).mediaInfo.mediaId);
                } else if (v.getId() == R.id.download_stop_btn) {
                    vdoDownloadManager.stopDownload(downloadStatusList.get(position).mediaInfo.mediaId);
                } else if (v.getId() == R.id.download_delete_btn) {
                    vdoDownloadManager.remove(downloadStatusList.get(position).mediaInfo.mediaId);
                } else {
                    if (position != RecyclerView.NO_POSITION) {
                        DownloadStatus status = statusList.get(position);
                        showItemSelectedDialog(status);
                    }
                }
            }
        }

        private final List<DownloadStatus> statusList;

        public DownloadsAdapter(List<DownloadStatus> statusList) {
            this.statusList = statusList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View itemView = inflater.inflate(R.layout.sample_list_item, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DownloadStatus status = statusList.get(position);
            holder.title.setText(status.mediaInfo.title);
            holder.status.setText(DownloadsActivity.statusString(status).toUpperCase());
            holder.downloadPercent.setText(String.format("%s%%",status.downloadPercent));
            if (status.status == VdoDownloadManager.STATUS_DOWNLOADING) {
                holder.stop.setEnabled(true);
                holder.resume.setEnabled(false);
            } else {
                holder.stop.setEnabled(false);
                holder.resume.setEnabled(status.status == VdoDownloadManager.STATUS_PAUSED);
            }
        }

        @Override
        public int getItemCount() {
            return statusList.size();
        }
    }
}
