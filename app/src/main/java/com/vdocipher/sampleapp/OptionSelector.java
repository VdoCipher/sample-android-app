package com.vdocipher.sampleapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.offline.DownloadOptions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.vdocipher.sampleapp.Utils.getSizeString;

/**
 * Shows a dialog for selecting from available track download options.
 */

public class OptionSelector implements DialogInterface.OnClickListener, View.OnClickListener {
    public interface OptionsSelectedCallback {
        void onTracksSelected(DownloadOptions downloadOptions, int[] selectedTracks);
    }

    private static final String TAG = "OptionSelector";

    private final DownloadOptions downloadOptions;
    private final long durationMs;
    private final OptionsSelectedCallback selectedCallback;

    private List<Integer> selectedTracks = new ArrayList<>();

    public OptionSelector(DownloadOptions options, long durMs, OptionsSelectedCallback callback) {
        downloadOptions = options;
        durationMs = durMs;
        selectedCallback = callback;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_NEGATIVE) {
            selectedTracks.clear();
        } else if (which == DialogInterface.BUTTON_POSITIVE) {
            int[] selectionIndices = new int[selectedTracks.size()];
            for (int i = 0; i < selectedTracks.size(); i++) selectionIndices[i] = selectedTracks.get(i);
            Arrays.sort(selectionIndices);
            selectedCallback.onTracksSelected(downloadOptions, selectionIndices);
        }
    }

    @Override
    public void onClick(View v) {
        if (v instanceof CheckedTextView) {
            CheckedTextView trackView = (CheckedTextView)v;
            int trackIndex = (int)trackView.getTag();
            if (trackView.isChecked()) {
                selectedTracks.remove(Integer.valueOf(trackIndex));
                trackView.setChecked(false);
            } else {
                if (!selectedTracks.contains(trackIndex)) selectedTracks.add(trackIndex);
                trackView.setChecked(true);
            }
        }
    }

    public void showSelectionDialog(Context context, CharSequence title) {
        selectedTracks.clear();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setView(buildView(context, downloadOptions.availableTracks))
                .setPositiveButton("Download", this)
                .setNegativeButton(android.R.string.cancel, this)
                .create()
                .show();
    }

    private View buildView(Context context, Track[] tracks) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.track_selection_dialog, null);
        ViewGroup root = (ViewGroup) view.findViewById(R.id.root);

        TypedArray attributeArray = context.getTheme().obtainStyledAttributes(
                new int[] {android.R.attr.selectableItemBackground});
        int selectableItemBackgroundResourceId = attributeArray.getResourceId(0, 0);
        attributeArray.recycle();

        // video and audio type track views
        int[] vidTrackIndices = getTypeIndices(tracks, Track.TYPE_VIDEO);
        Log.i(TAG, vidTrackIndices.length + " video tracks at " + Arrays.toString(vidTrackIndices));
        addTypeTracksToView(inflater, root, selectableItemBackgroundResourceId, tracks, vidTrackIndices);
        root.addView(inflater.inflate(R.layout.list_divider, root, false));
        int[] audTrackIndices = getTypeIndices(tracks, Track.TYPE_AUDIO);
        Log.i(TAG, audTrackIndices.length + " audio tracks at " + Arrays.toString(audTrackIndices));
        addTypeTracksToView(inflater, root, selectableItemBackgroundResourceId, tracks, audTrackIndices);

        return view;
    }

    private void addTypeTracksToView(LayoutInflater inflater, ViewGroup root, int selectableItemBackgroundResourceId,
                                     Track[] allTracks, int[] typeIndices) {
        for (int typeIndex : typeIndices) {
            Track track = allTracks[typeIndex];
            int trackViewLayoutId = android.R.layout.simple_list_item_single_choice;
            CheckedTextView trackView = (CheckedTextView) inflater.inflate(
                    trackViewLayoutId, root, false);
            trackView.setBackgroundResource(selectableItemBackgroundResourceId);
            trackView.setText(getDownloadItemName(track, durationMs));
            trackView.setFocusable(true);
            trackView.setChecked(false);
            trackView.setTag(typeIndex);
            trackView.setOnClickListener(this);
            root.addView(trackView);
        }
    }

    private static int[] getTypeIndices(Track[] tracks, int type) {
        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < tracks.length; i++) {
            if (type == tracks[i].type) {
                indexList.add(i);
            }
        }
        int[] indices = new int[indexList.size()];
        for (int x = 0; x < indexList.size(); x++) {
            indices[x] = indexList.get(x);
        }
        return indices;
    }

    private static String getDownloadItemName(Track track, long durationMs) {
        String type = track.type == Track.TYPE_VIDEO ? "V" : track.type == Track.TYPE_AUDIO ? "A" : "?";
        return type + " " + (track.bitrate / 1024) + " kbps, " +
                getSizeString(track.bitrate, durationMs);
    }
}
