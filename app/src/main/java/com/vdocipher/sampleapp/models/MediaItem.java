package com.vdocipher.sampleapp.models;

import java.util.Objects;

public class MediaItem {
    public String id;
    public String title;
    public String poster;
    public int duration;
    public String otp;
    public String playbackInfo;

    public Boolean isPlaying;

    public MediaItem(String id, String title, String poster, int duration, String otp, String playbackInfo, Boolean isPlaying) {
        this.id = id;
        this.title = title;
        this.poster = poster;
        this.duration = duration;
        this.otp = otp;
        this.playbackInfo = playbackInfo;
        this.isPlaying = isPlaying;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaItem mediaItem = (MediaItem) o;
        return duration == mediaItem.duration && Objects.equals(id, mediaItem.id) && Objects.equals(title, mediaItem.title) && Objects.equals(poster, mediaItem.poster) && Objects.equals(otp, mediaItem.otp) && Objects.equals(playbackInfo, mediaItem.playbackInfo) && Objects.equals(isPlaying, mediaItem.isPlaying);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, poster, duration, otp, playbackInfo, isPlaying);
    }
}
