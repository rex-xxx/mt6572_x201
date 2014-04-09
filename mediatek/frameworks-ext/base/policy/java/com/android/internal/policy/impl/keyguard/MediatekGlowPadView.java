/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.internal.policy.impl.keyguard;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManagerNative;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.View.OnTouchListener;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;

import com.android.internal.R;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A re-usable widget containing a center, outer ring and wave animation.
 */
public class MediatekGlowPadView extends View implements OnTouchListener {
    private static final String TAG = "MediatekGlowPadView";
    private static final boolean DEBUG = true;

    // Wave state machine
    private static final int STATE_IDLE = 0;
    private static final int STATE_START = 1;
    private static final int STATE_FIRST_TOUCH = 2;
    private static final int STATE_TRACKING = 3;
    private static final int STATE_SNAP = 4;
    private static final int STATE_FINISH = 5;

    // Animation properties.
    private static final float SNAP_MARGIN_DEFAULT = 20.0f; // distance to ring before we snap to it

    public interface OnTriggerListener {
        int NO_HANDLE = 0;
        int CENTER_HANDLE = 1;
        public void onGrabbed(View v, int handle);
        public void onReleased(View v, int handle);
        public void onTrigger(View v, int target);
        public void onGrabbedStateChange(View v, int handle);
        public void onFinishFinalAnimation();
    }

    // Tuneable parameters for animation
    private static final int WAVE_ANIMATION_DURATION = 1350;
    private static final int RETURN_TO_HOME_DELAY = 1200;
    private static final int RETURN_TO_HOME_DURATION = 200;
    private static final int HIDE_ANIMATION_DELAY = 200;
    private static final int HIDE_ANIMATION_DURATION = 200;
    private static final int SHOW_ANIMATION_DURATION = 200;
    private static final int SHOW_ANIMATION_DELAY = 50;
    private static final int INITIAL_SHOW_HANDLE_DURATION = 200;
    private static final int REVEAL_GLOW_DELAY = 0;
    private static final int REVEAL_GLOW_DURATION = 0;

    private static final float TAP_RADIUS_SCALE_ACCESSIBILITY_ENABLED = 1.3f;
    private static final float TARGET_SCALE_EXPANDED = 1.0f;
    private static final float TARGET_SCALE_COLLAPSED = 0.8f;
    private static final float RING_SCALE_EXPANDED = 1.0f;
    private static final float RING_SCALE_COLLAPSED = 0.5f;

    private ArrayList<TargetDrawable> mTargetDrawables = new ArrayList<TargetDrawable>();
    private AnimationBundle mWaveAnimations = new AnimationBundle();
    private AnimationBundle mTargetAnimations = new AnimationBundle();
    private AnimationBundle mGlowAnimations = new AnimationBundle();
    private ArrayList<String> mTargetDescriptions;
    private ArrayList<String> mDirectionDescriptions;
    private OnTriggerListener mOnTriggerListener;
    private TargetDrawable mHandleDrawable;
    private TargetDrawable mOuterRing;
    private Vibrator mVibrator;

    private int mFeedbackCount = 3;
    private int mVibrationDuration = 0;
    private int mGrabbedState;
    private int mActiveTarget = -1;
    private float mGlowRadius;
    private float mWaveCenterX;
    private float mWaveCenterY;
    private int mMaxTargetHeight;
    private int mMaxTargetWidth;
    private float mRingScaleFactor = 1f;
    private boolean mAllowScaling;

    private float mOuterRadius = 0.0f;
    private float mSnapMargin = 0.0f;
    private float mFirstItemOffset = 0.0f;
    private boolean mMagneticTargets = false;
    private boolean mDragging;
    private int mNewTargetResources;
    /// M: when ACTION_CANCEL, it should not trigger events, fix ALPS00339234
    private boolean mActionCancel = false;

    private class AnimationBundle extends ArrayList<Tweener> {
        private static final long serialVersionUID = 0xA84D78726F127468L;
        private boolean mSuspended;

        public void start() {
            if (mSuspended) return; // ignore attempts to start animations
            final int count = size();
            for (int i = 0; i < count; i++) {
                Tweener anim = get(i);
                anim.animator.start();
            }
        }

        public void cancel() {
            final int count = size();
            for (int i = 0; i < count; i++) {
                Tweener anim = get(i);
                anim.animator.cancel();
            }
            clear();
        }

        public void stop() {
            final int count = size();
            for (int i = 0; i < count; i++) {
                Tweener anim = get(i);
                anim.animator.end();
            }
            clear();
        }

        public void setSuspended(boolean suspend) {
            mSuspended = suspend;
        }
    };

