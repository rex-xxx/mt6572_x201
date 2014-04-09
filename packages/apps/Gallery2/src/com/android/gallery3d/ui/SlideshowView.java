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

package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;

import com.android.gallery3d.R;
import com.android.gallery3d.anim.CanvasAnimation;
import com.android.gallery3d.anim.FloatAnimation;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.MediaObject;

import java.util.Random;

import javax.microedition.khronos.opengles.GL11;

import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.util.MediatekFeature;

public class SlideshowView extends GLView {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/SlideshowView";

    private static final int SLIDESHOW_DURATION = 3500;
    private static final int TRANSITION_DURATION = 1000;

    private static final float SCALE_SPEED = 0.20f ;
    private static final float MOVE_SPEED = SCALE_SPEED;

    private int mCurrentRotation;
    private BitmapTexture mCurrentTexture;
    private SlideshowAnimation mCurrentAnimation;

    private int mPrevRotation;
    private BitmapTexture mPrevTexture;
    private SlideshowAnimation mPrevAnimation;

    /// M: added for Mediatek special display @{
    private int mCurrentSubType;
    private int mPrevSubType;
    private int mCurrentMediaType;
    private int mPrevMediaType;
    /// @}

    private static final int PLACEHOLDER_COLOR = 0xFF222222;

    private final FloatAnimation mTransitionAnimation =
            new FloatAnimation(0, 1, TRANSITION_DURATION);

    private Random mRandom = new Random();

    /// M: added for video thumbnail slide show @{
    private Context mContext;
    private Texture mVideoPlayIcon;

    public SlideshowView(AbstractGalleryActivity activity) {
        mContext = activity.getAndroidContext();
        mVideoPlayIcon = new ResourceTexture(mContext, R.drawable.ic_control_play);
    }

    protected void drawVideoOverlay(GLCanvas canvas) {
        int viewWidth = this.getWidth();
        int viewHeight = this.getHeight();
        int iconSize = (int)(Math.min(viewWidth, viewHeight) + 0.5f) / 6;
        mVideoPlayIcon.draw(canvas, (viewWidth - iconSize) / 2,
                (viewHeight - iconSize) / 2, iconSize, iconSize);
    }
    /// @}

    public void next(Bitmap bitmap, int rotation) {
        /// M: pass media type inside
        next(bitmap, rotation, 0, MediaObject.MEDIA_TYPE_UNKNOWN);
    }

    /// M: add parameter mediaType
    public void next(Bitmap bitmap, int rotation, int subType, int mediaType) {

        mTransitionAnimation.start();

        if (mPrevTexture != null) {
            mPrevTexture.getBitmap().recycle();
            mPrevTexture.recycle();
        }

        mPrevTexture = mCurrentTexture;
        mPrevAnimation = mCurrentAnimation;
        mPrevRotation = mCurrentRotation;

        // M: add for drm slide show
        mPrevSubType = mCurrentSubType;
        mCurrentSubType = subType;
        /// M: add for video thumbnail slide show @{
        mPrevMediaType = mCurrentMediaType;
        mCurrentMediaType = mediaType;
        /// @}

        mCurrentRotation = rotation;
        mCurrentTexture = new BitmapTexture(bitmap);
        if (((rotation / 90) & 0x01) == 0) {
            mCurrentAnimation = new SlideshowAnimation(
                    mCurrentTexture.getWidth(), mCurrentTexture.getHeight(),
                    mRandom);
        } else {
            mCurrentAnimation = new SlideshowAnimation(
                    mCurrentTexture.getHeight(), mCurrentTexture.getWidth(),
                    mRandom);
        }

        // M: add for drm slide show
        mCurrentAnimation.setSubType(subType);

        mCurrentAnimation.start();

        invalidate();
    }

    public void release() {
        if (mPrevTexture != null) {
            mPrevTexture.recycle();
            mPrevTexture = null;
        }
        if (mCurrentTexture != null) {
            mCurrentTexture.recycle();
            mCurrentTexture = null;
        }
    }

