package com.mediatek.gallery3d.data;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.graphics.Bitmap;
//import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.FloatMath;
import android.util.Log;

import com.android.gallery3d.app.PhotoDataAdapter.MavListener;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DecodeUtils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.mediatek.gallery3d.mpo.MpoDecoderWrapper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekFeature.Params;
import com.mediatek.gallery3d.util.MtkLog;

public class DecodeHelper {
    private static final String TAG = "Gallery2/DecodeHelper";

    public static final int HW_LIMITATION_2D_TO_3D = 2048;
    public static final int MAX_BITMAP_BYTE_COUNT = 10 * 1024 * 1024;
    
    private static final int TARGET_DISPLAY_WIDTH [] = {/*1920,*/ 1280, 1280, 960, 800, 640, 480};
    private static final int TARGET_DISPLAY_HEIGHT [] = {/*1080,*/ 800, 720, 540, 480, 480, 320};
    private static final int MAX_BITMAP_DIM = 8000;

    public static int calculateSampleSize(int maxBytes, int maxDim, int width,
                                           int height) {
        //we need to recalculate the target Size to decode large bitmap
        //we believe (partially base on survey) that height/width ration of
        //normal panorama is more or less 7. We hope that the decode image
        //is at least 540 (1080/2) pixels in width or height. So the budget 
        //is about 540*7*540*4=8MB memory. So totally we hope the decoded
        //Bitmap is 540 pixels or within 10MB memory.
        //targetSize = xxxxx

        int sampleSize = 1;
        //first, calculate the smallest sample size that makes the decoded
        //Bitmap's memory size is less than the budget
        while (width * height * 4 / sampleSize / sampleSize > maxBytes) {
            sampleSize *= 2;
        }
        //second, check if the dimension of decode bitmap exceed max dimension
        //If exceed, continue enlarge sample size
        while (maxDim > 0 &&
            (width / sampleSize > maxDim || height / sampleSize > maxDim)) {
            sampleSize *= 2;
        }

        return sampleSize;
    }

    //this function is actually copied from com.android.gallery3d.data.DecodeUtils
    //should align with it if it changes.
    public static int calculateSampleSizeByType(int width, int height, 
                                                int type, int targetSize) {
        int sampleSize = 1;
        if (type == MediaItem.TYPE_MICROTHUMBNAIL) {
            // We center-crop the original image as it's micro thumbnail.
            // In this case, we want to make sure the shorter side >= "targetSize".
            float scale = (float) targetSize / Math.min(width, height);
            sampleSize = BitmapUtils.computeSampleSizeLarger(scale);

            // For an extremely wide image, e.g. 300x30000, we may got OOM
            // when decoding it for TYPE_MICROTHUMBNAIL. So we add a max
            // number of pixels limit here.
            final int MAX_PIXEL_COUNT = 640000; // 400 x 1600
            if ((width / sampleSize) * (height / sampleSize) > MAX_PIXEL_COUNT) {
                sampleSize = BitmapUtils.computeSampleSize(
                    FloatMath.sqrt((float) MAX_PIXEL_COUNT / (width * height)));
            }
        } else {
            // For screen nail, we only want to keep the longer side >= targetSize.
            float scale = (float) targetSize / Math.max(width, height);
            sampleSize = BitmapUtils.computeSampleSizeLarger(scale);
        }
        return sampleSize;
    }

    public static Bitmap postScaleDown(Bitmap bitmap, int type, int targetSize) {
        if (null == bitmap) return null;
        //scale down according to type
        if (type == MediaItem.TYPE_MICROTHUMBNAIL) {
            bitmap = BitmapUtils.resizeAndCropCenter(bitmap, targetSize, true);
        } else {
            bitmap = BitmapUtils.resizeDownBySideLength(bitmap, targetSize, true);
        }
        bitmap = DecodeUtils.ensureGLCompatibleBitmap(bitmap);
        return bitmap;
    }

