/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.webkit;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;

/// M: Add mediatek import. @{
import android.view.ViewParent;
import android.widget.ScrollView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
/// @}

import android.os.Bundle;
import android.os.SystemClock;
import android.util.FloatMath;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * The ZoomManager is responsible for maintaining the WebView's current zoom
 * level state.  It is also responsible for managing the on-screen zoom controls
 * as well as any animation of the WebView due to zooming.
 *
 * Currently, there are two methods for animating the zoom of a WebView.
 *
 * (1) The first method is triggered by startZoomAnimation(...) and is a fixed
 * length animation where the final zoom scale is known at startup.  This type of
 * animation notifies webkit of the final scale BEFORE it animates. The animation
 * is then done by scaling the CANVAS incrementally based on a stepping function.
 *
 * (2) The second method is triggered by a multi-touch pinch and the new scale
 * is determined dynamically based on the user's gesture. This type of animation
 * only notifies webkit of new scale AFTER the gesture is complete. The animation
 * effect is achieved by scaling the VIEWS (both WebView and ViewManager.ChildView)
 * to the new scale in response to events related to the user's gesture.
 */
class ZoomManager {

    static final String LOGTAG = "webviewZoom";

    private final WebViewClassic mWebView;
    private final CallbackProxy mCallbackProxy;

    // Widgets responsible for the on-screen zoom functions of the WebView.
    private ZoomControlEmbedded mEmbeddedZoomControl;
    private ZoomControlExternal mExternalZoomControl;

    /*
     * The scale factors that determine the upper and lower bounds for the
     * default zoom scale.
     */
    protected static final float DEFAULT_MAX_ZOOM_SCALE_FACTOR = 4.00f;
    protected static final float DEFAULT_MIN_ZOOM_SCALE_FACTOR = 0.25f;

    // The default scale limits, which are dependent on the display density.
    private float mDefaultMaxZoomScale;
    private float mDefaultMinZoomScale;

    // The actual scale limits, which can be set through a webpage's viewport
    // meta-tag.
    private float mMaxZoomScale;
    private float mMinZoomScale;

    // Locks the minimum ZoomScale to the value currently set in mMinZoomScale.
    private boolean mMinZoomScaleFixed = true;

    /*
     * When loading a new page the WebView does not initially know the final
     * width of the page. Therefore, when a new page is loaded in overview mode
     * the overview scale is initialized to a default value. This flag is then
     * set and used to notify the ZoomManager to take the width of the next
     * picture from webkit and use that width to enter into zoom overview mode.
     */
    private boolean mInitialZoomOverview = false;

    /*
     * When in the zoom overview mode, the page's width is fully fit to the
     * current window. Additionally while the page is in this state it is
     * active, in other words, you can click to follow the links. We cache a
     * boolean to enable us to quickly check whether or not we are in overview
     * mode, but this value should only be modified by changes to the zoom
     * scale.
     */
    private boolean mInZoomOverview = false;
    private int mZoomOverviewWidth;
    private float mInvZoomOverviewWidth;

    /*
     * These variables track the center point of the zoom and they are used to
     * determine the point around which we should zoom. They are stored in view
     * coordinates.
     */
    private float mZoomCenterX;
    private float mZoomCenterY;

    /*
     * Similar to mZoomCenterX(Y), these track the focus point of the scale
     * gesture. The difference is these get updated every time when onScale is
     * invoked no matter if a zooming really happens.
     */
    private float mFocusX;
    private float mFocusY;

    /*
     * mFocusMovementQueue keeps track of the previous focus point movement
     * has been through. Comparing to the difference of the gesture's previous
     * span and current span, it determines if the gesture is for panning or
     * zooming or both.
     */
    private FocusMovementQueue mFocusMovementQueue;

    /*
     * These values represent the point around which the screen should be
     * centered after zooming. In other words it is used to determine the center
     * point of the visible document after the page has finished zooming. This
     * is important because the zoom may have potentially reflowed the text and
     * we need to ensure the proper portion of the document remains on the
     * screen.
     */
    private int mAnchorX;
    private int mAnchorY;

    // The scale factor that is used to determine the column width for text
    private float mTextWrapScale;

    /*
     * The default zoom scale is the scale factor used when the user triggers a
     * zoom in by double tapping on the WebView. The value is initially set
     * based on the display density, but can be changed at any time via the
     * WebSettings.
     */
    private float mDefaultScale;
    private float mInvDefaultScale;

    /*
     * The logical density of the display. This is a scaling factor for the
     * Density Independent Pixel unit, where one DIP is one pixel on an
     * approximately 160 dpi screen (see android.util.DisplayMetrics.density)
     */
    private float mDisplayDensity;

    /*
     * The factor that is used to tweak the zoom scale on a double-tap,
     * and can be changed via WebSettings. Range is from 0.75f to 1.25f.
     */
    private float mDoubleTapZoomFactor = 1.0f;

    /*
     * The scale factor that is used as the minimum increment when going from
     * overview to reading level on a double tap.
     */
    private static float MIN_DOUBLE_TAP_SCALE_INCREMENT = 0.5f;

    // the current computed zoom scale and its inverse.
    private float mActualScale;
    private float mInvActualScale;
    
    /*
     * The initial scale for the WebView. 0 means default. If initial scale is
     * greater than 0, the WebView starts with this value as its initial scale.
     */
    private float mInitialScale;

    private static float MINIMUM_SCALE_INCREMENT = 0.007f;

    /*
     *  The touch points could be changed even the fingers stop moving.
     *  We use the following to filter out the zooming jitters.
     */
    private static float MINIMUM_SCALE_WITHOUT_JITTER = 0.007f;

    /*
     *  We use the following scale factor base to adapt the filter
     *  based on MINIMUM_SCALE_WITHOUT_JITTER
     */
    private static float JITTER_SCALE_FACTOR_BASE = 3.9f;

    /*
     *  ScrollView focus y of E-Mail AP
     */
    private float mScrollViewFocusYforEmail = 0.0f;

    /*
     * The following member variables are only to be used for animating zoom. If
     * mZoomScale is non-zero then we are in the middle of a zoom animation. The
     * other variables are used as a cache (e.g. inverse) or as a way to store
     * the state of the view prior to animating (e.g. initial scroll coords).
     */
    private float mZoomScale;
    private float mInvInitialZoomScale;
    private float mInvFinalZoomScale;
    private int mInitialScrollX;
    private int mInitialScrollY;
    private long mZoomStart;

    private static final int ZOOM_ANIMATION_LENGTH = 175;

    // whether support multi-touch
    private boolean mSupportMultiTouch;
    
    /**
     * True if we have a touch panel capable of detecting smooth pan/scale at the same time
     */
    private boolean mAllowPanAndScale;

    // use the framework's ScaleGestureDetector to handle scaling gestures
    private ScaleGestureDetector mScaleDetector;
    private boolean mPinchToZoomAnimating = false;

    private boolean mHardwareAccelerated = false;
    private boolean mInHWAcceleratedZoom = false;

