package com.android.camera;

import android.os.Handler;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class AnimationController {
    private static final String TAG = "AnimationController";
    public static final int ANIM_DURATION = 180;
    private ViewGroup[] mDirectionIndicators;
    private ViewGroup mCenterArrow;
    private Handler mHanler = new Handler();
    private int mCenterDotIndex;
    private int mDirectionDotIndex;
    private static final boolean LOG = Log.LOGV;

    private Runnable mApplyCenterArrowAnim = new Runnable() {
        private int mDotCount;

        public void run() {
            if (mDotCount == 0) {
                mDotCount = mCenterArrow.getChildCount();
            }
            if (mCenterDotIndex >= mDotCount) {
                return;
            }
            AlphaAnimation alpha = new AlphaAnimation(1.0f, 0.0f);
            alpha.setDuration(ANIM_DURATION * 8);
            alpha.setRepeatCount(Animation.INFINITE);

            Log.i("camera", "start Arrow animation of " + mCenterDotIndex);
            mCenterArrow.getChildAt(mCenterDotIndex).startAnimation(alpha);
            alpha.startNow();
            mCenterDotIndex++;
            mHanler.postDelayed(this, ANIM_DURATION * 2 / mDotCount);
        }
    };

    private Runnable mApplyDirectionAnim = new Runnable() {
        private int mDotCount;

        public void run() {
            for (ViewGroup viewGroup : mDirectionIndicators) {
                if (viewGroup == null) {
                    if (LOG) {
                        Log.v(TAG, "mDirectionIndicators is null");
                    }
                    return;
                }
            }
            if (mDotCount == 0) {
                mDotCount = mDirectionIndicators[0].getChildCount();
            }
            if (mDirectionDotIndex >= mDotCount) {
                return;
            }
            if (LOG) {
                Log.i("camera", "start Direction animation: " + mDirectionDotIndex);
            }
            AlphaAnimation alpha = new AlphaAnimation(1.0f, 0.0f);
            alpha.setDuration(ANIM_DURATION * mDotCount * 3 / 2);
            alpha.setRepeatCount(Animation.INFINITE);

            mDirectionIndicators[0].getChildAt(mDirectionDotIndex).startAnimation(alpha);
            mDirectionIndicators[1].getChildAt(mDotCount - mDirectionDotIndex - 1).startAnimation(alpha);
            mDirectionIndicators[2].getChildAt(mDotCount - mDirectionDotIndex - 1).startAnimation(alpha);
            mDirectionIndicators[3].getChildAt(mDirectionDotIndex).startAnimation(alpha);
            alpha.startNow();

            mDirectionDotIndex++;
            mHanler.postDelayed(this, ANIM_DURATION / 2);
        }
    };

    public AnimationController(ViewGroup[] indicators, ViewGroup arrow) {
        mDirectionIndicators = indicators;
        mCenterArrow = arrow;
    }

    public void startDirectionAnimation() {
        mDirectionDotIndex = 0;
        mApplyDirectionAnim.run();
    }

    public void startCenterAnimation() {
        mCenterDotIndex = 0;
        mApplyCenterArrowAnim.run();
    }

    public void stopDirectionAnimation() {

    }

    public void stopCenterAnimation() {
        for (int i = 0; i < mCenterArrow.getChildCount(); i++) {
            mCenterArrow.getChildAt(i).clearAnimation();
        }
    }
}
