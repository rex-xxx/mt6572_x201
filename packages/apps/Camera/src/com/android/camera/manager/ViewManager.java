package com.android.camera.manager;

import android.view.View;
import android.view.animation.Animation;

import com.android.camera.Camera;
import com.android.camera.Log;
import com.android.camera.SettingUtils;
import com.android.camera.Util;

public abstract class ViewManager implements Camera.OnOrientationListener {
    private static final String TAG = "ViewManager";
    private static final boolean LOG = Log.LOGV;

    public static final int VIEW_LAYER_BOTTOM = -1;
    public static final int VIEW_LAYER_NORMAL = 0;
    public static final int VIEW_LAYER_TOP = 1;
    public static final int VIEW_LAYER_SHUTTER = 2;
    public static final int VIEW_LAYER_SETTING = 3;
    public static final int VIEW_LAYER_OVERLAY = 4;

    public static final int UNKNOWN = -1;

    private Camera mContext;
    private View mView;
    private final int mViewLayer;
    private boolean mShowing;
    private int mOrientation;
    private boolean mEnabled = true;
    private boolean mFilter = true;
    private Animation mFadeIn;
    private Animation mFadeOut;
    private boolean mShowAnimationEnabled = true;
    private boolean mHideAnimationEnabled = true;
    private int mConfigOrientation = UNKNOWN;

    public ViewManager(Camera context, int layer) {
        mContext = context;
        mContext.addViewManager(this);
        mContext.addOnOrientationListener(this);
        mOrientation = mContext.getOrientationCompensation();
        mViewLayer = layer;
    }

    public ViewManager(Camera context) {
        this(context, VIEW_LAYER_NORMAL);
    }

    public final Camera getContext() {
        return mContext;
    }

    public int getViewLayer() {
        return mViewLayer;
    }

    public void setFileter(boolean filter) {
        mFilter = filter;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public boolean isShowing() {
        return mShowing;
    }

    public boolean isEnabled() {
        return mEnabled;
    }
    
    public void setAnimationEnabled(boolean showAnimationEnabled, boolean hideAnimationEnabled) {
        mShowAnimationEnabled = showAnimationEnabled;
        mHideAnimationEnabled = hideAnimationEnabled;
    }
    
    public boolean getShowAnimationEnabled() {
        return mShowAnimationEnabled;
    }
    
    public boolean getHideAnimationEnabled() {
        return mHideAnimationEnabled;
    }

    public void show() {
        if (LOG) {
            Log.v(TAG, "show() " + this);
        }
        if (mView == null) {
            mConfigOrientation = mContext.getResources().getConfiguration().orientation;
            mView = getView();
            getContext().addView(mView, mViewLayer);
            Util.setOrientation(mView, mOrientation, false);
        }
        if (mView != null && !mShowing) {
            mShowing = true;
            setEnabled(mEnabled);
            refresh();// refresh view state
            fadeIn();
            mView.setVisibility(View.VISIBLE);
        } else if (mShowing) {
            refresh();
        }
    }
    
    public void checkConfiguration() {
        int newConfigOrientation = mContext.getResources().getConfiguration().orientation;
        if (LOG) {
            Log.v(TAG, "checkConfiguration() mConfigOrientation=" + mConfigOrientation
                    + ", newConfigOrientation=" + newConfigOrientation + ", this=" + this);
        }
        if (mConfigOrientation != UNKNOWN && newConfigOrientation != mConfigOrientation) {
            reInflate();
        }
    }

    protected void fadeIn() {
        if (mShowAnimationEnabled) {
            if (mFadeIn == null) {
                mFadeIn = getFadeInAnimation();
            }
            if (mFadeIn != null) {
                mView.startAnimation(mFadeIn);
            } else {
                Util.fadeIn(mView);
            }
        }
    }

    public void hide() {
        if (LOG) {
            Log.v(TAG, "hide() " + this);
        }
        if (mView != null && mShowing) {
            mShowing = false;
            fadeOut();
            mView.setVisibility(View.GONE);
        }
    }

    protected void fadeOut() {
        if (mHideAnimationEnabled) {
            if (mFadeOut == null) {
                mFadeOut = getFadeOutAnimation();
            }
            if (mFadeOut != null) {
                mView.startAnimation(mFadeOut);
            } else {
                Util.fadeOut(mView);
            }
        }
    }

    public final View inflate(int layoutId) {
        return getContext().inflate(layoutId, mViewLayer);
    }

    public final void reInflate() {
        boolean showing = mShowing;
        hide();
        if (mView != null) {
            getContext().removeView(mView, mViewLayer);
        }
        onRelease();
        mView = null;
        if (showing) {
            show();
        }
    }

    public final void refresh() {
        if (mShowing) {
            onRefresh();
        }
    }

    public final void release() {
        hide();
        if (mView != null) {
            getContext().removeView(mView, mViewLayer);
        }
        onRelease();
        mView = null;
        mContext.removeViewManager(this);
        mContext.removeOnOrientationListener(this);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (mOrientation != orientation) {
            mOrientation = orientation;
            Util.setOrientation(mView, mOrientation, true);
        }
    }

    public boolean collapse(boolean force) {
        return false;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        if (mView != null) {
            mView.setEnabled(mEnabled);
            if (mFilter) {
                SettingUtils.setEnabledState(mView, mEnabled);
            }
        }
    }

    /**
     * will be called when app call release() to unload views from view hierarchy.
     */
    protected void onRelease() {
    }

    /**
     * Will be called when App call refresh and isShowing().
     */
    protected void onRefresh() {
    }

    /**
     * will be called if app want to show current view which hasn't been created.
     * 
     * @return
     */
    protected abstract View getView();

    protected Animation getFadeInAnimation() {
        return null;
    }

    protected Animation getFadeOutAnimation() {
        return null;
    }
}
