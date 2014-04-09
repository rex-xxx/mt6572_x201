package com.mediatek.gallery3d.pq;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import com.android.gallery3d.app.Log;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.ui.TileImageView;
import com.android.gallery3d.util.GalleryUtils;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;

import android.content.Context;
public class TileImageDecoder extends ImageDecoder{
    String TAG = "Gallery2/TileImageDecoder";
    ImageDecoder mscreenNailDecoder = null;
    BitmapRegionDecoder decoder = null;
    Rect mDesRect = null;
    Handler mHandler = null;
    int mLevel;
    int fromLevel;
    int endLevel;
    int TILE_SIZE;
    final static int SCALE_LIMIT = 4;
    private final int TILE_BORDER = 1;
    public TileImageDecoder(Context context, String mPqUri, int width, int height, int targetSize, Handler mHandler, int viewWidth, int viewHeight, int level) {
        super(context, mPqUri, width, height, targetSize, viewWidth, viewHeight,level);
        this.mHandler = mHandler;
        if (GalleryUtils.isHighResolution(mContext)) {
            TILE_SIZE = 511;
        } else {
            TILE_SIZE = 255;
        }
        MtkLog.d(TAG," TILE_SIZE===="+TILE_SIZE); 
    }

    @Override
    public Bitmap decodeImage() {
        caculateInSampleSize();
        options.inJustDecodeBounds = false;
        mScreenNail = decoder();
        calculateCurrentLevel();
            try {
                decoder = BitmapRegionDecoder.newInstance(mContext.getContentResolver().openInputStream(Uri.parse(mUri)), false);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                decoder = null;
                MtkLog.d(TAG, "FileNotFoundException!!!!!!!!!!!!!!!!!!!!"+e.toString());
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                decoder = null;
                MtkLog.d(TAG, "IOException!!!!!!!!!!!!!!!!!!!!"+e.toString());
                e.printStackTrace();
            }
           (new Thread(mApply)).start();
        return mScreenNail;
    }

    @Override
    public Bitmap apply() {
        if (decoder == null) {
           return super.apply();
        } else {
            try {
                return decodeTileImage(1, mLevel);
            } catch (OutOfMemoryError e) {
                MtkLog.e(TAG, e.toString());
                return null;
               /* int imagewidth = decoder.getWidth();
                int imageheight = decoder.getHeight();
                float scale = Math.min(((float) mScreenWidth)/imagewidth, ((float)mScreenHeight)/imageheight);
                return decodeTileImage(scale, mLevel);*/
            }

        }
    }
    private Bitmap decodeTileImage(float scale, int sample) {
        int imagewidth = decoder.getWidth();
        int imageheight = decoder.getHeight();
        MtkLog.d(TAG, "scale==="+scale);
        imagewidth = (int)(imagewidth*scale);
        imageheight = (int)(imageheight*scale);
        Bitmap result = Bitmap.createBitmap(
                imagewidth >> sample, imageheight >> sample, Config.ARGB_8888); //
        Canvas canvas = new Canvas(result);
        Rect desRect = new Rect(0, 0, result.getWidth(), result.getHeight());
        Rect rect= new Rect(0, 0, decoder.getWidth(), decoder.getHeight());

/*        canvas.translate(desRect.left, desRect.top);
        canvas.scale((float) (desRect.width()<< sample) / rect.width(),
                (float) (desRect.height()<< sample) / rect.height());*/

        drawInTiles(canvas, decoder, rect, desRect, sample);
        return result;
    }
    
