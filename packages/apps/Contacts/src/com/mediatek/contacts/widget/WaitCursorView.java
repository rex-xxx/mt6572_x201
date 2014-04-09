package com.mediatek.contacts.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

public class WaitCursorView {

    private static final String TAG = "WaitCursorView";
    private static final int WAIT_CURSOR_START = 1230;
    private static final long WAIT_CURSOR_DELAY_TIME = 500;
    private TextView mLoadingText;

    private ProgressBar mProgress;

    private View mLoadingContainer;
    private boolean mFinished = false;
    private Context mContext;
    
    public WaitCursorView(Context context, View loadingContainer, ProgressBar progress,
            TextView loadingText) {
        mContext = context;
        mLoadingContainer = loadingContainer;
        mProgress = progress;
        mLoadingText = loadingText;
    }

    public void startWaitCursor() {
        mFinished = false;
        mHandler.sendMessageDelayed(mHandler.obtainMessage(WAIT_CURSOR_START),
                WAIT_CURSOR_DELAY_TIME);
    }

    public void stopWaitCursor() {
        mFinished = true;
        mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(mContext,
                android.R.anim.fade_out));
        mLoadingContainer.setVisibility(View.GONE);
        mLoadingText.setVisibility(View.GONE);
        mProgress.setVisibility(View.GONE);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "handleMessage msg==== " + msg.what);

            switch (msg.what) {

                case WAIT_CURSOR_START:
                    Log.i(TAG, "start WAIT_CURSOR_START !isFinished : " + !mFinished);
                    if (!mFinished) {
                        mLoadingContainer.setVisibility(View.VISIBLE);
                        mLoadingText.setVisibility(View.VISIBLE);
                        mProgress.setVisibility(View.VISIBLE);
                    }
                    break;

                default:
                    break;
            }
        }
    };
}
