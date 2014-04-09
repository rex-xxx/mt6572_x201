package com.android.camera.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.camera.Log;
import com.android.camera.R;

public class ProgressIndicator {

    private static final String TAG = "ProgressIndicator";
    public static final int TYPE_MAV = 1;
    public static final int TYPE_PANO = 2;
    public static final int TYPE_SINGLE3D = 3;

    public static final int BLOCK_NUMBERS = 9;
    public static final int BLOCK_NUMBERS_SINGLE3D = 2;

    // those sizes are designed for mdpi in pixels, you need to change progress_indicator.xml when change the values here.
    private final int mPanoBlockSizes[] = { 17, 15, 13, 12, 11, 12, 13, 15, 17 };
    private final int mMavBlockSizes[] = { 11, 12, 13, 15, 17, 15, 13, 12, 11 };
    private final int mSingle3DBlockSizes[] = { 11, 11 };

    public static final int MAV_CAPTURE_NUM = 15;
    public static final int PANORAMA_CAPTURE_NUM = 9;

    private int mBlockPadding = 4;

    private View mProgressView;
    private ImageView mProgressBars;
    private static int sIndicatorMarginLong = 0;
    private static int sIndicatorMarginShort = 0;
    private static final boolean LOG = Log.LOGV;

    public ProgressIndicator(Activity activity, int indicatorType) {
        mProgressView = activity.findViewById(R.id.progress_indicator);
        mProgressView.setVisibility(View.VISIBLE);
        mProgressBars = (ImageView) activity.findViewById(R.id.progress_bars);

        Resources res = activity.getResources();
        final float scale = res.getDisplayMetrics().density;
        if (indicatorType == TYPE_MAV) {
            if (scale != 1.0f) {
                mBlockPadding = (int) (mBlockPadding * scale + 0.5f);
                for (int i = 0; i < BLOCK_NUMBERS; i++) {
                    mMavBlockSizes[i] = (int) (mMavBlockSizes[i] * scale + 0.5f);
                }
            }
            mProgressBars.setImageDrawable(new ProgressBarDrawable(activity, mProgressBars, mMavBlockSizes, mBlockPadding));
        } else if (indicatorType == TYPE_PANO) {
            if (scale != 1.0f) {
                mBlockPadding = (int) (mBlockPadding * scale + 0.5f);
                for (int i = 0; i < BLOCK_NUMBERS; i++) {
                    mPanoBlockSizes[i] = (int) (mPanoBlockSizes[i] * scale + 0.5f);
                    if (LOG) {
                        Log.v(TAG, "mPanoBlockSizes[i]: " + mPanoBlockSizes[i]);
                    }
                }
            }
            mProgressBars.setImageDrawable(new ProgressBarDrawable(activity, mProgressBars, mPanoBlockSizes, mBlockPadding));
        } else if (indicatorType == TYPE_SINGLE3D) {
            if (scale != 1.0f) {
                mBlockPadding = (int) (mBlockPadding * scale + 0.5f);
                for (int i = 0; i < BLOCK_NUMBERS_SINGLE3D; i++) {
                    mSingle3DBlockSizes[i] = (int) (mSingle3DBlockSizes[i] * scale + 0.5f);
                }
            }
            mProgressBars.setImageDrawable(new ProgressBarDrawable(activity, mProgressBars, mSingle3DBlockSizes,
                    mBlockPadding));
        }
        getIndicatorMargin();
//        setOrientation(0);
    }

    public void setVisibility(int visibility) {
        mProgressView.setVisibility(visibility);
    }

    public void setProgress(int progress) {
        if (LOG) {
            Log.v(TAG, "setProgress: " + progress);
        }
        mProgressBars.setImageLevel(progress);
    }

    private void getIndicatorMargin() {
        if (sIndicatorMarginLong == 0 && sIndicatorMarginShort == 0) {
            Resources res = mProgressView.getResources();
            sIndicatorMarginLong = res.getDimensionPixelSize(R.dimen.progress_indicator_bottom_long);
            sIndicatorMarginShort = res.getDimensionPixelSize(R.dimen.progress_indicator_bottom_short);
        }
        if (LOG) {
            Log.v(TAG, "getIndicatorMargin: sIndicatorMarginLong = " + sIndicatorMarginLong
                    + " sIndicatorMarginShort = " + sIndicatorMarginShort);
        }
    }

    public void setOrientation(int orientation) {
        LinearLayout progressViewLayout = (LinearLayout) mProgressView;
        RelativeLayout.LayoutParams rp = new RelativeLayout.LayoutParams(progressViewLayout.getLayoutParams());
        int activityOrientation = mProgressView.getResources().getConfiguration().orientation;
        if ((Configuration.ORIENTATION_LANDSCAPE == activityOrientation && (orientation == 0 || orientation == 180))
                || (Configuration.ORIENTATION_PORTRAIT == activityOrientation
                        && (orientation == 90 || orientation == 270))) {
            rp.setMargins(rp.leftMargin, rp.topMargin, rp.rightMargin, sIndicatorMarginShort);
        } else {
            rp.setMargins(rp.leftMargin, rp.topMargin, rp.rightMargin, sIndicatorMarginLong);
        }

        rp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        rp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        progressViewLayout.setLayoutParams(rp);
        progressViewLayout.requestLayout();
    }
}
