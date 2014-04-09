package com.android.camera.ui;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;

import com.android.camera.Log;

public class RotateAnimationImageView extends RotateImageView {
    private static final String TAG = "RotateAnimationImageView";
    private static final boolean LOG = Log.LOGV;
    
    public RotateAnimationImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public void setImageResource(int resId) {
        setAnimationRunning(false);
        super.setImageResource(resId);
        setAnimationRunning(true);
    }
    
    private void setAnimationRunning(boolean run) {
        if (LOG) {
            Log.v(TAG, "setAnimationRunning(" + run + ")");
        }
        AnimationDrawable anim = null;
        if (getDrawable() instanceof AnimationDrawable) {
            anim = (AnimationDrawable)getDrawable();
        }
        if (anim != null) {
            if (run && !anim.isRunning()) {
                anim.start();
            }
            if (!run && anim.isRunning()) {
                anim.stop();
            }
        }
    }
}
