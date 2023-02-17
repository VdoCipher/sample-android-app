package com.vdocipher.sampleapp.tvapp;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/*
 * Movie class represents video entity with title, description, image thumbs and video url.
 */
public class Video implements Parcelable {
    private long id;
    private String title;
    private String description;
    private String cardImageUrl;
    private String videoOtp;
    private String videoPlaybackInfo;

    public Video() {
    }

    protected Video(Parcel in) {
        id = in.readLong();
        title = in.readString();
        description = in.readString();
        cardImageUrl = in.readString();
        videoOtp = in.readString();
        videoPlaybackInfo = in.readString();
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel in) {
            return new Video(in);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCardImageUrl() {
        return cardImageUrl;
    }

    public void setCardImageUrl(String cardImageUrl) {
        this.cardImageUrl = cardImageUrl;
    }

    public String getVideoOtp() {
        return videoOtp;
    }

    public void setVideoOtp(String videoOtp) {
        this.videoOtp = videoOtp;
    }

    public String getVideoPlaybackInfo() {
        return videoPlaybackInfo;
    }

    public void setVideoPlaybackInfo(String videoPlaybackInfo) {
        this.videoPlaybackInfo = videoPlaybackInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(title);
        parcel.writeString(description);
        parcel.writeString(cardImageUrl);
        parcel.writeString(videoOtp);
        parcel.writeString(videoPlaybackInfo);
    }
}