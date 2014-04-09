/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera.manager;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.camera.Camera;
import com.android.camera.Camera.OnOrientationListener;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.Util;

/**
 * A on-screen hint is a view containing a little message for the user and will
 * be shown on the screen continuously.  This class helps you create and show
 * those.
 *
 * <p>
 * When the view is shown to the user, appears as a floating view over the
 * application.
 * <p>
 * The easiest way to use this class is to call one of the static methods that
 * constructs everything you need and returns a new {@code OnScreenHint} object.
 */
public class OnScreenToast implements OnOrientationListener {
    static final String TAG = "OnScreenToast";

    //int mGravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
    //int mX;
    //int mY;
    //float mHorizontalMargin;
    //float mVerticalMargin;
    private View mView;
    private View mNextView;
    private RelativeLayout mLayout;
    private TextView mText;

    private final WindowManager.LayoutParams mParams =
            new WindowManager.LayoutParams();
    private final WindowManager mWM;
    private final Handler mHandler = new Handler();

    /**
     * Construct an empty OnScreenHint object.
     *
     * @param context  The context to use.  Usually your
     *                 {@link android.app.Application} or
     *                 {@link android.app.Activity} object.
     */
    private OnScreenToast(Context context) {
        mWM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        /*Configuration newConfig = context.getResources().getConfiguration();
        mY = context.getResources().getDimensionPixelSize(
                newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
                        ? R.dimen.screen_margin_left
                        : R.dimen.screen_margin_right);
        */
        mParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        mParams.format = PixelFormat.TRANSLUCENT;
        //mParams.windowAnimations = R.style.Animation_OnScreenHint;
        mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        mParams.setTitle("OnScreenHint");
        
        mContext = context;
    }

    /**
     * Show the view on the screen.
     */
    public void show() {
        if (mNextView == null) {
            throw new RuntimeException("View is not initialized");
        }
        mHandler.post(mShow);
    }

    /**
     * Close the view if it's showing.
     */
    public void cancel() {
        mHandler.post(mHide);
    }

    /**
     * Make a standard hint that just contains a text view.
     *
     * @param context  The context to use.  Usually your
     *                 {@link android.app.Application} or
     *                 {@link android.app.Activity} object.
     * @param text     The text to show.  Can be formatted text.
     *
     */
    public static OnScreenToast makeText(Context context, CharSequence text) {
        OnScreenToast result = new OnScreenToast(context);

        LayoutInflater inflate = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View v = inflate.inflate(R.layout.onscreen_mode_toast, null);
        TextView tv = (TextView) v.findViewById(R.id.message);
        tv.setText(text);

        result.mNextView = v;
        result.mLayout = (RelativeLayout) v.findViewById(R.id.onscreen_toast_layout);
        result.mText = tv;

        return result;
    }

    /**
     * Update the text in a OnScreenHint that was previously created using one
     * of the makeText() methods.
     * @param s The new text for the OnScreenHint.
     */
    public void setText(CharSequence s) {
        if (mNextView == null) {
            throw new RuntimeException("This OnScreenHint was not "
                    + "created with OnScreenHint.makeText()");
        }
        TextView tv = (TextView) mNextView.findViewById(R.id.message);
        if (tv == null) {
            throw new RuntimeException("This OnScreenHint was not "
                    + "created with OnScreenHint.makeText()");
        }
        tv.setText(s);
    }

    private synchronized void handleShow() {
        if (mView != mNextView) {
            // remove the old view if necessary
            handleHide();
            mView = mNextView;
            /// M: we set hint center_horizontal and bottom in xml.
            //final int gravity = mGravity;
            //mParams.gravity = gravity;
            //if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK)
            //        == Gravity.FILL_HORIZONTAL) {
            //    mParams.horizontalWeight = 1.0f;
            //}
            //if ((gravity & Gravity.VERTICAL_GRAVITY_MASK)
            //        == Gravity.FILL_VERTICAL) {
            //    mParams.verticalWeight = 1.0f;
            //}
            //mParams.x = mX;
            //mParams.y = mY;
            //mParams.verticalMargin = mVerticalMargin;
            //mParams.horizontalMargin = mHorizontalMargin;
            mParams.x = 0;
            mParams.y = 0;
            mParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            mParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            try {
                if (mView.getParent() != null) {
                    mWM.removeView(mView);
                }
                mWM.addView(mView, mParams);
            } catch (BadTokenException ex) {
                ex.printStackTrace();
            }
            Util.fadeIn(mView);
        }
    }

    private synchronized void handleHide() {
        if (mView != null) {
            // note: checking parent() just to make sure the view has
            // been added...  i have seen cases where we get here when
            // the view isn't yet added, so let's try not to crash.
            Util.fadeOut(mView);
            try {
                if (mView.getParent() != null) {
                    mWM.removeView(mView);
                }
            } catch (BadTokenException ex) {
                ex.printStackTrace();
            }
            mView = null;
        }
    }

    private final Runnable mShow = new Runnable() {
        @Override
        public void run() {
            handleShow();
            if (mContext instanceof Camera) { //observe orientation changed.
                ((Camera) mContext).addOnOrientationListener(OnScreenToast.this);
                onOrientationChanged(((Camera) mContext).getOrientationCompensation());
            }
        }
    };

    private final Runnable mHide = new Runnable() {
        @Override
        public void run() {
            handleHide();
            if (mContext instanceof Camera) { //stop observe orientation changed.
                ((Camera) mContext).removeOnOrientationListener(OnScreenToast.this);
            }
        }
    };

    /// M: for orientation function.
    private static final boolean LOG = Log.LOGV;
    private static final int TOAST_DURATION = 2000; // milliseconds
    private Context mContext;
    private int mOrientation;
    @Override
    public void onOrientationChanged(int orientation) {
        if (mOrientation != orientation) {
            mOrientation = orientation;
            Util.setOrientation(mView, mOrientation, true);
        }
        
        if (mView != null) {
            if (isLandcape()) {
                ViewGroup.LayoutParams vp = mLayout.getLayoutParams();
                vp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                vp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mLayout.setLayoutParams(vp);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.CENTER_VERTICAL);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                mText.setLayoutParams(params);
            } else {
                ViewGroup.LayoutParams vp = mLayout.getLayoutParams();
                vp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                vp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                mLayout.setLayoutParams(vp);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                mText.setLayoutParams(params);
            }
            mView.requestLayout();
        }
    }
    
    private boolean isLandcape() {
        int orientation = ((Camera) mContext).getOrietation();
        //should be checked for sensor setup orientation
        boolean land = orientation == 90 || orientation == 270;
        if (LOG) {
            Log.v(TAG, "isLandcape() orientation=" + orientation + ", return " + land);
        }
        return land;
    }
    
    public void showToast() {
        if (mNextView == null) {
            throw new RuntimeException("View is not initialized");
        }
        mHandler.removeCallbacks(mShow);
        mHandler.removeCallbacks(mHide);
        mHandler.post(mShow);
        mHandler.postDelayed(mHide, TOAST_DURATION);
    }
}

