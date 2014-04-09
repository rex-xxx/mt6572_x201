/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.lockscreensettings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;
import android.util.LongSparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.mediatek.xlog.Xlog;

import java.lang.ref.WeakReference;

/**
 * M: new added class for lock screen style settings.
 */
public abstract class ReflectionDecorAdapter extends BaseAdapter {
    private static final String TAG = "ReflectionDecorAdapter";
    private static final boolean DBG = true;

    private static final int START_GRADIENT_COLOR = 0x80ffffff;
    private static final int END_GRADIENT_COLOR = 0x00ffffff;

    private static final float DEFAULT_REFLECTION = 0.1f;
    private static final int DEFAULT_REFLECTION_GAP = 4;

    private float mReflection = DEFAULT_REFLECTION;
    private int mStartGradientColor = START_GRADIENT_COLOR;
    private int mEndGradientColor = END_GRADIENT_COLOR;

    private Matrix mTransMatrix = new Matrix();
    private Paint mNormalPaint = new Paint();
    private Paint mGradientPaint;

    // TODO: Use fixed size seems less flexible and the image may be cut,
    // consider using fill_parent.
    private int mDispWidth;
    private int mDispHeight;

    /**
     * Store the total height of the child, equals <b>mDispHeight * (1 +
     * mReflection)</b>, use it to save time instead calculating it in getView
     * every time.
     */
    private int mTotalDispHeight;
    
    private int mCurrentChosen;
    private int mIndicatorResId = -1;
    private Bitmap mIndicatorBitmap;
    private Context mContext;

    private final Object mLock = new Object();

    private final LongSparseArray<WeakReference<Bitmap>> mBitmapCache = new LongSparseArray<WeakReference<Bitmap>>();

    public ReflectionDecorAdapter(Context context) {
        mContext = context;
    }

    /**
     * Get the content bitmap of the given position.
     * 
     * @param position
     * @return
     */
    public abstract Bitmap getContentAt(final int position);

    /**
     * Get the reflection image container view id.
     * 
     * @return
     */
    public abstract int getContainerId();

    /**
     * Inflate your owner layout, you can implement it as the same with getView.
     * 
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    public abstract View getViewInner(final int position, View convertView, ViewGroup parent);

    /**
     * Set the displayed size of the image.
     * 
     * @param width the width of the view to display image.
     * @param height the height of the view to display image.
     */
    public void setImageDispSize(final int width, final int height) {
        mDispWidth = width;
        mDispHeight = height;
        mTotalDispHeight = (int) (mDispHeight * (1 + mReflection));

        // Create a linear gradient shader to implement transition effect.
        initPaintWithShader();
    }

    /**
     * Set the start and end color of the grandient.
     * 
     * @param startColor
     * @param endColor
     */
    public void setGrandientColor(final int startColor, final int endColor) {
        mStartGradientColor = startColor;
        mEndGradientColor = endColor;
    }

    /**
     * Set image reflection rate shows below the origin image.
     * 
     * @param reflect
     */
    public void setImageReflection(final float reflect) {
        mReflection = reflect;
        mTotalDispHeight = (int) (mDispHeight * (1 + mReflection));
    }

    /**
     * Set the current chosen item position.
     * 
     * @param pos
     */
    public void setChosenItem(final int pos) {
        mCurrentChosen = pos;
        mBitmapCache.delete(mCurrentChosen);
    }

    /**
     * Get the current chosen item position.
     * 
     * @return
     */
    public int getChosenItem() {
        return mCurrentChosen;
    }

    /**
     * Set the drawable with given resource id as the indicator.
     * 
     * @param resId
     */
    public void setIndicatorBitmapResource(final int resId) {
        mIndicatorResId = resId;
        mIndicatorBitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
    }

    /**
     * Set the given bitmap as the indicator.
     * 
     * @param bitmap
     */
    public void setIndicatorBitmap(final Bitmap bitmap) {
        mIndicatorBitmap = bitmap;
    }

    /**
     * When data of bookmark adapter changes, the bitmap cache need to be cleared. It is also highly recommend application
     * call this to destroy bitmap cache when onDestroy().
     */
    public void clearBitmapCache() {
        synchronized (mLock) {
            final int size = mBitmapCache.size();
            if (DBG) {
                Xlog.d(TAG, "clearBitmapCache: size = " + size);
            }
            Bitmap bmp = null;
            for (int i = 0; i < size; i++) {
                bmp = mBitmapCache.valueAt(i).get();
                if (DBG) {
                    Xlog.d(TAG, "clearBitmapCache: i = " + i + ",bmp = " + bmp + ",recycled = "
                            + ((bmp != null) ? bmp.isRecycled() : true));
                }
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle();
                    bmp = null;
                }
            }
            mBitmapCache.clear();
        }

        if (mIndicatorBitmap != null && !mIndicatorBitmap.isRecycled()) {
            mIndicatorBitmap.recycle();
            mIndicatorBitmap = null;
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ImageView imageContent = null;
        final View view = getViewInner(position, convertView, parent);
        final ImageView refContainer = (ImageView) view.findViewById(getContainerId());
        decorAndSetContent(refContainer, getContentAt(position), position);

        return view;
    }

