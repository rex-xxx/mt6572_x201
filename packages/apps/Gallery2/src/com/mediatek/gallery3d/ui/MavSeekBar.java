package com.mediatek.gallery3d.ui;

import com.mediatek.gallery3d.util.MtkLog;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.android.gallery3d.R;

/**
 * The MavSeekBar is used to indicates loading progress and change view angle 
 */
public class MavSeekBar extends SeekBar {
    
    private static final String TAG = "Gallery2/MavSeekBar";
    
    // current state of MavSeekBar
    // if the state is loading, it like a progress bar, no thumb, and unable seek
    // if the state is sliding, it like a seek bar, show thumb, and enable seek
    public static final int STATE_LOADING = 0;
    public static final int STATE_SLIDING = 1;
    
    private int mState;
    private Drawable mThumb;
    private Drawable mProgressDrawableLoading;
    private Drawable mProgressDrawableSliding;
    
    public MavSeekBar(Context context) {
        super(context);
        MtkLog.v(TAG, "constructor #1 called");
        initializeDrawable();
        init();
    }
    public MavSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        MtkLog.v(TAG, "constructor #2 called");
        initializeDrawable();
        init();
    }
    
    public MavSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        MtkLog.v(TAG, "constructor #3 called");
        initializeDrawable();
        init();
    }

    private void setState(int state) {
        mState = state;
        if (mState == STATE_LOADING) {
            MtkLog.v(TAG, "set MavSeekBar state as STATE_LOADING");
            // first remove "MSG_UPDATE_THUMB_APHPA" from message queue
            // to avoid MavSeekBbar display abnormally
            mHander.removeMessages(MSG_UPDATE_THUMB_APHPA);
            // hide slide thumb
            mThumb.setAlpha(0);
            setThumb(mThumb);
            setProgress(0);
            setProgressDrawable(mProgressDrawableLoading);
            setEnabled(false);
        } else if (mState == STATE_SLIDING){
            MtkLog.d(TAG, "set MavSeekBar state as STATE_SLIDING");
            setProgress(getMax()/2);
            // show slide thumb
//            mThumb.setAlpha(255);
            showThumb();
//            setThumb(mThumb);
            // to avoid NullPointException
            setProgressDrawable(mProgressDrawableSliding);
            setEnabled(true);
        }
    }
    
//    int alpha = 0;
    public void showThumb() {
        alpha = 0;
        mHander.sendEmptyMessage(MSG_UPDATE_THUMB_APHPA);
    }
    
    private static final int MSG_UPDATE_THUMB_APHPA = 0;
    private int alpha = 0;
    private Handler mHander = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch(msg.what) {
            case MSG_UPDATE_THUMB_APHPA:
                if(mThumb == null) return;
                mThumb.setAlpha(alpha);
                setThumb(mThumb);
                alpha += 10;
                if(alpha > 255) return;
                Message newMsg = obtainMessage(MSG_UPDATE_THUMB_APHPA);
                mHander.sendMessageDelayed(newMsg, 1);
                break;
            }
        }
    };
    public void setHandler(Handler handler) {
        mHander = handler;
    }
    
    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        if(mState == STATE_LOADING && progress >= getMax()) {
            MtkLog.d("TAG", "enter sliding mode, state: " + mState + ", max: " + getMax() + ", progress: " +  progress);
            setState(STATE_SLIDING);
        } else if(mState == STATE_SLIDING && progress == 0) {
            init();
        }
    }
    
    public void syncProgressByGyroSensor(int progress) {
        super.setProgress(progress);
    }
    
    public int getState() {
        return mState;
    }
    
    private void init() {
        setState(STATE_LOADING);
    }
    
    /**
     * restore default state
     */
    public void restore() {
        init();
    }
    
    private void initializeDrawable() {
        mThumb = getResources().getDrawable(R.drawable.mavseekbar_control_selector);
        mProgressDrawableLoading = getResources().getDrawable(R.drawable.mavseekbar_progress_loading);
        mProgressDrawableSliding = getResources().getDrawable(R.drawable.mavseekbar_progress_sliding);
    }
    
    @Override
    public void setVisibility(int v) {
        super.setVisibility(v);
        if(v == View.INVISIBLE) {
            restore();
        }
    }
    
}
