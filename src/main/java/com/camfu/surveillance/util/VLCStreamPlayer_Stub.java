package com.camfu.surveillance.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub VLC Stream Player - Display only, no actual VLC integration
 * Allows the application window to display properly
 */
public class VLCStreamPlayer_Stub {
    private static final Logger logger = LoggerFactory.getLogger(VLCStreamPlayer_Stub.class);

    public static void initializeVLC() {
        logger.info("VLC initialization (stub)");
    }

    public static boolean isVLCAvailable() {
        return false; // Disable VLC for now
    }

    public static boolean isValidStreamUrl(String url) {
        return url != null && !url.trim().isEmpty();
    }

    public static void playStream(Object component, String streamUrl) {
        logger.info("Playing stream (stub): " + streamUrl);
    }

    public static void stopStream(Object component) {
        logger.debug("Stopped stream (stub)");
    }

    public static void pauseStream(Object component) {
        logger.debug("Paused stream (stub)");
    }

    public static void resumeStream(Object component) {
        logger.debug("Resumed stream (stub)");
    }

    public static void releaseResources(Object component) {
        logger.debug("Released resources (stub)");
    }

    public static long getCurrentPosition(Object component) {
        return 0;
    }

    public static long getDuration(Object component) {
        return 0;
    }

    public static boolean isPlaying(Object component) {
        return false;
    }
}

