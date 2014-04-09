package com.mediatek.gallery3d.video;

import android.content.Intent;
import android.os.Bundle;

import com.mediatek.gallery3d.ext.MtkLog;
import com.mediatek.notification.NotificationManagerPlus;

public class NotificationPlusHooker extends MovieHooker {
    private static final String TAG = "Gallery2/VideoPlayer/NotificationPlusHooker";
    private static final boolean LOG = true;

    private static final String EXTRA_FULLSCREEN_NOTIFICATION = "mediatek.intent.extra.FULLSCREEN_NOTIFICATION";
    private NotificationManagerPlus mPlusNotification;
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableNMP();
    }
    @Override
    public void onStart() {
        super.onStart();
        startListening();
    }
    @Override
    public void onStop() {
        super.onStop();
        stopListening();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        clearNotifications();
    }
    
    private void enableNMP() {
        boolean extraEnable = false;
        final Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra(EXTRA_FULLSCREEN_NOTIFICATION, false)) {
            extraEnable = true;
        }
        if (extraEnable || ExtensionHelper.getMovieStrategy(getContext()).shouldEnableNMP(getMovieItem())) {
            mPlusNotification = new NotificationManagerPlus.ManagerBuilder(getContext())
                    .setPositiveButton(getContext().getString(android.R.string.ok), null)
                    .setNeutralButton(null, null)
                    .setNegativeButton(null, null)
                    .setOnFirstShowListener(getPlayer())
                    .setOnLastDismissListener(getPlayer())
                    .create();
        }
        if (LOG) {
            MtkLog.v(TAG, "enableNMP() extraEnable=" + extraEnable);
        }
    }
    
    private void startListening() {
        if (mPlusNotification != null) {
            mPlusNotification.startListening();
        }
        if (LOG) {
            MtkLog.v(TAG, "startListening() mPlusNotification=" + mPlusNotification);
        }
    }
    
    private void stopListening() {
        if (mPlusNotification != null) {
            mPlusNotification.stopListening();
        }
        if (LOG) {
            MtkLog.v(TAG, "stopListening() mPlusNotification=" + mPlusNotification);
        }
    }
    
    private void clearNotifications() {
        if (mPlusNotification != null) {
            mPlusNotification.clearAll();
        }
        if (LOG) {
            MtkLog.v(TAG, "clearNotifications() mPlusNotification=" + mPlusNotification);
        }
    }
}
