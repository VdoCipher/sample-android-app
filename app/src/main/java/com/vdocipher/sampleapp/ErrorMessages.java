package com.vdocipher.sampleapp;

import com.vdocipher.aegis.media.ErrorDescription;

/**
 * Descriptive error messages to show in player user interface for cases where user action might be
 * helpful.
 */
public final class ErrorMessages {
    /**
     * Customize error message displayed on the player view depending on the error code.
     */
    public static String getErrorMessage(ErrorDescription errorDescription) {
        String messagePrefix = "Error: " + errorDescription.errorCode + ". ";

        switch (errorDescription.errorCode) {
            case 1060:
                return  messagePrefix + "Offline DRM error. " +
                        "Kindly update your OS, restart the phone and app. If still not " +
                        "corrected, factory reset can be tried if possible.";
            case 1201:
            case 1202:
                return messagePrefix + "Please make sure USB debugging is disabled. In " +
                        "\"Settings > System > Advanced > Developer options > Turn off USB debugging\"";
            case 2013:
            case 2018:
                return messagePrefix + "OTP is expired or invalid. Please go back, and start " +
                        "playback again.";
            case 4101:
                return messagePrefix + "Invalid video parameters. Please contact the app developer.";
            case 4102:
                return messagePrefix + "Offline video not found. Please make sure the video was " +
                        "downloaded successfully and not deleted.";
            case 5110:
            case 5114:
            case 5124:
            case 5130:
                return messagePrefix + "Please check your internet connection and try restarting " +
                        "the app.";
            case 5113:
            case 5123:
            case 5133:
            case 5152:
                return messagePrefix + "Temporary service error. This should automatically resolve " +
                        "quickly. Please try playback again.";
            case 5151:
                return messagePrefix + "Network error, possibly with your local ISP. Please try " +
                        "after some time.";
            case 5160:
            case 5161:
                return messagePrefix + "Downloaded media files have been accidentally deleted by " +
                        "some other app in your mobile. Kindly download the video again and do " +
                        "not use cleaner apps.";
            case 6101:
            case 6120:
            case 6122:
                return messagePrefix + "Error decoding video. Kindly try restarting the phone and app.";
            case 6102:
                return messagePrefix + "Offline video download is not yet complete or it failed. " +
                        "Please make sure it is successfully downloaded.";
            case 1220:
            case 1250:
            case 1253:
            case 2021:
            case 2022:
            case 6155:
            case 6156:
            case 6157:
            case 6161:
            case 6166:
            case 6172:
            case 6177:
            case 6178:
            case 6181:
            case 6186:
            case 6190:
            case 6196:
                return messagePrefix + "Phone is not compatible for secure playback. " +
                        "Kindly update your OS, restart the phone and app. If still not " +
                        "corrected, factory reset can be tried if possible.";
            case 6187:
                return messagePrefix + "Rental license for downloaded video has expired. Kindly " +
                        "download again.";
            case 6197:
            case 6198:
                return messagePrefix + "DRM provisioning error. Kindly try restarting the phone and reinstalling app";
            default:
                return "An error occurred: " + errorDescription.errorCode + "\nTap to retry";
        }
    }
}
