package com.vdocipher.sampleapp;

import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;

@SuppressWarnings("deprecation")
public class ExoPlayback extends AppCompatActivity {

    private static final String SAMPLE_MANIFEST_URI = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exo_playback);
        PlayerView playerView = findViewById(R.id.playerView);


        ExoPlayer exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        MediaSource mediaSource = buildMediaSource(Uri.parse(SAMPLE_MANIFEST_URI));

        exoPlayer.setMediaSource(mediaSource);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.prepare();
    }

    private MediaSource buildMediaSource(Uri manifestUri) {
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(manifestUri)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, "exoplayer-demo");
        return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
    }

}