    public static Bitmap decodeThumbnail(JobContext jc, byte[] buffer, 
                             int offset, int length, Options options,
                             int targetSize, int type) {
        if (options == null) options = new Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(buffer, offset, length, options);
        if (jc.isCancelled()) return null;

        int w = options.outWidth;
        int h = options.outHeight;

        if (type == MediaItem.TYPE_MICROTHUMBNAIL) {
            // We center-crop the original image as it's micro thumbnail. In this case,
            // we want to make sure the shorter side >= "targetSize".
            float scale = (float) targetSize / Math.min(w, h);
            options.inSampleSize = BitmapUtils.computeSampleSizeLarger(scale);

            // For an extremely wide image, e.g. 300x30000, we may got OOM when decoding
            // it for TYPE_MICROTHUMBNAIL. So we add a max number of pixels limit here.
            final int MAX_PIXEL_COUNT = 640000; // 400 x 1600
            if ((w / options.inSampleSize) * (h / options.inSampleSize) > MAX_PIXEL_COUNT) {
                options.inSampleSize = BitmapUtils.computeSampleSize(
                        FloatMath.sqrt((float) MAX_PIXEL_COUNT / (w * h)));
            }
        } else {
            // For screen nail, we only want to keep the longer side >= targetSize.
            float scale = (float) targetSize / Math.max(w, h);
            options.inSampleSize = BitmapUtils.computeSampleSizeLarger(scale);
        }

        options.inJustDecodeBounds = false;

        Bitmap result = BitmapFactory.decodeByteArray(buffer, offset, length, options);
        if (result == null) return null;

        // We need to resize down if the decoder does not support inSampleSize
        // (For example, GIF images)
        float scale = (float) targetSize / (type == MediaItem.TYPE_MICROTHUMBNAIL
                ? Math.min(result.getWidth(), result.getHeight())
                : Math.max(result.getWidth(), result.getHeight()));

        if (scale <= 0.5) result = BitmapUtils.resizeBitmapByScale(result, scale, true);
        return DecodeUtils.ensureGLCompatibleBitmap(result);
    }

    public static Bitmap decodeFrame(JobContext jc,
                               MpoDecoderWrapper mpoDecoderWrapper,
                               int frameIndex, Options options) {
        if (null == mpoDecoderWrapper || frameIndex < 0 || null == options) {
            Log.w(TAG, "decodeFrame:invalid paramters");
            return null;
        }
        Bitmap bitmap = mpoDecoderWrapper.frameBitmap(frameIndex, options);
        if (null != jc && jc.isCancelled()) {
            bitmap.recycle();
            bitmap = null;
        }
        return bitmap;
    }

    public static Bitmap decodeLargeBitmap(JobContext jc, Params params,
                                           FileDescriptor fd) {
        Bitmap bitmap = null;
        if (null == fd) {
            Log.e(TAG,"decodeLargeBitmap:get null FileDescriptor");
            return null;
        }
        Options options = new Options();

        try {
            //decode bounds to get dimension
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fd, null, options);

            if (null != jc && jc.isCancelled()) return null;
            options.inSampleSize = calculateSampleSize(
                        MAX_BITMAP_BYTE_COUNT, HW_LIMITATION_2D_TO_3D,
                        options.outWidth, options.outHeight); 

            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            //currently, this function is used to create regindecoder,
            //so PQ is allowed.
            bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap decodeLargeBitmap(JobContext jc, Params params,
                                           String localFilePath) {
        Bitmap bitmap = null;
        if (null == localFilePath) {
            Log.e(TAG,"decodeLargeBitmap:get null local path");
            return null;
        }
        Options options = new Options();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(localFilePath);
            FileDescriptor fd = fis.getFD();

            bitmap = decodeLargeBitmap(jc, params, fd);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            Utils.closeSilently(fis);
        }
        return bitmap;
    }