    public ZoomManager(WebViewClassic webView, CallbackProxy callbackProxy) {
        mWebView = webView;
        mCallbackProxy = callbackProxy;

        /*
         * Ideally mZoomOverviewWidth should be mContentWidth. But sites like
         * ESPN and Engadget always have wider mContentWidth no matter what the
         * viewport size is.
         */
        setZoomOverviewWidth(WebViewClassic.DEFAULT_VIEWPORT_WIDTH);

        mFocusMovementQueue = new FocusMovementQueue();
    }

    /**
     * Initialize both the default and actual zoom scale to the given density.
     *
     * @param density The logical density of the display. This is a scaling factor
     * for the Density Independent Pixel unit, where one DIP is one pixel on an
     * approximately 160 dpi screen (see android.util.DisplayMetrics.density).
     */
    public void init(float density) {
        assert density > 0;

        mDisplayDensity = density;
        setDefaultZoomScale(density);
        mActualScale = density;
        mInvActualScale = 1 / density;
        mTextWrapScale = getReadingLevelScale();
    }

    /**
     * Update the default zoom scale using the given density. It will also reset
     * the current min and max zoom scales to the default boundaries as well as
     * ensure that the actual scale falls within those boundaries.
     *
     * @param density The logical density of the display. This is a scaling factor
     * for the Density Independent Pixel unit, where one DIP is one pixel on an
     * approximately 160 dpi screen (see android.util.DisplayMetrics.density).
     */
    public void updateDefaultZoomDensity(float density) {
        assert density > 0;

        if (Math.abs(density - mDefaultScale) > MINIMUM_SCALE_INCREMENT) {
            // Remember the current zoom density before it gets changed.
            final float originalDefault = mDefaultScale;
            // set the new default density
            mDisplayDensity = density;
            setDefaultZoomScale(density);
            float scaleChange = (originalDefault > 0.0) ? density / originalDefault: 1.0f;
            // adjust the scale if it falls outside the new zoom bounds
            setZoomScale(mActualScale * scaleChange, true);
        }
    }

    private void setDefaultZoomScale(float defaultScale) {
        final float originalDefault = mDefaultScale;
        mDefaultScale = defaultScale;
        mInvDefaultScale = 1 / defaultScale;
        mDefaultMaxZoomScale = defaultScale * DEFAULT_MAX_ZOOM_SCALE_FACTOR;
        mDefaultMinZoomScale = defaultScale * DEFAULT_MIN_ZOOM_SCALE_FACTOR;
        if (originalDefault > 0.0 && mMaxZoomScale > 0.0) {
            // Keeps max zoom scale when zoom density changes.
            mMaxZoomScale = defaultScale / originalDefault * mMaxZoomScale;
        } else {
            mMaxZoomScale = mDefaultMaxZoomScale;
        }
        if (originalDefault > 0.0 && mMinZoomScale > 0.0) {
            // Keeps min zoom scale when zoom density changes.
            mMinZoomScale = defaultScale / originalDefault * mMinZoomScale;
        } else {
            mMinZoomScale = mDefaultMinZoomScale;
        }
        if (!exceedsMinScaleIncrement(mMinZoomScale, mMaxZoomScale)) {
            mMaxZoomScale = mMinZoomScale;
        }
        sanitizeMinMaxScales();
    }

    public final float getScale() {
        return mActualScale;
    }

    public final float getInvScale() {
        return mInvActualScale;
    }

    public final float getTextWrapScale() {
        return mTextWrapScale;
    }

    public final float getMaxZoomScale() {
        return mMaxZoomScale;
    }

    public final float getMinZoomScale() {
        return mMinZoomScale;
    }

    public final float getDefaultScale() {
        return mDefaultScale;
    }

    /**
     * Returns the zoom scale used for reading text on a double-tap.
     */
    public final float getReadingLevelScale() {
        return computeScaleWithLimits(computeReadingLevelScale(getZoomOverviewScale()));
    }

    /* package */ final float computeReadingLevelScale(float scale) {
        /// M: If not zoom overview, modify reading level scale. @{
        if (mInZoomOverview) {
            return Math.max(mDisplayDensity * mDoubleTapZoomFactor,
                    scale + MIN_DOUBLE_TAP_SCALE_INCREMENT);
        } else {
            return mDisplayDensity * mDoubleTapZoomFactor;
        }
        /// @}
    }

    public final float getInvDefaultScale() {
        return mInvDefaultScale;
    }

    public final float getDefaultMaxZoomScale() {
        return mDefaultMaxZoomScale;
    }

    public final float getDefaultMinZoomScale() {
        return mDefaultMinZoomScale;
    }

    public final int getDocumentAnchorX() {
        return mAnchorX;
    }

    public final int getDocumentAnchorY() {
        return mAnchorY;
    }

    public final void clearDocumentAnchor() {
        mAnchorX = mAnchorY = 0;
    }

    public final void setZoomCenter(float x, float y) {
        mZoomCenterX = x;
        mZoomCenterY = y;
    }

    public final void setInitialScaleInPercent(int scaleInPercent) {
        mInitialScale = scaleInPercent * 0.01f;
    }

    public final float computeScaleWithLimits(float scale) {
        if (scale < mMinZoomScale) {
            scale = mMinZoomScale;
        } else if (scale > mMaxZoomScale) {
            scale = mMaxZoomScale;
        }
        return scale;
    }

    public final boolean isScaleOverLimits(float scale) {
        return scale <= mMinZoomScale || scale >= mMaxZoomScale;
    }

    public final boolean isZoomScaleFixed() {
        return mMinZoomScale >= mMaxZoomScale;
    }

    public static final boolean exceedsMinScaleIncrement(float scaleA, float scaleB) {
        return Math.abs(scaleA - scaleB) >= MINIMUM_SCALE_INCREMENT;
    }

    public boolean willScaleTriggerZoom(float scale) {
        return exceedsMinScaleIncrement(scale, mActualScale);
    }

    public final boolean canZoomIn() {
        return mMaxZoomScale - mActualScale > MINIMUM_SCALE_INCREMENT;
    }

    public final boolean canZoomOut() {
        return mActualScale - mMinZoomScale > MINIMUM_SCALE_INCREMENT;
    }

    public boolean zoomIn() {
        return zoom(1.25f);
    }

    public boolean zoomOut() {
        return zoom(0.8f);
    }

    // returns TRUE if zoom out succeeds and FALSE if no zoom changes.
    private boolean zoom(float zoomMultiplier) {
        mInitialZoomOverview = false;
        // TODO: alternatively we can disallow this during draw history mode
        mWebView.switchOutDrawHistory();
        // Center zooming to the center of the screen.
        mZoomCenterX = mWebView.getViewWidth() * .5f;
        mZoomCenterY = mWebView.getViewHeight() * .5f;
        mAnchorX = mWebView.viewToContentX((int) mZoomCenterX + mWebView.getScrollX());
        mAnchorY = mWebView.viewToContentY((int) mZoomCenterY + mWebView.getScrollY());
        return startZoomAnimation(mActualScale * zoomMultiplier, 
            !mWebView.getSettings().getUseFixedViewport());
    }

