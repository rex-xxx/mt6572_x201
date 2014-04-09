package com.android.contacts.ext;

import android.widget.ImageView;

public class CallListExtension {

    public int layoutExtentionIcon(int leftBound, int topBound, int bottomBound, int rightBound,
            int mGapBetweenImageAndText, ImageView mExtentionIcon, String commd) {
        return rightBound;
    }

    public void measureExtention(ImageView mExtentionIcon, String commd) {
    }

    public void setExtentionImageView(ImageView view, String commd) {
        // do nothing
    }

    public boolean setExtentionIcon(String number, String commd) {
        return false;
    }

    public boolean checkPluginSupport(String commd) {
        return false;
    }
}