    public static Bitmap decodeLargeBitmap(JobContext jc, Params params,
                                  byte[] buffer, int offset, int length) {
        Bitmap bitmap = null;
        if (null == buffer) {
            Log.e(TAG,"decodeLargeBitmap:get null buffer");
            return null;
        }
        Options options = new Options();

        try {
            //decode bounds to get dimension
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(buffer, offset, length, options);

            if (null != jc && jc.isCancelled()) return null;
            options.inSampleSize = calculateSampleSize(
                        MAX_BITMAP_BYTE_COUNT, HW_LIMITATION_2D_TO_3D,
                        options.outWidth, options.outHeight); 

            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            //currently, this function is used to create regindecoder,
            //so PQ is allowed.
            bitmap = BitmapFactory.decodeByteArray(
                                       buffer, offset, length, options);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return bitmap;
    }

    public static Bitmap decodeFrameSafe(JobContext jc,
                               MpoDecoderWrapper mpoDecoderWrapper,
                               int frameIndex, Options options) {
        if (null == mpoDecoderWrapper || frameIndex < 0 || null == options) {
            Log.w(TAG, "decodeFrameSafe:invalid paramters");
            return null;
        }
        //As there is a chance no enough dvm memory for decoded Bitmap,
        //Skia will return a null Bitmap. In this case, we have to
        //downscale the decoded Bitmap by increase the options.inSampleSize
        Bitmap bitmap = null;
        final int maxTryNum = 8;
        for (int i=0; i < maxTryNum && (null == jc || !jc.isCancelled()); i++) {
            //we increase inSampleSize to expect a smaller Bitamp
            Log.v(TAG,"decodeFrameSafe:try for sample size " +
                      options.inSampleSize);
            try {
                bitmap = decodeFrame(jc, mpoDecoderWrapper, frameIndex, options);
            } catch (OutOfMemoryError e) {
                Log.w(TAG,"decodeFrameSafe:out of memory when decoding:"+e);
            }
            if (null != bitmap) break;
            options.inSampleSize *= 2;
        }

        return bitmap;
    }

    public static RegionDecoder getRegionDecoder(JobContext jc, Bitmap bitmap,
                                    boolean recycleInput) {
        if (null == bitmap) return null;
        byte[] array = BitmapUtils.compressToBytes(bitmap);
        if (null == array) return null;

        if (null != jc && jc.isCancelled()) return null;
        if (recycleInput) bitmap.recycle();

        RegionDecoder regionDecoder = new RegionDecoder();
        regionDecoder.jpegBuffer = array;
        regionDecoder.regionDecoder = 
                     DecodeUtils.createBitmapRegionDecoder(
                                       jc, array, 0, array.length, true);
        return regionDecoder;
    }

    public static RegionDecoder getRegionDecoder(JobContext jc,
                       MpoDecoderWrapper mpoDecoderWrapper, int frameIndex) {
        if (null == mpoDecoderWrapper || frameIndex < 0) {
            Log.w(TAG, "getRegionDecoder:got null decoder or frameIndex!");
            return null;
        }
        Options options = new Options();
        options.inSampleSize = calculateSampleSize(MAX_BITMAP_BYTE_COUNT, -1,
                mpoDecoderWrapper.width(), mpoDecoderWrapper.height());
        //as we decode buffer for region decoder, we close PQ enhance option
        //to prevent double enhancement.

        Bitmap bitmap = decodeFrameSafe(jc, mpoDecoderWrapper,
                                        frameIndex, options);
        if (null == bitmap) return null;
        if (null != jc && jc.isCancelled()) {
            bitmap.recycle();
            return null;
        }
        return getRegionDecoder(jc, bitmap, true);
    }

    public static Bitmap decodeImageRegionNoRetry(JobContext jc, BitmapRegionDecoder
                              regionDecoder, Rect imageRect, Options options) {
        if (null == regionDecoder || null == imageRect || null == options) {
            Log.e(TAG,"safeDecodeImageRegion:invalid region decoder or rect");
            return null;
        }
        try {
Log.i(TAG,"safeDecodeImageRegion:decodeRegion(rect="+imageRect+"..)");
            return regionDecoder.decodeRegion(imageRect, options);
        } catch (OutOfMemoryError e) {
            Log.w(TAG,"safeDecodeImageRegion:out of memory when decoding:"+e);
            return null;
        }
    }

    //this function return part of origin image as a Bitmap. It may shrink the
    //Bitmap size in case that out of memory happens
    public static Bitmap safeDecodeImageRegion(JobContext jc, BitmapRegionDecoder
                              regionDecoder, Rect imageRect, Options options) {
        if (null == regionDecoder || null == imageRect || null == options) {
            Log.e(TAG,"safeDecodeImageRegion:invalid region decoder or rect");
            return null;
        }

        if (null != jc && jc.isCancelled()) return null;

        Bitmap bitmap = null;

        //add protection in case rect is not valid
        try {
            //As there is a chance no enough dvm memory for decoded Bitmap,
            //Skia will return a null Bitmap. In this case, we have to
            //downscale the decoded Bitmap by increase the options.inSampleSize
            final int maxTryNum = 8;
            for (int i = 0; i < maxTryNum; i++) {
                if (null != jc && jc.isCancelled()) return null;
                Log.d(TAG,"decodeImageRegionNoRetry:try for sample size " +
                          options.inSampleSize);
                bitmap = decodeImageRegionNoRetry(jc, regionDecoder, imageRect,
                                                  options);
                if (null != bitmap) break;
                //we increase inSampleSize to expect a smaller Bitamp
                options.inSampleSize *= 2;
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG,"safeDecodeImageRegion:got exception:"+e);
        }
        return bitmap;
    }
    
	public static Bitmap[] decodeMpoFrames(JobContext jc, Params params,
			MpoDecoderWrapper mpoDecoderWrapper, MavListener listener) {
		if (null == params || null == mpoDecoderWrapper) {
			Log.e(TAG, "decodeMpoFrames:got null decoder or params!");
			return null;
		}
		int targetDisplayWidth = params.inTargetDisplayWidth;
		int targetDisplayHeight = params.inTargetDisplayHeight;
		int frameCount = mpoDecoderWrapper.frameCount();
		int frameWidth = mpoDecoderWrapper.width();
		int frameHeight = mpoDecoderWrapper.height();
		MtkLog.d(TAG, "mpo frame width: " + frameWidth + ", frame height: "
				+ frameHeight);
		if (targetDisplayWidth <= 0 || targetDisplayHeight <= 0
				|| MpoDecoderWrapper.INVALID_VALUE == frameCount
				|| MpoDecoderWrapper.INVALID_VALUE == frameWidth
				|| MpoDecoderWrapper.INVALID_VALUE == frameHeight) {
			Log.e(TAG, "decodeMpoFrames:got invalid parameters");
			return null;
		}

		// now as paramters are all valid, we start to decode mpo frames
		Bitmap[] mpoFrames = null;
		try {
			mpoFrames = tryDecodeMpoFrames(jc, mpoDecoderWrapper, params,
					targetDisplayWidth, targetDisplayHeight, listener);
		} catch (OutOfMemoryError e) {
			Log.w(TAG, "decodeMpoFrames:out memory when decode mpo frames");
			e.printStackTrace();
			// when out of memory happend, we decode smaller mpo frames
			// we try smaller display size
			int targetDisplayPixelCount = targetDisplayWidth
					* targetDisplayHeight;
			for (int i = 0; i < TARGET_DISPLAY_WIDTH.length; i++) {
				int pixelCount = TARGET_DISPLAY_WIDTH[i]
						* TARGET_DISPLAY_HEIGHT[i];
				if (pixelCount >= targetDisplayPixelCount) {
					continue;
				} else {
					if (jc != null && jc.isCancelled()) {
						Log.v(TAG, "decodeMpoFrames:job cancelled");
						break;
					}
					Log.i(TAG, "decodeMpoFrames:try display ("
							+ TARGET_DISPLAY_WIDTH[i] + " x "
							+ TARGET_DISPLAY_HEIGHT[i] + ")");
					try {
						mpoFrames = tryDecodeMpoFrames(jc, mpoDecoderWrapper,
								params, TARGET_DISPLAY_WIDTH[i],
								TARGET_DISPLAY_HEIGHT[i], listener);
					} catch (OutOfMemoryError oom) {
						Log.w(TAG, "decodeMpoFrames:out of memory again:" + oom);
						continue;
					}
					Log.d(TAG, "decodeMpoFrame: we finished decoding process");
					break;
				}
			}
		}
		if (jc != null && jc.isCancelled()) {
			Log.d(TAG, "decodeMpoFrame:job cancelled, recycle decoded");
			recycleBitmapArray(mpoFrames);
			return null;
		}
		return mpoFrames;
	}

	public static Bitmap[] tryDecodeMpoFrames(JobContext jc,
			MpoDecoderWrapper mpoDecoderWrapper, Params params,
			int targetDisplayWidth, int targetDisplayHeight,
			MavListener listener) {
		// we believe all the parameters are valid
		int frameCount = mpoDecoderWrapper.frameCount();
		int frameWidth = mpoDecoderWrapper.width();
		int frameHeight = mpoDecoderWrapper.height();

		Options options = new Options();
		int initTargetSize = targetDisplayWidth > targetDisplayHeight ? targetDisplayWidth
				: targetDisplayHeight;
		float scale = (float) initTargetSize
				/ Math.max(frameWidth, frameHeight);
		options.inSampleSize = BitmapUtils.computeSampleSizeLarger(scale);
		MediatekFeature
				.enablePictureQualityEnhance(options, params.inPQEnhance);

		Bitmap[] mpoFrames = new Bitmap[frameCount];
		boolean decodeFailed = false;
		try {
			for (int i = 0; i < frameCount; i++) {
				if (jc != null && jc.isCancelled()) {
					Log.d(TAG, "tryDecodeMpoFrames:job cancelled");
					break;
				}
				Bitmap bitmap = decodeFrame(jc, mpoDecoderWrapper, i, options);
				if (null == bitmap) {
					Log.e(TAG, "tryDecodeMpoFrames:got null frame");
					decodeFailed = true;
					break;
				}
				float scaleDown = largerDisplayScale(bitmap.getWidth(),
						bitmap.getHeight(), targetDisplayWidth,
						targetDisplayHeight);
				if (scaleDown < 1.0f) {
					mpoFrames[i] = resizeBitmap(bitmap, scaleDown, true);
				} else {
					mpoFrames[i] = bitmap;
				}
				if (null != mpoFrames[i]) {
					Log.v(TAG,
							"tryDecodeMpoFrames:got mpoFrames[" + i + "]:["
									+ mpoFrames[i].getWidth() + "x"
									+ mpoFrames[i].getHeight() + "]");
				}
				// update progress
				if (listener != null) {
					MtkLog.d("CGW", "update mav progress: " + i);
					listener.setProgress(i);
				}
			}
		} catch (OutOfMemoryError e) {
			Log.w(TAG, "tryDecodeMpoFrames:out of memory, recycle decoded");
			recycleBitmapArray(mpoFrames);
			throw e;
		}
		if (jc != null && jc.isCancelled() || decodeFailed) {
			Log.d(TAG,
					"tryDecodeMpoFrames:job cancelled or decode failed, recycle decoded");
			recycleBitmapArray(mpoFrames);
			return null;
		}
		return mpoFrames;
	}

    public static void recycleBitmapArray(Bitmap[] bitmapArray) {
        if (null == bitmapArray) {
            return;
        }
        for (int i = 0; i < bitmapArray.length; i++) {
            if (null == bitmapArray[i]) {
                continue;
            }
            //Log.v(TAG, "recycleBitmapArray:recycle bitmapArray[" + i + "]");
            bitmapArray[i].recycle();
        }
    }

	public static float largerDisplayScale(int frameWidth, int frameHeight,
			int targetDisplayWidth, int targetDisplayHeight) {
		if (targetDisplayWidth <= 0 || targetDisplayHeight <= 0
				|| frameWidth <= 0 || frameHeight <= 0) {
			Log.w(TAG, "largerDisplayScale:invalid parameters");
			return 1.0f;
		}
		float initRate = 1.0f;
		initRate = Math.min((float) targetDisplayWidth / frameWidth,
				(float) targetDisplayHeight / frameHeight);
		initRate = Math.max(initRate, Math.min((float) targetDisplayWidth
				/ frameHeight, (float) targetDisplayHeight / frameWidth));
		initRate = Math.min(initRate, 1.0f);
		// Log.v(TAG, "largerDisplayScale:initRate=" + initRate);
		return initRate;
	}
    
    public static InputStream openUriInputStream(ContentResolver cr, Uri uri) {
        if (null == cr || null == uri) return null;
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme) || 
            ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme) || 
            ContentResolver.SCHEME_FILE.equals(scheme)) {
            try {
                return cr.openInputStream(uri);
            } catch (FileNotFoundException e) {
                Log.w(TAG, "openUriInputStream:fail to open: " + uri, e);
                return null;
            }
        }
        Log.w(TAG,"openUriInputStream:encountered unknow scheme!");
        return null;
    }

    public static Bitmap resizeBitmap(Bitmap source, float scale, boolean recycleInput) {
        if (null == source || scale <= 0.0f) {
            Log.e(TAG, "resizeBitmap:invalid parameters");
            return source;
        }
        if (scale == 1.0f) {
            // no bother to scale down
            return source;
        }

        int newWidth = Math.round((float)source.getWidth() * scale);
        int newHeight = Math.round((float)source.getHeight() * scale);
        if (newWidth > MAX_BITMAP_DIM || newHeight > MAX_BITMAP_DIM) {
            Log.w(TAG, "resizeBitmap:too large new Bitmap for scale:" + scale);
            return source;
        }

        Bitmap target = Bitmap.createBitmap(newWidth, newHeight,
                                            Bitmap.Config.ARGB_8888);
        //draw source bitmap onto it
        Canvas canvas = new Canvas(target);
        Rect src = new Rect(0, 0, source.getWidth(), source.getHeight());
        RectF dst = new RectF(0, 0, (float)newWidth, (float)newHeight);
        canvas.drawBitmap(source, src, dst, null);
        if (recycleInput) {
            source.recycle();
        }
        return target;
    }

    //the following functios are used for debugging

    public static void showBitmapInfo(Bitmap b) {
        Log.d(TAG,"showBitmapInfo("+b+")");
        if (b != null) {
            Log.v(TAG,"showBitmapInfo:["+b.getWidth()+"x"+b.getHeight()+"]");
            Log.v(TAG,"showBitmapInfo:config:"+b.getConfig());
        }
    }

    public static void dumpBitmap(Bitmap b) {
        showBitmapInfo(b);
        if (b != null) {
            java.io.FileOutputStream fos = null;
            try {
                String filename =
                    android.os.Environment.getExternalStorageDirectory().toString()
                    + "/DCIM/Bitmap[" + android.os.SystemClock.uptimeMillis() + "].png";
                fos = new java.io.FileOutputStream(filename);
                b.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (java.io.IOException ex) {
                // MINI_THUMBNAIL not exists, ignore the exception and generate one.
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (java.io.IOException ex) {
                    }
                }
            }
        }
    }

}
