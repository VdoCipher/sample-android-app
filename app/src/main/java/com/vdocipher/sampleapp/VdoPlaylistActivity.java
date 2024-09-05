package com.vdocipher.sampleapp;

import android.app.PictureInPictureParams;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vdocipher.aegis.media.ErrorDescription;
import com.vdocipher.aegis.media.PlayerOption;
import com.vdocipher.aegis.media.Track;
import com.vdocipher.aegis.player.PlayerHost;
import com.vdocipher.aegis.player.VdoInitParams;
import com.vdocipher.aegis.player.VdoPlayer;
import com.vdocipher.aegis.ui.view.FullScreenActionListener;
import com.vdocipher.aegis.ui.view.VdoPlayerUIFragment;
import com.vdocipher.sampleapp.models.MediaItem;
import com.vdocipher.sampleapp.models.PlaylistHolder;

import java.util.ArrayList;

public class VdoPlaylistActivity extends AppCompatActivity implements VideoAdapter.MediaItemSelected {

    private static final String TAG = VdoPlaylistActivity.class.getName();
    private VdoPlayerUIFragment newFragment;
    private VdoPlayer vdoPlayer;
    private int currentMediaIndex = 0;
    private final ArrayList<MediaItem> mediaItems = new ArrayList<>();
    private VideoAdapter videoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vdo_playlist);

        // Initialize the VdoPlayerUIFragment and set fullscreen listener
        newFragment = (VdoPlayerUIFragment) getSupportFragmentManager().findFragmentById(R.id.vdo_player_ui_fragment);
        newFragment.setFullscreenActionListener(fullscreenToggleListener);

        // Initialize RecyclerView with a LinearLayoutManager
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                LinearLayoutManager.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        // Initialize the VideoAdapter and set it to the RecyclerView
        videoAdapter = new VideoAdapter(this);
        recyclerView.setAdapter(videoAdapter);

        // Load media items
        loadPlaylist();
    }

    private void loadPlaylist() {
        mediaItems.addAll(new PlaylistHolder().getMediaItems());
        videoAdapter.submitList(new ArrayList<>(mediaItems));
        newFragment.initialize(initializationListener);
    }

    /**
     * Plays the next video in the playlist.
     */
    private void playNextVideo() {
        // Play the video at the new position
        playViewAtPosition(currentMediaIndex + 1);
    }

    private void playViewAtPosition(int position) {
        // If we reach the end of the list, loop back to the start
        if (position >= mediaItems.size()) {
            position = 0;
        }

        // Store the current position as previous
        int previousPosition = currentMediaIndex;

        // Set the current media index to the position
        currentMediaIndex = position;

        // Load the video at the new position
        loadVideo(position);

        // Only update the items if they are different
        if (previousPosition != currentMediaIndex) {
            updateIsPlaying(previousPosition, false);
        }

        // Mark the current item as playing
        updateIsPlaying(currentMediaIndex, true);

        // Submit the updated list to the adapter
        videoAdapter.submitList(mediaItems);
    }

    public void updateIsPlaying(int position, boolean isPlaying) {
        if (position < 0 || position >= mediaItems.size()) return;
        MediaItem oldItem = mediaItems.get(position);

        // Create a new MediaItem only if the isPlaying status has changed
        if (oldItem.isPlaying != isPlaying) {
            MediaItem updatedItem = new MediaItem(
                    oldItem.id,
                    oldItem.title,
                    oldItem.poster,
                    oldItem.duration,
                    oldItem.otp,
                    oldItem.playbackInfo,
                    isPlaying
            );

            // Update the list with the new item
            mediaItems.set(position, updatedItem);

            // Notify the adapter of the specific item that changed
            videoAdapter.notifyItemChanged(position);
        }
    }

    private void loadVideo(int position) {
        if (position < 0 || position >= mediaItems.size()) return;
        MediaItem mediaItem = mediaItems.get(position);
        VdoInitParams vdoInitParams = new VdoInitParams.Builder()
                .setOtp(mediaItem.otp)
                .setPlaybackInfo(mediaItem.playbackInfo)
                .setPreferredCaptionsLanguage("en")
                .build();
        vdoPlayer.load(vdoInitParams);
    }

    // Listener for player initialization events
    PlayerHost.InitializationListener initializationListener = new PlayerHost.InitializationListener() {
        @Override
        public void onInitializationSuccess(PlayerHost playerHost, VdoPlayer player, boolean wasRestored) {
            VdoPlaylistActivity.this.vdoPlayer = player;
            player.addPlaybackEventListener(playbackListener);
            playViewAtPosition(0);
        }

        @Override
        public void onInitializationFailure(PlayerHost playerHost, ErrorDescription errorDescription) {
            Log.e(TAG, "Player initialization failed: " + errorDescription.errorMsg);
        }

        @Override
        public void onDeInitializationSuccess() {
            Log.i(TAG, "Player de-initialized successfully");
        }
    };

    // Listener for playback events
    private final VdoPlayer.PlaybackEventListener playbackListener = new VdoPlayer.PlaybackEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            // Handle player state changes
        }

        @Override
        public void onTracksChanged(Track[] availableTracks, Track[] selectedTracks) {
            Log.i(TAG, "Tracks changed");
        }

        @Override
        public void onMetaDataLoaded(PlayerOption playerOption) {
            // Handle metadata loaded
        }

        @Override
        public void onBufferUpdate(long bufferTime) {
            // Handle buffer updates
        }

        @Override
        public void onSeekTo(long millis) {
            Log.i(TAG, "Seeked to: " + millis);
        }

        @Override
        public void onProgress(long millis) {
            // Handle progress updates
        }

        @Override
        public void onPlaybackSpeedChanged(float speed) {
            Log.i(TAG, "Playback speed changed to " + speed);
        }

        @Override
        public void onLoading(VdoInitParams vdoInitParams) {
            Log.i(TAG, "Video loading");
        }

        @Override
        public void onLoadError(VdoInitParams vdoInitParams, ErrorDescription errorDescription) {
            Log.e(TAG, "Load error: " + errorDescription.errorCode + ": " + errorDescription.errorMsg);
        }

        @Override
        public void onLoaded(VdoInitParams vdoInitParams) {
            Log.i(TAG, "Video loaded");
        }

        @Override
        public void onError(VdoInitParams vdoParams, ErrorDescription errorDescription) {
            Log.e(TAG, "Playback error: " + errorDescription.errorCode + ": " + errorDescription.errorMsg);
        }

        @Override
        public void onMediaEnded(VdoInitParams vdoInitParams) {
            Log.i(TAG, "Media ended");
            playNextVideo(); // Play the next video in the playlist
        }
    };

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Enter Picture-in-Picture mode if supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
        }
    }

    // Listener for fullscreen toggle events
    private final FullScreenActionListener fullscreenToggleListener = enterFullscreen -> {
        Log.d(TAG, "Fullscreen = " + enterFullscreen);
        return true;
    };

    @Override
    public void onMediaSelected(int position) {
        playViewAtPosition(position);
    }
}