package com.mediatek.gallery3d.videothumbnail;

import javax.microedition.khronos.opengles.GL11;

import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.ui.ExtTexture;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.GLRoot;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.Matrix;
import android.os.HandlerThread;
import android.util.Log;

public abstract class VideoThumbnail implements
        SurfaceTexture.OnFrameAvailableListener,
        MediaPlayer.OnVideoSizeChangedListener, GLRoot.OnGLIdleListener {

    private class VideoFrameTexture extends ExtTexture {
        public VideoFrameTexture(int target) {
            super(target);
        }

        public void setSize(int width, int height) {
            super.setSize(width, height);
        }
    }

    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/VideoThumbnail";
    static final int TEXTURE_HEIGHT = 128;
    static final int TEXTURE_WIDTH = 128;

    private VideoFrameTexture mVideoFrameTexture;
    private SurfaceTexture mSurfaceTexture;
    private int mWidth = TEXTURE_WIDTH;
    private int mHeight = TEXTURE_HEIGHT;
    private RectF mSrcRect;
    private RectF mDestRect;
    private float[] mTransformFromSurfaceTexture = new float[16];
    private float[] mTransformForCropingCenter = new float[16];
    private float[] mTransformFinal = new float[16];
    private boolean mHasTexture = false;
    protected boolean mHasNewFrame = false;
    protected boolean isReadyForRender = false;
    public volatile boolean isWorking = false;

    public void acquireSurfaceTexture() {
        mVideoFrameTexture = new VideoFrameTexture(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mVideoFrameTexture.setSize(TEXTURE_WIDTH, TEXTURE_HEIGHT);
        mSurfaceTexture = new SurfaceTexture(mVideoFrameTexture.getId());
        setDefaultBufferSize(mSurfaceTexture, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        synchronized (this) {
            mHasTexture = true;
        }
    }

    private static void setDefaultBufferSize(SurfaceTexture st, int width,
            int height) {
        if (ApiHelper.HAS_SET_DEFALT_BUFFER_SIZE) {
            st.setDefaultBufferSize(width, height);
        }
    }

    private static void releaseSurfaceTexture(SurfaceTexture st) {
        st.setOnFrameAvailableListener(null);
        if (ApiHelper.HAS_RELEASE_SURFACE_TEXTURE) {
            st.release();
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void releaseSurfaceTexture() {
        synchronized (this) {
            if (!mHasTexture) return;
            mHasTexture = false;
        }
        mVideoFrameTexture.recycle();
        mVideoFrameTexture = null;
        releaseSurfaceTexture(mSurfaceTexture);
        mSurfaceTexture = null;
    }

//    private void drawRegardlessOfAspectRatio(GLCanvas canvas, int slotWidth,
//            int slotHeight) {
//        synchronized (this) {
//            if (!mHasTexture) {
//                return;
//            }
////            if (mHasNewFrame) {
////                mSurfaceTexture.updateTexImage();
////                mHasNewFrame = false;
////            }
//            mSurfaceTexture.getTransformMatrix(mTransformFromSurfaceTexture);
//
//            // Flip vertically.
//            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
//            int cx = slotWidth / 2;
//            int cy = slotHeight / 2;
//            canvas.translate(cx, cy);
//            canvas.scale(1, -1, 1);
//            canvas.translate(-cx, -cy);
//            canvas.drawTexture(mVideoFrameTexture,
//                    mTransformFromSurfaceTexture, 0, 0, slotWidth, slotHeight);
//            canvas.restore();
//        }
//    }

//    private void drawByKeepingAspectRatio(GLCanvas canvas, int slotWidth,
//            int slotHeight) {
//        synchronized (this) {
//            if (!mHasTexture) {
//                return;
//            }
////            if (mHasNewFrame) {
////                mSurfaceTexture.updateTexImage();
////                mHasNewFrame = false;
////            }
//            mSurfaceTexture.getTransformMatrix(mTransformFromSurfaceTexture);
//
//            // Flip vertically.
//            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
//            int cx = slotWidth / 2;
//            int cy = slotHeight / 2;
//            canvas.translate(cx, cy);
//            canvas.scale(1, -1, 1);
//            canvas.translate(-cx, -cy);
//            float shortSideStart;
//            float shortSideEnd;
//            RectF targetRect;
//            // To reduce computing complexity, the following logic based on the
//            // fact that slotWidth = slotHeight. Strictly, the condition should
//            // be
//            // if ((float)mWidth / mHeight > (float)slotWidth / slotHeight)
//            if (mWidth > mHeight) {
//                shortSideStart = (mWidth - mHeight) * slotHeight
//                        / (float) ((mWidth * 2));
//                shortSideEnd = slotHeight - shortSideStart;
//                targetRect = new RectF(0, shortSideStart, slotWidth,
//                        shortSideEnd);
//            } else {
//                shortSideStart = (mHeight - mWidth) * slotWidth
//                        / (float) ((mHeight * 2));
//                shortSideEnd = slotWidth - shortSideStart;
//                targetRect = new RectF(shortSideStart, 0, shortSideEnd,
//                        slotHeight);
//            }
//            canvas.drawTexture(mVideoFrameTexture,
//                    mTransformFromSurfaceTexture, (int) targetRect.left,
//                    (int) targetRect.top, (int) targetRect.width(),
//                    (int) targetRect.height());
//            canvas.restore();
//        }
//    }

    private void drawByCropingCenter(GLCanvas canvas, int slotWidth,
            int slotHeight) {
        synchronized (this) {
            if (!mHasTexture || !isWorking) {
                return;
            }
//            if (mHasNewFrame) {
//                mSurfaceTexture.updateTexImage();
//                mHasNewFrame = false;
//            }
            mSurfaceTexture.getTransformMatrix(mTransformFromSurfaceTexture);

            // Flip vertically.
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            int cx = slotWidth / 2;
            int cy = slotHeight / 2;
            canvas.translate(cx, cy);
            canvas.scale(1, -1, 1);
            canvas.translate(-cx, -cy);
            float longSideStart;
            float longSideEnd;
            RectF sourceRect;
            RectF targetRect = new RectF(0, 0, slotWidth, slotHeight);
            // To reduce computing complexity, the following logic based on the
            // fact that slotWidth = slotHeight. Strictly, the condition should
            // be
            // if ((float)mWidth / mHeight > (float)slotWidth / slotHeight)
            if (mWidth > mHeight) {
                longSideStart = (mWidth - mHeight) * TEXTURE_WIDTH
                        / (float) ((mWidth * 2));
                longSideEnd = TEXTURE_WIDTH - longSideStart;
                sourceRect = new RectF(longSideStart, 0, longSideEnd,
                        TEXTURE_HEIGHT);
            } else {
                longSideStart = (mHeight - mWidth) * TEXTURE_HEIGHT
                        / (float) ((mHeight * 2));
                longSideEnd = TEXTURE_HEIGHT - longSideStart;
                sourceRect = new RectF(0, longSideStart, TEXTURE_WIDTH,
                        longSideEnd);
            }

            genCononTexCoords(sourceRect, targetRect, mVideoFrameTexture);
            genExtTexMatForSubTile(sourceRect);
            Matrix.multiplyMM(mTransformFinal, 0, mTransformFromSurfaceTexture, 0,
                    mTransformForCropingCenter, 0);
            canvas.drawTexture(mVideoFrameTexture, mTransformFinal,
                    (int) targetRect.left, (int) targetRect.top,
                    (int) targetRect.width(), (int) targetRect.height());
            canvas.restore();
        }
    }

    public void draw(GLCanvas canvas, int slotWidth, int slotHeight) {
//        switch (VideoThumbnailFeatureOption.OPTION_RENDER_TYPE) {
//        case VideoThumbnailFeatureOption.OPTION_RENDER_TYPE_REGARDLESS_OF_ASPECT_RATIO:
//            drawRegardlessOfAspectRatio(canvas, slotWidth, slotHeight);
//            break;
//
//        case VideoThumbnailFeatureOption.OPTION_RENDER_TYPE_KEEP_ASPECT_RATIO:
//            drawByKeepingAspectRatio(canvas, slotWidth, slotHeight);
//            break;
//
//        case VideoThumbnailFeatureOption.OPTION_RENDER_TYPE_CROP_CENTER:
            drawByCropingCenter(canvas, slotWidth, slotHeight);
//            break;
//
//        default:
//            break;
//        }
    }

    abstract public void onFrameAvailable(SurfaceTexture surfaceTexture);

    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public boolean onGLIdle(
            GLCanvas canvas, boolean renderRequested) {
        // if (renderRequested) return true;
        synchronized (this) {
            if (isWorking && mHasTexture && mHasNewFrame) {
                if (mSurfaceTexture != null) {
                    try {
                    mSurfaceTexture.updateTexImage();
                    } catch (IllegalStateException e) {
                        Log.v(TAG, "notify author that mSurfaceTexture in thumbnail released when updating tex img");
                        return false;
                    }
                }
                mHasNewFrame = false;
                isReadyForRender = true;
            }
        }
        return false;
    }

    // This function changes the source coordinate to the texture coordinates.
    // It also clips the source and target coordinates if it is beyond the
    // bound of the texture.
    private static void genCononTexCoords(RectF source, RectF target,
            VideoFrameTexture texture) {

        int width = texture.getWidth();
        int height = texture.getHeight();
        int texWidth = texture.getTextureWidth();
        int texHeight = texture.getTextureHeight();
        // Convert to texture coordinates
        source.left /= texWidth;
        source.right /= texWidth;
        source.top /= texHeight;
        source.bottom /= texHeight;

        // Clip if the rendering range is beyond the bound of the texture.
        float xBound = (float) width / texWidth;
        if (source.right > xBound) {
            target.right = target.left + target.width()
                    * (xBound - source.left) / source.width();
            source.right = xBound;
        }
        float yBound = (float) height / texHeight;
        if (source.bottom > yBound) {
            target.bottom = target.top + target.height()
                    * (yBound - source.top) / source.height();
            source.bottom = yBound;
        }
    }

    private void genExtTexMatForSubTile(RectF subRange) {
        mTransformForCropingCenter[0] = subRange.right - subRange.left;
        mTransformForCropingCenter[5] = subRange.bottom - subRange.top;
        mTransformForCropingCenter[10] = 1;
        mTransformForCropingCenter[12] = subRange.left;
        mTransformForCropingCenter[13] = subRange.top;
        mTransformForCropingCenter[15] = 1;
    }
}