    @Override
    protected void render(GLCanvas canvas) {
        long animTime = AnimationTime.get();
        boolean requestRender = mTransitionAnimation.calculate(animTime);
        GL11 gl = canvas.getGLInstance();
        gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        float alpha = mPrevTexture == null ? 1f : mTransitionAnimation.get();

        if (mPrevTexture != null && alpha != 1f) {
            requestRender |= mPrevAnimation.calculate(animTime);
            canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
            canvas.setAlpha(1f - alpha);
            mPrevAnimation.apply(canvas);
            canvas.rotate(mPrevRotation, 0, 0, 1);
            int width = mPrevTexture.getWidth();
            int height = mPrevTexture.getHeight();
            int x = - width / 2;
            int y = - height / 2;
            if (MediatekFeature.permitShowThumb(mPrevSubType)) {
                mPrevTexture.draw(canvas, x, y);
            } else {
                canvas.fillRect(x, y, width, height, PLACEHOLDER_COLOR);
            }
            DrmHelper.renderSubTypeOverlay(canvas, x, y, width, height,
                          mPrevSubType, 1.0f / mPrevAnimation.getCurrentScale());

            canvas.restore();
            /// M: slide show video thumbnail @{
            gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            if (mPrevMediaType == MediaObject.MEDIA_TYPE_VIDEO) {
                drawVideoOverlay(canvas);
            }
            /// @}
        }
        if (mCurrentTexture != null) {
            requestRender |= mCurrentAnimation.calculate(animTime);
            canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
            canvas.setAlpha(alpha);
            mCurrentAnimation.apply(canvas);
            canvas.rotate(mCurrentRotation, 0, 0, 1);
            int width = mCurrentTexture.getWidth();
            int height = mCurrentTexture.getHeight();
            int x = - width / 2;
            int y = - height / 2;
            if (MediatekFeature.permitShowThumb(mCurrentSubType)) {
                mCurrentTexture.draw(canvas, x, y);
            } else {
                canvas.fillRect(x, y, width, height, PLACEHOLDER_COLOR);
            }
            DrmHelper.renderSubTypeOverlay(canvas, x, y, width, height,
                          mCurrentSubType, 1.0f / mCurrentAnimation.getCurrentScale());

            canvas.restore();
            /// M: slide show video thumbnail @{
            gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            if (mCurrentMediaType == MediaObject.MEDIA_TYPE_VIDEO) {
                drawVideoOverlay(canvas);
            }
            /// @}
        }
        if (requestRender) invalidate();
        gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private class SlideshowAnimation extends CanvasAnimation {
        private final int mWidth;
        private final int mHeight;

        private final PointF mMovingVector;
        private float mProgress;

        // M: added for MediatekFeature
        private int mSubType = 0;
        private float mCurrentScale = 1.0f;

        public SlideshowAnimation(int width, int height, Random random) {
            mWidth = width;
            mHeight = height;
            mMovingVector = new PointF(
                    MOVE_SPEED * mWidth * (random.nextFloat() - 0.5f),
                    MOVE_SPEED * mHeight * (random.nextFloat() - 0.5f));
            setDuration(SLIDESHOW_DURATION);
        }

        @Override
        public void apply(GLCanvas canvas) {
            int viewWidth = getWidth();
            int viewHeight = getHeight();

            float initScale = Math.min((float)
                    viewWidth / mWidth, (float) viewHeight / mHeight);

            // M: if special SUB_TYPE, display play at original size
            if (0 != mSubType) {
                //this logic is familar with that of PositionController.java
                float minScale = MediatekFeature.getMinimalScale(viewWidth,
                                     viewHeight, mWidth, mHeight, mSubType);
                float tempScaleLimit = MediatekFeature.minScaleLimit(mSubType);
                float scaleLimit = tempScaleLimit > 0.0f ? tempScaleLimit : 2.0f;
                minScale = Math.min(scaleLimit, minScale);
                initScale = minScale / (1 + SCALE_SPEED);
            }

            float scale = initScale * (1 + SCALE_SPEED * mProgress);

            // M: record previous scale
            mCurrentScale = scale;

            float centerX = viewWidth / 2 + mMovingVector.x * mProgress;
            float centerY = viewHeight / 2 + mMovingVector.y * mProgress;

            canvas.translate(centerX, centerY);
            canvas.scale(scale, scale, 0);
        }

        @Override
        public int getCanvasSaveFlags() {
            return GLCanvas.SAVE_FLAG_MATRIX;
        }

        @Override
        protected void onCalculate(float progress) {
            mProgress = progress;
        }

        public void setSubType(int subType) {
            mSubType = subType;
        }

        public float getCurrentScale() {
            return mCurrentScale;
        }
    }
}
