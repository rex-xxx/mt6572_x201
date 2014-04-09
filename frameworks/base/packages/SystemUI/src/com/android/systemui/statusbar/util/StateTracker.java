package com.android.systemui.statusbar.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.view.View;
import android.widget.ImageView;

import com.mediatek.xlog.Xlog;

/**
 * M: [SystemUI] Support "Notification toolbar".
 * 
 * The state machine for a setting's toggling, tracking reality versus the
 * user's intent.
 * 
 * This is necessary because reality moves relatively slowly (turning on &amp;
 * off radio drivers), compared to user's expectations.
 */
public abstract class StateTracker {
    private static final String TAG = "StateTracker";
    private static final boolean DBG = true;
    public static final int STATE_DISABLED = 0;
    public static final int STATE_ENABLED = 1;
    public static final int STATE_TURNING_ON = 2;
    public static final int STATE_TURNING_OFF = 3;
    public static final int STATE_INTERMEDIATE = -1;
    public static final int STATE_UNKNOWN = -2;
    // Is the state in the process of changing?
    protected boolean mInTransition = false;
    private Boolean mActualState = null; // initially not set
    private Boolean mIntendedState = null; // initially not set

    // Did a toggle request arrive while a state update was
    // already in-flight? If so, the mIntendedState needs to be
    // requested when the other one is done, unless we happened to
    // arrive at that state already.
    private boolean mDeferredStateChangeRequestNeeded = false;
    
    // User clicks button and phone is doing switching background, this flag indicates 
    // whether switching is ongoing
    protected boolean mIsUserSwitching = false;
    
    public boolean isClickable() {
        Xlog.i(TAG, this + " mIsUserSwitching is " + mIsUserSwitching);
        return !mIsUserSwitching;
    }
    
    public void setIsUserSwitching(boolean enable) {
        mIsUserSwitching = enable;
    }
    
    public boolean getIsUserSwitching() {
        return mIsUserSwitching;
    }

    /**
     * User pressed a button to change the state. Something should immediately
     * appear to the user afterwards, even if we effectively do nothing. Their
     * press must be heard.
     */
    public void toggleState(Context context) {
        Xlog.i("ClickEvent", "toggleState");
        mIsUserSwitching = true;
        getTileView().setEnabled(isClickable());
        int currentState = getTriState(context);
        boolean newState = false;
        switch (currentState) {
        case STATE_ENABLED:
            newState = false;
            break;
        case STATE_DISABLED:
            newState = true;
            break;
        case STATE_INTERMEDIATE:
            if (mIntendedState != null) {
                newState = !mIntendedState;
            }
            break;
        default:
            break;
        }
        mIntendedState = newState;
        if (mInTransition) {
            // We don't send off a transition request if we're
            // already transitioning. Makes our state tracking
            // easier, and is probably nicer on lower levels.
            // (even though they should be able to take it...)
            mDeferredStateChangeRequestNeeded = true;
        } else {
            mInTransition = true;
            requestStateChange(context, newState);
        }
    }

    // /**
    // * Return the ID of the main large image button for the setting.
    // */
    // public abstract int getButtonId();
    //
    // /**
    // * Returns the small indicator image ID underneath the setting.
    // */
    // public abstract int getIndicatorId();
    //
    // /**
    // * Returns the resource ID of the image to show as a function of the
    // * on-vs-off state.
    // */
    // public abstract int getButtonImageId(boolean on);

    public abstract ImageView getImageButtonView();

    public abstract View getTileView();
    
    public abstract ImageView getIndicatorView();
    
    public ImageView getSwitchingGifView() {
        return null;
    }

    public abstract int getDisabledResource();

    public abstract int getEnabledResource();
    
    public int getInterMedateResource() {
        return -1;
    }

