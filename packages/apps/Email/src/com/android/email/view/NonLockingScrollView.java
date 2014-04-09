/*
 * Copyright (C) 2011 The Android Open Source Project
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


package com.android.email.view;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
/// M: Add mediatek import. @{
import android.widget.LinearLayout;
/// @}
import android.webkit.WebView;
import android.widget.ScrollView;

import java.util.ArrayList;

import com.android.emailcommon.Logging;

/**
 * A {@link ScrollView} that will never lock scrolling in a particular direction.
 *
 * Usually ScrollView will capture all touch events once a drag has begun. In some cases,
 * we want to delegate those touches to children as normal, even in the middle of a drag. This is
 * useful when there are childviews like a WebView tha handles scrolling in the horizontal direction
 * even while the ScrollView drags vertically.
 *
 * This is only tested to work for ScrollViews where the content scrolls in one direction.
 */
public class NonLockingScrollView extends ScrollView {
    private int mContentLen = 0;
    private int mViewHeight = 0;
    private int mBottomHeight = 0;
    private long mElapsedOverScrolled = 0;
    private static final long TIME_ELAPSED = 500;
    private boolean mToggleLoading = false;
    private Message mMsg = null;
    private static final int OVERSCROLLED_MSG = 200;
    private float mMaximumVelocity;

    /// M: use this rate to disable horizontal shake and velocity @{
    private static final float MAX_VELOCITY_RATE = 1.2f;
    private static final int VELOCITY_SCALE = 5;
    /// @}

    private OnOverScrollListener mOnOverScrollListener;

