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
package com.mediatek.gallery3d.drm;

import com.android.gallery3d.R;

import android.os.Bundle;
import com.mediatek.drm.OmaDrmStore;
import com.mediatek.drm.OmaDrmUtils;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmUiUtils;
import android.provider.MediaStore.Files.FileColumns;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContentValues;
import com.mediatek.dcfdecoder.DcfDecoder;
import android.view.Display;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.LocalVideo;

import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.Texture;
import com.android.gallery3d.ui.ResourceTexture;

import com.mediatek.gallery3d.util.MediatekFeature;

public class DrmHelper {

    private static final String TAG = "Gallery2/DrmHelper";

    public static final int DRM_MICRO_THUMB_PIXEL_COUNT = 40000;
    public static final float DRM_MICRO_THUMB_IN_DIP = 200f/1.5f;
    // this value should be equal to the default bg color in AlbumSetSlidingWindow & AlbumSlidingWindow
    public static final int DRM_MICRO_THUMB_DEFAULT_BG = 0xFF444444;
    public static final int DRM_MICRO_THUMB_BLACK_BG = 0x00000000;

    public static final int INVALID_DRM_LEVEL = -1;
    public static final int NO_DRM_INCLUSION = 0;

    //DRM inclusion definitions are shifted to MediatekFeature class

    //drm inclusion extra signature in Bundle
    public static final String DRM_INCLUSION = "GalleryDrmInclusion";

    private static final boolean mIsDrmSupported = 
                                          MediatekFeature.isDrmSupported();

    private static OmaDrmClient mDrmManagerClient = null;
    //default display
    private static Display mDefaultDisplay = null;

    public static boolean showDrmMicroThumb(int subType) {
        if (0 != (subType & MediaObject.SUBTYPE_DRM_HAS_RIGHT) ||
            0 != (subType & MediaObject.SUBTYPE_DRM_NO_RIGHT)) {
            return true;
        } else {
            return false;
        }
    }

    public static int getDrmMicroThumbDim(Activity activity) {
        if (null == mDefaultDisplay) {
            mDefaultDisplay = activity.getWindowManager().getDefaultDisplay(); 
        }
        DisplayMetrics metrics = new DisplayMetrics();
        mDefaultDisplay.getMetrics(metrics);
        return (int)(DRM_MICRO_THUMB_IN_DIP * metrics.density);
    }

    public static int getDrmInclusionFromData(Bundle data) {
        int drmInclusion = NO_DRM_INCLUSION;
        if (null == data) {
            return drmInclusion;
        }

        int drmLevel = data.getInt(OmaDrmStore.DrmExtra.EXTRA_DRM_LEVEL, INVALID_DRM_LEVEL);
        if (drmLevel != INVALID_DRM_LEVEL) {
            if (OmaDrmStore.DrmExtra.DRM_LEVEL_FL == drmLevel) {
                drmInclusion |= MediatekFeature.INCLUDE_FL_DRM_MEDIA;
            } else if (OmaDrmStore.DrmExtra.DRM_LEVEL_SD == drmLevel) {
                drmInclusion |= MediatekFeature.INCLUDE_SD_DRM_MEDIA;
            } else if (OmaDrmStore.DrmExtra.DRM_LEVEL_ALL == drmLevel) {
                drmInclusion |= MediatekFeature.ALL_DRM_MEDIA;
            }
        }

        return drmInclusion;
    }

