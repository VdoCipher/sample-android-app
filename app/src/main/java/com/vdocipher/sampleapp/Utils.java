package com.vdocipher.sampleapp;

/**
 * Utility class
 */

class Utils {
    static String digitalTime(int timeInMilliSeconds) {
        int totalSeconds = timeInMilliSeconds/1000;
        int hours = totalSeconds / (60 * 60);
        int minutes = (totalSeconds - hours * 60 * 60) / 60;
        int seconds = (totalSeconds - hours * 60 * 60 - minutes * 60);

        String timeThumb = "";
        if (hours > 0) {
            if (hours < 10) {
                timeThumb += "0" + hours + ":";
            } else {
                timeThumb += hours + ":";
            }
        }
        if (minutes > 0) {
            if (minutes < 10) {
                timeThumb += "0" + minutes + ":";
            } else {
                timeThumb += minutes + ":";
            }
        } else {
            timeThumb += "00" + ":";
        }
        if (seconds < 10) {
            timeThumb += "0" + seconds;
        } else {
            timeThumb += seconds;
        }
        return timeThumb;
    }
}