    /**
     * Updates the remote views depending on the state (off, on, turning off,
     * turning on) of the setting.
     */
    public void setImageViewResources(Context context) {
        // int buttonId = getButtonId();
        // int indicatorId = getIndicatorId();
        if (DBG) {
            Xlog.i(TAG, this +  "setImageViewResources state is " + getTriState(context));
        }
        ImageView indicatorView = getIndicatorView();
        ImageView mtkImageView;
        AnimationDrawable mFrameAnimation;
        switch (getTriState(context)) {
        case STATE_DISABLED:
            mIsUserSwitching = false;
            getTileView().setEnabled(isClickable());
            getImageButtonView().setVisibility(View.VISIBLE);
            getImageButtonView().setImageResource(getDisabledResource());
            if (indicatorView != null) {
                indicatorView.setVisibility(View.GONE);
            }
            mtkImageView = getSwitchingGifView();
            if (mtkImageView != null) {
                mtkImageView.setVisibility(View.GONE);
                mFrameAnimation = (AnimationDrawable)mtkImageView.getDrawable();
                if (mFrameAnimation != null && mFrameAnimation.isRunning()) {
                    mFrameAnimation.stop();
                }
            }
            break;
        case STATE_ENABLED:
            mIsUserSwitching = false;
            getTileView().setEnabled(isClickable());
            getImageButtonView().setVisibility(View.VISIBLE);
            getImageButtonView().setImageResource(getEnabledResource());
            if (indicatorView != null) {
                indicatorView.setVisibility(View.VISIBLE);
            }
            mtkImageView = getSwitchingGifView();
            if (mtkImageView != null) {
                mtkImageView.setVisibility(View.GONE);
                mFrameAnimation = (AnimationDrawable) mtkImageView.getDrawable();
                if (mFrameAnimation != null && mFrameAnimation.isRunning()) {
                    mFrameAnimation.stop();
                }
            }
            break;
        case STATE_INTERMEDIATE:
            // In the transitional state, the bottom green bar
            // shows the tri-state (on, off, transitioning), but
            // the top dark-gray-or-bright-white logo shows the
            // user's intent. This is much easier to see in
            // sunlight.
            getTileView().setEnabled(false);
            mtkImageView = getSwitchingGifView();
            if (getInterMedateResource() != -1 && mtkImageView != null) {
                getImageButtonView().setVisibility(View.GONE);
                if (indicatorView != null) {
                    indicatorView.setVisibility(View.GONE);
                }
                if (mtkImageView != null) {
                    mtkImageView.setVisibility(View.VISIBLE);
                    mtkImageView.setImageResource(getInterMedateResource());
                    mFrameAnimation = (AnimationDrawable) mtkImageView.getDrawable();
                    if (mFrameAnimation != null && !mFrameAnimation.isRunning()) {
                        mFrameAnimation.start();
                    }
                }
            }
            break;
        default:
            break;
        }
    }

    /**
     * Update internal state from a broadcast state change.
     */
    public abstract void onActualStateChange(Context context, Intent intent);

    /**
     * Sets the value that we're now in. To be called from onActualStateChange.
     * 
     * @param newState one of STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON, STATE_TURNING_OFF, STATE_UNKNOWN
     */
    public final void setCurrentState(Context context, int newState) {
        Xlog.v(TAG, "setCurrentState: newState is " + newState);
        final boolean wasInTransition = mInTransition;
        switch (newState) {
        case STATE_DISABLED:
            mInTransition = false;
            mActualState = false;
            break;
        case STATE_ENABLED:
            mInTransition = false;
            mActualState = true;
            break;
        case STATE_TURNING_ON:
            mInTransition = true;
            mActualState = false;
            break;
        case STATE_TURNING_OFF:
            mInTransition = true;
            mActualState = true;
            break;
        default:
            break;
        }

        if (wasInTransition && !mInTransition) {
            if (mDeferredStateChangeRequestNeeded) {
                Xlog.v(TAG, "processing deferred state change");
                if (mActualState != null && mIntendedState != null
                        && mIntendedState.equals(mActualState)) {
                    Xlog.v(TAG, "... but intended state matches, so no changes.");
                } else if (mIntendedState != null) {
                    mInTransition = true;
                    requestStateChange(context, mIntendedState);
                }
                mDeferredStateChangeRequestNeeded = false;
            }
        }
    }

    /**
     * If we're in a transition mode, this returns true if we're transitioning
     * towards being enabled.
     */
    public final boolean isTurningOn() {
        return mIntendedState != null && mIntendedState;
    }

    /**
     * Returns simplified 3-state value from underlying 5-state.
     * 
     * @param context
     * @return STATE_ENABLED, STATE_DISABLED, or STATE_INTERMEDIATE
     */
    public final int getTriState(Context context) {
        if (mInTransition) {
            // If we know we just got a toggle request recently
            // (which set mInTransition), don't even ask the
            // underlying interface for its state. We know we're
            // changing. This avoids blocking the UI thread
            // during UI refresh post-toggle if the underlying
            // service state accessor has coarse locking on its
            // state (to be fixed separately).
            return STATE_INTERMEDIATE;
        }
        switch (getActualState(context)) {
        case STATE_DISABLED:
            return STATE_DISABLED;
        case STATE_ENABLED:
            return STATE_ENABLED;
        default:
            return STATE_INTERMEDIATE;
        }
    }

    /**
     * Gets underlying actual state.
     * 
     * @param context
     * @return STATE_ENABLED, STATE_DISABLED, STATE_ENABLING, STATE_DISABLING, or or STATE_UNKNOWN.
     */
    public abstract int getActualState(Context context);

    /**
     * Actually make the desired change to the underlying radio API.
     */
    protected abstract void requestStateChange(Context context, boolean desiredState);
}