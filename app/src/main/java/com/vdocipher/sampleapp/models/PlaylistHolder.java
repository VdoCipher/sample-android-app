package com.vdocipher.sampleapp.models;

import java.util.ArrayList;
import java.util.List;

public class PlaylistHolder {

    List<MediaItem> mediaItems = new ArrayList<>();

    public PlaylistHolder(boolean isAudio) {
        if (isAudio)
            prepareAudioMediaItems();
        else
            prepareVideoMediaItems();
    }

    public List<MediaItem> getMediaItems() {
        return mediaItems;
    }

    private void prepareVideoMediaItems() {
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


    private void prepareAudioMediaItems() {
        MediaItem mediaItem_1 = new MediaItem(
                "08d7bae1ef904aa883e156a924758c3d",
                "Minimalistic chill ambient",
                "https://d1z78r8i505acl.cloudfront.net/poster/GzpQYRg795VkE.l6tcy9bqmc.200.jpeg",
                70,
                "20160313versASE32324gmgvMH9rpVoMN45p7retBfe74tH9EhzBtE1lLfK0b3Gy",
                "eyJ2aWRlb0lkIjoiMDhkN2JhZTFlZjkwNGFhODgzZTE1NmE5MjQ3NThjM2QifQ==",
                false
        );

        MediaItem mediaItem_2 = new MediaItem(
                "bf4bc9c0de5844d0a62fab17a8416dbf",
                "LKB one hip-hop beat",
                "https://d1z78r8i505acl.cloudfront.net/poster/NtzJI8Ay6XgaU.irrdo8dir9.200.jpeg",
                161,
                "20160313versASE323Gylhx1AUJ0hbnHNoOXKla1gCvzybfg8yxv5BkL9Kosq4Xk",
                "eyJ2aWRlb0lkIjoiYmY0YmM5YzBkZTU4NDRkMGE2MmZhYjE3YTg0MTZkYmYifQ==",
                false
        );

        MediaItem mediaItem_3 = new MediaItem(
                "808a69941cc544d3bf2651e1fd875a6c",
                "Inspiring motivation ambient",
                "https://d1z78r8i505acl.cloudfront.net/poster/vkrxG5N8DIhWF.ly03wujdfx.200.jpeg",
                144,
                "20160313versASE323iciPcN37aZwzIQxQkm4CKWbAlHbHPXi5b78QI2S82mIL0W",
                "eyJ2aWRlb0lkIjoiODA4YTY5OTQxY2M1NDRkM2JmMjY1MWUxZmQ4NzVhNmMifQ==",
                false
        );

        MediaItem mediaItem_4 = new MediaItem(
                "865b1b0b1e1d4dc5b6d714d895b46b4f",
                "Ga Ga gangster",
                "https://d1z78r8i505acl.cloudfront.net/poster/CSDzVwEYjn2DT.tazgybwc3r.200.jpeg",
                284,
                "20160313versASE323VoJOKou5HF3jaqFTyvozpUooHJzoSTqoTCBcfIxzHT0tav",
                "eyJ2aWRlb0lkIjoiODY1YjFiMGIxZTFkNGRjNWI2ZDcxNGQ4OTViNDZiNGYifQ==",
                false
        );

        mediaItems.add(mediaItem_1);
        mediaItems.add(mediaItem_2);
        mediaItems.add(mediaItem_3);
        mediaItems.add(mediaItem_4);
    }
}