    public static String getDrmWhereClause(int drmInclusion) {
        drmInclusion = drmInclusion & MediatekFeature.ALL_DRM_MEDIA;
        if (MediatekFeature.ALL_DRM_MEDIA == drmInclusion) {
            return null;
        }
        
        String noDrmClause = FileColumns.IS_DRM + "=0 OR " + FileColumns.IS_DRM + " IS NULL";
        if (NO_DRM_INCLUSION == drmInclusion) {
            return noDrmClause;
        }

        String whereClause = null;

        if ((drmInclusion & MediatekFeature.INCLUDE_FL_DRM_MEDIA) != 0) {
            whereClause = (null == whereClause) ? 
                          FileColumns.DRM_METHOD + "=" + OmaDrmStore.DrmMethod.METHOD_FL :
                          whereClause + " OR " + FileColumns.DRM_METHOD + "=" + 
                          OmaDrmStore.DrmMethod.METHOD_FL;
        }
        if ((drmInclusion & MediatekFeature.INCLUDE_CD_DRM_MEDIA) != 0) {
            whereClause = (null == whereClause) ? 
                          FileColumns.DRM_METHOD + "=" + OmaDrmStore.DrmMethod.METHOD_CD :
                          whereClause + " OR " + FileColumns.DRM_METHOD + "=" + 
                          OmaDrmStore.DrmMethod.METHOD_CD;
        }
        if ((drmInclusion & MediatekFeature.INCLUDE_SD_DRM_MEDIA) != 0) {
            whereClause = (null == whereClause) ? 
                          FileColumns.DRM_METHOD + "=" + OmaDrmStore.DrmMethod.METHOD_SD :
                          whereClause + " OR " + FileColumns.DRM_METHOD + "=" + 
                          OmaDrmStore.DrmMethod.METHOD_SD;
        }
        if ((drmInclusion & MediatekFeature.INCLUDE_FLDCF_DRM_MEDIA) != 0) {
            whereClause = (null == whereClause) ? 
                          FileColumns.DRM_METHOD + "=" + OmaDrmStore.DrmMethod.METHOD_FLDCF :
                          whereClause + " OR " + FileColumns.DRM_METHOD + "=" + 
                          OmaDrmStore.DrmMethod.METHOD_FLDCF;
        }
        whereClause = (null != whereClause) ?
                      "(" + noDrmClause + ") OR (" + FileColumns.IS_DRM + "=1 AND (" + 
                      whereClause + "))" : noDrmClause;

        return whereClause;
    }

    public static Bitmap forceDecodeDrmUri(ContentResolver cr, Uri drmUri, 
                               BitmapFactory.Options options, boolean consume) {
        if (! mIsDrmSupported) {
            Log.w(TAG,"Decode Drm image when Drm is not supported.");
            return null;
        }

        if (null == options) {
            options = new BitmapFactory.Options();
        }

        if (options.mCancel) {
            return null;
        }

        DcfDecoder tempDcfDecoder = new DcfDecoder();
        return tempDcfDecoder.forceDecodeUri(cr, drmUri, options, consume);
    }

    public static int checkRightsStatus(Context context, String path, int action) {
        if (null == mDrmManagerClient) {
            mDrmManagerClient = new OmaDrmClient(context);
        }
        if (null == path) {
            Log.e(TAG,"checkRightsStatus:got null file path");
        }
        return mDrmManagerClient.checkRightsStatus(path, action);
    }

    public static int checkRightsStatusForTap(Context context,
                                              String path, int action) {
        if (null == mDrmManagerClient) {
            mDrmManagerClient = new OmaDrmClient(context);
        }
        if (null == path) {
            Log.e(TAG,"checkRightsStatusForTap:got null file path");
        }
        return mDrmManagerClient.checkRightsStatusForTap(path, action);
    }

    public static OmaDrmClient getDrmManagerClient(Context context) {
        if (null == mDrmManagerClient) {
            mDrmManagerClient = new OmaDrmClient(context);
        }
        return mDrmManagerClient;
    }

    public static void showProtectInfo(Activity activity, Uri uri) {
        Log.i(TAG,"showProtectInfo(uri="+uri+")");
        if (null == activity || null == uri) return;
        OmaDrmClient drmManagerClient =
                                 getDrmManagerClient((Context)activity);
        if (null != drmManagerClient) {
            OmaDrmUiUtils.showProtectionInfoDialog(activity, uri);
        } else {
            Log.e(TAG,"showProtectInfo:get drm manager client failed!");
        }
    }

    private static Bitmap createBitmap(int width, int height, Bitmap.Config config,
                                       int bgColor) {
        if (width <= 0 || height <= 0 || null == config) {
            Log.e(TAG,"createBitmap:invalid Bitmap argumentation");
            return null;
        }

        //create Bitmap to hold thumbnail
        Bitmap canvasBitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(canvasBitmap);
        //draw Background color to avoid merging in to black background
        canvas.drawColor(bgColor);

        return canvasBitmap;
    }

    public static Texture getOverlay(int subType) {
        if (0 != (subType & MediaObject.SUBTYPE_DRM_HAS_RIGHT)) {
            if (sDrmGreenIcon == null) {
                sDrmGreenIcon = new ResourceTexture(MediatekFeature.sContext, 
                        com.mediatek.internal.R.drawable.drm_green_lock);
            }
            return sDrmGreenIcon;
        } else if (0 != (subType & MediaObject.SUBTYPE_DRM_NO_RIGHT)) {
            if (sDrmRedIcon == null) {
                sDrmRedIcon = new ResourceTexture(MediatekFeature.sContext, 
                        com.mediatek.internal.R.drawable.drm_red_lock);
            }
            return sDrmRedIcon;
        }
        return null;
    }