    public NonLockingScrollView(Context context) {
        super(context);
    }
    public NonLockingScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }
    public NonLockingScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private boolean mIsScrollFreeze;

    public void scrollFreeze() {
        mIsScrollFreeze = true;
    }

    public boolean isScrollFreeze() {
        return mIsScrollFreeze;
    }

    @Override
    public void scrollTo(int x, int y) {
        if (mIsScrollFreeze) {
            // Actually I don't know why need this.
            super.scrollTo(0, 0);
        } else {
            super.scrollTo(x, y);
        }
    }

    private Handler mHandler = new Handler() {
        boolean mOverHandleInterval = true;
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what) {
            case OVERSCROLLED_MSG:
                if (!mOverHandleInterval) {
                    break;
                }
                mOverHandleInterval = false;
                Logging.d("NonLockingScrollView handleMessage toggling loading actually !!! spend "
                        + (System.currentTimeMillis() - mElapsedOverScrolled) + " ms");
                mOnOverScrollListener.onOverScrolled();
                mToggleLoading = false;
                mElapsedOverScrolled = 0;
                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        mOverHandleInterval = true;
                    }

                }, 1000);
                break;
            default:
                break;
            }
        }
    };

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX,
            boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        mContentLen = getChildAt(0).getHeight();
        mViewHeight = getHeight();
        if (clampedY && (scrollY > 0 || mContentLen <= mViewHeight)) {
            mMsg = new Message();
            mMsg.what = OVERSCROLLED_MSG;
            mHandler.sendMessage(mMsg);
        }
    }

    public boolean getToggleLoading() {
        return mToggleLoading;
    }

    /**
     * Whether or not the contents of this view is being dragged by one of the children in
     * {@link #mChildrenNeedingAllTouches}.
     */
    private boolean mInCustomDrag = false;
    /**
     * When the Y axis velocity beyond INTERCEPT_MAX_VELOCITY, we should intercept
     * the following events to webview. Cause we need the scroll fling not shaking
     * right or left.
     */
    private boolean mInterceptXMotion = false;
    private static final int INTERCEPT_MAX_VELOCITY = 2000;
    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;

    /**
     * The list of children who should always receive touch events, and not have them intercepted.
     */
    private final ArrayList<View> mChildrenNeedingAllTouches = new ArrayList<View>();

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mIsScrollFreeze = false;
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);
        final int action = ev.getActionMasked();

        /// M: Added for search functionality in webview for email. tell
        /// webview what's its current visible parameters inside ScrollView. @{
        boolean isOver = false;
        int topY = 0;
        int y = this.getScrollY();
        LinearLayout lLayout = (LinearLayout) this.getChildAt(0);
        if (null != lLayout) {
            int count = lLayout.getChildCount();
            Rect visibleRect = new Rect(0, 0, 0, 0);
            WebView rigidWebView = null;
            for (int i = 0; i < count; i++) {
                View tempchild = (View) lLayout.getChildAt(i);
                if (tempchild instanceof WebView) {
                    rigidWebView = (WebView) tempchild;
                    rigidWebView.getGlobalVisibleRect(visibleRect);
                    topY = rigidWebView.getTop();
                }
            }

            if (rigidWebView != null) {
                isOver = y > topY ? true : false;
                rigidWebView.setVisibleParameterForEmail(visibleRect.width(),
                        visibleRect.height(), isOver, topY);
            }
        }
        /// @}

        /// M: Do the velocity computing to improve the performance of read mail. @{
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE:
                needInterceptXMotion(ev);
                break;
            case MotionEvent.ACTION_UP: 
                recycleVelocityTracker();
                mInterceptXMotion = false;
                break;
            default :
                break;
        }
        /// @}
        return super.onTouchEvent(ev);
    }

    /**
     * M: Intercept the historical scroll to avoid webview shake left and right,
     * when user scroll the webview fastly with a angle of slope.
     * Locking historical scroll with the following conditions:
     * 1) The slope (YVelocity/XVelocity) should be bigger than MAX_VELOCITY_RATE,
     * 2) YVelocity && XVelocity > mMaximumVelocity/VELOCITY_SCALE.
     */
    private void needInterceptXMotion(MotionEvent ev) {
        if (ev.getPointerCount() > 1) {
            // Not handle multi-touch event.
            mInterceptXMotion = false;
            return;
        }

        mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
        float scrollVelocityY = (float) mVelocityTracker.getYVelocity(ev
                .getPointerId(0));
        float scrollVelocityX = (float) mVelocityTracker.getXVelocity(ev
                .getPointerId(0));

        float threshold = mMaximumVelocity / VELOCITY_SCALE;
        if (Math.abs(scrollVelocityY) <= threshold
                && Math.abs(scrollVelocityX) <= threshold) {
            // Only handle fast scroll.
            mInterceptXMotion = false;
            return;
        }

        float rate = scrollVelocityY / scrollVelocityX;
        if (Math.abs(rate) >= MAX_VELOCITY_RATE) {
            mInterceptXMotion = true;
        } else {
            mInterceptXMotion = false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        final boolean isUp = action == MotionEvent.ACTION_UP;

        // Note: the normal scrollview implementation is to intercept all touch
        // events after it has detected a drag starting. But we will handle all touch events for
        // some special child views(e.g. horizontal scrolling views or webview ).
        if (!isEventOverChild(ev, mChildrenNeedingAllTouches)) {
            return super.onInterceptTouchEvent(ev);
        }else {
            onTouchEvent(ev);
            // Whether intercepting events to webview depends on Y axis scroll speed.
            return mInterceptXMotion;
        }
/*       For views which were in mChildrenNeedingAllTouches , we intercept up event only.
        if (isUp && mInCustomDrag) {
            // An up event after a drag should be intercepted so that child views don't handle
            // click events falsely after a drag.
            mInCustomDrag = false;
            onTouchEvent(ev);
            
            return true;
        }

        if (!mInCustomDrag && !isEventOverChild(ev, mChildrenNeedingAllTouches)) {
            return super.onInterceptTouchEvent(ev);
        }

        // Note the normal scrollview implementation is to intercept all touch events after it has
        // detected a drag starting. We will handle this ourselves.
        mInCustomDrag = super.onInterceptTouchEvent(ev);
        if (mInCustomDrag) {
            onTouchEvent(ev);
        }

        // Don't intercept events - pass them on to children as normal.
        return false;*/
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        excludeChildrenFromInterceptions(this);
    }

    /**
     * Traverses the view tree for {@link WebView}s so they can be excluded from touch
     * interceptions and receive all events.
     */
    private void excludeChildrenFromInterceptions(View node) {
        // If additional types of children should be excluded (e.g. horizontal scrolling banners),
        // this needs to be modified accordingly.
        if (node instanceof WebView) {
            mChildrenNeedingAllTouches.add(node);
        } else if (node instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) node;
            final int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = viewGroup.getChildAt(i);
                excludeChildrenFromInterceptions(child);
            }
        }
    }

    private static final Rect sHitFrame = new Rect();
    private boolean isEventOverChild(MotionEvent ev, ArrayList<View> children) {
        final int actionIndex = ev.getActionIndex();
        final float x = ev.getX(actionIndex) + getScrollX();
        final float y = ev.getY(actionIndex) + getScrollY();

        for (View child : children) {
            if (!canViewReceivePointerEvents(child)) {
                continue;
            }
            child.getHitRect(sHitFrame);

           // child can receive the motion event.
            if (sHitFrame.contains((int) x, (int) y)) {
                return true;
            }
        }
        return false;
    }

    private boolean canViewReceivePointerEvents(View child) {
        return child.getVisibility() == VISIBLE || (child.getAnimation() != null);
    }

    public interface OnOverScrollListener {
        public void onOverScrolled();
    }

    public void setOnOverScrollListener(OnOverScrollListener onOverScrollListener) {
        this.mOnOverScrollListener = onOverScrollListener;
    }
}