    private void drawInTiles(Canvas canvas,
            BitmapRegionDecoder decoder, Rect rect, Rect dest, int sample) {
        int tileSize = (TILE_SIZE << sample);
        int borderSize = (TILE_BORDER << sample);
        Rect tileRect = new Rect();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.ARGB_8888;
        options.inPreferQualityOverSpeed = true;
        //options.inBitmap = bitmap;
        options.inPostProc = true;
        options.inSampleSize = (1 << sample);
        MtkLog.v(TAG, "sample===="+sample);
        Paint paint = new Paint();
        for (int tx = rect.left, x = 0;
                tx < rect.right; tx += tileSize, x += TILE_SIZE) {
            for (int ty = rect.top, y = 0;
                    ty < rect.bottom; ty += tileSize, y += TILE_SIZE) {
                tileRect.set(tx, ty, tx + tileSize + borderSize, ty + tileSize + borderSize);
                if (tileRect.intersect(rect)) {
                    int height = tileRect.width();
                    Bitmap bitmap = null;//Bitmap.createBitmap(tileRect.width(), tileRect.height(), Config.ARGB_8888);
                    //Bitmap bitmap;
                    //add protection in case rect is not valid
                    try {
                        // To prevent concurrent access in GLThread
                        synchronized (decoder) {
                            bitmap = decoder.decodeRegion(tileRect, options);
                            MtkLog.v(TAG, "drawInTiles() end decodeRegion() bitmap.width=="
                                    +bitmap.getWidth()+" height=="+bitmap.getHeight());
                        }
                        MtkLog.d(TAG, "pixelX==="+x+"  pixelY===="+y);
                        canvas.drawBitmap(bitmap, x, y, paint);
                        bitmap.recycle();
                    } catch (IllegalArgumentException e) {
                        MtkLog.w(TAG,"drawInTiles:got exception:"+e);
                    }
                }
            }
        }
    }

    public class RegionDecoder implements Runnable{
        private  BitmapRegionDecoder mImageDecoder ;
        private String mUri;
        private int mScreenWidth;
        private int mScreenHeight;
        public RegionDecoder(BitmapRegionDecoder decoder, String mUri, int screenWidth, int screenHeight) {
            this.mImageDecoder = decoder;
            this.mUri = mUri;
            mScreenWidth = screenWidth;
            mScreenHeight = screenHeight;
        }
        public void run() {
            // TODO Auto-generated method stub
            mApply.run();
        }
    }
    
    public void calculateCurrentLevel() {
        float currentScale = getScaleMin();
        mLevel = Utils.clamp(Utils.floorLog2(1f / currentScale), 0, mLevelCount);
        MtkLog.d(TAG, "decodeImage   currentScale===="+currentScale +"  mOriginalImageWidth=="+mOriginalImageWidth+ " mOriginalImageHeight  "+mOriginalImageHeight
                + " mScreenNail.getWidth()="+ mScreenNail.getWidth()+
                " mScreenNail.getWidth() =="+mScreenNail.getHeight() +"  levelCount===="+mLevelCount +"  mLevel=="+mLevel );
    }

    @Override
    public void recycle() {
        super.recycle();
        if (decoder != null) {
            decoder.recycle();
            decoder = null;
        }
    }

    public float getScaleMin() {

/*            if (0 != b.mSubType) {
            //adjust scale ratio for many kinds of purpose
            float scale = MediatekFeature.getMinimalScale(wFactor * viewW, 
                                hFactor * viewH, b.mImageW, b.mImageH, b.mSubType);
            float tempScaleLimit = MediatekFeature.minScaleLimit(b.mSubType);
            float scaleLimit = tempScaleLimit > 0.0f ? tempScaleLimit : SCALE_LIMIT;
 
            return Math.min(scaleLimit, scale);
        }*/
        float s = Math.min(((float) mGLviewWidth) / mOriginalImageWidth,
                ((float) mGLviewHeight) / mOriginalImageHeight);
        MtkLog.d(TAG, " viewW=="+mGLviewWidth+"  viewH=="+mGLviewHeight +"  mOriginalImageWidth=="+ mOriginalImageWidth+ 
                "  mOriginalImageHeight=="+mOriginalImageHeight);
        return Math.min(SCALE_LIMIT, s);
    }

}