    /**
     * Initiates an animated zoom of the WebView.
     *
     * @return true if the new scale triggered an animation and false otherwise.
     */
    public boolean startZoomAnimation(float scale, boolean reflowText) {
        mInitialZoomOverview = false;
        float oldScale = mActualScale;
        mInitialScrollX = mWebView.getScrollX();
        mInitialScrollY = mWebView.getScrollY();

        // snap to reading level scale if it is close
        if (!exceedsMinScaleIncrement(scale, getReadingLevelScale())) {
            scale = getReadingLevelScale();
        }

        setZoomScale(scale, reflowText);

        if (oldScale != mActualScale) {
            if (mHardwareAccelerated) {
                mInHWAcceleratedZoom = true;
            }
            // use mZoomPickerScale to see zoom preview first
            mZoomStart = SystemClock.uptimeMillis();
            mInvInitialZoomScale = 1.0f / oldScale;
            mInvFinalZoomScale = 1.0f / mActualScale;
            mZoomScale = mActualScale;
            mWebView.onFixedLengthZoomAnimationStart();
            mWebView.invalidate();
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method is called by the WebView's drawing code when a fixed length zoom
     * animation is occurring. Its purpose is to animate the zooming of the canvas
     * to the desired scale which was specified in startZoomAnimation(...).
     *
     * A fixed length animation begins when startZoomAnimation(...) is called and
     * continues until the ZOOM_ANIMATION_LENGTH time has elapsed. During that
     * interval each time the WebView draws it calls this function which is 
     * responsible for generating the animation.
     *
     * Additionally, the WebView can check to see if such an animation is currently
     * in progress by calling isFixedLengthAnimationInProgress().
     */
    public void animateZoom(Canvas canvas) {
        mInitialZoomOverview = false;
        if (mZoomScale == 0) {
            Log.w(LOGTAG, "A WebView is attempting to perform a fixed length "
                    + "zoom animation when no zoom is in progress");
            // Now that we've logged about it, go ahead and just recover
            mInHWAcceleratedZoom = false;
            return;
        }

        float zoomScale;
        int interval = (int) (SystemClock.uptimeMillis() - mZoomStart);
        if (interval < ZOOM_ANIMATION_LENGTH) {
            float ratio = (float) interval / ZOOM_ANIMATION_LENGTH;
            zoomScale = 1.0f / (mInvInitialZoomScale
                    + (mInvFinalZoomScale - mInvInitialZoomScale) * ratio);
            mWebView.invalidate();
        } else {
            zoomScale = mZoomScale;
            // set mZoomScale to be 0 as we have finished animating
            mZoomScale = 0;
            mWebView.onFixedLengthZoomAnimationEnd();
        }
        // calculate the intermediate scroll position. Since we need to use
        // zoomScale, we can't use the WebView's pinLocX/Y functions directly.
        float scale = zoomScale * mInvInitialZoomScale;
        int tx = Math.round(scale * (mInitialScrollX + mZoomCenterX) - mZoomCenterX);
        tx = -WebViewClassic.pinLoc(tx, mWebView.getViewWidth(), Math.round(mWebView.getContentWidth()
                * zoomScale)) + mWebView.getScrollX();
        int titleHeight = mWebView.getTitleHeight();
        int ty = Math.round(scale
                * (mInitialScrollY + mZoomCenterY - titleHeight)
                - (mZoomCenterY - titleHeight));
        ty = -(ty <= titleHeight ? Math.max(ty, 0) : WebViewClassic.pinLoc(ty
                - titleHeight, mWebView.getViewHeight(), Math.round(mWebView.getContentHeight()
                * zoomScale)) + titleHeight) + mWebView.getScrollY();

        if (mHardwareAccelerated) {
            mWebView.updateScrollCoordinates(mWebView.getScrollX() - tx, mWebView.getScrollY() - ty);
            // By adding webView matrix, we need to offset the canvas a bit
            // to make the animation smooth.
            canvas.translate(tx, ty);
            setZoomScale(zoomScale, false);

            if (mZoomScale == 0) {
                // We've reached the end of the zoom animation.
                mInHWAcceleratedZoom = false;

                // Ensure that the zoom level is pushed to WebCore. This has not
                // yet occurred because we prevent it from happening while
                // mInHWAcceleratedZoom is true.
                mWebView.sendViewSizeZoom(false);
            }
        } else {
            canvas.translate(tx, ty);
            canvas.scale(zoomScale, zoomScale);
        }
    }

    public boolean isZoomAnimating() {
        return isFixedLengthAnimationInProgress() || mPinchToZoomAnimating;
    }

    public boolean isFixedLengthAnimationInProgress() {
        return mZoomScale != 0 || mInHWAcceleratedZoom;
    }

    /// M: Used only when WebView is used by Email app, to get ScrollView @{
    ScrollView getEmailScrollView() {
        ScrollView sv = null;

        if (mWebView.isEmailUsing()) {
            ViewParent parent = mWebView.getWebView().getParent();
            while (parent != null) {
                if (parent instanceof ScrollView) {
                    sv = (ScrollView) parent;

                    break;
                }

                if (parent instanceof View) {
                    parent = ((View) parent).getParent();
                } else {
                    parent = null;
                }
            }
        }

        return sv;
    }
    /// @}

    /// M: ALPS00378288 improve Pinch Zoom performance of E-Mail APK Phase 2 @{
    public int getLimitScrollToValueOfEmailAp(int dy)
    {
        int MaxHeight = 0;
        int ScrollBarHeight = 0;
        ScrollView localScrollView;
        ViewParent localViewParent = mWebView.getWebView().getParent();

        while (localViewParent != null) {
            if (localViewParent instanceof ScrollView) {
                localScrollView = (ScrollView) localViewParent;

                Rect localRect = new Rect();
                localScrollView.getGlobalVisibleRect(localRect);
                ScrollBarHeight = localRect.bottom - localRect.top;

                LinearLayout localLinearLayout = (LinearLayout)localScrollView.getChildAt(0);

                if (localLinearLayout != null) {
                    for (int k = 0; k < localLinearLayout.getChildCount(); k++) {
                        if ((localLinearLayout.getChildAt(k) instanceof ViewGroup)) {
                            MaxHeight += ((android.view.ViewGroup) localLinearLayout.getChildAt(k)).getHeight();
                        }
                    }

                    if (localScrollView.getScrollY() - dy < 0) {
                        dy = localScrollView.getScrollY();
                    } else if (localScrollView.getScrollY() - dy > (MaxHeight - ScrollBarHeight)) {
                        dy = 0;
                    }
                }
                break;
            }

            if (localViewParent instanceof View) {
                localViewParent = ((View) localViewParent).getParent();
            } else {
                localViewParent = null;
            }
        }

        return dy;
    }
    /// @}

    public void updateDoubleTapZoom(int doubleTapZoom) {
        boolean zoomIn = (mTextWrapScale - mActualScale) < .1f;
        mDoubleTapZoomFactor = doubleTapZoom / 100.0f;
        mTextWrapScale = getReadingLevelScale();
        float newScale = zoomIn ? mTextWrapScale
                : Math.min(mTextWrapScale, mActualScale);
        setZoomScale(newScale, true, true);
    }

    public void refreshZoomScale(boolean reflowText) {
        setZoomScale(mActualScale, reflowText, true);
    }

    public void setZoomScale(float scale, boolean reflowText) {
        setZoomScale(scale, reflowText, false);
    }

    private void setZoomScale(float scale, boolean reflowText, boolean force) {
        final boolean isScaleLessThanMinZoom = scale < mMinZoomScale;
        scale = computeScaleWithLimits(scale);

        // determine whether or not we are in the zoom overview mode
        if (isScaleLessThanMinZoom && mMinZoomScale < mDefaultScale) {
            mInZoomOverview = true;
        } else {
            mInZoomOverview = !exceedsMinScaleIncrement(scale, getZoomOverviewScale());
        }

        if (reflowText && !mWebView.getSettings().getUseFixedViewport()) {
            mTextWrapScale = scale;
        }

        if (scale != mActualScale || force) {
            float oldScale = mActualScale;
            float oldInvScale = mInvActualScale;

            if (scale != mActualScale && !mPinchToZoomAnimating) {
                mCallbackProxy.onScaleChanged(mActualScale, scale);
            }

            mActualScale = scale;
            mInvActualScale = 1 / scale;

            if (!mWebView.drawHistory() && !mInHWAcceleratedZoom) {

                // If history Picture is drawn, don't update scroll. They will
                // be updated when we get out of that mode.
                // update our scroll so we don't appear to jump
                // i.e. keep the center of the doc in the center of the view
                // If this is part of a zoom on a HW accelerated canvas, we
                // have already updated the scroll so don't do it again.
                int oldX = mWebView.getScrollX();
                int oldY = mWebView.getScrollY();
                float ratio = scale * oldInvScale;
                float sx = ratio * oldX + (ratio - 1) * mZoomCenterX;
                /// M: need computed differently when WebView is used by Email app
                float sy = 0f;

                /// M: computed when WebView is used by Email app
                if (mWebView.isEmailUsing()) {
                    /// M: ALPS00378288 improve Pinch Zoom performance of E-Mail APK Phase 2 @{
                    if (mZoomCenterY != 0.0f) {
                        sy = (ratio - 1.0f) * mScrollViewFocusYforEmail - (mZoomCenterY - mScrollViewFocusYforEmail);
                        mScrollViewFocusYforEmail = mZoomCenterY + sy;
                    }
                    /// @}
                } else {
                    sy = ratio * oldY + (ratio - 1) * (mZoomCenterY - mWebView.getTitleHeight());
                }

                // Scale all the child views
                mWebView.mViewManager.scaleAll();

                // as we don't have animation for scaling, don't do animation
                // for scrolling, as it causes weird intermediate state
                int scrollX = mWebView.pinLocX(Math.round(sx));
                /// M: if it is used by Email application, the y coordinate should be the same as the origination.
                int scrollY = 0;
                if (mWebView.isEmailUsing()) {
                    // When it is used by email, we should not adjust the coordinate y.
                    scrollY = Math.round(sy);
                } else {
                    // note: Google original implementation.
                    scrollY = mWebView.pinLocY(Math.round(sy));
                }
                if(!mWebView.updateScrollCoordinates(scrollX, scrollY)) {
                    // the scroll position is adjusted at the beginning of the
                    // zoom animation. But we want to update the WebKit at the
                    // end of the zoom animation. See comments in onScaleEnd().
                    mWebView.sendOurVisibleRect();
                }
            }

            // if the we need to reflow the text then force the VIEW_SIZE_CHANGED
            // event to be sent to WebKit
            mWebView.sendViewSizeZoom(reflowText);
        }
    }

    public boolean isDoubleTapEnabled() {
        WebSettings settings = mWebView.getSettings();
        return settings != null && settings.getUseWideViewPort();
    }

    /**
     * The double tap gesture can result in different behaviors depending on the
     * content that is tapped.
     *
     * (1) PLUGINS: If the taps occur on a plugin then we maximize the plugin on
     * the screen. If the plugin is already maximized then zoom the user into
     * overview mode.
     *
     * (2) HTML/OTHER: If the taps occur outside a plugin then the following
     * heuristic is used.
     *   A. If the current text wrap scale differs from newly calculated and the
     *      layout algorithm specifies the use of NARROW_COLUMNS, then fit to
     *      column by reflowing the text.
     *   B. If the page is not in overview mode then change to overview mode.
     *   C. If the page is in overmode then change to the default scale.
     */
    public void handleDoubleTap(float lastTouchX, float lastTouchY) {
        // User takes action, set initial zoom overview to false.
        mInitialZoomOverview = false;
        WebSettingsClassic settings = mWebView.getSettings();
        if (!isDoubleTapEnabled()) {
            return;
        }

        setZoomCenter(lastTouchX, lastTouchY);
        mAnchorX = mWebView.viewToContentX((int) lastTouchX + mWebView.getScrollX());
        mAnchorY = mWebView.viewToContentY((int) lastTouchY + mWebView.getScrollY());
        settings.setDoubleTapToastCount(0);

        // remove the zoom control after double tap
        dismissZoomPicker();

        final float newTextWrapScale;
        if (settings.getUseFixedViewport()) {
            newTextWrapScale = Math.max(mActualScale, getReadingLevelScale());
        } else {
            newTextWrapScale = mActualScale;
        }
        final boolean firstTimeReflow = !exceedsMinScaleIncrement(mActualScale, mTextWrapScale);
        if (firstTimeReflow || mInZoomOverview) {
            // In case first time reflow or in zoom overview mode, let reflow and zoom
            // happen at the same time.
            mTextWrapScale = newTextWrapScale;
        }
        if (settings.isNarrowColumnLayout()
                && exceedsMinScaleIncrement(mTextWrapScale, newTextWrapScale)
                /// M: if benchmark app force zoom
                && (!firstTimeReflow && !mWebView.isBenchmarkApp())
                && !mInZoomOverview) {
            // Reflow only.
            mTextWrapScale = newTextWrapScale;
            refreshZoomScale(true);
        } else if (!mInZoomOverview && willScaleTriggerZoom(getZoomOverviewScale())) {
            // Reflow, if necessary.
            if (mTextWrapScale > getReadingLevelScale()) {
                mTextWrapScale = getReadingLevelScale();
                refreshZoomScale(true);
            }
            zoomToOverview();
        } else {
            zoomToReadingLevel();
        }
    }

    private void setZoomOverviewWidth(int width) {
        if (width == 0) {
            mZoomOverviewWidth = WebViewClassic.DEFAULT_VIEWPORT_WIDTH;
        } else {
            mZoomOverviewWidth = width;
        }
        mInvZoomOverviewWidth = 1.0f / width;
    }

    /* package */ float getZoomOverviewScale() {
        return mWebView.getViewWidth() * mInvZoomOverviewWidth;
    }

    public boolean isInZoomOverview() {
        return mInZoomOverview;
    }

    private void zoomToOverview() {
        // Force the titlebar fully reveal in overview mode
        int scrollY = mWebView.getScrollY();
        if (scrollY < mWebView.getTitleHeight()) {
            mWebView.updateScrollCoordinates(mWebView.getScrollX(), 0);
        }
        startZoomAnimation(getZoomOverviewScale(), 
            !mWebView.getSettings().getUseFixedViewport());
    }

    private void zoomToReadingLevel() {
        final float readingScale = getReadingLevelScale();

        int left = mWebView.getBlockLeftEdge(mAnchorX, mAnchorY, readingScale);
        if (left != WebViewClassic.NO_LEFTEDGE) {
            // add a 5pt padding to the left edge.
            int viewLeft = mWebView.contentToViewX(left < 5 ? 0 : (left - 5))
                    - mWebView.getScrollX();
            // Re-calculate the zoom center so that the new scroll x will be
            // on the left edge.
            if (viewLeft > 0) {
                mZoomCenterX = viewLeft * readingScale / (readingScale - mActualScale);
            } else {
                mWebView.getWebView().scrollBy(viewLeft, 0);
                mZoomCenterX = 0;
            }
        }
        startZoomAnimation(readingScale,
            !mWebView.getSettings().getUseFixedViewport());
    }

    public void updateMultiTouchSupport(Context context) {
        // check the preconditions
        assert mWebView.getSettings() != null;

        final WebSettings settings = mWebView.getSettings();
        final PackageManager pm = context.getPackageManager();
        mSupportMultiTouch = 
                (pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH)
                 || pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT))
                && settings.supportZoom() && settings.getBuiltInZoomControls();
        mAllowPanAndScale =
                pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT)
                || pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT);

        if (mSupportMultiTouch && (mScaleDetector == null)) {
            mScaleDetector = new ScaleGestureDetector(context, new ScaleDetectorListener());
        } else if (!mSupportMultiTouch && (mScaleDetector != null)) {
            mScaleDetector = null;
        }
    }

    public boolean supportsMultiTouchZoom() {
        return mSupportMultiTouch;
    }

    public boolean supportsPanDuringZoom() {
        return mAllowPanAndScale;
    }

    /**
     * Notifies the caller that the ZoomManager is requesting that scale related
     * updates should not be sent to webkit. This can occur in cases where the
     * ZoomManager is performing an animation and does not want webkit to update
     * until the animation is complete.
     *
     * @return true if scale related updates should not be sent to webkit and
     *         false otherwise.
     */
    public boolean isPreventingWebkitUpdates() {
        // currently only animating a multi-touch zoom and fixed length
        // animations prevent updates, but others can add their own conditions
        // to this method if necessary.
        return isZoomAnimating();
    }

    public ScaleGestureDetector getScaleGestureDetector() {
        return mScaleDetector;
    }

    private class FocusMovementQueue {
        private static final int QUEUE_CAPACITY = 5;
        private float[] mQueue;
        private float mSum;
        private int mSize;
        private int mIndex;

        FocusMovementQueue() {
            mQueue = new float[QUEUE_CAPACITY];
            mSize = 0;
            mSum = 0;
            mIndex = 0;
        }

        private void clear() {
            mSize = 0;
            mSum = 0;
            mIndex = 0;
            for (int i = 0; i < QUEUE_CAPACITY; ++i) {
                mQueue[i] = 0;
            }
        }

        private void add(float focusDelta) {
            mSum += focusDelta;
            if (mSize < QUEUE_CAPACITY) {  // fill up the queue.
                mSize++;
            } else {  // circulate the queue.
                mSum -= mQueue[mIndex];
            }
            mQueue[mIndex] = focusDelta;
            mIndex = (mIndex + 1) % QUEUE_CAPACITY;
        }

        private float getSum() {
            return mSum;
        }
    }

    /// M: ALPS00337875 improve the pinch zoom in/out performance @{
    public interface ScaleFilterAlgorithm {
        /**
         * Responds to the beginning of a scaling gesture for filtering
         *
         * @param detects transformation gestures
         */
        public void onFilterBegin(ScaleGestureDetector detector);

        /**
         * Processing the scale when gesture detected
         *
         * @param detects transformation gestures
         * @return the "scale factor" from the previous scale event to the current event
         */
        public float onFilter(ScaleGestureDetector detector);

        /**
         * Returns x-axis representation of the focus(center) of the associated
         * gesture, it is used for the zoom in/out focus
         *
         * @return the value of x-axis
         */
        public float getFocusX();

        /**
         * Returns y-axis representation of the focus(center) of the associated
         * gesture, it is used for the zoom in/out focus
         *
         * @return the value of y-axis
         */
        public float getFocusY();
    }

    private static class LowPassFilter implements ScaleFilterAlgorithm {
        private float mPrevDistanceDiff;
        private float mPrevXDistanceDiff;
        private float mPrevYDistanceDiff;
        private float mPrevFocusX;
        private float mPrevFocusY;

        // alpha value for the low pass filter
        private static final float ALPHA_FACTOR = 0.75f;

        public void onFilterBegin(ScaleGestureDetector detector) {
            // init v(t-1) = 0
            mPrevDistanceDiff = 0.0f;
            mPrevXDistanceDiff = 0.0f;
            mPrevYDistanceDiff = 0.0f;
            mPrevFocusX = detector.getFocusX();
            mPrevFocusY = detector.getFocusY();
        }

        public float onFilter(ScaleGestureDetector detector) {
            float currentDistanceDiff = 0.0f;
            float previousDistance = detector.getPreviousSpan();
            float scale = 0.0f;

            // applied low pass filter v(t) = a x v(t-1) + (1-a) x vs(t) based on velocity
            // consider the distance different as it's velocity

            // processing scale
            currentDistanceDiff = detector.getCurrentSpan() - previousDistance;
            currentDistanceDiff = ALPHA_FACTOR * mPrevDistanceDiff + (1 - ALPHA_FACTOR) * currentDistanceDiff;
            mPrevDistanceDiff = currentDistanceDiff;
            scale = Math.abs((previousDistance + currentDistanceDiff) / previousDistance);

            // processing x-axis
            currentDistanceDiff = detector.getFocusX() - mPrevFocusX;
            currentDistanceDiff = ALPHA_FACTOR * mPrevXDistanceDiff + (1 - ALPHA_FACTOR) * currentDistanceDiff;
            mPrevXDistanceDiff = currentDistanceDiff;
            mPrevFocusX += mPrevXDistanceDiff;

            // processing y-axis
            currentDistanceDiff = detector.getFocusY() - mPrevFocusY;
            currentDistanceDiff = ALPHA_FACTOR * mPrevYDistanceDiff + (1 - ALPHA_FACTOR) * currentDistanceDiff;
            mPrevYDistanceDiff = currentDistanceDiff;
            mPrevFocusY += mPrevYDistanceDiff;

            return scale;
        }

        public float getFocusX() {
            return mPrevFocusX;
        }

        public float getFocusY() {
            return mPrevFocusY;
        }
    }
    /// @}

    private class ScaleDetectorListener implements ScaleGestureDetector.OnScaleGestureListener {
        private float mAccumulatedSpan;
        /// M: ALPS00337875 improve the pinch zoom in/out performance @{
        private ScaleFilterAlgorithm mScaleFilterAlgorithm = new LowPassFilter();
        /// @}
        /// M: ALPS00378288 improve Pinch Zoom performance of E-Mail APK Phase 2 @{
        private boolean hasScrollToBefore = false;
        private float preFocusX = 0.0f;
        /// @}

        public boolean onScaleBegin(ScaleGestureDetector detector) {
            /// M: When WebView is used by Email app, should disallow ScrollView intercept touch events @{
            if (mWebView.isEmailUsing()) {
                ScrollView sv = getEmailScrollView();

                if (sv != null) {
                    sv.requestDisallowInterceptTouchEvent(true);
                }
            }
            /// @}

            mInitialZoomOverview = false;
            dismissZoomPicker();
            mFocusMovementQueue.clear();
            mFocusX = detector.getFocusX();
            mFocusY = detector.getFocusY();
            mWebView.mViewManager.startZoom();
            mWebView.onPinchToZoomAnimationStart();
            mAccumulatedSpan = 0;

            /// M: ALPS00337875 improve the pinch zoom in/out performance @{
            mScaleFilterAlgorithm.onFilterBegin(detector);
            /// @}
            /// M: ALPS00378288 improve Pinch Zoom performance of E-Mail APK Phase 2 @{
            hasScrollToBefore = true;
            preFocusX = detector.getFocusX();
            mScrollViewFocusYforEmail = detector.getFocusY();
            /// @}
            return true;
        }

            // If the user moves the fingers but keeps the same distance between them,
            // we should do panning only.
        public boolean isPanningOnly(ScaleGestureDetector detector) {
            /// M: ALPS00352787 improve Pinch Zoom performance of E-Mail APK @{
            if (mWebView.isEmailUsing()) {
                return false;
            }
            /// @}

            float prevFocusX = mFocusX;
            float prevFocusY = mFocusY;
            mFocusX = detector.getFocusX();
            mFocusY = detector.getFocusY();
            float focusDelta = (prevFocusX == 0 && prevFocusY == 0) ? 0 :
                    FloatMath.sqrt((mFocusX - prevFocusX) * (mFocusX - prevFocusX)
                                   + (mFocusY - prevFocusY) * (mFocusY - prevFocusY));
            mFocusMovementQueue.add(focusDelta);
            float deltaSpan = detector.getCurrentSpan() - detector.getPreviousSpan() +
                    mAccumulatedSpan;
            final boolean result = mFocusMovementQueue.getSum() > Math.abs(deltaSpan);
            if (result) {
                mAccumulatedSpan += deltaSpan;
            } else {
                mAccumulatedSpan = 0;
            }
            return result;
        }

        public boolean handleScale(ScaleGestureDetector detector) {
            float scale;

            /// M: ALPS00337875 improve the pinch zoom in/out performance @{
            scale = mScaleFilterAlgorithm.onFilter(detector) * mActualScale;
            /// @}

            // if scale is limited by any reason, don't zoom but do ask
            // the detector to update the event.
            boolean isScaleLimited =
                    isScaleOverLimits(scale) || scale < getZoomOverviewScale();

            // Prevent scaling beyond overview scale.
            scale = Math.max(computeScaleWithLimits(scale), getZoomOverviewScale());

            if (mPinchToZoomAnimating || willScaleTriggerZoom(scale)) {
                mPinchToZoomAnimating = true;
                // limit the scale change per step
                if (scale > mActualScale) {
                    scale = Math.min(scale, mActualScale * 1.25f);
                } else {
                    scale = Math.max(scale, mActualScale * 0.8f);
                }
                scale = computeScaleWithLimits(scale);
                // if the scale change is too small, regard it as jitter and skip it.
                /// M: ALPS00337875 improve the pinch zoom in/out performance @{
                if (Math.abs(scale - mActualScale) / mActualScale <
                        MINIMUM_SCALE_WITHOUT_JITTER / JITTER_SCALE_FACTOR_BASE) {
                    return isScaleLimited;
                }
                /// M: ALPS00378288 improve Pinch Zoom performance of E-Mail APK Phase 2 @{
                if (mWebView.isEmailUsing()) {
                    setZoomCenter(detector.getFocusX(), detector.getFocusY());
                    hasScrollToBefore = true;
                } else {
                    setZoomCenter(mScaleFilterAlgorithm.getFocusX(), mScaleFilterAlgorithm.getFocusY());
                }
                /// @}
                /// @}
                setZoomScale(scale, false);
                mWebView.invalidate();
                return true;
            }
            return isScaleLimited;
        }

        public boolean onScale(ScaleGestureDetector detector) {
            /// M: ALPS00378288 improve Pinch Zoom performance of E-Mail APK Phase 2 @{
            boolean result = false;

            if (mWebView.isEmailUsing()) {
                mWebView.pinScrollByDx((-1) * (int) (detector.getFocusX() - preFocusX));
                preFocusX = detector.getFocusX();
            }
            hasScrollToBefore = false;
            /// @}

            if (isPanningOnly(detector) || handleScale(detector)) {
                mFocusMovementQueue.clear();
                result = true;
            }

            /// M: ALPS00378288 improve Pinch Zoom performance of E-Mail APK Phase 2 @{
            if (mWebView.isEmailUsing() && (!hasScrollToBefore)) {
                ScrollView sv = getEmailScrollView();
                if (sv != null) {
                    int dy = (int) (detector.getFocusY() - mScrollViewFocusYforEmail);
                    dy = getLimitScrollToValueOfEmailAp(dy);
                    if (dy != 0) {
                        sv.scrollTo(sv.getScrollX(), sv.getScrollY() - dy);
                    }
                    mScrollViewFocusYforEmail = detector.getFocusY() - dy;
                }
            }
            return result;
            /// @}
        }

        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mPinchToZoomAnimating) {
                mPinchToZoomAnimating = false;
                mAnchorX = mWebView.viewToContentX((int) mZoomCenterX + mWebView.getScrollX());
                mAnchorY = mWebView.viewToContentY((int) mZoomCenterY + mWebView.getScrollY());
                // don't reflow when zoom in; when zoom out, do reflow if the
                // new scale is almost minimum scale.
                boolean reflowNow = !canZoomOut() || (mActualScale <= 0.8 * mTextWrapScale);
                // force zoom after mPreviewZoomOnly is set to false so that the
                // new view size will be passed to the WebKit
                refreshZoomScale(reflowNow &&
                    !mWebView.getSettings().getUseFixedViewport());
                // call invalidate() to draw without zoom filter
                mWebView.invalidate();
            }

            mWebView.mViewManager.endZoom();
            mWebView.onPinchToZoomAnimationEnd(detector);
        }
    }

    private void sanitizeMinMaxScales() {
        if (mMinZoomScale > mMaxZoomScale) {
            Log.w(LOGTAG, "mMinZoom > mMaxZoom!!! " + mMinZoomScale + " > " + mMaxZoomScale,
                    new Exception());
            mMaxZoomScale = mMinZoomScale;
        }
    }

    public void onSizeChanged(int w, int h, int ow, int oh) {
        // reset zoom and anchor to the top left corner of the screen
        // unless we are already zooming
        if (!isFixedLengthAnimationInProgress()) {
            int visibleTitleHeight = mWebView.getVisibleTitleHeight();
            mZoomCenterX = 0;
            mZoomCenterY = visibleTitleHeight;
            mAnchorX = mWebView.viewToContentX(mWebView.getScrollX());
            mAnchorY = mWebView.viewToContentY(visibleTitleHeight + mWebView.getScrollY());
        }

        // update mMinZoomScale if the minimum zoom scale is not fixed
        if (!mMinZoomScaleFixed) {
            // when change from narrow screen to wide screen, the new viewWidth
            // can be wider than the old content width. We limit the minimum
            // scale to 1.0f. The proper minimum scale will be calculated when
            // the new picture shows up.
            mMinZoomScale = Math.min(1.0f, (float) mWebView.getViewWidth()
                    / (mWebView.drawHistory() ? mWebView.getHistoryPictureWidth()
                            : mZoomOverviewWidth));
            // limit the minZoomScale to the initialScale if it is set
            if (mInitialScale > 0 && mInitialScale < mMinZoomScale) {
                mMinZoomScale = mInitialScale;
            }
            sanitizeMinMaxScales();
        }

        dismissZoomPicker();

        // onSizeChanged() is called during WebView layout. And any
        // requestLayout() is blocked during layout. As refreshZoomScale() will
        // cause its child View to reposition itself through ViewManager's
        // scaleAll(), we need to post a Runnable to ensure requestLayout().
        // Additionally, only update the text wrap scale if the width changed.

        /// M: ALPS00352787 improve Pinch Zoom performance of E-Mail APK @{
        if (mScaleDetector != null) {
            if (mWebView.isEmailUsing() && mScaleDetector.isInProgress()) {
                return;
            }
        }
        /// @}
        
        mWebView.getWebView().post(new PostScale(w != ow &&
            !mWebView.getSettings().getUseFixedViewport(), mInZoomOverview, w < ow));
    }

    private class PostScale implements Runnable {
        final boolean mUpdateTextWrap;
        // Remember the zoom overview state right after rotation since
        // it could be changed between the time this callback is initiated and
        // the time it's actually run.
        final boolean mInZoomOverviewBeforeSizeChange;
        final boolean mInPortraitMode;

        public PostScale(boolean updateTextWrap,
                         boolean inZoomOverview,
                         boolean inPortraitMode) {
            mUpdateTextWrap = updateTextWrap;
            mInZoomOverviewBeforeSizeChange = inZoomOverview;
            mInPortraitMode = inPortraitMode;
        }

        public void run() {
            if (mWebView.getWebViewCore() != null) {
                // we always force, in case our height changed, in which case we
                // still want to send the notification over to webkit.
                // Keep overview mode unchanged when rotating.
                float newScale = mActualScale;
                if (mWebView.getSettings().getUseWideViewPort() &&
                    mInPortraitMode &&
                    mInZoomOverviewBeforeSizeChange) {
                    newScale = getZoomOverviewScale();
                }
                setZoomScale(newScale, mUpdateTextWrap, true);
                // update the zoom buttons as the scale can be changed
                updateZoomPicker();
            }
        }
    }

    public void updateZoomRange(WebViewCore.ViewState viewState,
            int viewWidth, int minPrefWidth) {
        if (viewState.mMinScale == 0) {
            if (viewState.mMobileSite) {
                if (minPrefWidth > Math.max(0, viewWidth)) {
                    mMinZoomScale = (float) viewWidth / minPrefWidth;
                    mMinZoomScaleFixed = false;
                } else {
                    mMinZoomScale = viewState.mDefaultScale;
                    mMinZoomScaleFixed = true;
                }
            } else {
                mMinZoomScale = mDefaultMinZoomScale;
                mMinZoomScaleFixed = false;
            }
        } else {
            mMinZoomScale = viewState.mMinScale;
            mMinZoomScaleFixed = true;
        }
        if (viewState.mMaxScale == 0) {
            mMaxZoomScale = mDefaultMaxZoomScale;
        } else {
            mMaxZoomScale = viewState.mMaxScale;
        }
        sanitizeMinMaxScales();
    }

    /**
     * Updates zoom values when Webkit produces a new picture. This method
     * should only be called from the UI thread's message handler.
     *
     * @return True if zoom value has changed
     */
    public boolean onNewPicture(WebViewCore.DrawData drawData) {
        final int viewWidth = mWebView.getViewWidth();
        final boolean zoomOverviewWidthChanged = setupZoomOverviewWidth(drawData, viewWidth);
        final float newZoomOverviewScale = getZoomOverviewScale();
        WebSettingsClassic settings = mWebView.getSettings();
        if (zoomOverviewWidthChanged && settings.isNarrowColumnLayout() &&
            settings.getUseFixedViewport() &&
            (mInitialZoomOverview || mInZoomOverview)) {
            // Keep mobile site's text wrap scale unchanged.  For mobile sites,
            // the text wrap scale is the same as zoom overview scale.
            if (exceedsMinScaleIncrement(mTextWrapScale, mDefaultScale) ||
                    exceedsMinScaleIncrement(newZoomOverviewScale, mDefaultScale)) {
                mTextWrapScale = getReadingLevelScale();
            } else {
                mTextWrapScale = newZoomOverviewScale;
            }
        }

        if (!mMinZoomScaleFixed || settings.getUseWideViewPort()) {
            mMinZoomScale = newZoomOverviewScale;
            mMaxZoomScale = Math.max(mMaxZoomScale, mMinZoomScale);
            sanitizeMinMaxScales();
        }
        // fit the content width to the current view for the first new picture
        // after first layout.
        boolean scaleHasDiff = exceedsMinScaleIncrement(newZoomOverviewScale, mActualScale);
        // Make sure the actual scale is no less than zoom overview scale.
        boolean scaleLessThanOverview =
                (newZoomOverviewScale - mActualScale) >= MINIMUM_SCALE_INCREMENT;
        // Make sure mobile sites are correctly handled since mobile site will
        // change content width after rotating.
        boolean mobileSiteInOverview = mInZoomOverview &&
                !exceedsMinScaleIncrement(newZoomOverviewScale, mDefaultScale);
        if (!mWebView.drawHistory() &&
            ((scaleLessThanOverview && settings.getUseWideViewPort())||
                ((mInitialZoomOverview || mobileSiteInOverview) &&
                    scaleHasDiff && zoomOverviewWidthChanged))) {
            mInitialZoomOverview = false;
            /// M: fix initial-scale issue. @{
            float newZoomScale = newZoomOverviewScale;
            if (mWebView != null && mWebView.getWebViewCore() != null && 
                mWebView.getWebViewCore().getViewportInitialScale() > 0) {
                newZoomScale = mWebView.getWebViewCore().getViewportInitialScale() * 0.01f;
                mTextWrapScale = newZoomScale;
            }
            setZoomScale(newZoomScale, !willScaleTriggerZoom(mTextWrapScale) &&
                !mWebView.getSettings().getUseFixedViewport());
            /// @}
        } else {
            mInZoomOverview = !scaleHasDiff;
        }
        if (drawData.mFirstLayoutForNonStandardLoad && settings.getLoadWithOverviewMode()) {
            // Set mInitialZoomOverview in case this is the first picture for non standard load,
            // so next new picture could be forced into overview mode if it's true.
            mInitialZoomOverview = mInZoomOverview;
        }

        return scaleHasDiff;
    }

    /**
     * Set up correct zoom overview width based on different settings.
     *
     * @param drawData webviewcore draw data
     * @param viewWidth current view width
     */
    private boolean setupZoomOverviewWidth(WebViewCore.DrawData drawData, final int viewWidth) {
        WebSettings settings = mWebView.getSettings();
        int newZoomOverviewWidth = mZoomOverviewWidth;
        if (settings.getUseWideViewPort()) {
            if (drawData.mContentSize.x > 0) {
                // The webkitDraw for layers will not populate contentSize, and it'll be
                // ignored for zoom overview width update.
                newZoomOverviewWidth = Math.min(WebViewClassic.sMaxViewportWidth,
                    drawData.mContentSize.x);
            }
        } else {
            // If not use wide viewport, use view width as the zoom overview width.
            newZoomOverviewWidth = Math.round(viewWidth / mDefaultScale);
        }
        if (newZoomOverviewWidth != mZoomOverviewWidth) {
            setZoomOverviewWidth(newZoomOverviewWidth);
            return true;
        }
        return false;
    }

    /**
     * Updates zoom values after Webkit completes the initial page layout. It
     * is called when visiting a page for the first time as well as when the
     * user navigates back to a page (in which case we may need to restore the
     * zoom levels to the state they were when you left the page). This method
     * should only be called from the UI thread's message handler.
     */
    public void onFirstLayout(WebViewCore.DrawData drawData) {
        // precondition check
        assert drawData != null;
        assert drawData.mViewState != null;
        assert mWebView.getSettings() != null;

        WebViewCore.ViewState viewState = drawData.mViewState;
        final Point viewSize = drawData.mViewSize;
        updateZoomRange(viewState, viewSize.x, drawData.mMinPrefWidth);
        setupZoomOverviewWidth(drawData, mWebView.getViewWidth());
        final float overviewScale = getZoomOverviewScale();
        WebSettingsClassic settings = mWebView.getSettings();
        if (!mMinZoomScaleFixed || settings.getUseWideViewPort()) {
            mMinZoomScale = (mInitialScale > 0) ?
                    Math.min(mInitialScale, overviewScale) : overviewScale;
            mMaxZoomScale = Math.max(mMaxZoomScale, mMinZoomScale);
            sanitizeMinMaxScales();
        }

        if (!mWebView.drawHistory()) {
            float scale;
            if (mInitialScale > 0) {
                scale = mInitialScale;
            } else if (viewState.mIsRestored || viewState.mViewScale > 0) {
                scale = (viewState.mViewScale > 0)
                    ? viewState.mViewScale : overviewScale;
                mTextWrapScale = (viewState.mTextWrapScale > 0)
                    ? viewState.mTextWrapScale : getReadingLevelScale();
            } else {
                scale = overviewScale;
                if (!settings.getUseWideViewPort()
                    || !settings.getLoadWithOverviewMode()) {
                    scale = Math.max(mDefaultScale, scale);
                }
                if (settings.isNarrowColumnLayout() &&
                    settings.getUseFixedViewport()) {
                    // When first layout, reflow using the reading level scale to avoid
                    // reflow when double tapped.
                    mTextWrapScale = getReadingLevelScale();
                }
            }
            boolean reflowText = false;
            if (!viewState.mIsRestored) {
                if (settings.getUseFixedViewport()) {
                    // Override the scale only in case of fixed viewport.
                    scale = Math.max(scale, overviewScale);
                    mTextWrapScale = Math.max(mTextWrapScale, overviewScale);
                }
                reflowText = exceedsMinScaleIncrement(mTextWrapScale, scale);
            }
            mInitialZoomOverview = settings.getLoadWithOverviewMode() &&
                    !exceedsMinScaleIncrement(scale, overviewScale);
            setZoomScale(scale, reflowText);

            // update the zoom buttons as the scale can be changed
            updateZoomPicker();
        }
    }

    public void saveZoomState(Bundle b) {
        b.putFloat("scale", mActualScale);
        b.putFloat("textwrapScale", mTextWrapScale);
        b.putBoolean("overview", mInZoomOverview);
    }

    public void restoreZoomState(Bundle b) {
        // as getWidth() / getHeight() of the view are not available yet, set up
        // mActualScale, so that when onSizeChanged() is called, the rest will
        // be set correctly
        mActualScale = b.getFloat("scale", 1.0f);
        mInvActualScale = 1 / mActualScale;
        mTextWrapScale = b.getFloat("textwrapScale", mActualScale);
        mInZoomOverview = b.getBoolean("overview");
    }

    private ZoomControlBase getCurrentZoomControl() {
        if (mWebView.getSettings() != null && mWebView.getSettings().supportZoom()) {
            if (mWebView.getSettings().getBuiltInZoomControls()) {
                if ((mEmbeddedZoomControl == null)
                        && mWebView.getSettings().getDisplayZoomControls()) {
                    mEmbeddedZoomControl = new ZoomControlEmbedded(this, mWebView);
                }
                return mEmbeddedZoomControl;
            } else {
                if (mExternalZoomControl == null) {
                    mExternalZoomControl = new ZoomControlExternal(mWebView);
                }
                return mExternalZoomControl;
            }
        }
        return null;
    }

    public void invokeZoomPicker() {
        ZoomControlBase control = getCurrentZoomControl();
        if (control != null) {
            control.show();
        }
    }

    public void dismissZoomPicker() {
        ZoomControlBase control = getCurrentZoomControl();
        if (control != null) {
            control.hide();
        }
    }

    public boolean isZoomPickerVisible() {
        ZoomControlBase control = getCurrentZoomControl();
        return (control != null) ? control.isVisible() : false;
    }

    public void updateZoomPicker() {
        ZoomControlBase control = getCurrentZoomControl();
        if (control != null) {
            control.update();
        }
    }

    /**
     * The embedded zoom control intercepts touch events and automatically stays
     * visible. The external control needs to constantly refresh its internal
     * timer to stay visible.
     */
    public void keepZoomPickerVisible() {
        ZoomControlBase control = getCurrentZoomControl();
        if (control != null && control == mExternalZoomControl) {
            control.show();
        }
    }

    public View getExternalZoomPicker() {
        ZoomControlBase control = getCurrentZoomControl();
        if (control != null && control == mExternalZoomControl) {
            return mExternalZoomControl.getControls();
        } else {
            return null;
        }
    }

    public void setHardwareAccelerated() {
        mHardwareAccelerated = true;
    }

    /**
     * OnPageFinished called by webview when a page is fully loaded.
     */
    /* package*/ void onPageFinished(String url) {
        // Turn off initial zoom overview flag when a page is fully loaded.
        mInitialZoomOverview = false;
    }
}
