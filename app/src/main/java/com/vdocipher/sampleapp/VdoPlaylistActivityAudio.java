package com.vdocipher.sampleapp;

import android.app.PictureInPictureParams;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import com.vdocipher.sampleapp.utils.Utils;

import java.util.ArrayList;
import java.util.Random;

public class VdoPlaylistActivityAudio extends AppCompatActivity implements VideoAdapter.MediaItemSelected {

    private static final String TAG = VdoPlaylistActivityAudio.class.getName();
    private VdoPlayerUIFragment newFragment;
    private VdoPlayer vdoPlayer;
    private int currentMediaIndex = 0;
    private final ArrayList<MediaItem> mediaItems = new ArrayList<>();
    private VideoAdapter videoAdapter;
    private ImageButton btnNext, btnPrevious, playPauseButton, btnShuffle, btnRepeat;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvDuration, tvTitle;
    boolean isShuffle = false;
    boolean isRepeat = false;

    Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vdo_playlist_audio);
        // Initialize the VdoPlayerUIFragment and set fullscreen listener
        newFragment = (VdoPlayerUIFragment) getSupportFragmentManager().findFragmentById(R.id.vdo_player_ui_fragment);
        newFragment.setFullscreenActionListener(fullscreenToggleListener);

        playPauseButton = (ImageButton) findViewById(R.id.btn_play_pause);


        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String currentTag = (String) playPauseButton.getTag();


                if ("Play".equals(currentTag)) {
                    playPauseButton.setImageResource(R.drawable.ic_play);
                    vdoPlayer.setPlayWhenReady(false);
                    playPauseButton.setTag("Pause");
                } else {
                    vdoPlayer.setPlayWhenReady(true);
                    playPauseButton.setImageResource(R.drawable.ic_pause);
                    playPauseButton.setTag("Play");

                }
            }
        });

        btnNext = (ImageButton) findViewById(R.id.btn_next);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNextMedia();

            }
        });

        btnPrevious = (ImageButton) findViewById(R.id.btn_previous);

        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                playLastMedia();
            }
        });


        btnShuffle = (ImageButton) findViewById(R.id.btn_shuffle);
        btnShuffle.setTag("ShuffleOn");

        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String currentTag = (String) btnShuffle.getTag();

                if ("ShuffleOn".equals(currentTag)) {
                    isShuffle = true;
                    btnShuffle.setImageResource(R.drawable.baseline_shuffle_on_24);
                    btnShuffle.setTag("ShuffleOff");

                } else {
                    isShuffle = false;
                    btnShuffle.setImageResource(R.drawable.baseline_shuffle_24);
                    btnShuffle.setTag("ShuffleOn");

                }
            }
        });

        btnRepeat = (ImageButton) findViewById(R.id.btn_repeat);
        btnRepeat.setTag("RepeatOn");

        btnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentTag = (String) btnRepeat.getTag();


                if ("RepeatOn".equals(currentTag)) {

                    isRepeat = true;
                    btnRepeat.setImageResource(R.drawable.baseline_repeat_on_24);
                    btnRepeat.setTag("RepeatOff");
                } else {
                    isRepeat = false;
                    btnShuffle.setImageResource(R.drawable.baseline_repeat_24);
                    btnRepeat.setTag("RepeatOn");

                }
            }
        });
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                vdoPlayer.seekTo(seekBar.getProgress() * 1000L);

            }
        });

        tvCurrentTime = (TextView) findViewById(R.id.tv_current_time);

        tvDuration = (TextView) findViewById(R.id.tv_total_duration);
        tvTitle = (TextView) findViewById(R.id.title);

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
        mediaItems.addAll(new PlaylistHolder(true).getMediaItems());
        videoAdapter.submitList(new ArrayList<>(mediaItems));
        newFragment.initialize(initializationListener);
    }

    /**
     * Plays the next video in the playlist.
     */
    private void playNextMedia() {

        int position;
        if (isRepeat) {
            vdoPlayer.seekTo(0);
            return;
        } else if (isShuffle)
            position = random.nextInt(4);
        else
            position = currentMediaIndex + 1;

        // Play the video at the new position
        playViewAtPosition(position);
    }

    private void playLastMedia() {
        if (currentMediaIndex < 1)
            playViewAtPosition(0);
        else
            playViewAtPosition(currentMediaIndex - 1);
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
        seekBar.setMax(mediaItem.duration);
        tvDuration.setText(Utils.convertMillisToTime(mediaItem.duration * 1000L));
        tvTitle.setText(mediaItem.title);

    }

    // Listener for player initialization events
    PlayerHost.InitializationListener initializationListener = new PlayerHost.InitializationListener() {
        @Override
        public void onInitializationSuccess(PlayerHost playerHost, VdoPlayer player, boolean wasRestored) {
            VdoPlaylistActivityAudio.this.vdoPlayer = player;
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
            seekBar.setProgress((int) millis / 1000);
            tvCurrentTime.setText(Utils.convertMillisToTime(millis));
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
            playPauseButton.setTag("Play");
            playPauseButton.setImageResource(R.drawable.ic_pause);
        }

        @Override
        public void onError(VdoInitParams vdoParams, ErrorDescription errorDescription) {
            Log.e(TAG, "Playback error: " + errorDescription.errorCode + ": " + errorDescription.errorMsg);
        }

        @Override
        public void onMediaEnded(VdoInitParams vdoInitParams) {
            Log.i(TAG, "Media ended");
            playNextMedia(); // Play the next video in the playlist
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