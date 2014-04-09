package com.mediatek.gallery3d.ext;
/**
 * Small features option for video playback
 *
 */
public interface IMovieStrategy {
    /**
     * Enable NotificationManagerPlus or not.
     * If you want to return true by this function,
     * you should let IMoiveExtension.getFeatureList() return values contain 
     * IMoiveExtension.FEATURE_ENABLE_NOTIFICATION_PLUS.
     * @param item
     * @return
     */
    boolean shouldEnableNMP(IMovieItem item);
    /**
     * Enable checking server timeout or not.
     * @return
     */
    boolean shouldEnableServerTimeout();
    /**
     * Enable checking long sleep(>=180s) or not.
     * @return
     */
    boolean shouldEnableCheckLongSleep();
    /**
     * Enable rewind, forward, step option settings.
     * @return
     */
    boolean shouldEnableRewindAndForward();
}