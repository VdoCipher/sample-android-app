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
                "Elephant Dream",
                "Big buck bunny",
        };

        String[] otp = {
                "20160313versASE323NSOCfVwXoiEbuNJpdsgJbGjMecO9UhdWmftrGDETv7KlCy",
                "20160313versASE323IME8y94TjOPbFXlgZuElNTaT7D2k7yyGHobjRDuBWFoeDK",
        };

        String[] playbackInfo = {
                "eyJ2aWRlb0lkIjoiMjY0ZDUxMWM1NDJhNGQyM2IwY2M3MzE3ODY3YWM0ODMifQ==",
                "eyJ2aWRlb0lkIjoiZWQxYzk0NDUxMjlkNDJhNmJkYTJkYWE3M2MxYzU1ZWUifQ==",
        };

        String[] description = {
                "The first Blender Open Movie from 2006",
                "Big Buck Bunny tells the story of a giant rabbit with a heart bigger than himself."
        };


        String[] cardImageUrl = {
                "https://d1z78r8i505acl.cloudfront.net/poster/BGbRHpGC5OAzy.720.jpeg",
                "https://d1z78r8i505acl.cloudfront.net/poster/toQsFmrSDfY8z.720.jpeg"
        };

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