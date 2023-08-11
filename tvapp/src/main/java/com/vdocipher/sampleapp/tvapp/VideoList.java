package com.vdocipher.sampleapp.tvapp;

import java.util.ArrayList;
import java.util.List;

public final class VideoList {
    public static final String[] VIDEO_CATEGORY = {
            "Sample Videos",
            "Sample Videos (Custom Ui)",
    };

    private static List<Video> list;
    private static long count = 0;

    public static List<Video> getList() {
        if (list == null) {
            list = setupVideos();
        }
        return list;
    }

    public static List<Video> setupVideos() {
        list = new ArrayList<>();

        String[] title = {
                "Sample"};

        String[] otp = {
                "20150519versASE31ba8fc50a0ac49b8e74b9c40f49e099755cd36dc8adccaa3"};

        String[] playbackInfo = {
                "eyJ2aWRlb0lkIjoiM2YyOWI1NDM0YTVjNjE1Y2RhMThiMTZhNjIzMmZkNzUifQ=="};

        String[] description = {
                "Sample Video"};


        String[] cardImageUrl = {
                "https://d1z78r8i505acl.cloudfront.net/poster/toQsFmrSDfY8z.720.jpeg"};

        for (int index = 0; index < title.length; ++index) {
            list.add(
                    buildVideoInfo(
                            title[index],
                            description[index],
                            otp[index],
                            playbackInfo[index],
                            cardImageUrl[index]));
        }

        return list;
    }

    private static Video buildVideoInfo(
            String title,
            String description,
            String otp,
            String playbackInfo,
            String cardImageUrl) {
        Video video = new Video();
        video.setId(count++);
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoOtp(otp);
        video.setVideoPlaybackInfo(playbackInfo);
        video.setCardImageUrl(cardImageUrl);
        return video;
    }
}