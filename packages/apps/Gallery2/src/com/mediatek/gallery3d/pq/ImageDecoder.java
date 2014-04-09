package com.mediatek.gallery3d.pq;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

public class ImageDecoder {
    private String TAG ="Gallery2/ImageDecoder";
    int mScreenWidth;
    int mScreenHeight;
    int mOriginalImageWidth;
    int mOriginalImageHeight;
    int mGLviewWidth;
    int mGLviewHeight;
    public String mUri = null;
    int targetSize ;
    Context mContext;
    BitmapFactory.Options options = null;
    Bitmap mScreenNail = null;
    Runnable mApply = null;
    int mLevelCount;
    public ImageDecoder(Context context, String mPqUri, int width, int height, int targetSize , int viewWidth, int viewHeight,int levelCount) {
        // TODO Auto-generated constructor stub
        mScreenWidth = width;
        mScreenHeight = height;
        mGLviewWidth = viewWidth;
        mGLviewHeight = viewHeight;
        mLevelCount = levelCount;
        this.mUri = mPqUri;
        this.targetSize = targetSize;
        mContext = context;
        options = new BitmapFactory.Options();
        if (MediatekFeature.isPictureQualityEnhanceSupported()) {
            options.inPostProc = true;
        }
    }

    public Bitmap apply() {
        Bitmap bitmap = decoder();
        if (bitmap != null) {
            mScreenNail = bitmap;
        } else {
            Log.d(TAG, "apply bitmap == null");
        }
        return mScreenNail;
    }

    public Bitmap decoder() {
        InputStream is = null;
        Bitmap mBitmap = null;
        try {
            is = mContext.getContentResolver().openInputStream(Uri.parse(mUri));
            mBitmap = BitmapFactory.decodeStream(is, null, options);
            }catch (FileNotFoundException e) {
                MtkLog.e(TAG, "bitmapfactory decodestream fail");
            }catch (IOException e) {
                MtkLog.e(TAG, "bitmapfactory decodestream fail");
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (mBitmap != null) {
                    float scale = (float) targetSize / Math.max(mBitmap.getWidth(), mBitmap.getHeight());
                    if (scale <= 0.5) {
                        return BitmapUtils.resizeBitmapByScale(mBitmap, scale, true);
                    }
                }
               // return mBitmap;
            }
            return mBitmap;
    }

    public Bitmap decodeImage() {
        caculateInSampleSize();
        options.inJustDecodeBounds = false;
        mScreenNail = decoder();
        return mScreenNail;
    }

    public int caculateInSampleSize() {
        InputStream is = null;
        options.inJustDecodeBounds = true;
        try {
            is = mContext.getContentResolver().openInputStream(Uri.parse(mUri));
            BitmapFactory.decodeStream(is, null, options);
            }catch (FileNotFoundException e) {
                MtkLog.e(TAG, "bitmapfactory decodestream fail");
            }catch (IOException e) {
                MtkLog.e(TAG, "bitmapfactory decodestream fail");
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            float scale = 1;
            if (options.outWidth > 0 && options.outHeight > 0) {
                mOriginalImageWidth = options.outWidth;
                mOriginalImageHeight = options.outHeight;
                scale = (float) targetSize / Math.max(options.outWidth, options.outHeight);
            }
        options.inSampleSize = BitmapUtils.computeSampleSizeLarger(scale);
        MtkLog.d(TAG, " pq  options.inSampleSize=="+options.inSampleSize +" width=="+options.outWidth+ " height=="+options.outHeight + "targetSize=="+targetSize);
        return options.inSampleSize;
    }

    public void setApply (Runnable apply) {
        mApply = apply;
    }

    public void recycle() {
        // TODO Auto-generated method stub
        if (mScreenNail != null) {
            mScreenNail.recycle();
            mScreenNail = null;
        }
    }

}
