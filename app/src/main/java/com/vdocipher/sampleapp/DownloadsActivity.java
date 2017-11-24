package com.vdocipher.sampleapp;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
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
    private static final String OTP_1 = "20160313versASE323ddb249fbcaf2a589401e0a5c06c601cbd4df6889b89898";

    private static final String SAMPLE_NAME_2 = "Tears of steel";
    private static final String MEDIA_ID_2 = "5392515b761ef71e8c00a2301e1cece3";
    private static final String PLAYBACK_INFO_2 = "eyJ2aWRlb0lkIjoiNTM5MjUxNWI3NjFlZjcxZThjMDBhMjMwMWUxY2VjZTMifQ==";
    private static final String OTP_2 = "20160313versASE3236ed272a9c42ff098324e4969c92f8da978ad54a586a881";

    private Button download1, download2;
    private RecyclerView downloadsListView;

    // dataset which backs the adapter for downloads recyclerview
    private ArrayList<DownloadStatus> downloadStatusList;
    private DownloadsAdapter downloadsAdapter;

    private volatile VdoDownloadManager vdoDownloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        download1 = (Button)findViewById(R.id.download_btn_1);
        download2 = (Button)findViewById(R.id.download_btn_2);
        download1.setEnabled(false);
        download2.setEnabled(false);
        downloadsListView = (RecyclerView)findViewById(R.id.downloads_list);

        downloadStatusList = new ArrayList<>();
        downloadsAdapter = new DownloadsAdapter(downloadStatusList);
        downloadsListView.setAdapter(downloadsAdapter);
        downloadsListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        findViewById(R.id.refresh_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshDownloadsList();
            }
        });

        if (Build.VERSION.SDK_INT >= 23) {
            ArrayList<String> needPermissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                needPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (!needPermissions.isEmpty()) {
                requestPermissions((needPermissions.toArray(new String[needPermissions.size()])), 0);
                return;
            }
        }
        refreshDownloadsList();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            refreshDownloadsList();
        } else {
            showToastAndLog("storage permission denied", Toast.LENGTH_LONG);
            finish();
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
        Log.e(TAG, mediaId + " download error: " + downloadStatus.reason);
        Toast.makeText(this, " download error: " + downloadStatus.reason,
                Toast.LENGTH_LONG).show();
        updateListItem(downloadStatus);
    }

    @Override
    public void onDeleted(String mediaId) {
        showToastAndLog("Deleted " + mediaId, Toast.LENGTH_SHORT);
        removeListItem(mediaId);
    }

    private void maybeCreateManager() {
        if (vdoDownloadManager == null) {
            vdoDownloadManager = VdoDownloadManager.getInstance(this);
        }
    }

    private void refreshDownloadsList() {
        maybeCreateManager();
        vdoDownloadManager.query(new VdoDownloadManager.Query(), new VdoDownloadManager.QueryResultListener() {
            @Override
            public void onQueryResult(List<DownloadStatus> statusList) {
                // enable sample download buttons for media not downloaded or queued
                if (!containsMediaId(statusList, MEDIA_ID_1))
                    setDownloadListeners(download1, "sample 1", OTP_1, PLAYBACK_INFO_1);
                if (!containsMediaId(statusList, MEDIA_ID_2))
                    setDownloadListeners(download2, "sample 2", OTP_2, PLAYBACK_INFO_2);

                // notify recyclerview
                downloadStatusList.clear();
                downloadStatusList.addAll(statusList);
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
            }
        });
    }

    private boolean containsMediaId(List<DownloadStatus> statusList, String mediaId) {
        for (DownloadStatus status : statusList) {
            if (status.mediaInfo.mediaId.equals(mediaId)) return true;
        }
        return false;
    }

    private void setDownloadListeners(final Button downloadButton, final String mediaName,
                                      final String otp, final String playbackInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                downloadButton.setEnabled(true);
                downloadButton.setText("Download " + mediaName);
                downloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getOptions(otp, playbackInfo);
                    }
                });
            }
        });
    }

    private void getOptions(final String otp, final String playbackInfo) {
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        new Handler(handlerThread.getLooper()).post(new Runnable() {
            @Override
            public void run() {
                new OptionsDownloader().downloadOptionsWithOtp(otp, playbackInfo, new OptionsDownloader.Callback() {
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
                });
            }
        });
    }

    public void showSelectionDialog(DownloadOptions downloadOptions, long durationMs) {
        new OptionSelector(downloadOptions, durationMs, optionsSelectedCallback)
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
                }
            };

    private void downloadSelectedOptions(DownloadOptions downloadOptions, int[] selectionIndices) {
        DownloadSelections selections = new DownloadSelections(downloadOptions, selectionIndices);
        String downloadLocation;
        try {
            downloadLocation = getExternalFilesDir(null).getPath() + File.separator + "offlineVdos";
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            Log.e(TAG, "external storage not available");
            Toast.makeText(this, "external storage not available", Toast.LENGTH_LONG).show();
            return;
        }
        Log.i(TAG, "will save media to " + downloadLocation);
        DownloadRequest request = new DownloadRequest.Builder(selections, downloadLocation).build();
        VdoDownloadManager vdoDownloadManager = VdoDownloadManager.getInstance(this);
        vdoDownloadManager.enqueue(request);
    }

    private void showItemSelectedDialog(final DownloadStatus downloadStatus) {
        AlertDialog.Builder builder = new AlertDialog.Builder(DownloadsActivity.this);
        builder.setTitle(downloadStatus.mediaInfo.title)
                .setMessage("Status: " + statusString(downloadStatus).toUpperCase());

        if (downloadStatus.status == VdoDownloadManager.STATUS_COMPLETED) {
            builder.setPositiveButton("PLAY", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startPlayback(downloadStatus);
                    dialog.dismiss();
                }
            });
        } else {
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        }
        builder.setNegativeButton("DELETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                maybeCreateManager();
                vdoDownloadManager.remove(downloadStatus.mediaInfo.mediaId);
                dialog.dismiss();
            }
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
    }

    private void addListItem(DownloadStatus downloadStatus) {
        downloadStatusList.add(0, downloadStatus);
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
    }

    private void startPlayback(DownloadStatus downloadStatus) {
        if (downloadStatus.status != VdoDownloadManager.STATUS_COMPLETED) {
            showToastAndLog("Download not complete", Toast.LENGTH_SHORT);
            return;
        }
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_MEDIA_ID, downloadStatus.mediaInfo.mediaId);
        startActivity(intent);
    }

    private void showToastAndLog(final String message, final int toastLength) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, toastLength).show();
            }
        });
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
                return "Completed";
            case VdoDownloadManager.STATUS_FAILED:
                return "Error " + status.reason;
            case VdoDownloadManager.STATUS_PENDING:
                return "Queued";
            case VdoDownloadManager.STATUS_PAUSED:
                return "Paused " + status.downloadPercent + "%";
            case VdoDownloadManager.STATUS_DOWNLOADING:
                return "Downloading " + status.downloadPercent + "%";
            default:
                return "Not found";
        }
    }

    private class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            public TextView title;
            public TextView status;

            public ViewHolder(View itemView) {
                super(itemView);
                title = (TextView)itemView.findViewById(R.id.vdo_title);
                status = (TextView)itemView.findViewById(R.id.download_status);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    DownloadStatus status = statusList.get(position);
                    showItemSelectedDialog(status);
                }
            }
        }

        private List<DownloadStatus> statusList;

        public DownloadsAdapter(List<DownloadStatus> statusList) {
            this.statusList = statusList;
        }

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
        }

        @Override
        public int getItemCount() {
            return statusList.size();
        }
    }
}