    public static boolean permitShowThumb(int subType) {
        return 0 == (subType & MediaObject.SUBTYPE_DRM_NO_RIGHT);
    }

    public static void renderSubTypeOverlay(GLCanvas canvas,
                           int x, int y, int width, int height, int subType) {
        renderSubTypeOverlay(canvas, x, y, width, height, subType, 1.0f);
    }

    public static void renderSubTypeOverlay(GLCanvas canvas, int x, int y,
                           int width, int height, int subType, float scale) {
        Texture overlay = getOverlay(subType);
        if (null == overlay) return;
        drawRightBottom(canvas, overlay, x, y, width, height, scale);
    }

    public static void drawRightBottom(GLCanvas canvas, Texture tex, int x,
                           int y, int width, int height, float scale) {
        if (null == tex) return;
        int texWidth = (int)((float)tex.getWidth() * scale);
        int texHeight = (int)((float)tex.getHeight() * scale);
        tex.draw(canvas, x + width - texWidth, y + height - texHeight, 
                 texWidth, texHeight);
    }

    //this function guanrentee the returned bitmap is mutable.
    //Note: it may recycle the in passed Bitmap.
    public static Bitmap ensureBitmapMutable(Bitmap b) {
        if (null == b) return null;
        if (b.isMutable()) {
            return b;
        }
        //now we recreate a Bitmap with same content
        Bitmap temp = Bitmap.createBitmap(b.getHeight(), b.getWidth(), 
                                              Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(temp);
        tempCanvas.drawColor(DRM_MICRO_THUMB_DEFAULT_BG);
        tempCanvas.drawBitmap(b,new Matrix(),null);
        b.recycle();
        return temp;
    }

    private static boolean isDrmLockIconInited = false;
    private static Bitmap mDrmRedLockOverlay = null;
    private static Bitmap mDrmGreenLockOverlay = null;

    public static void initDrmLockIcons(Context context) {
        if (!isDrmLockIconInited) {
            mDrmRedLockOverlay = getResBitmap(context, 
                                     com.mediatek.internal.R.drawable.drm_red_lock);
            mDrmGreenLockOverlay = getResBitmap(context, 
                                     com.mediatek.internal.R.drawable.drm_green_lock);
            isDrmLockIconInited = true;
        }
    }

    private static Bitmap getResBitmap(Context context, int resId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeResource(
                context.getResources(), resId, options);
    }

    public static boolean isTimeIntervalMedia(Context context, String path, int action) {
        if (null == mDrmManagerClient) {
            mDrmManagerClient = new OmaDrmClient(context);
        }
        ContentValues values = mDrmManagerClient.getConstraints(path, action);
        if (null != values && (
            -1 != values.getAsInteger(OmaDrmStore.ConstraintsColumns.LICENSE_START_TIME) ||
            -1 != values.getAsInteger(OmaDrmStore.ConstraintsColumns.LICENSE_EXPIRY_TIME))) {
            return true;
        } else {
            return false;
        }
    }

    public static Texture sDrmRedIcon;
    public static Texture sDrmGreenIcon;

//    public static void initialize(Context context) {
//        sDrmRedIcon = new ResourceTexture(context, 
//                                 com.mediatek.internal.R.drawable.drm_red_lock);
//        sDrmGreenIcon = new ResourceTexture(context, 
//                                 com.mediatek.internal.R.drawable.drm_green_lock);
//
//        initDrmLockIcons(context);
//    }

    //this is a high performance version of getSupported operation.
    public static boolean supportConsume(MediaItem item) {
        if (null == item) return false;
        if (item.isDrm() && !item.isDrmMethod(OmaDrmStore.DrmMethod.METHOD_FL)) {
            return true;
        } else {
            return false;
        }
    }

    public static byte[] forceDecryptFile(String pathName, boolean consume) {
        if (null == pathName || !pathName.toLowerCase().endsWith(".dcf")) return null;
        DcfDecoder dcfDecoder = new DcfDecoder();
        return dcfDecoder.forceDecryptFile(pathName, consume);
    }

    public static String getOriginalMimeType(Context context, String path) {
        if (null == mDrmManagerClient) {
            mDrmManagerClient = new OmaDrmClient(context);
        }
        return mDrmManagerClient.getOriginalMimeType(path);
    }

}
