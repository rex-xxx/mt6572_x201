/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.Log;
import android.view.View;

import com.android.internal.policy.impl.keyguard.LockScreenLayout.LayoutParams;

public class DragView extends View {
    private static final String TAG = "DragView";
    private static final boolean DEBUG = true;

    private Bitmap mBitmap;
    private Paint mPaint;
    private int mRegistrationX;
    private int mRegistrationY;

    private LockScreenLayout mLockScreenLayout = null;

    /**
     * Construct the drag view.
     * <p>
     * The registration point is the point inside our view that the touch events should
     * be centered upon.
     *
     * @param launcher The Launcher instance
     * @param bitmap The view that we're dragging around.  We scale it up when we draw it.
     * @param registrationX The x coordinate of the registration point.
     * @param registrationY The y coordinate of the registration point.
     */
    public DragView(LockScreenLayout lockScreenView, Bitmap bitmap, int registrationX, int registrationY,
            int left, int top, int width, int height, final float initialScale) {
        super(lockScreenView.getContext());
        mLockScreenLayout = lockScreenView;

        mBitmap = Bitmap.createBitmap(bitmap, left, top, width, height);

        // The point in our bitmap that the touch events are located
        mRegistrationX = registrationX;
        mRegistrationY = registrationY;

        if (DEBUG) {
            Log.d(TAG, "DragView constructor: mRegistrationX = " + mRegistrationX
                    + ", mRegistrationY = " + mRegistrationY + ", this = " + this);
        }

        // Force a measure, because Workspace uses getMeasuredHeight() before the layout pass
        int ms = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        measure(ms, ms);
        mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mBitmap.getWidth(), mBitmap.getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        @SuppressWarnings("all") // suppress dead code warning
        final boolean debug = false;
        if (debug) {
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x66ffffff);
            canvas.drawRect(0, 0, getWidth(), getHeight(), p);
        }
        canvas.drawBitmap(mBitmap, 0.0f, 0.0f, mPaint);
    }

    /**
     * Create a window containing this view and show it.
     *
     * @param windowToken obtained from v.getWindowToken() from one of your views
     * @param touchX the x coordinate the user touched in DragLayer coordinates
     * @param touchY the y coordinate the user touched in DragLayer coordinates
     */
    public void show(int touchX, int touchY) {
        mLockScreenLayout.addView(this);

        this.setDrawingCacheEnabled(true);

        // Start the pick-up animation
        LayoutParams lp = new LayoutParams(0, 0);
        lp.width = mBitmap.getWidth();
        lp.height = mBitmap.getHeight();
        lp.x = mRegistrationX;
        lp.y = mRegistrationY;
        lp.customPosition = true;
        setLayoutParams(lp);
        setTranslationX(touchX - mRegistrationX);
        setTranslationY(touchY - mRegistrationY);
    }
    
    /**
     * Move the window containing this view.
     *
     * @param touchX the x coordinate the user touched in DragLayer coordinates
     * @param touchY the y coordinate the user touched in DragLayer coordinates
     */
    void move(float touchX, float touchY) {
        Log.d(TAG, "move touchX=" + touchX + ", touchY=" + touchY);
        setTranslationX(touchX - mRegistrationX);
        setTranslationY(touchY - mRegistrationY);
    }

    void remove() {
        if (DEBUG) {
            Log.d(TAG, "remove DragView: this = " + this);
        }

        if (getParent() != null) {
            // Disable hw-layers on this view
//            setLayerType(View.LAYER_TYPE_NONE, null);

            this.setDrawingCacheEnabled(false);
            mLockScreenLayout.removeView(DragView.this);
        }
    }
}

