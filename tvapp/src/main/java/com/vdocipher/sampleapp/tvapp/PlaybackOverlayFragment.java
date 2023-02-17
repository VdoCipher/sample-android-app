package com.vdocipher.sampleapp.tvapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.leanback.app.PlaybackSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.ControlButtonPresenterSelector;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackControlsRowPresenter;

/**
 * Handles the UI for playback controls and state.
 * <p>
 * The UI is updated by calls from the host activity when it receives appropriate callbacks from
 * the VdoPlayer.
 */

public class PlaybackOverlayFragment extends PlaybackSupportFragment {
    private static final String TAG = PlaybackOverlayFragment.class.getSimpleName();

    private PlaybackControlsRow mPlaybackControlsRow;

    private PlaybackControlsRow.PlayPauseAction mPlayPauseAction;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;

    private ArrayObjectAdapter mRowsAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setBackgroundType(PlaybackOverlayFragment.BG_LIGHT);
        setControlsOverlayAutoHideEnabled(true);

        setUpRows();
    }

    /**
     * called when state change callback is received from VdoPlayer
     *
     * @param playWhenReady true if currently playing else false.
     * @param state         current state of playback. One of the {@link com.vdocipher.aegis.player.VdoPlayer#STATE_IDLE},
     *                      {@link com.vdocipher.aegis.player.VdoPlayer#STATE_BUFFERING},
     *                      {@link com.vdocipher.aegis.player.VdoPlayer#STATE_READY}, {@link com.vdocipher.aegis.player.VdoPlayer#STATE_ENDED}
     */
    public void playbackStateChanged(boolean playWhenReady, int state) {
        if (playWhenReady) {
            setControlsOverlayAutoHideEnabled(true);
            mPlayPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE);
            mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE));
        } else {
            mPlayPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PLAY);
            mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlaybackControlsRow.PlayPauseAction.INDEX_PLAY));
        }
        notifyPlaybackRowChanged();
    }

    public void playbackPositionChanged(long positionMs) {
        mPlaybackControlsRow.setCurrentPosition(positionMs);
    }

    public void playbackDurationChanged(long durationMs) {
        mPlaybackControlsRow.setDuration(durationMs);
    }

    private void setUpRows() {
        ClassPresenterSelector ps = new ClassPresenterSelector();

        PlaybackControlsRowPresenter playbackControlsRowPresenter =
                new PlaybackControlsRowPresenter();

        ps.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);

        // add as first Row of mRowsAdapter
        addPlaybackControlsRow();

        // set action click listener
        playbackControlsRowPresenter.setOnActionClickedListener(action -> {
            if (action.getId() == mPlayPauseAction.getId()) {
                togglePlayback(mPlayPauseAction.getIndex() == PlaybackControlsRow.PlayPauseAction.INDEX_PLAY);
            } else if (action.getId() == mFastForwardAction.getId()) {
                fastForward();
            } else if (action.getId() == mRewindAction.getId()) {
                rewind();
            }
        });
        setAdapter(mRowsAdapter);
    }

    private void addPlaybackControlsRow() {
        mPlaybackControlsRow = new PlaybackControlsRow();
        mRowsAdapter.add(mPlaybackControlsRow);

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        ArrayObjectAdapter mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);

        Activity activity = requireActivity();
        mRewindAction = new PlaybackControlsRow.RewindAction(activity);
        mPlayPauseAction = new PlaybackControlsRow.PlayPauseAction(activity);
        mFastForwardAction = new PlaybackControlsRow.FastForwardAction(activity);

        // PrimaryAction setting
        mPrimaryActionsAdapter.add(mRewindAction);
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        mPrimaryActionsAdapter.add(mFastForwardAction);
    }

    private void togglePlayback(boolean playWhenReady) {
        ((TvPlayerActivity) requireActivity()).setPlayWhenReady(playWhenReady);
    }

    // fast forward will simply seek 5 seconds forward
    private void fastForward() {
        ((TvPlayerActivity) requireActivity()).fastForward();
    }

    // rewind will simply seek 5 seconds backward
    private void rewind() {
        ((TvPlayerActivity) requireActivity()).rewind();
    }
}