    /**
     * We use a weak reference cache to save bitmap instances of each position instead of load it every time we getView to
     * save time, we also decode bitmap from image resource only once, then we will store the reference for next time.
     * 
     * @param child
     * @param originalBmp
     * @param position
     */
    private void decorAndSetContent(ImageView child, Bitmap originalBmp, int position) {
        final Bitmap cacheBitmap = getCachedBitmap(mBitmapCache, position);
        if (cacheBitmap != null) {
            child.setImageBitmap(cacheBitmap);
            if (DBG) {
                Xlog.d(TAG, "decorAndSetContent return cached bitmap: position = " + position + ",cacheBitmap = "
                        + cacheBitmap);
            }
            return;
        }

        final int width = originalBmp.getWidth();
        final int height = originalBmp.getHeight();
        if (DBG) {
            Xlog.d(TAG, "decorAndSetContent: position = " + position + ",width = " + width + ",height = " + height
                    + ",mReflection = " + mReflection + ",mDispWidth= " + mDispWidth + ",mDispHeight = " + mDispHeight);
        }

        // The scaledBitmap is the same with originalImage, so we won't recycle
        // it, application should recycle the originalImage indeed.
        Bitmap operBitmap = originalBmp;
        boolean scaled = false;
        if (mDispWidth == 0 || mDispHeight == 0) {
            mDispWidth = width;
            mDispHeight = height;
            mTotalDispHeight = (int) (mDispHeight * (1 + mReflection));
        } else if (mDispWidth != width || mDispHeight != height) {
            // Create a bitmap by scaling the origin image to fit the view size.
            mTransMatrix.reset();
            mTransMatrix.postScale((float) mDispWidth / width, (float) mDispHeight / height);

            operBitmap = Bitmap.createBitmap(originalBmp, 0, 0, width, height, mTransMatrix, true);
            scaled = true;
        }

        // Whether to draw reflection, float variable should not use "=".
        if (mReflection > 0.0f || mReflection < 0.0f) {
            // Rotate for 180.
            mTransMatrix.reset();
            mTransMatrix.preScale(1, -1);

            Bitmap reflectedBitmap = Bitmap.createBitmap(operBitmap, 0, (int) (mDispHeight * (1 - mReflection)), mDispWidth,
                    (int) (mDispHeight * mReflection), mTransMatrix, false);
            final Bitmap bitmapWithReflection = Bitmap.createBitmap(mDispWidth, mTotalDispHeight, Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmapWithReflection);
            // Draw the origin bitmap.
            canvas.drawBitmap(operBitmap, 0, 0, null);

            // Draw indicator on current chosen item.
            if (position == mCurrentChosen && mIndicatorBitmap != null) {
                canvas.drawBitmap(mIndicatorBitmap, 0, 0, null);
            }

            // Draw a rectangle to separate the origin bitmap and the reflection bitmap.
            canvas.drawRect(0, mDispHeight, mDispWidth, mDispHeight + DEFAULT_REFLECTION_GAP, mNormalPaint);
            // Draw reflection bitmap.
            canvas.drawBitmap(reflectedBitmap, 0, mDispHeight + DEFAULT_REFLECTION_GAP, null);

            initPaintWithShader();
            canvas.drawRect(0, mDispHeight, mDispWidth, mTotalDispHeight + DEFAULT_REFLECTION_GAP, mGradientPaint);
            Xlog.d(TAG, "decorAndSetContent end: width = " + bitmapWithReflection.getWidth() + ",height = "
                    + bitmapWithReflection.getHeight());
            child.setImageBitmap(bitmapWithReflection);

            synchronized (mLock) {
                if (DBG) {
                    Xlog.d(TAG, "decorAndSetContent cache reflection bitmap: position = " + position
                            + ",bitmapWithReflection = " + bitmapWithReflection);
                }
                mBitmapCache.put(position, new WeakReference<Bitmap>(bitmapWithReflection));
            }

            // Recycle reflected bitmap.
            reflectedBitmap.recycle();
            reflectedBitmap = null;
        } else {
            child.setImageBitmap(originalBmp);
            synchronized (mLock) {
                if (DBG) {
                    Xlog.d(TAG, "decorAndSetContent cache scaled bitmap: position = " + position + ",scaledBitmap = "
                            + originalBmp);
                }
                mBitmapCache.put(position, new WeakReference<Bitmap>(originalBmp));
            }
        }

        if (scaled) {
            operBitmap.recycle();
            operBitmap = null;
        }
    }

    private void initPaintWithShader() {
        if (mGradientPaint == null) {
            mGradientPaint = new Paint();
        }

        // Create a linear gradient shader to implement transition effect.
        final LinearGradient shader = new LinearGradient(0, mDispHeight, 0, mTotalDispHeight + DEFAULT_REFLECTION_GAP,
                mStartGradientColor, mEndGradientColor, TileMode.CLAMP);
        mGradientPaint.setShader(shader);

        // Set the Xfermode.
        mGradientPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
    }

    /**
     * Get bitmap from bitmap cache.
     * 
     * @param bmpCache
     * @param key
     * @return
     */
    private Bitmap getCachedBitmap(final LongSparseArray<WeakReference<Bitmap>> bmpCache, final long key) {
        synchronized (mLock) {
            final WeakReference<Bitmap> wr = bmpCache.get(key);
            if (wr != null) { // We have the key.
                Bitmap entry = wr.get();
                if (entry != null) {
                    return entry;
                } else { // Our entry has been purged.
                    bmpCache.delete(key);
                }
            }
        }
        return null;
    }
}
