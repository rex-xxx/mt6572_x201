package com.android.internal.policy.impl.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManagerNative;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.HashMap;

public class LockScreenLayout extends FrameLayout {
    private static final boolean DEBUG = false;
    private static final String TAG = "LockScreenLayout";
    
    protected static final int INVALID_POINTER = -1;
    
    private final Rect mTempRect = new Rect();
    private UnReadEventView mUnReadEventView;
    // The view we drag around, we will draw the touched NewEventView on this View's canvas and move it around
    
    private float mLockScreenX;
    
    private float mLockScreenY;
    
    
    final int DRAG_BITMAP_PADDING = 0;
    
    // Indicate the drag target has accepted the draw source
    private boolean mTouchTriggered;
    
    // Animation parameters
    private static final int ANIMATE_BACK_DURATION = 200;
    private static final int ANIMATE_BACK_DELAY = 200;
    
    // UnReadEvent state machine
    private static final int TOUCH_RESET = 0;
    private static final int TOUCH_DOWN_EMPTY = 1;
    private static final int TOUCH_MOVE_EMPTY = 2;
    private static final int TOUCH_UP_EMPTY = 3;
    private static final int TOUCH_DOWN_ICON = 4;
    private static final int TOUCH_MOVE_ICON_DRAG_START = 5;
    private static final int TOUCH_MOVE_ICON_DRAG_MOVE = 6;
    private static final int TOUCH_MOVE_ICON_DRAG_TRIGGER = 7;
    private static final int TOUCH_UP_ICON = 8;
    
    private ArrayList<UnReadHintAnimation>
    mUnReadHintAnimations = new ArrayList<UnReadHintAnimation>();
    
    public LockScreenLayout(Context context) {
        super(context);
    }

    public LockScreenLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMotionEventSplittingEnabled(false);
    }
    
    public void setUnReadEventView(UnReadEventView unReadEventView) {
        mUnReadEventView = unReadEventView;
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            final FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) child.getLayoutParams();
            if (flp instanceof LayoutParams) {
                final LayoutParams lp = (LayoutParams) flp;
                if (lp.customPosition) {
                    child.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
                }
            }
        }
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mUnReadEventView == null) {
            return super.dispatchTouchEvent(ev);
        }

        if (super.dispatchTouchEvent(ev)) {
            return true;
        }
        mTempRect.set(0, 0, 0, 0);
        offsetRectIntoDescendantCoords(mUnReadEventView, mTempRect);
        ev.setLocation(ev.getX() + mTempRect.left, ev.getY() + mTempRect.top);
        return mUnReadEventView.dispatchTouchEvent(ev);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mUnReadEventView == null || mUnReadEventView.getNewEventViewList() == null 
                || mUnReadEventView.getNewEventViewList().size() <= 0) {
            return super.onTouchEvent(ev);
        } else {
            int action = ev.getAction();
            switch(action) {
                case MotionEvent.ACTION_DOWN:
                    beginShakeAnimation();
                    break;
            }
            return true;
        }
    }
    
    void beginShakeAnimation() {
        if (mUnReadEventView == null || mUnReadEventView.getNewEventViewList() == null 
                || mUnReadEventView.getNewEventViewList().size() <= 0) {
            return;
        }
        finishShakeAnimation();
        ArrayList<LockScreenNewEventView> newEventViews = mUnReadEventView.getNewEventViewList();
        for(int i = 0; i < newEventViews.size(); i++) {
            UnReadHintAnimation shakeAnimation = new UnReadHintAnimation(newEventViews.get(i));
            mUnReadHintAnimations.add(shakeAnimation);
            shakeAnimation.animate();
        }
    }
    
    private void finishShakeAnimation() {
        if (mUnReadEventView == null || mUnReadEventView.getNewEventViewList() == null 
                || mUnReadEventView.getNewEventViewList().size() <= 0) {
            return;
        }
        int unReadAnimCount = mUnReadHintAnimations.size();
        for(int i = 0; i < unReadAnimCount; i++) {
            UnReadHintAnimation shakeAnimation = mUnReadHintAnimations.get(i);
            shakeAnimation.completeAnimationImmediately();
        }
        mUnReadHintAnimations.clear();
    }
    
    public void getLocationInLockScreenLayout(View child, int[] loc) {
        loc[0] = 0;
        loc[1] = 0;
        getDescendantCoordRelativeToSelf(child, loc);
    }

    /**
     * Given a coordinate relative to the descendant, find the coordinate in this LockScreenLayout's
     * coordinates.
     *
     * @param descendant The descendant to which the passed coordinate is relative.
     * @param coord The coordinate that we want mapped.
     * @return The factor by which this descendant is scaled relative to this LockScreenLayout.
     */
    private float getDescendantCoordRelativeToSelf(View descendant, int[] coord) {
        float scale = 1.0f;
        float[] pt = {coord[0], coord[1]};
        descendant.getMatrix().mapPoints(pt);
        scale *= descendant.getScaleX();
        pt[0] += descendant.getLeft();
        pt[1] += descendant.getTop();
        ViewParent viewParent = descendant.getParent();
        while (viewParent instanceof View && viewParent != this) {
            final View view = (View)viewParent;
            view.getMatrix().mapPoints(pt);
            scale *= view.getScaleX();
            pt[0] += view.getLeft() - view.getScrollX();
            pt[1] += view.getTop() - view.getScrollY();
            viewParent = view.getParent();
        }
        coord[0] = (int) Math.round(pt[0]);
        coord[1] = (int) Math.round(pt[1]);
        return scale;
    }
    
    public static class LayoutParams extends FrameLayout.LayoutParams {
        public int x, y;
        public boolean customPosition = false;
        
        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }
}
