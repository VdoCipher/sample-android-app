package com.vdocipher.sampleapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.offline.DownloadOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.vdocipher.sampleapp.Utils.getSizeBytes;
import static com.vdocipher.sampleapp.Utils.getSizeString;
import static com.vdocipher.sampleapp.Utils.round;

/**
 * Shows a dialog for selecting from available track download options.
 */

public class OptionSelector implements DialogInterface.OnClickListener, View.OnClickListener {
    public interface OptionsSelectedCallback {
        void onTracksSelected(DownloadOptions downloadOptions, int[] selectedTracks);
    }

    private static final String TAG = "OptionSelector";

    enum OptionStyle {
        SHOW_INDIVIDUAL_TRACKS,
        SHOW_HIGHEST_AND_LOWEST_QUALITY
    }

    private final DownloadOptions downloadOptions;
    private final long durationMs;
    private final OptionStyle optionStyle;
    private final OptionsSelectedCallback selectedCallback;

    private List<Integer> selectedTracks = new ArrayList<>();

    public OptionSelector(DownloadOptions options, long durMs, OptionsSelectedCallback callback,
                          OptionStyle optionStyle) {
        downloadOptions = options;
        durationMs = durMs;
        selectedCallback = callback;
        this.optionStyle = optionStyle;
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
            CheckedTextView view = (CheckedTextView)v;
            if (optionStyle == OptionStyle.SHOW_INDIVIDUAL_TRACKS || view.getTag() instanceof Integer) {
                int trackIndex = (int)view.getTag();
                if (view.isChecked()) {
                    selectedTracks.remove(Integer.valueOf(trackIndex));
                    view.setChecked(false);
                } else {
                    if (!selectedTracks.contains(trackIndex)) selectedTracks.add(trackIndex);
                    view.setChecked(true);
                }
            } else {
                Pair<Integer, Integer> audVidTrackIndexPair = (Pair<Integer, Integer>)view.getTag();
                int audioTrackIndex = audVidTrackIndexPair.first;
                int videoTrackIndex = audVidTrackIndexPair.second;
                if (view.isChecked()) {
                    selectedTracks.remove(Integer.valueOf(audioTrackIndex));
                    selectedTracks.remove(Integer.valueOf(videoTrackIndex));
                    view.setChecked(false);
                } else {
                    selectedTracks.clear();
                    selectedTracks.add(audioTrackIndex);
                    selectedTracks.add(videoTrackIndex);
                    view.setChecked(true);
                }
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

        if (optionStyle == OptionStyle.SHOW_INDIVIDUAL_TRACKS) {
            buildIndividualTracksView(context, inflater, root, selectableItemBackgroundResourceId, tracks);
        } else {
            buildCombinedTrackView(context, inflater, root, selectableItemBackgroundResourceId, tracks);
        }

        return view;
    }

    private void buildIndividualTracksView(Context context, LayoutInflater inflater, ViewGroup root,
                                           int selectableItemBackgroundResourceId,Track[] tracks) {
        // video and audio type track views
        int[] vidTrackIndices = getTypeIndices(tracks, Track.TYPE_VIDEO);
        Log.i(TAG, vidTrackIndices.length + " video tracks at " + Arrays.toString(vidTrackIndices));
        addTypeTracksToView(inflater, root, selectableItemBackgroundResourceId, tracks, vidTrackIndices);
        root.addView(inflater.inflate(R.layout.list_divider, root, false));
        int[] audTrackIndices = getTypeIndices(tracks, Track.TYPE_AUDIO);
        if (audTrackIndices.length != 0) {
            Log.i(TAG, audTrackIndices.length + " audio tracks at " + Arrays.toString(audTrackIndices));
            addTypeTracksToView(inflater, root, selectableItemBackgroundResourceId, tracks, audTrackIndices);
        }
    }

    private void buildCombinedTrackView(Context context, LayoutInflater inflater, ViewGroup root,
                                        int selectableItemBackgroundResourceId,Track[] tracks) {
        int[] audTrackIndices = getTypeIndices(tracks, Track.TYPE_AUDIO);
        if (audTrackIndices.length == 0) {
            buildIndividualTracksView(context, inflater, root, selectableItemBackgroundResourceId, tracks);
            return;
        }
        // we'll show two options: highest quality (audio + video) and lowest quality (audio + video)

        int highestQualityVidTrackIndex = getHighestBitrateIndex(tracks, Track.TYPE_VIDEO);
        int lowestQualityVidTrackIndex = getLowestBitrateIndex(tracks, Track.TYPE_VIDEO);

        int highestQualityAudTrackIndex = getHighestBitrateIndex(tracks, Track.TYPE_AUDIO);
        int lowestQualityAudTrackIndex = getLowestBitrateIndex(tracks, Track.TYPE_AUDIO);

        // if there is only one video track and one audio track, show only one option
        boolean showSingleOption = highestQualityVidTrackIndex == lowestQualityVidTrackIndex
                && highestQualityAudTrackIndex == lowestQualityAudTrackIndex;
        if (showSingleOption) {
            Log.i(TAG, String.format(Locale.US, "High quality indices (%d, %d)", highestQualityAudTrackIndex, highestQualityVidTrackIndex));
            Pair<Integer, Integer> audVidIndexPair = Pair.create(highestQualityAudTrackIndex, highestQualityVidTrackIndex);
            addOptionToView("High", inflater, root, selectableItemBackgroundResourceId, tracks, audVidIndexPair);
            root.addView(inflater.inflate(R.layout.list_divider, root, false));
        } else {
            Log.i(TAG, String.format(Locale.US, "High quality indices (%d, %d)", highestQualityAudTrackIndex, highestQualityVidTrackIndex));
            Pair<Integer, Integer> highQualityAudVidIndexPair = Pair.create(highestQualityAudTrackIndex, highestQualityVidTrackIndex);
            addOptionToView("High", inflater, root, selectableItemBackgroundResourceId, tracks, highQualityAudVidIndexPair);
            root.addView(inflater.inflate(R.layout.list_divider, root, false));

            Log.i(TAG, String.format(Locale.US, "Low quality indices (%d, %d)", lowestQualityAudTrackIndex, lowestQualityVidTrackIndex));
            Pair<Integer, Integer> lowQualityAudVidIndexPair = Pair.create(lowestQualityAudTrackIndex, lowestQualityVidTrackIndex);
            addOptionToView("Low", inflater, root, selectableItemBackgroundResourceId, tracks, lowQualityAudVidIndexPair);
        }
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

    private void addOptionToView(String optionName, LayoutInflater inflater, ViewGroup root,
                                 int selectableItemBackgroundResourceId, Track[] allTracks,
                                 Pair<Integer, Integer> audioVideoTrackIndexPair) {
        String optionText = makeOptionText(optionName, audioVideoTrackIndexPair.first,
                audioVideoTrackIndexPair.second, allTracks, durationMs);
        int trackViewLayoutId = android.R.layout.simple_list_item_single_choice;
        CheckedTextView trackView = (CheckedTextView) inflater.inflate(
                trackViewLayoutId, root, false);
        trackView.setBackgroundResource(selectableItemBackgroundResourceId);
        trackView.setText(optionText);
        trackView.setFocusable(true);
        trackView.setChecked(false);
        trackView.setTag(audioVideoTrackIndexPair);
        trackView.setOnClickListener(this);
        root.addView(trackView);
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

    private static String makeOptionText(String optionName, int audioIndex, int videoIndex, Track[] tracks, long durationMs) {
        Track audioTrack = tracks[audioIndex];
        Track videoTrack = tracks[videoIndex];
        long videoSizeBytes = getSizeBytes(videoTrack.bitrate, durationMs);
        long audioSizeBytes = getSizeBytes(audioTrack.bitrate, durationMs);
        long totalSizeBytes = videoSizeBytes + audioSizeBytes;
        String totalSizeMB = round(totalSizeBytes / (1024 * 1024), 2) + " MB";

        return optionName + " (" + (tracks[videoIndex].bitrate / 1024) + " kbps), " + totalSizeMB;
    }

    /**
     * @return index of the track in provided array with highest bitrate, or -1 if none of type found
     */
    private static int getHighestBitrateIndex(Track[] tracks, int trackType) {
        int highestBitrateTrackIndex = -1;
        int highestBitrate = 0;
        for (int i = 0; i < tracks.length; i++) {
            Track track = tracks[i];
            if (track.type == trackType) {
                int bitrate = track.bitrate;
                if (bitrate >= highestBitrate) {
                    highestBitrate = bitrate;
                    highestBitrateTrackIndex = i;
                }
            }
        }
        return highestBitrateTrackIndex;
    }

    /**
     * @return index of the track in provided array with lowest bitrate, or -1 if none of type found
     */
    private static int getLowestBitrateIndex(Track[] tracks, int trackType) {
        int lowestBitrateTrackIndex = -1;
        int lowestBitrate = Integer.MAX_VALUE;
        for (int i = 0; i < tracks.length; i++) {
            Track track = tracks[i];
            if (track.type == trackType) {
                int bitrate = track.bitrate;
                if (bitrate <= lowestBitrate) {
                    lowestBitrate = bitrate;
                    lowestBitrateTrackIndex = i;
                }
            }
        }
        return lowestBitrateTrackIndex;
    }

    private static String getDownloadItemName(Track track, long durationMs) {
        String type = track.type == Track.TYPE_VIDEO ? "V" : track.type == Track.TYPE_AUDIO ? "A" : "?";
        return type + " " + (track.bitrate / 1024) + " kbps, " +
                getSizeString(track.bitrate, durationMs);
    }
}
