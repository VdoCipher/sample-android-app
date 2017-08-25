package com.vdocipher.sampleapp.tv;

import android.app.Activity;
import android.os.Bundle;
import android.support.v17.leanback.app.PlaybackFragment;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.util.Log;

import com.vdocipher.aegis.media.MediaInfo;

/**
 * Handles the UI for playback controls and state.
 *
 * The UI is updated by calls from the host activity when it receives appropriate callbacks from
 * the VdoPlayer.
 */

public class PlaybackOverlayFragment extends PlaybackFragment {
    private static final String TAG = PlaybackOverlayFragment.class.getSimpleName();

    private MediaInfo mMediaInfo;
    private PlaybackControlsRow mPlaybackControlsRow;
    private ArrayObjectAdapter mPrimaryActionsAdapter;
    private int mVdoPlayerState;

    private PlaybackControlsRow.PlayPauseAction mPlayPauseAction;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;

    private ArrayObjectAdapter mRowsAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setBackgroundType(PlaybackOverlayFragment.BG_LIGHT);
        setFadingEnabled(true);

        setUpRows();
    }

    // called when state change callback is received from VdoPlayer
    public void playbackStateChanged(boolean playWhenReady, int state) {
        mVdoPlayerState = state;
        if (playWhenReady) {
            setFadingEnabled(true);
            mPlayPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.PAUSE);
            mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlaybackControlsRow.PlayPauseAction.PAUSE));
        } else {
            //setFadingEnabled(false);
            mPlayPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.PLAY);
            mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlaybackControlsRow.PlayPauseAction.PLAY));
        }
        notifyPlaybackRowChanged();
    }

    public void playbackPositionChanged(long positionMs) {
        mPlaybackControlsRow.setCurrentTimeLong(positionMs);
    }

    public void playbackDurationChanged(long durationMs) {
        mPlaybackControlsRow.setTotalTimeLong(durationMs);
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
        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                if (action.getId() == mPlayPauseAction.getId()) {
                    togglePlayback(mPlayPauseAction.getIndex() == PlaybackControlsRow.PlayPauseAction.PLAY);
                } else if (action.getId() == mFastForwardAction.getId()) {
                    fastForward();
                } else if (action.getId() == mRewindAction.getId()) {
                    rewind();
                }
            }
        });
        setAdapter(mRowsAdapter);
    }

    private void addPlaybackControlsRow() {
        mPlaybackControlsRow = new PlaybackControlsRow();
        mRowsAdapter.add(mPlaybackControlsRow);

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);

        Activity activity = getActivity();
        mRewindAction = new PlaybackControlsRow.RewindAction(activity);
        mPlayPauseAction = new PlaybackControlsRow.PlayPauseAction(activity);
        mFastForwardAction = new PlaybackControlsRow.FastForwardAction(activity);

        // PrimaryAction setting
        mPrimaryActionsAdapter.add(mRewindAction);
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        mPrimaryActionsAdapter.add(mFastForwardAction);
    }

    private void togglePlayback(boolean playWhenReady) {
        ((TvPlayerActivity)getActivity()).setPlayWhenReady(playWhenReady);
    }

    // fast forward will simply seek 5 seconds forward
    private void fastForward() {
        ((TvPlayerActivity)getActivity()).fastForward();
    }

    // rewind will simply seek 5 seconds backward
    private void rewind() {
        ((TvPlayerActivity)getActivity()).rewind();
    }

    // todo to be used for displaying optional details above primary action row
    private static class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

        @Override
        protected void onBindDescription(ViewHolder viewHolder, Object item) {
            MediaInfo mediaInfo = (MediaInfo) item;
            if (mediaInfo != null) {
                viewHolder.getTitle().setText(mediaInfo.title);
                viewHolder.getBody().setText(mediaInfo.description);
            }
        }
    }
}
