package com.vdocipher.sampleapp.models;

import java.util.ArrayList;
import java.util.List;

public class PlaylistHolder {

    List<MediaItem> mediaItems = new ArrayList<>();

    public PlaylistHolder() {
        prepareMediaItems();
    }

    public List<MediaItem> getMediaItems() {
        return mediaItems;
    }

    private void prepareMediaItems() {
        MediaItem mediaItem_1 = new MediaItem(
                "df18b398c85a48b2827ec694d96e0967",
                "Big Buck Bunny",
                "https://d1z78r8i505acl.cloudfront.net/poster/w5wTtBSuo2Mv3.480.jpeg",
                596,
                "20160313versASE323yV19xZdf8ir7Bg2fO4YUxMJB7eVi0ag2eU4XLJg0rpCcq2",
                "eyJ2aWRlb0lkIjoiZGYxOGIzOThjODVhNDhiMjgyN2VjNjk0ZDk2ZTA5NjcifQ==",
                false
        );

        MediaItem mediaItem_2 = new MediaItem(
                "786673d2ff8f4790a5b79f107c3e567f",
                "Tears of Steel",
                "https://d1z78r8i505acl.cloudfront.net/poster/ev5iTFAwmGoCL.480.jpeg",
                734,
                "20160313versASE323JN6ECwfzS1s9NSSVEYVObAc34FoHMHgyQoBgraBa5xEK5K",
                "eyJ2aWRlb0lkIjoiNzg2NjczZDJmZjhmNDc5MGE1Yjc5ZjEwN2MzZTU2N2YifQ==",
                false
        );

        MediaItem mediaItem_3 = new MediaItem(
                "48f744dd24494b7d82a77cdea045b61f",
                "Elephant Dream",
                "https://d1z78r8i505acl.cloudfront.net/poster/FRFTAbZDfXk8f.480.jpeg",
                654,
                "20160313versASE323YZWe4AC1Mm1Agz4BsRzCKJSuOIYKR21q2iVVCVL78vRpiv",
                "eyJ2aWRlb0lkIjoiNDhmNzQ0ZGQyNDQ5NGI3ZDgyYTc3Y2RlYTA0NWI2MWYifQ==",
                false
        );

        MediaItem mediaItem_4 = new MediaItem(
                "ca9bc81eb44348dd94ef9d6b6164a711",
                "Sintel",
                "https://d1z78r8i505acl.cloudfront.net/poster/FmDaw6i6JVSvr.480.jpeg",
                888,
                "20160313versASE3234ou3rA1Bb3uX7uNX5vK6obwFC7DL4FkwLBl6kLIaJGtLH3",
                "eyJ2aWRlb0lkIjoiY2E5YmM4MWViNDQzNDhkZDk0ZWY5ZDZiNjE2NGE3MTEifQ==",
                false
        );

        mediaItems.add(mediaItem_1);
        mediaItems.add(mediaItem_2);
        mediaItems.add(mediaItem_3);
        mediaItems.add(mediaItem_4);
    }
}