    private AnimatorListener mResetListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            switchToState(STATE_IDLE, mWaveCenterX, mWaveCenterY);
            dispatchOnFinishFinalAnimation();
        }
    };

    private AnimatorListener mResetListenerWithPing = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            pingInternal();
            switchToState(STATE_IDLE, mWaveCenterX, mWaveCenterY);
            dispatchOnFinishFinalAnimation();
        }
    };

    private AnimatorUpdateListener mUpdateListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            invalidate();
        }
    };

    private boolean mAnimatingTargets;
    private AnimatorListener mTargetUpdateListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            if (mNewTargetResources != 0) {
                internalSetTargetResources(mNewTargetResources);
                mNewTargetResources = 0;
                hideTargets(false, false);
            }
            mAnimatingTargets = false;
        }
    };
    private int mTargetResourceId;
    private int mTargetDescriptionsResourceId;
    private int mDirectionDescriptionsResourceId;
    private boolean mAlwaysTrackFinger;
    private int mHorizontalInset;
    private int mVerticalInset;
    private int mGravity = Gravity.TOP;
    private boolean mInitialLayout = true;
    private Tweener mBackgroundAnimator;
    private PointCloud mPointCloud;
    private float mInnerRadius;
    private int mPointerId;

    /// M: Mediatek modify field begin @{
    
    private static final int DRAG_ANIMATION_DURATION = 350;
    private static final int DRAG_ANIMATION_DELAY = 50;
    
    private static final int DRAG_BACK_ANIMATION_DURATION = 200;
    private static final int DRAG_BACK_ANIMATION_DELAY = 20;
    
    /// M: The touch event Recepient, we will dispatch event outside center ring to the Recepient first
    private View mTouchRecepient;
    
    /// M: Add a global layout listener, so that we will compute the TouchEvent X and Y offset every time layout changed
    private OnLayoutChangeListener mOnLayoutListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
                int oldTop, int oldRight, int oldBottom) {
            refreshPositions();
            
        }
    };
    
    /// M: Indicate whether touch event is intercepted by touch Recepient, if yes we will copy the touch event and dispatch a
    /// new touch event to it
    private boolean mTouchIntercepted;

    /// M: private flag to indicate whether global layout listener is set
    private boolean mLayoutListenerSet;
    
    /// M: Save the touch interceptor's raw location on screen
    private final int[] mTouchRecepientRawLocation = new int [2];

    /// M: Save GlowPadView's raw location on screen
    private final int[] mOwnerRawLocation = new int [2];
    
    /// M: MotionEvent's x offset from GlowPadView to Recepient, we will offset MotionEvent 
    /// to x offset before send it to the Recepient
    private final int[] mOwnerRecepientOffsets = new int [2];
    
    private final int[] mNewEventViewLocationInLockScreen = new int [2];
    
    private final int[] mOwnerLocationInLockScreen = new int [2];
    
    private final int[] mOwnerNewEventViewOffset = new int [2];
    
    private UnReadEventView mUnReadEventView;
    
    private float mTouchMoveIntersectAnimThreshold;
    private static final float TOUCH_MOVE_INTERSECT_ANIM_THRESHOLD = 10;
    
    // Save current NewEventView's location in LockScreenLayout
    private final int[] mTempXY = new int[2];
    
    // Save the touch event offset to NewEventView's top left
    private final Point mPoint = new Point();
    
    // Used to track MotionEvent, if two touch move event's distance is not bigger than the threshold, we
    // drop it to avoid unnecessary overload
    private final int[] mHistoryTouchXY = new int[2];
    
    private static final int TOUCH_MOVE_TRIGGER_THRESHOLD = 3;
    
    // Touch move trigger threshold
    private int mTouchMoveTriggerThreshold;
    
    private Point mHandlePosition = new Point();
    
    // Save whether the drag target should do intersect animation
    private boolean mShouldDoIntersectAnim;
    private Runnable mCheckForInterSectAnimation = new IntersectAnimationRunnable();
    private boolean mDoingIntersectAnim;

    private AnimationBundle mDragViewAnimations = new AnimationBundle();
    
    private LockScreenLayout mLockScreenLayout;
    private DragView mDragView;
    private LockScreenNewEventView mCurrentNewEventView;
    private int mInitDownMotionLocationX;
    private int mInitDownMotionLocationY;
    private Rect mTempRect = new Rect();
    private int mNewEventViewInLockScreenX;
    private int mNewEventViewInLockScreenY;
    private final int[] mOwnerDragViewOffsets = new int [2];
    
    private boolean mTouchTriggered;
    
    // For Handler animation
    private TargetDrawable mFakeHandleDrawable;
    
    private AnimationBundle mHandleAnimations = new AnimationBundle();
    private AnimationBundle mFakeHandleAnimations = new AnimationBundle();
    
    private Canvas mCanvas = new Canvas();
    
    // For NewEventView related Gesture
    private GestureDetector mGestureDetector;
    private SimpleOnGestureListener mSimpleOnGestureListener;
    
    // Used to save whether the touch event is a tap, GestureDetector's onSingleTapUp may be called after
    // a long press release without move out of the touch slop
    private boolean mTapTimeOut = false;
    
    // Used to indicate whether the DragView is accepting touch event
    private boolean mDragViewDoingTouch;
    
    // Indicate the touch event should be dropped, GlowPadView and DragView should never process touch event at the same time
    private boolean mTouchDropped;
    
    // The to be launched activity's component name 
    private static final String MMS_PACKAGE_NAME = "com.android.mms";
    private static final String MMS_CLASS_NAME = "com.android.mms.ui.ConversationList";
    
    private static final String CALL_LOG_PACKAGE_NAME = "com.android.contacts";
    private static final String CALL_LOG_CLASS_NAME = "com.android.contacts.activities.DialtactsActivity";
    
    private static final String EMAIL_PACKAGE_NAME = "com.android.email";
    private static final String EMAIL_CLASS_NAME = "com.android.email.activity.Welcome";

    private static final int TOUCH_INIT = 0;
    private static final int TOUCH_HANDLE_ANIM = 1;
    private static final int TOUCH_INTERSECT = 2;
    private static final int TOUCH_DRAGGING = 3;
    private static final int TOUCH_FLING = 4;
    private static final int TOUCH_TRIGGER = 5;
    private static final int TOUCH_ANIM_BACK = 6;
    private static final int TOUCH_END = 7;
    
    // Save the state of default GlowPadView's state
    private int mGlowPadViewState;
    
    // Save fake handle drawable and dragview's animation state
    private int mDragViewState;
    // Save the pending state, we need to let fake handle done its scale animation before drag or fling it
    private int mPendingDragViewState;
    // Indicating whether fake handle drawable is doing scale animation
    private boolean mFakeHandleAnimating;
    // Save pending motion event's position and volecity
    private PendingEvent mPendingEvent = new PendingEvent();
    
    private class PendingEvent {
        int eventX;
        int eventY;
        float velocityX;
        float velocityY;
    }
    
    // The fake center drawable's touch slop use this static variable multiples screen density
    private static final int ANIM_TOUCH_STLOP = 3;
    
    // Used to update fake handle drawable's position, this is the slop threshold
    private int mTouchSlop;
    private int mTouchSlopSqure;
    // Save Fake handle and DragView's snap margin
    private float mDragSnapMargin;
    
    // Used to save fake handle drawable' real position in GlowPadView's coordination
    private float mFakeHandleRealX;
    private float mFakeHandleRealY;
    
    // Used to save current motion event's target position in GlowPadView's coordination, for fake handle
    // to do a 
    private float mCurrentFakeHandleTargetX;
    private float mCurrentFakeHandleTargetY;

    /// M: Mediatek modify field end @}

    public MediatekGlowPadView(Context context) {
        this(context, null);
    }

    public MediatekGlowPadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GlowPadView);
        mInnerRadius = a.getDimension(R.styleable.GlowPadView_innerRadius, mInnerRadius);
        mOuterRadius = a.getDimension(R.styleable.GlowPadView_outerRadius, mOuterRadius);
        mSnapMargin = a.getDimension(R.styleable.GlowPadView_snapMargin, mSnapMargin);
        mFirstItemOffset = (float) Math.toRadians(
                a.getFloat(R.styleable.GlowPadView_firstItemOffset,
                        (float) Math.toDegrees(mFirstItemOffset)));
        mVibrationDuration = a.getInt(R.styleable.GlowPadView_vibrationDuration,
                mVibrationDuration);
        mFeedbackCount = a.getInt(R.styleable.GlowPadView_feedbackCount,
                mFeedbackCount);
        mAllowScaling = a.getBoolean(R.styleable.GlowPadView_allowScaling, false);
        TypedValue handle = a.peekValue(R.styleable.GlowPadView_handleDrawable);
        mHandleDrawable = new TargetDrawable(res, handle != null ? handle.resourceId : 0);
        mHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
        mOuterRing = new TargetDrawable(res,
                getResourceId(a, R.styleable.GlowPadView_outerRingDrawable));

        mAlwaysTrackFinger = a.getBoolean(R.styleable.GlowPadView_alwaysTrackFinger, false);
        mMagneticTargets = a.getBoolean(R.styleable.GlowPadView_magneticTargets, mMagneticTargets);

        int pointId = getResourceId(a, R.styleable.GlowPadView_pointDrawable);
        Drawable pointDrawable = pointId != 0 ? res.getDrawable(pointId) : null;
        mGlowRadius = a.getDimension(R.styleable.GlowPadView_glowRadius, 0.0f);

        TypedValue outValue = new TypedValue();

        // Read array of target drawables
        if (a.getValue(R.styleable.GlowPadView_targetDrawables, outValue)) {
            internalSetTargetResources(outValue.resourceId);
        }
        if (mTargetDrawables == null || mTargetDrawables.size() == 0) {
            throw new IllegalStateException("Must specify at least one target drawable");
        }

        // Read array of target descriptions
        if (a.getValue(R.styleable.GlowPadView_targetDescriptions, outValue)) {
            final int resourceId = outValue.resourceId;
            if (resourceId == 0) {
                throw new IllegalStateException("Must specify target descriptions");
            }
            setTargetDescriptionsResourceId(resourceId);
        }

        // Read array of direction descriptions
        if (a.getValue(R.styleable.GlowPadView_directionDescriptions, outValue)) {
            final int resourceId = outValue.resourceId;
            if (resourceId == 0) {
                throw new IllegalStateException("Must specify direction descriptions");
            }
            setDirectionDescriptionsResourceId(resourceId);
        }

        mGravity = a.getInt(R.styleable.GlowPadView_gravity, Gravity.TOP);

        a.recycle();

        setVibrateEnabled(mVibrationDuration > 0);

        assignDefaultsIfNeeded();

        mPointCloud = new PointCloud(pointDrawable);
        mPointCloud.makePointCloud(mInnerRadius, mOuterRadius);
        mPointCloud.glowManager.setRadius(mGlowRadius);
        
        mDragViewState = -1;
        mPendingDragViewState = -1;
    }

    private int getResourceId(TypedArray a, int id) {
        TypedValue tv = a.peekValue(id);
        return tv == null ? 0 : tv.resourceId;
    }

    private void dump() {
        Log.v(TAG, "Outer Radius = " + mOuterRadius);
        Log.v(TAG, "SnapMargin = " + mSnapMargin);
        Log.v(TAG, "FeedbackCount = " + mFeedbackCount);
        Log.v(TAG, "VibrationDuration = " + mVibrationDuration);
        Log.v(TAG, "GlowRadius = " + mGlowRadius);
        Log.v(TAG, "WaveCenterX = " + mWaveCenterX);
        Log.v(TAG, "WaveCenterY = " + mWaveCenterY);
    }

    public void suspendAnimations() {
        mWaveAnimations.setSuspended(true);
        mTargetAnimations.setSuspended(true);
        mGlowAnimations.setSuspended(true);
    }

    public void resumeAnimations() {
        mWaveAnimations.setSuspended(false);
        mTargetAnimations.setSuspended(false);
        mGlowAnimations.setSuspended(false);
        mWaveAnimations.start();
        mTargetAnimations.start();
        mGlowAnimations.start();
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        // View should be large enough to contain the background + handle and
        // target drawable on either edge.
        return (int) (Math.max(mOuterRing.getWidth(), 2 * mOuterRadius) + mMaxTargetWidth);
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        // View should be large enough to contain the unlock ring + target and
        // target drawable on either edge
        return (int) (Math.max(mOuterRing.getHeight(), 2 * mOuterRadius) + mMaxTargetHeight);
    }

    /**
     * This gets the suggested width accounting for the ring's scale factor.
     */
    protected int getScaledSuggestedMinimumWidth() {
        return (int) (mRingScaleFactor * Math.max(mOuterRing.getWidth(), 2 * mOuterRadius)
                + mMaxTargetWidth);
    }

    /**
     * This gets the suggested height accounting for the ring's scale factor.
     */
    protected int getScaledSuggestedMinimumHeight() {
        return (int) (mRingScaleFactor * Math.max(mOuterRing.getHeight(), 2 * mOuterRadius)
                + mMaxTargetHeight);
    }

    private int resolveMeasured(int measureSpec, int desired)
    {
        int result = 0;
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (MeasureSpec.getMode(measureSpec)) {
            case MeasureSpec.UNSPECIFIED:
                result = desired;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(specSize, desired);
                break;
            case MeasureSpec.EXACTLY:
            default:
                result = specSize;
        }
        return result;
    }

    private void switchToState(int state, float x, float y) {
        switch (state) {
            case STATE_IDLE:
                mGlowPadViewState = STATE_IDLE;
                deactivateTargets();
                hideGlow(0, 0, 0.0f, null);
                startBackgroundAnimation(0, 0.0f);
                mHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
                mHandleDrawable.setAlpha(1.0f);
                break;

            case STATE_START:
                mGlowPadViewState = STATE_IDLE;
                startBackgroundAnimation(0, 0.0f);
                break;

            case STATE_FIRST_TOUCH:
                mGlowPadViewState = STATE_FIRST_TOUCH;
                mHandleDrawable.setAlpha(0.0f);
                deactivateTargets();
                showTargets(true);
                startBackgroundAnimation(INITIAL_SHOW_HANDLE_DURATION, 1.0f);
                setGrabbedState(OnTriggerListener.CENTER_HANDLE);
                if (AccessibilityManager.getInstance(mContext).isEnabled()) {
                    announceTargets();
                }
                break;

            case STATE_TRACKING:
                mGlowPadViewState = STATE_TRACKING;
                mHandleDrawable.setAlpha(0.0f);
                showGlow(REVEAL_GLOW_DURATION , REVEAL_GLOW_DELAY, 1.0f, null);
                break;

            case STATE_SNAP:
                // TODO: Add transition states (see list_selector_background_transition.xml)
                mGlowPadViewState = STATE_SNAP;
                mHandleDrawable.setAlpha(0.0f);
                showGlow(REVEAL_GLOW_DURATION , REVEAL_GLOW_DELAY, 0.0f, null);
                break;

            case STATE_FINISH:
                mGlowPadViewState = STATE_FINISH;
                doFinish();
                break;
        }
    }

    private void showGlow(int duration, int delay, float finalAlpha,
            AnimatorListener finishListener) {
        mGlowAnimations.cancel();
        mGlowAnimations.add(Tweener.to(mPointCloud.glowManager, duration,
                "ease", Ease.Cubic.easeIn,
                "delay", delay,
                "alpha", finalAlpha,
                "onUpdate", mUpdateListener,
                "onComplete", finishListener));
        mGlowAnimations.start();
    }

    private void hideGlow(int duration, int delay, float finalAlpha,
            AnimatorListener finishListener) {
        mGlowAnimations.cancel();
        mGlowAnimations.add(Tweener.to(mPointCloud.glowManager, duration,
                "ease", Ease.Quart.easeOut,
                "delay", delay,
                "alpha", finalAlpha,
                "x", 0.0f,
                "y", 0.0f,
                "onUpdate", mUpdateListener,
                "onComplete", finishListener));
        mGlowAnimations.start();
    }

    private void deactivateTargets() {
        final int count = mTargetDrawables.size();
        for (int i = 0; i < count; i++) {
            TargetDrawable target = mTargetDrawables.get(i);
            target.setState(TargetDrawable.STATE_INACTIVE);
        }
        mActiveTarget = -1;
    }

    /**
     * Dispatches a trigger event to listener. Ignored if a listener is not set.
     * @param whichTarget the target that was triggered.
     */
    private void dispatchTriggerEvent(int whichTarget) {
        vibrate();
        if (mOnTriggerListener != null) {
            mOnTriggerListener.onTrigger(this, whichTarget);
        }
    }

    private void dispatchOnFinishFinalAnimation() {
        if (mOnTriggerListener != null) {
            mOnTriggerListener.onFinishFinalAnimation();
        }
    }

    private void doFinish() {
        final int activeTarget = mActiveTarget;
        final boolean targetHit =  activeTarget != -1;

        if (targetHit) {
            if (DEBUG) Log.v(TAG, "Finish with target hit = " + targetHit);

            highlightSelected(activeTarget);

            // Inform listener of any active targets.  Typically only one will be active.

            /// M: [ALPS00613044] We will force change to idle state on reset.
            /// hideGlow(RETURN_TO_HOME_DURATION, RETURN_TO_HOME_DELAY, 0.0f, mResetListener);

            /// M: when ACTION_CANCEL, it should not trigger event. @{
            if (!mActionCancel) {
                /// M: [ALPS00569797] Let GlowPadView stay silent to trigger event.
                mDragViewState = TOUCH_TRIGGER;
                dispatchTriggerEvent(activeTarget);
            }
            /// @}
            if (!mAlwaysTrackFinger) {
                // Force ring and targets to finish animation to final expanded state
                mTargetAnimations.stop();
            }
        } else {
            // Animate handle back to the center based on current state.
            hideGlow(HIDE_ANIMATION_DURATION, 0, 0.0f, mResetListenerWithPing);
            hideTargets(true, false);
        }

        /// M: [ALPS00536522] Request layout to update target position.
        setGrabbedState(OnTriggerListener.NO_HANDLE);
        if (mActionCancel) {
            requestLayout();
        }
    }

    private void highlightSelected(int activeTarget) {
        // Highlight the given target and fade others
        mTargetDrawables.get(activeTarget).setState(TargetDrawable.STATE_ACTIVE);
        hideUnselected(activeTarget);
    }

    private void hideUnselected(int active) {
        for (int i = 0; i < mTargetDrawables.size(); i++) {
            if (i != active) {
                mTargetDrawables.get(i).setAlpha(0.0f);
            }
        }
    }

    private void hideTargets(boolean animate, boolean expanded) {
        mTargetAnimations.cancel();
        // Note: these animations should complete at the same time so that we can swap out
        // the target assets asynchronously from the setTargetResources() call.
        mAnimatingTargets = animate;
        final int duration = animate ? HIDE_ANIMATION_DURATION : 0;
        final int delay = animate ? HIDE_ANIMATION_DELAY : 0;

        final float targetScale = expanded ?
                TARGET_SCALE_EXPANDED : TARGET_SCALE_COLLAPSED;
        final int length = mTargetDrawables.size();
        final TimeInterpolator interpolator = Ease.Cubic.easeOut;
        for (int i = 0; i < length; i++) {
            TargetDrawable target = mTargetDrawables.get(i);
            target.setState(TargetDrawable.STATE_INACTIVE);
            mTargetAnimations.add(Tweener.to(target, duration,
                    "ease", interpolator,
                    "alpha", 0.0f,
                    "scaleX", targetScale,
                    "scaleY", targetScale,
                    "delay", delay,
                    "onUpdate", mUpdateListener));
        }

        float ringScaleTarget = expanded ?
                RING_SCALE_EXPANDED : RING_SCALE_COLLAPSED;
        ringScaleTarget *= mRingScaleFactor;
        mTargetAnimations.add(Tweener.to(mOuterRing, duration,
                "ease", interpolator,
                "alpha", 0.0f,
                "scaleX", ringScaleTarget,
                "scaleY", ringScaleTarget,
                "delay", delay,
                "onUpdate", mUpdateListener,
                "onComplete", mTargetUpdateListener));

        mTargetAnimations.start();
    }

    private void showTargets(boolean animate) {
        mTargetAnimations.stop();
        mAnimatingTargets = animate;
        final int delay = animate ? SHOW_ANIMATION_DELAY : 0;
        final int duration = animate ? SHOW_ANIMATION_DURATION : 0;
        final int length = mTargetDrawables.size();
        for (int i = 0; i < length; i++) {
            TargetDrawable target = mTargetDrawables.get(i);
            target.setState(TargetDrawable.STATE_INACTIVE);
            mTargetAnimations.add(Tweener.to(target, duration,
                    "ease", Ease.Cubic.easeOut,
                    "alpha", 1.0f,
                    "scaleX", 1.0f,
                    "scaleY", 1.0f,
                    "delay", delay,
                    "onUpdate", mUpdateListener));
        }

        float ringScale = mRingScaleFactor * RING_SCALE_EXPANDED;
        mTargetAnimations.add(Tweener.to(mOuterRing, duration,
                "ease", Ease.Cubic.easeOut,
                "alpha", 1.0f,
                "scaleX", ringScale,
                "scaleY", ringScale,
                "delay", delay,
                "onUpdate", mUpdateListener,
                "onComplete", mTargetUpdateListener));

        mTargetAnimations.start();
    }

    private void vibrate() {
        final boolean hapticEnabled = Settings.System.getIntForUser(
                mContext.getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1,
                UserHandle.USER_CURRENT) != 0;
        if (mVibrator != null && hapticEnabled) {
            mVibrator.vibrate(mVibrationDuration);
        }
    }

    private ArrayList<TargetDrawable> loadDrawableArray(int resourceId) {
        Resources res = getContext().getResources();
        TypedArray array = res.obtainTypedArray(resourceId);
        final int count = array.length();
        ArrayList<TargetDrawable> drawables = new ArrayList<TargetDrawable>(count);
        for (int i = 0; i < count; i++) {
            TypedValue value = array.peekValue(i);
            TargetDrawable target = new TargetDrawable(res, value != null ? value.resourceId : 0);
            drawables.add(target);
        }
        array.recycle();
        return drawables;
    }

    private void internalSetTargetResources(int resourceId) {
        final ArrayList<TargetDrawable> targets = loadDrawableArray(resourceId);
        mTargetDrawables = targets;
        mTargetResourceId = resourceId;

        int maxWidth = mHandleDrawable.getWidth();
        int maxHeight = mHandleDrawable.getHeight();
        final int count = targets.size();
        for (int i = 0; i < count; i++) {
            TargetDrawable target = targets.get(i);
            maxWidth = Math.max(maxWidth, target.getWidth());
            maxHeight = Math.max(maxHeight, target.getHeight());
        }
        if (mMaxTargetWidth != maxWidth || mMaxTargetHeight != maxHeight) {
            mMaxTargetWidth = maxWidth;
            mMaxTargetHeight = maxHeight;
            requestLayout(); // required to resize layout and call updateTargetPositions()
        } else {
            updateTargetPositions(mWaveCenterX, mWaveCenterY);
            updatePointCloudPosition(mWaveCenterX, mWaveCenterY);
        }
    }

    /**
     * M: setHandleDrawableImage: set drawable object by resource id for TE.
     *    For example, glowPadView.setHandleDrawableImage(R.drawable.ic_lockscreen_handle1);
     *
     * @param id The desired resource identifier, as generated by the aapt tool.
     *
     * @hide @{
     */
    public void setHandleDrawableImage(int id){
    	Resources res = getResources();
        if (mHandleDrawable != null) {
            mHandleDrawable.setDrawable(res, id);
        }
    }
    /** @} */

    /**
     * Loads an array of drawables from the given resourceId.
     *
     * @param resourceId
     */
    public void setTargetResources(int resourceId) {
        if (mAnimatingTargets) {
            // postpone this change until we return to the initial state
            mNewTargetResources = resourceId;
        } else {
            internalSetTargetResources(resourceId);
        }
    }

    public int getTargetResourceId() {
        return mTargetResourceId;
    }

    /**
     * Sets the resource id specifying the target descriptions for accessibility.
     *
     * @param resourceId The resource id.
     */
    public void setTargetDescriptionsResourceId(int resourceId) {
        mTargetDescriptionsResourceId = resourceId;
        if (mTargetDescriptions != null) {
            mTargetDescriptions.clear();
        }
    }

    /**
     * Gets the resource id specifying the target descriptions for accessibility.
     *
     * @return The resource id.
     */
    public int getTargetDescriptionsResourceId() {
        return mTargetDescriptionsResourceId;
    }

    /**
     * Sets the resource id specifying the target direction descriptions for accessibility.
     *
     * @param resourceId The resource id.
     */
    public void setDirectionDescriptionsResourceId(int resourceId) {
        mDirectionDescriptionsResourceId = resourceId;
        if (mDirectionDescriptions != null) {
            mDirectionDescriptions.clear();
        }
    }

    /**
     * Gets the resource id specifying the target direction descriptions.
     *
     * @return The resource id.
     */
    public int getDirectionDescriptionsResourceId() {
        return mDirectionDescriptionsResourceId;
    }

    /**
     * Enable or disable vibrate on touch.
     *
     * @param enabled
     */
    public void setVibrateEnabled(boolean enabled) {
        if (enabled && mVibrator == null) {
            mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        } else {
            mVibrator = null;
        }
    }

    /**
     * Starts wave animation.
     *
     */
    public void ping() {
        pingInternal();
        doShakeAnimation();
    }
    
    public void pingInternal() {
        if (mFeedbackCount > 0) {
            boolean doWaveAnimation = true;
            final AnimationBundle waveAnimations = mWaveAnimations;

            // Don't do a wave if there's already one in progress
            if (waveAnimations.size() > 0 && waveAnimations.get(0).animator.isRunning()) {
                long t = waveAnimations.get(0).animator.getCurrentPlayTime();
                if (t < WAVE_ANIMATION_DURATION/2) {
                    doWaveAnimation = false;
                }
            }

            if (doWaveAnimation) {
                startWaveAnimation();
            }
        }
    }

    private void stopAndHideWaveAnimation() {
        mWaveAnimations.cancel();
        mPointCloud.waveManager.setAlpha(0.0f);
    }

    private void startWaveAnimation() {
        mWaveAnimations.cancel();
        mPointCloud.waveManager.setAlpha(1.0f);
        mPointCloud.waveManager.setRadius(mHandleDrawable.getWidth()/2.0f);
        mWaveAnimations.add(Tweener.to(mPointCloud.waveManager, WAVE_ANIMATION_DURATION,
                "ease", Ease.Quad.easeOut,
                "delay", 0,
                "radius", 2.0f * mOuterRadius,
                "onUpdate", mUpdateListener,
                "onComplete",
                new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animator) {
                        mPointCloud.waveManager.setRadius(0.0f);
                        mPointCloud.waveManager.setAlpha(0.0f);
                    }
                }));
        mWaveAnimations.start();
    }

    /**
     * Resets the widget to default state and cancels all animation. If animate is 'true', will
     * animate objects into place. Otherwise, objects will snap back to place.
     *
     * @param animate
     */
    public void reset(boolean animate) {
        mGlowAnimations.stop();
        mTargetAnimations.stop();
        ///startBackgroundAnimation(0, 0.0f); /// M: [ALPS00613044]
        stopAndHideWaveAnimation();
        hideTargets(animate, false);
        ///hideGlow(0, 0, 0.0f, null); /// M: [ALPS00613044]

        /// M: [ALPS00613044] We will force change to idle state on reset.
        switchToState(STATE_IDLE, mWaveCenterX, mWaveCenterY);
        
        mHandleAnimations.stop();
        mFakeHandleAnimations.stop();
        switchDragViewToState(TOUCH_END, 0, 0, 0.0f, 0.0f);
        
        Tweener.reset();
    }

    private void startBackgroundAnimation(int duration, float alpha) {
        final Drawable background = getBackground();
        if (mAlwaysTrackFinger && background != null) {
            if (mBackgroundAnimator != null) {
                mBackgroundAnimator.animator.cancel();
            }
            mBackgroundAnimator = Tweener.to(background, duration,
                    "ease", Ease.Cubic.easeIn,
                    "alpha", (int)(255.0f * alpha),
                    "delay", SHOW_ANIMATION_DELAY);
            mBackgroundAnimator.animator.start();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();

        /// M: Add for new event feature, when new event view is doing animation, let GlowPadView stay silent @{
        boolean shouldDropTouch = mTouchDropped;
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                if (mDragViewState >= TOUCH_INIT && mDragViewState < TOUCH_END) {
                    Xlog.d(TAG, "onTouchEvent return directly mDragViewState=" + mDragViewState);
                    mTouchDropped = true;
                    shouldDropTouch = true;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchDropped = false;
                break;
        }
        if (shouldDropTouch) {
            return true;
        }
        /// }@
        boolean handled = false;
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                if (DEBUG) Log.v(TAG, "*** DOWN ***");
                handleDown(event);
                handleMove(event);
                handled = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (DEBUG) Log.v(TAG, "*** MOVE ***");
                handleMove(event);
                handled = true;
                break;

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                if (DEBUG) Log.v(TAG, "*** UP ***");
                /// M: In default logic, ACTION_UP should trigger events, but ACTION_CANCEL should not.
                mActionCancel = false;
                handleMove(event);
                handleUp(event);
                handled = true;
                if (mLockScreenLayout != null) {
                    showAllNewEventViews();
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (DEBUG) Log.v(TAG, "*** CANCEL ***");
                /// M: In default logic, ACTION_CANCLE may trigger events, but it should not.
                mActionCancel = true;
                handleMove(event);
                handleCancel(event);
                handled = true;
                if (mLockScreenLayout != null) {
                    showAllNewEventViews();
                }
                break;

        }
        invalidate();
        return handled ? true : super.onTouchEvent(event);
    }

    private void updateGlowPosition(float x, float y) {
        float dx = x - mOuterRing.getX();
        float dy = y - mOuterRing.getY();
        dx *= 1f / mRingScaleFactor;
        dy *= 1f / mRingScaleFactor;
        mPointCloud.glowManager.setX(mOuterRing.getX() + dx);
        mPointCloud.glowManager.setY(mOuterRing.getY() + dy);
    }

    private void handleDown(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        float eventX = event.getX(actionIndex);
        float eventY = event.getY(actionIndex);
        switchToState(STATE_START, eventX, eventY);
        if (!trySwitchToFirstTouchState(eventX, eventY)) {
            mDragging = false;
            /// M: When handling touch down in GlowPadView's non-center handle area, let new even view do a shake animation
            doShakeAnimation();
        } else {
            mPointerId = event.getPointerId(actionIndex);
            updateGlowPosition(eventX, eventY);
        }
    }

    private void handleUp(MotionEvent event) {
        if (DEBUG && mDragging) Log.v(TAG, "** Handle RELEASE");
        int actionIndex = event.getActionIndex();
        if (event.getPointerId(actionIndex) == mPointerId) {
            switchToState(STATE_FINISH, event.getX(actionIndex), event.getY(actionIndex));
        }
    }

    private void handleCancel(MotionEvent event) {
        if (DEBUG && mDragging) Log.v(TAG, "** Handle CANCEL");

        // Drop the active target if canceled.
        mActiveTarget = -1; 

        int actionIndex = event.findPointerIndex(mPointerId);
        actionIndex = actionIndex == -1 ? 0 : actionIndex;
        switchToState(STATE_FINISH, event.getX(actionIndex), event.getY(actionIndex));
    }

    private void handleMove(MotionEvent event) {
        int activeTarget = -1;
        final int historySize = event.getHistorySize();
        ArrayList<TargetDrawable> targets = mTargetDrawables;
        int ntargets = targets.size();
        float x = 0.0f;
        float y = 0.0f;
        float activeAngle = 0.0f;
        int actionIndex = event.findPointerIndex(mPointerId);

        if (actionIndex == -1) {
            return;  // no data for this pointer
        }

        for (int k = 0; k < historySize + 1; k++) {
            float eventX = k < historySize ? event.getHistoricalX(actionIndex, k)
                    : event.getX(actionIndex);
            float eventY = k < historySize ? event.getHistoricalY(actionIndex, k)
                    : event.getY(actionIndex);
            // tx and ty are relative to wave center
            float tx = eventX - mWaveCenterX;
            float ty = eventY - mWaveCenterY;
            float touchRadius = (float) Math.sqrt(dist2(tx, ty));
            final float scale = touchRadius > mOuterRadius ? mOuterRadius / touchRadius : 1.0f;
            float limitX = tx * scale;
            float limitY = ty * scale;
            double angleRad = Math.atan2(-ty, tx);

            if (!mDragging) {
                trySwitchToFirstTouchState(eventX, eventY);
            }

            if (mDragging) {
                // For multiple targets, snap to the one that matches
                final float snapRadius = mRingScaleFactor * mOuterRadius - mSnapMargin;
                final float snapDistance2 = snapRadius * snapRadius;
                // Find first target in range
                for (int i = 0; i < ntargets; i++) {
                    TargetDrawable target = targets.get(i);

                    double targetMinRad = mFirstItemOffset + (i - 0.5) * 2 * Math.PI / ntargets;
                    double targetMaxRad = mFirstItemOffset + (i + 0.5) * 2 * Math.PI / ntargets;
                    if (target.isEnabled()) {
                        boolean angleMatches =
                            (angleRad > targetMinRad && angleRad <= targetMaxRad) ||
                            (angleRad + 2 * Math.PI > targetMinRad &&
                             angleRad + 2 * Math.PI <= targetMaxRad) ||
                            (angleRad - 2 * Math.PI > targetMinRad &&
                             angleRad - 2 * Math.PI <= targetMaxRad);
                        if (angleMatches && (dist2(tx, ty) > snapDistance2)) {
                            activeTarget = i;
                            activeAngle = (float) -angleRad;
                        }
                    }
                }
            }
            x = limitX;
            y = limitY;
        }

        if (!mDragging) {
            return;
        }

        if (activeTarget != -1) {
            switchToState(STATE_SNAP, x,y);
            updateGlowPosition(x, y);
        } else {
            switchToState(STATE_TRACKING, x, y);
            updateGlowPosition(x, y);
        }

        if (mActiveTarget != activeTarget) {
            // Defocus the old target
            if (mActiveTarget != -1) {
                TargetDrawable target = targets.get(mActiveTarget);
                if (target.hasState(TargetDrawable.STATE_FOCUSED)) {
                    target.setState(TargetDrawable.STATE_INACTIVE);
                }
                if (mMagneticTargets) {
                    updateTargetPosition(mActiveTarget, mWaveCenterX, mWaveCenterY);
                }
            }
            // Focus the new target
            if (activeTarget != -1) {
                TargetDrawable target = targets.get(activeTarget);
                if (target.hasState(TargetDrawable.STATE_FOCUSED)) {
                    target.setState(TargetDrawable.STATE_FOCUSED);
                }
                if (mMagneticTargets) {
                    updateTargetPosition(activeTarget, mWaveCenterX, mWaveCenterY, activeAngle);
                }
                if (AccessibilityManager.getInstance(mContext).isEnabled()) {
                    String targetContentDescription = getTargetDescription(activeTarget);
                    announceForAccessibility(targetContentDescription);
                }
            }
        }
        mActiveTarget = activeTarget;
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (AccessibilityManager.getInstance(mContext).isTouchExplorationEnabled()) {
            final int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    event.setAction(MotionEvent.ACTION_DOWN);
                    break;
                case MotionEvent.ACTION_HOVER_MOVE:
                    event.setAction(MotionEvent.ACTION_MOVE);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    event.setAction(MotionEvent.ACTION_UP);
                    break;
            }
            onTouchEvent(event);
            event.setAction(action);
        }
        super.onHoverEvent(event);
        return true;
    }

    /**
     * Sets the current grabbed state, and dispatches a grabbed state change
     * event to our listener.
     */
    private void setGrabbedState(int newState) {
        if (newState != mGrabbedState) {
            if (newState != OnTriggerListener.NO_HANDLE) {
                vibrate();
            }
            mGrabbedState = newState;
            if (mOnTriggerListener != null) {
                if (newState == OnTriggerListener.NO_HANDLE) {
                    mOnTriggerListener.onReleased(this, OnTriggerListener.CENTER_HANDLE);
                } else {
                    mOnTriggerListener.onGrabbed(this, OnTriggerListener.CENTER_HANDLE);
                }
                mOnTriggerListener.onGrabbedStateChange(this, newState);
            }
        }
    }

    private boolean trySwitchToFirstTouchState(float x, float y) {
        final float tx = x - mWaveCenterX;
        final float ty = y - mWaveCenterY;
        if (mAlwaysTrackFinger || dist2(tx,ty) <= getScaledGlowRadiusSquared()) {
            if (mLockScreenLayout != null) {
                hideAllNewEventViews();
            }
            if (DEBUG) Log.v(TAG, "** Handle HIT");
            switchToState(STATE_FIRST_TOUCH, x, y);
            updateGlowPosition(tx, ty);
            mDragging = true;
            return true;
        }
        return false;
    }

    private void assignDefaultsIfNeeded() {
        if (mOuterRadius == 0.0f) {
            mOuterRadius = Math.max(mOuterRing.getWidth(), mOuterRing.getHeight())/2.0f;
        }
        if (mSnapMargin == 0.0f) {
            mSnapMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    SNAP_MARGIN_DEFAULT, getContext().getResources().getDisplayMetrics());
        }
        if (mInnerRadius == 0.0f) {
            mInnerRadius = mHandleDrawable.getWidth() / 10.0f;
        }
    }

    private void computeInsets(int dx, int dy) {
        final int layoutDirection = getLayoutDirection();
        final int absoluteGravity = Gravity.getAbsoluteGravity(mGravity, layoutDirection);

        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT:
                mHorizontalInset = 0;
                break;
            case Gravity.RIGHT:
                mHorizontalInset = dx;
                break;
            case Gravity.CENTER_HORIZONTAL:
            default:
                mHorizontalInset = dx / 2;
                break;
        }
        switch (absoluteGravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP:
                mVerticalInset = 0;
                break;
            case Gravity.BOTTOM:
                mVerticalInset = dy;
                break;
            case Gravity.CENTER_VERTICAL:
            default:
                mVerticalInset = dy / 2;
                break;
        }
    }

    /**
     * Given the desired width and height of the ring and the allocated width and height, compute
     * how much we need to scale the ring.
     */
    private float computeScaleFactor(int desiredWidth, int desiredHeight,
            int actualWidth, int actualHeight) {

        // Return unity if scaling is not allowed.
        if (!mAllowScaling) return 1f;

        final int layoutDirection = getLayoutDirection();
        final int absoluteGravity = Gravity.getAbsoluteGravity(mGravity, layoutDirection);

        float scaleX = 1f;
        float scaleY = 1f;

        // We use the gravity as a cue for whether we want to scale on a particular axis.
        // We only scale to fit horizontally if we're not pinned to the left or right. Likewise,
        // we only scale to fit vertically if we're not pinned to the top or bottom. In these
        // cases, we want the ring to hang off the side or top/bottom, respectively.
        switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.LEFT:
            case Gravity.RIGHT:
                break;
            case Gravity.CENTER_HORIZONTAL:
            default:
                if (desiredWidth > actualWidth) {
                    scaleX = (1f * actualWidth - mMaxTargetWidth) /
                            (desiredWidth - mMaxTargetWidth);
                }
                break;
        }
        switch (absoluteGravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP:
            case Gravity.BOTTOM:
                break;
            case Gravity.CENTER_VERTICAL:
            default:
                if (desiredHeight > actualHeight) {
                    scaleY = (1f * actualHeight - mMaxTargetHeight) /
                            (desiredHeight - mMaxTargetHeight);
                }
                break;
        }
        return Math.min(scaleX, scaleY);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int minimumWidth = getSuggestedMinimumWidth();
        final int minimumHeight = getSuggestedMinimumHeight();
        int computedWidth = resolveMeasured(widthMeasureSpec, minimumWidth);
        int computedHeight = resolveMeasured(heightMeasureSpec, minimumHeight);

        mRingScaleFactor = computeScaleFactor(minimumWidth, minimumHeight,
                computedWidth, computedHeight);

        int scaledWidth = getScaledSuggestedMinimumWidth();
        int scaledHeight = getScaledSuggestedMinimumHeight();

        computeInsets(computedWidth - scaledWidth, computedHeight - scaledHeight);
        setMeasuredDimension(computedWidth, computedHeight);
    }

    private float getRingWidth() {
        return mRingScaleFactor * Math.max(mOuterRing.getWidth(), 2 * mOuterRadius);
    }

    private float getRingHeight() {
        return mRingScaleFactor * Math.max(mOuterRing.getHeight(), 2 * mOuterRadius);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final int width = right - left;
        final int height = bottom - top;

        // Target placement width/height. This puts the targets on the greater of the ring
        // width or the specified outer radius.
        final float placementWidth = getRingWidth();
        final float placementHeight = getRingHeight();
        float newWaveCenterX = mHorizontalInset
                + Math.max(width, mMaxTargetWidth + placementWidth) / 2;
        float newWaveCenterY = mVerticalInset
                + Math.max(height, + mMaxTargetHeight + placementHeight) / 2;

        if (mInitialLayout) {
            stopAndHideWaveAnimation();
            hideTargets(false, false);
            mInitialLayout = false;
        }

        mOuterRing.setPositionX(newWaveCenterX);
        mOuterRing.setPositionY(newWaveCenterY);

        mPointCloud.setScale(mRingScaleFactor);

        mHandleDrawable.setPositionX(newWaveCenterX);
        mHandleDrawable.setPositionY(newWaveCenterY);

        updateTargetPositions(newWaveCenterX, newWaveCenterY);
        updatePointCloudPosition(newWaveCenterX, newWaveCenterY);
        updateGlowPosition(newWaveCenterX, newWaveCenterY);

        mWaveCenterX = newWaveCenterX;
        mWaveCenterY = newWaveCenterY;

        if (DEBUG) dump();
    }

    private void updateTargetPosition(int i, float centerX, float centerY) {
        final float angle = getAngle(getSliceAngle(), i);
        updateTargetPosition(i, centerX, centerY, angle);
    }

    private void updateTargetPosition(int i, float centerX, float centerY, float angle) {
        final float placementRadiusX = getRingWidth() / 2;
        final float placementRadiusY = getRingHeight() / 2;
        if (i >= 0) {
            ArrayList<TargetDrawable> targets = mTargetDrawables;
            final TargetDrawable targetIcon = targets.get(i);
            targetIcon.setPositionX(centerX);
            targetIcon.setPositionY(centerY);
            targetIcon.setX(placementRadiusX * (float) Math.cos(angle));
            targetIcon.setY(placementRadiusY * (float) Math.sin(angle));
        }
    }

    private void updateTargetPositions(float centerX, float centerY) {
        updateTargetPositions(centerX, centerY, false);
    }

    private void updateTargetPositions(float centerX, float centerY, boolean skipActive) {
        final int size = mTargetDrawables.size();
        final float alpha = getSliceAngle();
        // Reposition the target drawables if the view changed.
        for (int i = 0; i < size; i++) {
            if (!skipActive || i != mActiveTarget) {
                updateTargetPosition(i, centerX, centerY, getAngle(alpha, i));
            }
        }
    }

    private float getAngle(float alpha, int i) {
        return mFirstItemOffset + alpha * i;
    }

    private float getSliceAngle() {
        return (float) (-2.0f * Math.PI / mTargetDrawables.size());
    }

    private void updatePointCloudPosition(float centerX, float centerY) {
        mPointCloud.setCenter(centerX, centerY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPointCloud.draw(canvas);
        mOuterRing.draw(canvas);
        final int ntargets = mTargetDrawables.size();
        for (int i = 0; i < ntargets; i++) {
            TargetDrawable target = mTargetDrawables.get(i);
            if (target != null) {
                target.draw(canvas);
            }
        }
        mHandleDrawable.draw(canvas);
        if (mFakeHandleDrawable != null) {
            mFakeHandleDrawable.draw(canvas);
            updateHandleDrawablePositions(false);
        }
    }

    public void setOnTriggerListener(OnTriggerListener listener) {
        mOnTriggerListener = listener;
    }

    private float square(float d) {
        return d * d;
    }

    private float dist2(float dx, float dy) {
        return dx*dx + dy*dy;
    }

    private float getScaledGlowRadiusSquared() {
        final float scaledTapRadius;
        if (AccessibilityManager.getInstance(mContext).isEnabled()) {
            scaledTapRadius = TAP_RADIUS_SCALE_ACCESSIBILITY_ENABLED * mGlowRadius;
        } else {
            scaledTapRadius = mGlowRadius;
        }
        return square(scaledTapRadius);
    }

    private void announceTargets() {
        StringBuilder utterance = new StringBuilder();
        final int targetCount = mTargetDrawables.size();
        for (int i = 0; i < targetCount; i++) {
            String targetDescription = getTargetDescription(i);
            String directionDescription = getDirectionDescription(i);
            if (!TextUtils.isEmpty(targetDescription)
                    && !TextUtils.isEmpty(directionDescription)) {
                String text = String.format(directionDescription, targetDescription);
                utterance.append(text);
            }
        }
        if (utterance.length() > 0) {
            announceForAccessibility(utterance.toString());
        }
    }

    private String getTargetDescription(int index) {
        if (mTargetDescriptions == null || mTargetDescriptions.isEmpty()) {
            mTargetDescriptions = loadDescriptions(mTargetDescriptionsResourceId);
            if (mTargetDrawables.size() != mTargetDescriptions.size()) {
                Log.w(TAG, "The number of target drawables must be"
                        + " equal to the number of target descriptions.");
                return null;
            }
        }
        return mTargetDescriptions.get(index);
    }

    private String getDirectionDescription(int index) {
        if (mDirectionDescriptions == null || mDirectionDescriptions.isEmpty()) {
            mDirectionDescriptions = loadDescriptions(mDirectionDescriptionsResourceId);
            if (mTargetDrawables.size() != mDirectionDescriptions.size()) {
                Log.w(TAG, "The number of target drawables must be"
                        + " equal to the number of direction descriptions.");
                return null;
            }
        }
        return mDirectionDescriptions.get(index);
    }

    private ArrayList<String> loadDescriptions(int resourceId) {
        TypedArray array = getContext().getResources().obtainTypedArray(resourceId);
        final int count = array.length();
        ArrayList<String> targetContentDescriptions = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            String contentDescription = array.getString(i);
            targetContentDescriptions.add(contentDescription);
        }
        array.recycle();
        return targetContentDescriptions;
    }

    /// M: Modify it to return -1 when Incoming indicator view want to launche activity, used to handle the case when UnLockScreen is shown behind LockScreen, we should
    /// first go to unlock mode first, or the launched activity will be hided by Keyguard; At the same time, KeyguardSelectorView will check if target id is -1, if yes then
    /// it will chekc secure state and try go to unlock screen if Keyguard is secure
    public int getResourceIdForTarget(int index) {
        int resId;
        if (index >= 0) {
            final TargetDrawable drawable = mTargetDrawables.get(index);
            resId = drawable == null ? 0 : drawable.getResourceId();
        } else {
            resId = -1;
        }
        return resId;
    }

    public void setEnableTarget(int resourceId, boolean enabled) {
        for (int i = 0; i < mTargetDrawables.size(); i++) {
            final TargetDrawable target = mTargetDrawables.get(i);
            if (target.getResourceId() == resourceId) {
                target.setEnabled(enabled);
                break; // should never be more than one match
            }
        }
    }

    /**
     * Gets the position of a target in the array that matches the given resource.
     * @param resourceId
     * @return the index or -1 if not found
     */
    public int getTargetPosition(int resourceId) {
        for (int i = 0; i < mTargetDrawables.size(); i++) {
            final TargetDrawable target = mTargetDrawables.get(i);
            if (target.getResourceId() == resourceId) {
                return i; // should never be more than one match
            }
        }
        return -1;
    }

    private boolean replaceTargetDrawables(Resources res, int existingResourceId,
            int newResourceId) {
        if (existingResourceId == 0 || newResourceId == 0) {
            return false;
        }

        boolean result = false;
        final ArrayList<TargetDrawable> drawables = mTargetDrawables;
        final int size = drawables.size();
        for (int i = 0; i < size; i++) {
            final TargetDrawable target = drawables.get(i);
            if (target != null && target.getResourceId() == existingResourceId) {
                target.setDrawable(res, newResourceId);
                result = true;
            }
        }

        if (result) {
            requestLayout(); // in case any given drawable's size changes
        }

        return result;
    }

    /**
     * Searches the given package for a resource to use to replace the Drawable on the
     * target with the given resource id
     * @param component of the .apk that contains the resource
     * @param name of the metadata in the .apk
     * @param existingResId the resource id of the target to search for
     * @return true if found in the given package and replaced at least one target Drawables
     */
    public boolean replaceTargetDrawablesIfPresent(ComponentName component, String name,
                int existingResId) {
        if (existingResId == 0) return false;

        boolean replaced = false;
        if (component != null) {
            try {
                PackageManager packageManager = mContext.getPackageManager();
                // Look for the search icon specified in the activity meta-data
                Bundle metaData = packageManager.getActivityInfo(
                        component, PackageManager.GET_META_DATA).metaData;
                if (metaData != null) {
                    int iconResId = metaData.getInt(name);
                    if (iconResId != 0) {
                        Resources res = packageManager.getResourcesForActivity(component);
                        replaced = replaceTargetDrawables(res, existingResId, iconResId);
                    }
                }
            } catch (NameNotFoundException e) {
                Log.w(TAG, "Failed to swap drawable; "
                        + component.flattenToShortString() + " not found", e);
            } catch (Resources.NotFoundException nfe) {
                Log.w(TAG, "Failed to swap drawable from "
                        + component.flattenToShortString(), nfe);
            }
        }
        if (!replaced) {
            // Restore the original drawable
            replaceTargetDrawables(mContext.getResources(), existingResId, existingResId);
        }
        return replaced;
    }
    
    /**
     * M: Set a touch event Recepient for GlowPadView. If we find that user touches out of the center ring, 
     * then we will try to dispatch this motion event to the touch Recepient view first, if it consumes the
     * motion event, we will return true to indicate motion event finish, or we handle it again in normal process.
     * 
     * This is added for the IKeyguardLayer view, which is added in the LockScreen view hierarchy's back, but
     * we want it to handle the event before GlowPadView.
     * 
     * @param the Recepient view we will dispatch touch event to, make sure it located in the same view hierarchy
     */
    public void setTouchRecepient(View view) {
        if (view != null) {
            mTouchRecepient = view;
            updateGlobalLayoutListenr(true);
            refreshPositions();
        } else {
            mTouchRecepient = null;
            updateGlobalLayoutListenr(false);
        }
    }

    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean handled = false;
        if (mTouchRecepient != null) {
            handled = interceptTouchEvent(event);
        }
        return handled ? true : super.dispatchTouchEvent(event);
    }
    
    private boolean interceptTouchEvent(MotionEvent event) {
        int action = event.getAction();

        boolean shouldIntercept = mTouchIntercepted;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                shouldIntercept = !hitInCenterRing(event.getX(), event.getY());
                setShouldIntercept(shouldIntercept);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setShouldIntercept(false);
                break;
        }


        if (shouldIntercept) {
            MotionEvent containerEvent = MotionEvent.obtain(event);
            // Convert the motion event into the touch interceptor view's coordinates (from
            // owner view's coordinates)
            containerEvent.offsetLocation(mOwnerRecepientOffsets[0], mOwnerRecepientOffsets[1]);
            boolean retValue = mTouchRecepient.dispatchTouchEvent(containerEvent);
            containerEvent.recycle();
            return retValue;
        } else {
            return false;
        }
    }

    /// M: Update the offset between GlowPadView and touch Recepient view, simple get both raw location on screen,
    /// and then save the x and y offset
    private void refreshPositions() {
        if (mTouchRecepient == null) {
            return;
        }
        // Calculate the both view's bounds on screen
        mTouchRecepient.getLocationOnScreen(mTouchRecepientRawLocation);
        this.getLocationOnScreen(mOwnerRawLocation);
        mOwnerRecepientOffsets[0] = mOwnerRawLocation[0] - mTouchRecepientRawLocation[0];
        mOwnerRecepientOffsets[1] = mOwnerRawLocation[1] - mTouchRecepientRawLocation[1];
    }
    
    /// M: Add global layout listener if it is set before
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateGlobalLayoutListenr(true);
    }
    
    /// M: Remove global layout listener when this view is detached
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        updateGlobalLayoutListenr(false);
        setTouchRecepient(null);
    }
    
    /// M: The entrance to update the GlobalLayout listener, true to add false to remove
    private void updateGlobalLayoutListenr (boolean shouldEnable) {
        if (shouldEnable) {
            if (!mLayoutListenerSet) {
                addOnLayoutChangeListener(mOnLayoutListener);
                mLayoutListenerSet = true;
            }
        } else {
            removeOnLayoutChangeListener(mOnLayoutListener);
            mLayoutListenerSet = false;
        }
    }
    
    private void setShouldIntercept(boolean shouldIntercept) {
        mTouchIntercepted = shouldIntercept;
    }
    
    /// M: Hit test, check if this point is located in the center ring
    private boolean hitInCenterRing(float x, float y) {
        final float tx = x - mWaveCenterX;
        final float ty = y - mWaveCenterY;
        return (mAlwaysTrackFinger || dist2(tx,ty) <= getScaledGlowRadiusSquared());
    }
    
    public void setLockScreenView(LockScreenLayout lockScreenLayout) {
        mLockScreenLayout = lockScreenLayout;
    }
    
    public void syncUnReadEventView(UnReadEventView unReadEventView) {
        mUnReadEventView = unReadEventView;
        ArrayList<LockScreenNewEventView> newEventViews = mUnReadEventView.getNewEventViewList();
        if (newEventViews == null || newEventViews.size() <= 0) {
            Xlog.w(TAG, "syncUnReadEventView get empty newEventViews");
            return;
        }
        final int count = newEventViews.size();
        for (int i = 0; i < count; i++) {
            newEventViews.get(i).setOnTouchListener(this);
        }
        mSimpleOnGestureListener = new SimpleOnGestureListener();
        mGestureDetector = new GestureDetector(getContext(), mSimpleOnGestureListener);
        mGestureDetector.setIsLongpressEnabled(false);
        
        mTouchSlop = (int) (ANIM_TOUCH_STLOP * getContext().getResources().getDisplayMetrics().density);
        mTouchSlopSqure = mTouchSlop * mTouchSlop;
        mDragSnapMargin = (float) Math.sqrt(dist2(newEventViews.get(0).getNewEventBitmapWidth(), newEventViews.get(0).getNewEventBitmapHeight()));
    }
    
    private void animDragView(int duration, int delay, float x, float y, AnimatorListener onCompleteListener) {
        if (DEBUG) {
            Xlog.w(TAG, "animDragView duration=" + duration + ", delay=" + delay + ", x=" + x + ", y=" + y);
        }
        if (mDragView != null) {
            mDragViewAnimations.cancel();
            mDragViewAnimations.add(Tweener.to(mDragView, duration,
                    "ease", Ease.Fling.easeOut,
                    "delay", delay,
                    "translationX", x,
                    "translationY", y,
                    "onUpdate", mUpdateListener,
                    "onComplete", onCompleteListener));
            mDragViewAnimations.start();
        } else {
            Xlog.e(TAG, "animDragView mDragView is null");
        }
    }

    private void activateHandle(final int duration, final int delay) {
        if (DEBUG) {
            Xlog.w(TAG, "activateHandle");
        }
        if (mFakeHandleDrawable == null) {
            // Init fake handle drawable, for touch down animation
            mFakeHandleDrawable = new TargetDrawable(mHandleDrawable);
            mFakeHandleDrawable.setDrawable(this.getResources(), com.mediatek.internal.R.drawable.ic_lockscreen_fake_handle);
        } else {
            mFakeHandleDrawable.setEnabled(true);
        }
        mFakeHandleDrawable.setAlpha(0.0f);
        mFakeHandleDrawable.setScaleX(0.4f);
        mFakeHandleDrawable.setScaleY(0.4f);
        mFakeHandleDrawable.setPositionX(mWaveCenterX);
        mFakeHandleDrawable.setPositionY(mWaveCenterY);
        mFakeHandleRealX = 0.0f;
        mFakeHandleRealY = 0.0f;
        
        mHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
        mHandleDrawable.setAlpha(0.0f);
        mHandleDrawable.setScaleX(0.0f);
        mHandleDrawable.setScaleY(0.0f);
        
        mFakeHandleAnimations.cancel();
        mFakeHandleAnimations.add(Tweener.to(mFakeHandleDrawable, duration/2,
                "ease", Ease.Fling.easeIn,
                "delay", delay,
                "scaleX", 1.0f,
                "scaleY", 1.0f,
                "alpha", 1.0f,
                "onUpdate", mHandleDrawableUpdateListener,
                "onComplete", mFakeHandleDrawableOutListener));
        mFakeHandleAnimations.start();
    }
    
    private AnimatorListener mFakeHandleDrawableOutListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            if (DEBUG) {
                Xlog.d(TAG, "mFakeHandleDrawableOutListener mPendingDragViewState=" + converDragViewStateCodeToString(mPendingDragViewState) +
                        ", mDragViewState" + converDragViewStateCodeToString(mDragViewState));
            }
            mFakeHandleAnimating = false;
            switchDragViewToState(mPendingDragViewState, mPendingEvent.eventX, mPendingEvent.eventY, mPendingEvent.velocityX, mPendingEvent.velocityY);
        }
    };
    
    private void deactivateHandle(final int duration, final int delay) {
        if (DEBUG) {
            Xlog.d(TAG, "deactivateHandle");
        }
        mFakeHandleAnimations.cancel();
        mFakeHandleAnimations.add(Tweener.to(mFakeHandleDrawable, duration/2,
                "ease", Ease.Fling.easeOut,
                "delay", delay,
                "x", 0.0f,
                "y", 0.0f,
                "onUpdate", mHandleDrawableUpdateListener,
                "onComplete", new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animator) {
                        Log.d(TAG, "fake handle drawable back Listener onAnimationEnd");
                        mFakeHandleAnimations.cancel();
                        mFakeHandleAnimations.add(Tweener.to(mFakeHandleDrawable, duration/2,
                                "ease", Ease.Fling.easeIn,
                                "delay", delay,
                                "scaleX", 0.0f,
                                "scaleY", 0.0f,
                                "onUpdate", mHandleDrawableUpdateListener,
                                "onComplete", mFakeHandleDrawableResetListener));
                        mFakeHandleAnimations.start();
                    }}));
        mFakeHandleAnimations.start();
    }
        
    private void updateHandleDrawablePositions(boolean force) {
        if (mDragViewState != TOUCH_DRAGGING) {
            return;
        }
        if (mFakeHandleDrawable != null) {
            if (!force) {
                float oldX = mFakeHandleDrawable.getX();
                float oldY = mFakeHandleDrawable.getY();
                int deltaX = (int) (mCurrentFakeHandleTargetX - oldX);
                int deltaY = (int) (mCurrentFakeHandleTargetY - oldY);
                int distanceSqure = (deltaX * deltaX) + (deltaY * deltaY);
                if (distanceSqure > mTouchSlopSqure) {
                    int distance = (int) Math.sqrt(distanceSqure);
                    int offsetX = deltaX * mTouchSlop / distance;
                    int offsetY = deltaY * mTouchSlop / distance;
                    mFakeHandleRealX += offsetX;
                    mFakeHandleRealY += offsetY;
                    if (DEBUG) {
                        Xlog.d(TAG, "oldX=" + oldX + ", oldY=" + oldY + ", deltaX=" + deltaX + ", deltaY=" + deltaY + 
                                ", distance=" + distance + ", offsetX=" + offsetX + ", offsetY=" + offsetY + 
                                ", mFakeHandleRealX=" + mFakeHandleRealX + ", mFakeHandleRealY=" + mFakeHandleRealY);
                    }
                    invalidate();
                } else {
                    mFakeHandleRealX = mCurrentFakeHandleTargetX;
                    mFakeHandleRealY = mCurrentFakeHandleTargetY;
                }
            } else {
                mFakeHandleRealX = mCurrentFakeHandleTargetX;
                mFakeHandleRealY = mCurrentFakeHandleTargetY;
            }
            mFakeHandleDrawable.setX(mFakeHandleRealX);
            mFakeHandleDrawable.setY(mFakeHandleRealY);
        }
    }
    
    private AnimatorUpdateListener mHandleDrawableUpdateListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            invalidate();
        }
    };
    
    private AnimatorListener mHandleDrawableResetListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            mHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
            invalidate();
        }
    };
    
    private AnimatorListener mFakeHandleDrawableResetListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            Log.d(TAG, "mFakeHandleDrawableResetListener");
            mHandleDrawable.setScaleX(1.0f);
            mHandleDrawable.setScaleY(1.0f);
            mHandleDrawable.setAlpha(1.0f);
            invalidate();
            switchDragViewToState(TOUCH_END, 0, 0, 0.0f, 0.0f);
        }
    };
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (DEBUG) {
            Xlog.d(TAG, "onTouch event.action=" + event.getAction() + ", event.getX()=" + event.getX() + ", event.getY()=" + event.getY() +
                    ", event.getRawX()=" + event.getRawX() + ", event.getRawY()=" + event.getRawY() + ", v=" + v +
                    ", mCurrentNewEventView=" + mCurrentNewEventView + ", mDragViewDoingTouch=" + mDragViewDoingTouch + ", mGlowPadViewState=" + mGlowPadViewState);
        }
        if (mGlowPadViewState != STATE_IDLE) {
            return false;
        }
        LockScreenNewEventView newEventView = (LockScreenNewEventView)v;
        if (mCurrentNewEventView != null && v != mCurrentNewEventView) {
            return false;
        }
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mCurrentNewEventView = (LockScreenNewEventView)v;
           // Offset so that touch event to NewEvevtView's top left position, seems more accurate
            mPoint.set((int) (-event.getX()), (int) (-event.getY()));
            // Compute GlowPadView and NewEventView parent's relative position offset in LockScreenLayout
            computeOwnerNewEventViewMotionOffset(mCurrentNewEventView);
            mPoint.offset(-mOwnerNewEventViewOffset[0], -mOwnerNewEventViewOffset[1]);
        }
        event.offsetLocation(mPoint.x, mPoint.y);
        if (DEBUG) {
            Xlog.d(TAG, "onTouch event.getX()=" + event.getX() + ", event.getY()=" + event.getY() +
                    ", mPoint.x=" + mPoint.x + ", mPoint.y=" + mPoint.y);
        }
        boolean handled = mGestureDetector.onTouchEvent(event);
        if (DEBUG) {
            Xlog.d(TAG, "onTouch handled=" + handled + ", mTapTimeOut=" + mTapTimeOut + ", mTouchTriggered=" + mTouchTriggered);
        }
        if (!handled) {
            int eventX = (int)event.getX();
            int eventY = (int)event.getY();
            if (action == MotionEvent.ACTION_UP) {
                if (mTouchTriggered) {
                    switchDragViewToState(TOUCH_TRIGGER, eventX, eventY, 0.0f, 0.0f);
                } else {
                    switchDragViewToState(TOUCH_ANIM_BACK, eventX, eventY, 0.0f, 0.0f);
                }
            } else if (action == MotionEvent.ACTION_CANCEL) {
                if (mDragViewState != TOUCH_END) {
                    switchDragViewToState(TOUCH_ANIM_BACK, eventX, eventY, 0.0f, 0.0f);
                }
            }
        }
        return true;
    }
    
    private void hideOtherNewEventViews() {
        if (DEBUG) {
            Xlog.d(TAG, "hideOtherNewEventViews");
        }
        ArrayList<LockScreenNewEventView> newEventViweList = mUnReadEventView.getNewEventViewList();
        for (int i = 0; i < newEventViweList.size(); i++) {
            LockScreenNewEventView tmpNewEventView = newEventViweList.get(i);
            if (tmpNewEventView != mCurrentNewEventView) {
                ObjectAnimator objectAnimator = getZoomAnim(true, tmpNewEventView.getAlpha(), 0.0f,
                        tmpNewEventView.getScaleX(), 0.0f, tmpNewEventView.getScaleY(), 0.0f);
                objectAnimator.setTarget(tmpNewEventView);
                objectAnimator.start();
            }
        }
    }
    
    private void showOtherNewEventViews() {
        if (DEBUG) {
            Xlog.d(TAG, "showOtherNewEventViews");
        }
        ArrayList<LockScreenNewEventView> newEventViweList = mUnReadEventView.getNewEventViewList();
        for (int i = 0; i < newEventViweList.size(); i++) {
            LockScreenNewEventView tmpNewEventView = newEventViweList.get(i);
            if (tmpNewEventView != mCurrentNewEventView) {
                ObjectAnimator objectAnimator = getZoomAnim(false, tmpNewEventView.getAlpha(), 1.0f,
                        tmpNewEventView.getScaleX(), 1.0f, tmpNewEventView.getScaleY(), 1.0f);
                objectAnimator.setTarget(tmpNewEventView);
                objectAnimator.start();
            }
        }
    }
    
    private void hideAllNewEventViews() {
        if (DEBUG) {
            Xlog.d(TAG, "hideAllNewEventViews");
        }
        ArrayList<LockScreenNewEventView> newEventViweList = mUnReadEventView.getNewEventViewList();
        for (int i = 0; i < newEventViweList.size(); i++) {
            LockScreenNewEventView tmpNewEventView = newEventViweList.get(i);
            ObjectAnimator objectAnimator = getZoomAnim(true, tmpNewEventView.getAlpha(), 0.0f,
                    tmpNewEventView.getScaleX(), 0.0f, tmpNewEventView.getScaleY(), 0.0f);
            objectAnimator.setTarget(tmpNewEventView);
            objectAnimator.start();
        }
    }
    
    private void showAllNewEventViews() {
        if (DEBUG) {
            Xlog.d(TAG, "showAllNewEventViews");
        }
        ArrayList<LockScreenNewEventView> newEventViweList = mUnReadEventView.getNewEventViewList();
        for (int i = 0; i < newEventViweList.size(); i++) {
            LockScreenNewEventView tmpNewEventView = newEventViweList.get(i);
            ObjectAnimator objectAnimator = getZoomAnim(false, tmpNewEventView.getAlpha(), 1.0f,
                    tmpNewEventView.getScaleX(), 1.0f, tmpNewEventView.getScaleY(), 1.0f);
            objectAnimator.setTarget(tmpNewEventView);
            objectAnimator.start();
        }
    }
    
    private ObjectAnimator getZoomAnim(boolean zoomIn, float alphaFrom, float alphaTo, float scaleXFrom, float scaleXTo,
            float scaleYFrom, float scaleYTo) {
        ObjectAnimator targetViewAnimation;
        PropertyValuesHolder pvhAlphaZoom;
        PropertyValuesHolder pvhScaleXZoom;
        PropertyValuesHolder pvhScaleYZoom;
        pvhAlphaZoom = PropertyValuesHolder.ofFloat("alpha", alphaFrom, alphaTo);
        pvhScaleXZoom = PropertyValuesHolder.ofFloat("scaleX", scaleXFrom, scaleXTo);
        pvhScaleYZoom = PropertyValuesHolder.ofFloat("scaleY", scaleYFrom, scaleYTo);
        targetViewAnimation = ObjectAnimator.ofPropertyValuesHolder((Object)null, pvhAlphaZoom, pvhScaleXZoom, pvhScaleYZoom);
        targetViewAnimation.setDuration(DRAG_ANIMATION_DURATION).setStartDelay(SHOW_ANIMATION_DELAY);
        return targetViewAnimation;
    }
    
    private void computeOwnerNewEventViewMotionOffset(View newEventView) {
        if (newEventView != null && mLockScreenLayout != null) {
            mLockScreenLayout.getLocationInLockScreenLayout(newEventView, mNewEventViewLocationInLockScreen);
            mLockScreenLayout.getLocationInLockScreenLayout(this, mOwnerLocationInLockScreen);
            mOwnerNewEventViewOffset[0] = mOwnerLocationInLockScreen[0] - mNewEventViewLocationInLockScreen[0];
            mOwnerNewEventViewOffset[1] = mOwnerLocationInLockScreen[1] - mNewEventViewLocationInLockScreen[1];
            Log.d(TAG, "computeOwnerNewEventViewMotionOffset mOwnerLocationInLockScreen[0]=" + mOwnerLocationInLockScreen[0] +
                    ", mOwnerLocationInLockScreen[1]=" + mOwnerLocationInLockScreen[1] + ", mNewEventViewParentLocationInLockScreen[0]=" + mNewEventViewLocationInLockScreen[0] +
                    ", mNewEventViewLocationInLockScreen[1]=" + mNewEventViewLocationInLockScreen[1] + ", mOwnerNewEventViewOffset[0]=" + mOwnerNewEventViewOffset[0] +
                    ", mOwnerNewEventViewOffset[1]=" + mOwnerNewEventViewOffset[1]);
        }
    }
    
    // handlerLocationOnCircle saves the handler's location on circle, so that DragView knows 
    // the intersect anim's target position
    private void onDragStart(int x, int y) {
        if (DEBUG) {
            Xlog.d(TAG, "onDragStart");
        }
        // When touch down comes, we compute the dragTarget intersect animation's target location
        float dragTargetCenterX = x + mDragView.getWidth() * 0.5f - mWaveCenterX;
        float dragTargetCenterY = y + mDragView.getHeight() * 0.5f - mWaveCenterY;
        float touchRadius = (float) Math.sqrt(dist2(dragTargetCenterX, dragTargetCenterY));
        final float currentOuterRadius = mOuterRadius * 0.5f;
        final float scale = touchRadius > currentOuterRadius ? currentOuterRadius / touchRadius : 1.0f;
        float limitX = dragTargetCenterX * scale;
        float limitY = dragTargetCenterY * scale;
        
        // save handledrawable's center position
        mHandlePosition.x = (int) (limitX);
        mHandlePosition.y = (int) (limitY);
        
        mFakeHandleRealX = mWaveCenterX;
        mFakeHandleRealY = mWaveCenterY;
        
        mCurrentFakeHandleTargetX = mWaveCenterX;
        mCurrentFakeHandleTargetY = mWaveCenterY;
        invalidate();
    }
    
    private void onDragMove(int x, int y) {
        // tx and ty are relative to wave center
        float tx = x - mWaveCenterX;
        float ty = y - mWaveCenterY;
        float touchRadius = (float) Math.sqrt(dist2(tx, ty));
        // When handling touch handler, set outer radius smaller, or trigger seems too easy
        final float FakeHandleRadius = mOuterRadius * 0.5f;
        final float scale = touchRadius > FakeHandleRadius ? FakeHandleRadius / touchRadius : 1.0f;
        mCurrentFakeHandleTargetX = tx * scale;
        mCurrentFakeHandleTargetY = ty * scale;
        updateHandleDrawablePositions(false);
        
        final float currentFakeHandleRadius = (float) Math.sqrt(dist2(mFakeHandleRealX, mFakeHandleRealY));
        final float snapRadius = currentFakeHandleRadius + mDragSnapMargin;
        final float snapDistance2 = snapRadius * snapRadius;
        if (dist2(tx, ty) < snapDistance2) {
            mTouchTriggered = true;
            int dragViewX = (int) (mCurrentFakeHandleTargetX + mWaveCenterX) - mCurrentNewEventView.getNewEventImageView().getWidth()/2;
            int dragViewY = (int) (mCurrentFakeHandleTargetY + mWaveCenterY) - mCurrentNewEventView.getNewEventImageView().getHeight()/2;
            mDragView.move(dragViewX + mOwnerLocationInLockScreen[0], dragViewY + mOwnerLocationInLockScreen[1]);
            updateHandleDrawablePositions(true);
            int [] state = mFakeHandleDrawable.getState();
            if (state != null && !Arrays.equals(state, TargetDrawable.STATE_FOCUSED)) {
                vibrate();
                mFakeHandleDrawable.setState(TargetDrawable.STATE_FOCUSED);
            }
        } else {
            mTouchTriggered = false;
            mFakeHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
        }
        invalidate();
        if (DEBUG) {
            Xlog.d(TAG, "onDragMove mTouchTriggered=" + mTouchTriggered + ", x=" + x + ", y=" + y + ",");
        }
    }

    private void freeFling(float velX, float velY) {
        Xlog.d(TAG, "freeFling velX=" + velX + ", velY=" + velY);
        switchDragViewToState(TOUCH_ANIM_BACK, 0, 0, 0.0f, 0.0f);
    }
    
    private void animHandlerBack(int duration, int delay) {
        Xlog.d(TAG, "animHandlerBack");
        animDragView(duration, delay, 0.0f, 0.0f, null);
        deactivateHandle(duration, delay);
    }
    
    private void doIntersectAnimation() {
        if (DEBUG) {
            Xlog.d(TAG, "doIntersectAnimation");
        }
        int targetX = mHandlePosition.x + (int)mWaveCenterX;
        int targetY = mHandlePosition.y + (int)mWaveCenterY;
        targetX += mOwnerNewEventViewOffset[0];
        targetX -= mCurrentNewEventView.getNewEventImageView().getWidth()/2;
        targetY += mOwnerNewEventViewOffset[1];
        targetY -= mCurrentNewEventView.getNewEventImageView().getHeight()/2;
        animDragView(DRAG_ANIMATION_DURATION, DRAG_ANIMATION_DELAY, targetX, targetY, null);
        
        mFakeHandleAnimations.cancel();
        mFakeHandleAnimations.add(Tweener.to(mFakeHandleDrawable, DRAG_ANIMATION_DURATION,
                "ease", Ease.Fling.easeOut,
                "delay", DRAG_ANIMATION_DELAY,
                "x", (float)mHandlePosition.x,
                "y", (float)mHandlePosition.y,
                "scaleX", 1.0f,
                "scaleY", 1.0f,
                "alpha", 1.0f,
                "onUpdate", mHandleDrawableUpdateListener,
                "onComplete", mIntersectAnimResetListener));
        mFakeHandleAnimations.start();
    }
    
    private AnimatorListener mIntersectAnimResetListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            Xlog.d(TAG, "mIntersectAnimResetListener onAnimationEnd");
            switchDragViewToState(TOUCH_ANIM_BACK, 0, 0, 0.0f, 0.0f);
        }
    };
    
    private void invlidateGlobalRegion(View childView) {
        int width = childView.getWidth();
        int height = childView.getHeight();
        RectF childBounds = new RectF(0, 0, width, height);
        childBounds.offset(childView.getX() - width/2, childView.getY() - height/2);
        View view = this;
        while (view.getParent() != null && view.getParent() instanceof View) {
            view = (View) view.getParent();
            view.getMatrix().mapRect(childBounds);
            view.invalidate((int) Math.floor(childBounds.left),
                    (int) Math.floor(childBounds.top),
                    (int) Math.ceil(childBounds.right),
                    (int) Math.ceil(childBounds.bottom));
        }
    }

    private void handleTouchCancel(MotionEvent event) {
        handleUp(event);
    }
    
    final class IntersectAnimationRunnable implements Runnable {
        @Override
        public void run() {
            if (mDragging) {
                mShouldDoIntersectAnim = false;
            }
        }
    }
    
    private void handleTrigger(LockScreenNewEventView newEventView) {
        ///M: In case unlock blocks newly launched activity, we should direct ueser to unlock mode first
        if (mOnTriggerListener != null) {
            mOnTriggerListener.onTrigger(newEventView, -1);
        }
        Intent intent = new Intent();
        int resourceId = newEventView.getResourceId();
        switch (resourceId) {
            case com.mediatek.internal.R.drawable.ic_newevent_smsmms:
                intent.setComponent(new ComponentName(MMS_PACKAGE_NAME, MMS_CLASS_NAME));
                launchActivity(intent);
                break;
            case com.mediatek.internal.R.drawable.ic_newevent_phone:
                intent.setComponent(new ComponentName(CALL_LOG_PACKAGE_NAME, CALL_LOG_CLASS_NAME));
                launchActivity(intent);
                break;
            case com.mediatek.internal.R.drawable.ic_newevent_email:
                intent.setComponent(new ComponentName(EMAIL_PACKAGE_NAME, EMAIL_CLASS_NAME));
                launchActivity(intent);
                break;
            default:
                Xlog.d(TAG, "handleTrigger unknown resource id, resourceId=" + resourceId);
        }
    }
    
    private void launchActivity(Intent intent) {
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (RemoteException e) {
            Xlog.w(TAG, "can't dismiss keyguard on launch");
        }
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Xlog.w(TAG, "Activity not found for intent + " + intent.getAction());
        }
    }
    
    /**
     * Returns a new bitmap to show when the given View is being dragged around.
     * Responsibility for the bitmap is transferred to the caller.
     */
    public Bitmap createDragBitmap(View v, Canvas canvas) {
        /// M: added for theme feature, get different outline color for different themes.
        
        Bitmap b = Bitmap.createBitmap(
                v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(b);
        drawDragView(v, canvas, true);
        canvas.setBitmap(null);

        return b;
    }
    
    /**
     * Draw the View v into the given Canvas.
     *
     * @param v the view to draw
     * @param destCanvas the canvas to draw on
     * @param padding the horizontal and vertical padding to use when drawing
     */
    private void drawDragView(View v, Canvas destCanvas, boolean pruneToDrawable) {
        final Rect clipRect = mTempRect;
        v.getDrawingRect(clipRect);

        boolean textVisible = false;

        destCanvas.save();
        destCanvas.clipRect(clipRect, Op.REPLACE);
        v.draw(destCanvas);
        destCanvas.restore();
    }
    
    private class SimpleOnGestureListener implements OnGestureListener {
        
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Xlog.d(TAG, "onSingleTapUp");
            // If user pressed and does not move around within tap timeout, then this will be a 
            // single tap; we should return true to indicate we consumed touch up event. Else we return false.
            if (!mTapTimeOut) {
                switchDragViewToState(TOUCH_INTERSECT, (int)e.getX(), (int)e.getY(), 0.0f, 0.0f);
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public void onShowPress(MotionEvent e) {
            Xlog.d(TAG, "onShowPress");
            mTapTimeOut = true;
            switchDragViewToState(TOUCH_DRAGGING, (int)e.getX(), (int)e.getY(), 0.0f, 0.0f);
        }
        
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (DEBUG) {
                Xlog.v(TAG, "*** onScroll *** mDragViewState=" + converDragViewStateCodeToString(mDragViewState) +
                        ", mPendingDragViewState=" + converDragViewStateCodeToString(mPendingDragViewState));
            }
            switchDragViewToState(TOUCH_DRAGGING, (int)e2.getX(), (int)e2.getY(), 0.0f, 0.0f);
            return true;
        }
        
        @Override
        public void onLongPress(MotionEvent e) {
            // TODO Auto-generated method stub
            Xlog.d(TAG, "onLongPress");
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Xlog.d(TAG, "onFling " + velocityX + ", " + velocityY);
            
            if (mTouchTriggered) {
                switchDragViewToState(TOUCH_TRIGGER, (int)e2.getX(), (int)e2.getY(), velocityX, velocityY);
            } else {
                switchDragViewToState(TOUCH_FLING, (int)e2.getX(), (int)e2.getY(), velocityX, velocityY);
            }
            return true;
        }
        
        @Override
        public boolean onDown(MotionEvent event) {
            Xlog.d(TAG, "onDown");
            mTapTimeOut = false;
            mPendingDragViewState = -1;
            switchDragViewToState(TOUCH_INIT, (int)event.getX(), (int)event.getY(), 0.0f, 0.0f);
            return true;
        }
    };
    
    private String converDragViewStateCodeToString(int state) {
        switch (state) {
            case TOUCH_INIT:
                return "TOUCH_INIT";
            case TOUCH_HANDLE_ANIM:
                return "TOUCH_HANDLE_ANIM";
            case TOUCH_INTERSECT:
                return "TOUCH_INTERSECT";
            case TOUCH_DRAGGING:
                return "TOUCH_DRAGGING";
            case TOUCH_FLING:
                return "TOUCH_FLING";
            case TOUCH_TRIGGER:
                return "TOUCH_TRIGGER";
            case TOUCH_ANIM_BACK:
                return "TOUCH_ANIM_BACK";
            case TOUCH_END:
                return "TOUCH_END";
            default:
                return null;
        }
    }
    
    private void switchDragViewToState(int state, int eventX, int eventY, float velocityX, float velocityY) {
        Xlog.d(TAG, "switchDrageViewToState enter mDragViewState=" + converDragViewStateCodeToString(mDragViewState) + ", new state=" + converDragViewStateCodeToString(state) +
                "mPendingDragViewState=" + converDragViewStateCodeToString(mPendingDragViewState) + ", eventX=" + eventX + ", eventY=" + eventY);
        if (mDragViewState == TOUCH_HANDLE_ANIM && mFakeHandleAnimating) {
            mPendingDragViewState = state;
            mPendingEvent.eventX = eventX;
            mPendingEvent.eventY = eventY;
            mPendingEvent.velocityX = velocityX;
            mPendingEvent.velocityY = velocityY;
            Xlog.d(TAG, "switchDrageViewToState exit, mDragViewState=" + mDragViewState + ", mPendingDragViewState=" + mPendingDragViewState);
            return;
        }
        switch (state) {
            case TOUCH_INIT:
                mOnTriggerListener.onGrabbedStateChange(this, OnTriggerListener.CENTER_HANDLE);
                mDragViewState = TOUCH_INIT;
                mDragViewDoingTouch = true;
                mFakeHandleAnimating = false;
                final Bitmap b = createDragBitmap(mCurrentNewEventView, mCanvas);
                mNewEventViewInLockScreenX = eventX;
                mNewEventViewInLockScreenY = eventY;
                // Add DragView, need to offset a little in, so that touch event seems more accurate
                final int registrationX = mNewEventViewInLockScreenX + mOwnerLocationInLockScreen[0];
                final int registrationY = mNewEventViewInLockScreenY + mOwnerLocationInLockScreen[1];
                if (DEBUG) {
                    Xlog.v(TAG, "handleTouchDown registrationX=" + registrationX +
                            ", registrationY=" + registrationY);
                }
                mDragView = new DragView(mLockScreenLayout, b, registrationX,
                        registrationY, 0, 0, b.getWidth(), b.getHeight(), mCurrentNewEventView.getScaleX());
                mDragView.show(registrationX, registrationY);
                // Will animate center handle to touch intersect point
                onDragStart(eventX + mCurrentNewEventView.getNewEventImageView().getWidth()/2, 
                        eventY + mCurrentNewEventView.getNewEventImageView().getHeight()/2);
              
                mCurrentNewEventView.setVisibility(View.INVISIBLE);
                hideOtherNewEventViews();
            case TOUCH_HANDLE_ANIM:
                mDragViewState = TOUCH_HANDLE_ANIM;
                mFakeHandleAnimating = true;
                activateHandle(100, 0);
                break;
            case TOUCH_INTERSECT:
                mDragViewState = TOUCH_INTERSECT;
                doIntersectAnimation();
                break;
            case TOUCH_DRAGGING:
                if (mDragViewState != TOUCH_DRAGGING) {
                    
                }
                mDragViewState = TOUCH_DRAGGING;
                mDragView.move(eventX + mOwnerLocationInLockScreen[0], eventY + mOwnerLocationInLockScreen[1]);
                onDragMove(eventX + mCurrentNewEventView.getNewEventImageView().getWidth()/2, 
                        eventY + mCurrentNewEventView.getNewEventImageView().getHeight()/2);
                break;
            case TOUCH_FLING:
                mDragViewState = TOUCH_FLING;
                freeFling(velocityX, velocityY);
                break;
            case TOUCH_ANIM_BACK:
                // In case action cancel cause animation back, so we reset trigger state here and also reset
                // fake handle to inactive state
                mTouchTriggered = false;
                mFakeHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
                mDragViewState = TOUCH_ANIM_BACK;
                showOtherNewEventViews();
                animHandlerBack(DRAG_BACK_ANIMATION_DURATION, DRAG_BACK_ANIMATION_DELAY);
                break;
            case TOUCH_TRIGGER:
                mDragViewState = TOUCH_TRIGGER;
                handleTrigger(mCurrentNewEventView);
                break;
            case TOUCH_END:
                // (1) reset fake handle drawable, set it invisible, (2) set current NewEventView visible (3) remove DragView
                if (mFakeHandleDrawable != null) {
                    mFakeHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
                    mFakeHandleDrawable.setEnabled(false);
                }
                if (mHandleDrawable != null) {
                    mHandleDrawable.setState(TargetDrawable.STATE_INACTIVE);
                }
                
                if (mCurrentNewEventView != null) {
                    mCurrentNewEventView.setVisibility(View.VISIBLE);
                    mCurrentNewEventView = null;
                }
                if (mDragView != null) {
                    mDragView.remove();
                    mDragView = null;
                }
                mFakeHandleRealX = mWaveCenterX;
                mFakeHandleRealY = mWaveCenterY;
                mCurrentFakeHandleTargetX = mWaveCenterX;
                mCurrentFakeHandleTargetY = mWaveCenterY;
                mDragViewState = TOUCH_END;
                mPendingDragViewState = -1;
                break;
        }
        Xlog.d(TAG, "switchDrageViewToState exit, mDragViewState=" + mDragViewState + ", mPendingDragViewState=" + mPendingDragViewState);
    }
    
    /// M: for test case to get wave center
    public float getWaveCenterX() {
        return mWaveCenterX;
    }
    
    /// M: for test case to get wave center
    public float getWaveCenterY() {
        return mWaveCenterY;
    }
    
    public void doShakeAnimation() {
        if (mLockScreenLayout != null) {
            mLockScreenLayout.beginShakeAnimation();
        }
    }
}
