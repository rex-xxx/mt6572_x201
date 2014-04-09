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
package com.mediatek.gallery3d.util;

import android.os.SystemProperties;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.mpo.MpoDecoder;
import com.mediatek.common.gifdecoder.IGifDecoder;
import com.mediatek.gallery3d.gif.GifDecoderWrapper;

// M: added to support Mediatek plug-in
import com.mediatek.gallery3d.ext.IImageOptions;
import com.mediatek.gallery3d.ext.ImageOptions;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumSetPage;
import com.android.gallery3d.app.PickerActivity;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.R;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.PhotoView.Size;
import com.android.gallery3d.ui.ResourceTexture;
import com.android.gallery3d.ui.ScreenNail;

import android.content.Intent;
import android.database.Cursor;
import android.provider.MediaStore;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import java.io.FileDescriptor;
import java.io.InputStream;

import com.mediatek.gallery3d.data.RegionDecoder;
import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.mpo.MpoHelper;
import com.mediatek.gallery3d.stereo.StereoConvertor;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.ui.MtkBitmapScreenNail;

//this class is used to identify what features are added to Gallery3D
//by MediaTek Corporation
public class MediatekFeature {

    private static final String TAG = "Gallery2/MediatekFeature";

    //GIF animation is a feature developed by MediaTek. It avails Skia
    //to play GIF animation
    private static final boolean supportGifAnimation = true;

    //DRM (Digital Rights management) is developed by MediaTek.
    //Gallery3d avails MtkPlugin via android DRM framework to manage
    //digital rights of videos and images
    private static final boolean supportDrm = FeatureOption.MTK_DRM_APP;

    //MPO (Multi-Picture Object) is series of 3D features developed by
    //MediaTek. Camera can shot MAV file or stereo image. Gallery is
    //responsible to list all mpo files add call corresponding module
    //to playback them.
    private static final boolean supportMpo = FeatureOption.MTK_MAV_PLAYBACK_SUPPORT;

    public static final boolean lcaRamOptimize = FeatureOption.MTK_LCA_RAM_OPTIMIZE;
    //Stereo Display
    private static final boolean supportStereoDisplay = MtkUtils.isSupport3d();//true;

    //This feature can stereoly display normal image. It takes advantage
    //of a special lib that can generate the right eye image from origin
    //(Origin image can be perceived as the left eye image)
    //The actual result depends on the outcome of the lib. So this feature
    //may be turned off if customer thinks this feature is not useful.
    private static final boolean supportDisplay2dAs3d = 
                                     supportStereoDisplay && true;

    //MyFavorite is a short cut type application developed by MediaTek
    //It provides entries like Camera Photo, My Music, My Videos
    private static final boolean customizedForMyFavorite = true;

    //VLW stands for Video Live Wallpaper developed by MediaTek. It is 
    //like Live Wallpaper which can display dynamic wallpapers, by unlike
    //Live wallpaper, VLW's source is common video/videos, not OpenGL 
    //program
    private static final boolean customizedForVLW = true;

    // Media3D app requires to pick an image folder, so we've added corresponding
    // logic in AlbumPicker/AlbumSetPage to support this.
    // This feature shares the common flow with VLW.
    private static final boolean customizedForMedia3D = true;

    //Bluetooth Print feature avails Bluetooth capacity on MediaTek
    //platform to print specified kind of image to a printer.
    private static final boolean supportBluetoothPrint = 
                                        false;//true;//FeatureOption.MTK_BT_PROFILE_BPP;

    //Picture quality enhancement feature avails Camera ISP hardware
    //to improve image quality displayed on the screen.
    private static final boolean supportPictureQualityEnhance = true;

    // HW limitation
    private static final int JPEG_LENGTH_MAX = 8192;

    // if system support thememanager,Cropview should get theme main color.
    private static final boolean IS_SUPPORT_THEMEMANAGER = FeatureOption.MTK_THEMEMANAGER_APP;
    public static boolean isGifAnimationSupported() {
        return supportGifAnimation;
    }

    public static boolean isDrmSupported() {
        return supportDrm;
    }

    public static boolean isMpoSupported() {
        return supportMpo;
    }

    public static boolean isStereoDisplaySupported() {
        return supportStereoDisplay;
    }

    public static boolean isDisplay2dAs3dSupported() {
        return supportDisplay2dAs3d;
    }

    public static boolean hasCustomizedForMyFavorite() {
        return customizedForMyFavorite;
    }

    public static boolean hasCustomizedForVLW() {
        return customizedForVLW;
    }
    
    public static boolean hasCustomizedForMedia3D() {
        return customizedForMedia3D;
    }

    public static boolean isBluetoothPrintSupported() {
        return supportBluetoothPrint;
    }

    public static boolean isPictureQualityEnhanceSupported() {
        return supportPictureQualityEnhance;
    }

    public static boolean preferDisplayOriginalSize() {
        if (!sIsImageOptionsPrepared) {
            prepareImageOptions(sContext);
        }
        return sImageOptions == null ? false :
            sImageOptions.shouldUseOriginalSize();
    }

    public static boolean isThemeManagerSupported() {
        return IS_SUPPORT_THEMEMANAGER;
    }

    public static int themeMainColor() {
        Resources resources = sContext.getResources();
        if (resources != null) {
            return resources.getThemeMainColor();
        } else {
            return 0;
        }
    }

    public static boolean preferGoTopWhenBack() {
        if (!sIsImageOptionsPrepared) {
            prepareImageOptions(sContext);
        }
        return sImageOptions == null ? false : sImageOptions.shouldReturnTopWhenBack();
    }

    //the following are variables or settings for Mediatek feature

    public static void initialize(Context context) {
//        DrmHelper.initialize(context);
//        MpoHelper.initialize(context);
//        StereoHelper.initialize(context);
//        StereoConvertor.initialize(context);
        //prepare for operator
//        prepareImageOptions(context);
        sContext = context;
    }

    //this variable indicates how many CPU cores are there in the device.
    //It is used for fine tune ThreadPool performance.
    private static final int cpuCoreNum = 2;

    public static int getCpuCoreNum() {
        return cpuCoreNum;
    }

    public static class Params {
        public static final int THUMBNAIL_TARGET_SIZE_LARGER = 960;
        public Params() {
            inOriginalFrame = false;
            inOriginalFullFrame = false;
            inFirstFrame = false;
            inSecondFrame = false;
            inFirstFullFrame = false;
            inSecondFullFrame = false;
            inGifDecoder = false;
            inMpoFrames = false;
            inMpoTotalCount = false;
            
            inType = MediaItem.TYPE_THUMBNAIL;
            inOriginalTargetSize = MediaItem.THUMBNAIL_TARGET_SIZE;
            inSampleDown = false;
            inPQEnhance = false;
            inTargetDisplayWidth = 0;
            inTargetDisplayHeight = 0;
        }

        public void info() {
            info(TAG);
        }

        public void info(String tag) {
            if (inOriginalFrame)
                Log.v(tag,"Params:inOriginalFrame="+inOriginalFrame);
            if (inFirstFrame)
                Log.v(tag,"Params:inFirstFrame="+inFirstFrame);
            if (inSecondFrame)
                Log.v(tag,"Params:inSecondFrame="+inSecondFrame);
            if (inOriginalFullFrame)
                Log.v(tag,"Params:inOriginalFullFrame="+inOriginalFullFrame);
            if (inFirstFullFrame)
                Log.v(tag,"Params:inFirstFullFrame="+inFirstFullFrame);
            if (inSecondFullFrame)
                Log.v(tag,"Params:inSecondFullFrame="+inSecondFullFrame);
            if (inGifDecoder)
                Log.v(tag,"Params:inGifDecoder="+inGifDecoder);
            if (0 != inOriginalTargetSize)
                Log.v(tag,"Params:inOriginalTargetSize="+inOriginalTargetSize);
            if (inSampleDown)
                Log.v(tag,"Params:inSampleDown="+inSampleDown);
            if (inPQEnhance)
                Log.v(tag,"Params:inPQEnhance="+inPQEnhance);
            if (inMpoFrames) {
                Log.v(tag,"Params:inMpoFrames=" + inMpoFrames);
            }
            if (inMpoTotalCount) {
                Log.v(tag,"Params:inMpoTotalCount=" + inMpoTotalCount);
            }
        }

        //added for stereo display feature, indicating whether we want the
        //original frame. For 2D to 3D function, original frame is different
        //from first frame. For Jps & Mpo image, original frame equals
        //first frame.
        public boolean inOriginalFrame;

        //added to decode full image. This may be used for drm source.
        public boolean inOriginalFullFrame;

        //added for stereo display feature, indicating whether we want the
        //logical first frame
        public boolean inFirstFrame;

        //added for stereo display feature, indicating whether we want the
        //logical second frame. 
        public boolean inSecondFrame;

        //added for stereo display feature, indicating whether we want the
        //logical first full frame
        public boolean inFirstFullFrame;

        //added for stereo display feature, indicating whether we want the
        //logical second full frame
        public boolean inSecondFullFrame;

        //tell if the GifDecoder should be retrieved
        public boolean inGifDecoder;

        //tell the type fo thumbnail to be retrieved
        public int inType;

        //tell how large the thumbnail should be scaled
        public int inOriginalTargetSize;

        //tell if the decoded Bitmap is to be sampled down the same as the
        //cache
        public boolean inSampleDown;

        //added for picture quality enhancement, indicating whether image
        //is enhanced
        public boolean inPQEnhance;
        
        //tell if MPO frames should be retrieved
        public boolean inMpoFrames;
        
        //tell if MPO total count should be retrieved
        public boolean inMpoTotalCount;
        
        //the two variables tells how large the decoded image will be
        //displayed on the screen. Currently, this variable is used only
        //for mpo.
        public int inTargetDisplayWidth;
        public int inTargetDisplayHeight;
    }

    public static class DataBundle {
        public DataBundle() {
            originalFrame = null;
            firstFrame = null;
            secondFrame = null;
            originalFullFrame = null;
            firstFullFrame = null;
            secondFullFrame = null;
            gifDecoder = null;
            mpoFrames = null;
            mpoTotalCount = 0;
        }
        public void recycle() {
            if (null != originalFrame) originalFrame.recycle();
            if (null != firstFrame) firstFrame.recycle();
            if (null != secondFrame) secondFrame.recycle();
            if (null != originalFullFrame) originalFullFrame.release();
            if (null != firstFullFrame) firstFullFrame.release();
            if (null != secondFullFrame) secondFullFrame.release();

            originalFrame = null;
            firstFrame = null;
            secondFrame = null;
            firstFullFrame = null;
            secondFullFrame = null;
            gifDecoder = null;
            mpoFrames = null;
            mpoTotalCount = 0;
        }

        public void info() {
            info(TAG);
        }

        public void info(String tag) {
            //Log.i(tag,"DataBundle:originalFrame="+originalFrame);
            if (null != originalFrame) {
                Log.v(tag,"DataBundle:originalFrame[" + originalFrame.getWidth()
                          + "x" + originalFrame.getHeight() + "]");
            }
            //Log.d(tag,"DataBundle:firstFrame="+firstFrame);
            if (null != firstFrame) {
                Log.v(tag,"DataBundle:firstFrame[" + firstFrame.getWidth()
                          + "x" + firstFrame.getHeight() + "]");
            }
            //Log.d(tag,"DataBundle:secondFrame="+secondFrame);
            if (null != secondFrame) {
                Log.v(tag,"DataBundle:secondFrame[" + secondFrame.getWidth()
                          + "x" + secondFrame.getHeight() + "]");
            }
            //Log.d(tag,"DataBundle:originalFullFrame="+originalFullFrame);
            if (null != originalFullFrame) {
                Log.v(tag,"DataBundle:originalFullFrame["
                          + originalFullFrame.getWidth()
                          + "x" + originalFullFrame.getHeight() + "]");
            }
            //Log.d(tag,"DataBundle:firstFullFrame="+firstFullFrame);
            if (null != firstFullFrame) {
                Log.v(tag,"DataBundle:firstFullFrame[" + firstFullFrame.getWidth()
                          + "x" + firstFullFrame.getHeight() + "]");
            }
            //Log.d(tag,"DataBundle:secondFullFrame="+secondFullFrame);
            if (null != secondFullFrame) {
                Log.v(tag,"DataBundle:secondFullFrame[" + secondFullFrame.getWidth()
                          + "x" + secondFullFrame.getHeight() + "]");
            }
            //Log.d(tag,"DataBundle:gifDecoder="+gifDecoder);
            if (null != gifDecoder) {
                Log.v(tag,"DataBundle:gifDecoder,getTotalFrameCount()="
                          + gifDecoder.getTotalFrameCount());
            }
            
            //Log.d(tag,"DataBundle:mpoFrames="+mpoFrames);
            if (null != mpoFrames) {
                Log.v(tag,"DataBundle:mpoFrames,length="
                          + mpoFrames.length);
            }
            
            //Log.d(tag,"DataBundle:mpoTotalCount="+mpoTotalCount);
            if (0 != mpoTotalCount) {
                Log.v(tag,"DataBundle:mpoTotalCount=" + mpoTotalCount);
            }
        }
        public Bitmap originalFrame;
        public Bitmap firstFrame;
        public Bitmap secondFrame;
        public RegionDecoder originalFullFrame;
        public RegionDecoder firstFullFrame;
        public RegionDecoder secondFullFrame;
        public GifDecoderWrapper gifDecoder;
        public Bitmap[] mpoFrames;
        public int mpoTotalCount;
    }

    //Stere Photo Display
    public static final int STEREO_DISPLAY_INVALID_PASS = -1;
    public static final int STEREO_DISPLAY_LEFT_PASS = 1;
    public static final int STEREO_DISPLAY_RIGHT_PASS = 2;

    //Mediatek inclusion used in querying data base
    //So far, we have add two kinds of inclusion
    //inc1: add new mime types, such as mpo, jps
    //inc2: add new way to operate the files: drm protection
    //influnce can be express as follow:
    //The media sets that are supported by Google default Gallery
    //are named as set A
    //when we add inc1, we are adding an extra media sets to A,
    //let's name it as B
    //before we add inc2, all we have is A+B, an enlarge media set
    //when we add inc2 (the drm protection), we have a converted
    //media set A' and B'. From the name, we can tell A' represents
    //set A that protected by drm, and B' represents set B that
    //protected by drm.
    //Note:as OMA drm v1.0 that we support in Gallery emerged very
    //early, mpo and jps kind media may not be protected by drm
    //in practise. Therefore, media set B' is empty at present, and
    //may be not empty in the future.

    //now we have A,A',B,B' four kinds of set, each launching is
    //actually set operation. for example
    //case 1, when all features are support and Gallery launches
    //    normally, drm inclusions, mpo inclusions and stereo
    //    are all setted, then we query all medias.
    //case 2, when drm are not supported, and Gallery launches to
    //    query stereo, mpo and stereo inclusion should be set, then
    //    A and B will be queried out.
    //case 3, when drm are supported and MAV (subset of MPO) is not
    //    supported, and stereo are supported, drm inclusion is set,
    //    mav is not set, stereo are set. When quering, mav media
    //    with drm protection should not be queried out.
    //case 4, when user only want view stereo image and video, 
    //    (default pictures such as jpg, gif should not be exhibited
    //    then, exclude default media bit is setted to 1, include 3d
    //    pan and 3d bit is setted to 1.

    public static final int EXCLUDE_DEFAULT_MEDIA = (1 << 0);
    //below is intended for DRM kind media
    //include fl type drm media
    public static final int INCLUDE_FL_DRM_MEDIA = (1 << 1);
    //include cd type drm media
    public static final int INCLUDE_CD_DRM_MEDIA = (1 << 2);
    //include sd type drm media
    public static final int INCLUDE_SD_DRM_MEDIA = (1 << 3);
    //include fldcf type drm media
    public static final int INCLUDE_FLDCF_DRM_MEDIA = (1 << 4);
    //include all types of drm media
    public static final int ALL_DRM_MEDIA = INCLUDE_FL_DRM_MEDIA |
                                            INCLUDE_CD_DRM_MEDIA |
                                            INCLUDE_SD_DRM_MEDIA |
                                            INCLUDE_FLDCF_DRM_MEDIA;

    //below is intended for Stereo & MPO feature
    public static final int INCLUDE_MPO_MAV = (1 << 6);
    public static final int INCLUDE_MPO_3D  = (1 << 7);
    public static final int INCLUDE_MPO_3D_PAN = (1 << 8);
    public static final int INCLUDE_MPO_UNKNOWN = (1 << 9);
    public static final int ALL_MPO_MEDIA = INCLUDE_MPO_UNKNOWN |
                                            INCLUDE_MPO_3D_PAN |
                                            INCLUDE_MPO_3D |
                                            INCLUDE_MPO_MAV;

    public static final int INCLUDE_STEREO_JPS = (1 << 12);
    public static final int INCLUDE_STEREO_PNS = (1 << 13);

    public static final int INCLUDE_STEREO_VIDEO = (1 << 14);

    //This field represents a virtual folder, rather than media types.
    //If this bit is set, we will create a virtual folder (3D Media)
    //for user to use. Or, the virtual folder will be created
    public static final int INCLUDE_STEREO_FOLDER = (1 << 15);
    
    public static final int EXCLUDE_MPO_MAV = (1 << 16);

    public static int getInclusionFromData(Bundle data) {
        return DrmHelper.getDrmInclusionFromData(data) |
               StereoHelper.getInclusionFromData(data);
    }
    
    public static int getMavInclusionFromData(Bundle data) {
        return MpoHelper.getInclusionFromData(data);
    }

    public static String getWhereClause(int mtkInclusion) {
        //Log.i(TAG,"getWhereClause(mtkInclusion="+mtkInclusion+")");
        //added support for Streao display
        String whereClauseStereo = StereoHelper.getWhereClause(mtkInclusion);

        //added support for MPO and DRM

        String whereClauseDrm = null;
        if (supportDrm) {
            whereClauseDrm = DrmHelper.getDrmWhereClause(mtkInclusion);
        } else {
            whereClauseDrm = DrmHelper.getDrmWhereClause(DrmHelper.NO_DRM_INCLUSION);
        }

        String whereClauseMpo = null;
        whereClauseMpo = MpoHelper.getMpoWhereClause(supportMpo);
        
        String whereGroup = null;
        if (null != whereClauseDrm) {
            whereGroup = (null == whereGroup) ? whereClauseDrm :
                         "(" + whereGroup + ") AND (" + whereClauseDrm +")";
        }
        
        if (null != whereClauseMpo) {
            whereGroup = (null == whereGroup) ? whereClauseMpo :
                         "(" + whereGroup + ") AND (" + whereClauseMpo +")";
        }
        
        //added to erase folder that Gallery secretly created ".ConvertedTo2D"
        if (null != whereClauseStereo) {
            whereGroup = (null == whereGroup) ? whereClauseStereo :
                         "(" + whereGroup + ") AND (" + whereClauseStereo +")";
        }
        return whereGroup;
    }

    public static String getWhereClause(int mtkInclusion, boolean queryVideo) {
        //Log.i(TAG,"getWhereClause(mtkInclusion="+mtkInclusion+")");
        //added support for Streao display
        String whereClauseStereo = 
                     StereoHelper.getWhereClause(mtkInclusion, queryVideo);

        //added support for MPO and DRM

        String whereClauseDrm = null;
        if (supportDrm) {
            whereClauseDrm = DrmHelper.getDrmWhereClause(mtkInclusion);
        } else {
            whereClauseDrm = DrmHelper.getDrmWhereClause(DrmHelper.NO_DRM_INCLUSION);
        }

        String whereClauseMpo = null;
        whereClauseMpo = MpoHelper.getMpoWhereClause(supportMpo);
        
        String whereGroup = null;
        if (null != whereClauseDrm) {
            whereGroup = (null == whereGroup) ? whereClauseDrm :
                         "(" + whereGroup + ") AND (" + whereClauseDrm +")";
        }
        if (null != whereClauseMpo) {
            whereGroup = (null == whereGroup) ? whereClauseMpo :
                         "(" + whereGroup + ") AND (" + whereClauseMpo +")";
        }
        
        //added to erase folder that Gallery secretly created ".ConvertedTo2D"
        if (null != whereClauseStereo) {
            whereGroup = (null == whereGroup) ? whereClauseStereo :
                         "(" + whereGroup + ") AND (" + whereClauseStereo +")";
        }
        return whereGroup;
    }

    public static String getOnlyStereoWhereClause(int drmInclusion, boolean queryVideo) {
        int stereoInclusion = 0;
        if (queryVideo) {
            stereoInclusion |= INCLUDE_STEREO_VIDEO;
        } else {
            stereoInclusion |= INCLUDE_MPO_3D;
            stereoInclusion |= INCLUDE_MPO_3D_PAN;
            stereoInclusion |= INCLUDE_STEREO_JPS;
        }
        //exclude normal images/videos
        stereoInclusion |= EXCLUDE_DEFAULT_MEDIA;
        return getWhereClause(stereoInclusion | drmInclusion, queryVideo);
    }
    public static String getOnlyStereoWhereClause(int drmInclusion) {
        int stereoInclusion = 0;
        stereoInclusion |= INCLUDE_STEREO_VIDEO;
        stereoInclusion |= INCLUDE_MPO_3D;
        stereoInclusion |= INCLUDE_MPO_3D_PAN;
        stereoInclusion |= INCLUDE_STEREO_JPS;
        //exclude normal images/videos
        stereoInclusion |= EXCLUDE_DEFAULT_MEDIA;
        return getWhereClause(stereoInclusion | drmInclusion);
    }

    public static String getAddedMimetype(String extension) {
        if (null == extension) return null;
        if (MpoHelper.MPO_EXTENSION.equalsIgnoreCase(extension)) {
            return MpoHelper.MPO_MIME_TYPE;
        } else if (StereoHelper.JPS_EXTENSION.equalsIgnoreCase(extension)) {
            return StereoHelper.JPS_MIME_TYPE;
        }
        //for default format, we return null
        return null;
    }

    public static int getPhotoWidgetInclusion() {
        int mtkInclusion = 0;
        if (MediatekFeature.isDrmSupported()) {
            //we only add fl kind drm image
            mtkInclusion |= MediatekFeature.INCLUDE_FL_DRM_MEDIA;
        }
        if (MediatekFeature.isMpoSupported()) {
            mtkInclusion |= MediatekFeature.INCLUDE_MPO_MAV;
            if (MediatekFeature.isStereoDisplaySupported()) {
                mtkInclusion |= MediatekFeature.INCLUDE_MPO_3D;
                mtkInclusion |= MediatekFeature.INCLUDE_MPO_3D_PAN;
                mtkInclusion |= MediatekFeature.INCLUDE_STEREO_JPS;
                mtkInclusion |= MediatekFeature.INCLUDE_STEREO_PNS;
                //this is nasty!!! but as StereoHelper is not well
                //developped, we add the inclusion here
                mtkInclusion |= MediatekFeature.INCLUDE_STEREO_VIDEO;
            }
        } else {
            if (MediatekFeature.isStereoDisplaySupported()) {
                mtkInclusion |= MediatekFeature.INCLUDE_STEREO_PNS;
                //this is nasty!!! but as StereoHelper is not well
                //developped, we add the inclusion here
                mtkInclusion |= MediatekFeature.INCLUDE_STEREO_VIDEO;
            }
        }
        return mtkInclusion;
    }

    public static Uri addMtkInclusion(Uri uri, Path path) {
        if (!supportDrm) return uri;
        if (null == uri || null == path) {
            Log.e(TAG, "addMtkInclusion:invalid parameter");
            return uri;
        }
        if (path.getMtkInclusion() != 0) {
            uri = uri.buildUpon().appendQueryParameter("mtkInclusion", 
                 String.valueOf(path.getMtkInclusion()))
                 .build();
            Log.i(TAG,"addMtkInclusion:uri="+uri);
        }
        return uri;
    }

    //replace Bitmap's back ground with specified color
    public static Bitmap replaceBitmapBgColor(Bitmap b, int color,
                                              boolean recycleInput) {
        if (null == b) return null;
        if (b.getConfig() == Bitmap.Config.RGB_565) {
            Log.i(TAG,"replaceBitmapBgColor:Bitmap has no alpha, no bother");
            return b;
        }
        //Bitmap has alpha channel, and should be replace its background color
        //1,create a new bitmap with same dimension and ARGB_8888
        if (b.getWidth() <= 0 || b.getHeight() <= 0) {
            Log.w(TAG,"replaceBitmapBgColor:invalid Bitmap dimension");
            return b;
        }
        Bitmap b2 = null;
        try {
            b2 = Bitmap.createBitmap(b.getWidth(), 
                     b.getHeight(), Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "failed to create new bitmap for replacing gif background: ", e);
            return b;
        }
        //2,create Canvas to encapulate new Bitmap
        Canvas canvas = new Canvas(b2);
        //3,draw background color
        canvas.drawColor(color);
        //4,draw original Bitmap on background
        canvas.drawBitmap(b,new Matrix(),null);
        //5,recycle original Bitmap if needed
        if (recycleInput) {
            b.recycle();
            b = null;
        }
        //6,return the output Bitmap
        return b2;
    }
    
    // crop input Bitmap to fit the desired aspect ratio
    public static Bitmap cropToFitAspectRatio(Bitmap image, int width,
                                              int height, boolean recycleInput) {
        Log.e(TAG, "cropToRetainAspectRatio");
        if (image  == null || width == 0 || height == 0) {
            return null;
        }
        int srcWidth = image.getWidth();
        int srcHeight = image.getHeight();
        float srcRatio = (float) srcWidth / (float) srcHeight;
        float destRatio = (float) width / (float) height;
        Log.i(TAG, " srcRatio=" + srcRatio + ", destRatio=" + destRatio);
        if (srcRatio == destRatio) {
            return image;
        }
        int destWidth = srcWidth;
        int destHeight = srcHeight;
        Bitmap ret = image;
        boolean shouldCropWidth = srcRatio > destRatio;
        Rect srcRect = null;
        Rect destRect = null;
        if (shouldCropWidth) {
            // crop width to fit the desired aspect ratio(which is width/height);
            destWidth = Math.round((float) srcHeight * destRatio);
            destHeight = srcHeight;
            srcRect = new Rect((srcWidth - destWidth) / 2, 0,
                               (srcWidth + destWidth) / 2, srcHeight);
        } else {
            // crop height to fit the desired aspect ratio
            destHeight = Math.round((float) srcWidth / destRatio);
            destWidth = srcWidth;
            srcRect = new Rect(0, (srcHeight - destHeight) / 2,
                               srcWidth, srcHeight / 2 + destHeight / 2);
        }
        destRect = new Rect(0, 0, destWidth, destHeight);
        ret = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(ret);
        c.drawBitmap(image, srcRect, destRect, null);
        
        if (recycleInput) {
            image.recycle();
        }
        return ret;
    }

    // M: added for mtk plug-in
    public static IImageOptions sImageOptions;
    public static boolean sIsImageOptionsPrepared = false;
    public static Context sContext = null;
    
    // M: added for mtk plug-in
    public static void prepareImageOptions(Context context) {
        if (context == null) {
            return;
        }
        if (sImageOptions == null) {
            try {
                sImageOptions = (IImageOptions) PluginManager.createPluginObject(
                        context.getApplicationContext(), IImageOptions.class.getName());
                Log.i(TAG,"prepareImageOptions:sImageOptions="+sImageOptions);
            } catch (Plugin.ObjectCreationException e) {
                e.printStackTrace();
                Log.w(TAG,"prepareImageOptions:JE happened"+e);
                sImageOptions = new ImageOptions();
            }
            sIsImageOptionsPrepared = true;
        }
    }
    
    public static IImageOptions getImageOptions() {
        if (!sIsImageOptionsPrepared) {
            prepareImageOptions(sContext);
        }
        return (sImageOptions != null ? sImageOptions : new ImageOptions());
    }

    // M: add for VideoLiveWallpaper support BEGIN
    private static final String MIMETYPE_VIDEO_ALL = "video/*";
    private static final String CURSOR_MIMETYPE_VIDEO = ContentResolver.CURSOR_DIR_BASE_TYPE + "/video";
    
    // M: added for media3D
    private static final String MIMETYPE_IMAGE_ALL = "image/*";
    private static final String CURSOR_MIMETYPE_IMAGE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/image";
    
    public static boolean checkForOtherPickActions(PickerActivity activity, Bundle data, Intent intent) {
        if (!customizedForVLW && !customizedForMedia3D) {
            return false;
        }
        String mimeType = intent.getType();
        boolean pickVideo = MIMETYPE_VIDEO_ALL.equalsIgnoreCase(mimeType) 
                || CURSOR_MIMETYPE_VIDEO.equalsIgnoreCase(mimeType);
        boolean pickImage = MIMETYPE_IMAGE_ALL.equalsIgnoreCase(mimeType) 
                || CURSOR_MIMETYPE_IMAGE.equalsIgnoreCase(mimeType);
        if (pickVideo) {
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, 
                    activity.getDataManager().getTopSetPath(DataManager.INCLUDE_VIDEO));
        } else if (pickImage) {
            data.putString(AlbumSetPage.KEY_MEDIA_PATH, 
                    activity.getDataManager().getTopSetPath(DataManager.INCLUDE_IMAGE));
        }
        return pickVideo || pickImage;
    }
    
    public static void insertBucketIdForPickActions(MediaSet targetSet, Intent result) {
        if (!customizedForVLW && !customizedForMedia3D) {
            return;
        }
        if (targetSet instanceof LocalAlbum) {
            String bucketId = targetSet.getPath().getSuffix();
            result.putExtra("bucketId", bucketId);
        }
    }
    // M: add for VideoLiveWallpaper support END
    
    // M: add for photo widget modification BEGIN
    private static final boolean supportMAV = FeatureOption.MTK_MAV_PLAYBACK_SUPPORT;
    public static final String MIMETYPE_MPO = "image/mpo";
    
    public static void drawWidgetImageTypeOverlay(Context context, Uri uri, Bitmap bitmap) {
        if (uri == null || bitmap == null) {
            return;
        }
        // M: query media DB for mime type and other info
        String[] columns = new String[] {MediaStore.Images.ImageColumns.MIME_TYPE,
                                         MediaStore.Video.Media.STEREO_TYPE};
        String mimeType = "";
        int stereoType = 0;
        Cursor c = context.getContentResolver().query(uri, columns, null, null, null);
        
        if (c != null) {
            if (c.moveToFirst()) {
                mimeType = c.getString(0);
                stereoType = c.getInt(1);
            }
            c.close();
        }
        
        boolean isStereo = (stereoType != MediaStore.Video.Media.STEREO_TYPE_2D);
        boolean isMAV = MIMETYPE_MPO.equalsIgnoreCase(mimeType) && !isStereo;
        
        if (isMAV) {
            MpoHelper.drawImageTypeOverlay(context, bitmap);
        }
        
        if (isStereo) {
            StereoHelper.drawImageTypeOverlay(context, bitmap);
        }
    }
    // M: add for photo widget modification END

    // M: added for rendering sub-type overlay on micro-thumbnail, e.g. MAV/Stereo3D/...
    private static ResourceTexture mMavOverlay = null;
    public static void renderSubTypeOverlay(Context context, GLCanvas canvas, int width,
            int height, int subType) {
        if (subType == 0) {
            return;
        }
        
        // M: for migration test only
        //new RuntimeException().fillInStackTrace().printStackTrace();
        
        boolean isMav = supportMAV;
        isMav &= (subType & MediaObject.SUBTYPE_MPO_MAV) != 0;
        if (isMav) {
            if (mMavOverlay == null) {
                mMavOverlay = new ResourceTexture(context, R.drawable.ic_mav_overlay);
            }
            int side = Math.min(width, height) / 5;
            /// TODO: retain mav overlay original aspect ratio
            mMavOverlay.draw(canvas, (width - side) / 2, (height - side) / 2, side, side);
        }

        //render drm icon
        DrmHelper.renderSubTypeOverlay(canvas, 0, 0, width, height, subType);

        //render stereo icon
        StereoHelper.renderSubTypeOverlay(canvas, 0, 0, width, height, subType);
    }

    // M: added to support MAV begin
    public static boolean handleMavPlayback(Context context, MediaItem item) {
        if (supportMAV && (item.getSubType() & MediaObject.SUBTYPE_MPO_MAV) != 0) {
            playMpo(context, item.getContentUri());
            return true;
        }
        return false;
    }
    
    public static boolean isMAVSupported() {
        return supportMAV;
    }
    
    private static final String MAV_VIEW_ACTION = "com.mediatek.action.VIEW_MAV";
    public static void playMpo(Context context, Uri uri) {
        try {
            Intent i = new Intent(MAV_VIEW_ACTION);
            i.setDataAndType(uri, MIMETYPE_MPO);
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Unable to open mpo file: ", e);
        }
    }
    // M: added to support MAV end
    
    // M: added for Gif animation support
    public static final String MIMETYPE_GIF = "image/gif";
    private static final int gifBackGroundColor = 0xFFFFFFFF;
    
    public static boolean isSupportedByGifDecoder(String mimeType) {
        if (!supportGifAnimation) return false;
        if (mimeType == null) return false;
        mimeType = mimeType.toLowerCase();
        return mimeType.equals("image/gif");
    }

    public static boolean isGifSupported() {
        return supportGifAnimation;
    }

    public static int getGifBackgroundColor() {
        return gifBackGroundColor;
    }

    public static Bitmap replaceGifBackGround(Bitmap bitmap, String filePath) {
        if (null == bitmap || null == filePath) return bitmap;
        if (supportGifAnimation &&filePath.toLowerCase().endsWith(".gif")) {
            //if needed, replace gif background
            return replaceGifBackground(bitmap);
        } else {
            return bitmap;
        }
    }

    public static Bitmap replaceGifBackground(Bitmap bitmap) {
        if (supportGifAnimation) {
            return replaceBitmapBgColor(bitmap, gifBackGroundColor, true);
        } else {
            return bitmap;
        }
    }

    public static GifDecoderWrapper createGifDecoder(String filePath) {
        if (null == filePath) return null;
        return GifDecoderWrapper.createGifDecoderWrapper(filePath);
    }

    public static GifDecoderWrapper createGifDecoder(InputStream is) {
        if (null == is) return null;
        return GifDecoderWrapper.createGifDecoderWrapper(is);
    }

    public static GifDecoderWrapper createGifDecoder(FileDescriptor fd) {
        if (null == fd) return null;
        return GifDecoderWrapper.createGifDecoderWrapper(fd);
    }
    // M: added to support GIF animation end

    // M: added for UI display

    public static boolean needMtkScreenNail(int subType) {
        if (0 != (subType & MediaObject.SUBTYPE_DRM_NO_RIGHT) ||
            0 != (subType & MediaObject.SUBTYPE_DRM_HAS_RIGHT) ||
            0 != (subType & MediaObject.SUBTYPE_ORIGIN_SIZE) ) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean permitShowThumb(int subType) {
        return DrmHelper.permitShowThumb(subType);
    }

    public static ScreenNail getMtkScreenNail(MediaItem item, Bitmap bitmap) {
        if (null == item || null == bitmap) return null;
        //add Mediatek feature options...
        int subType = item.getSubType();
        if (needMtkScreenNail(subType)) {
            int width = item.getWidth() > 0 ?
                        item.getWidth() : bitmap.getWidth();
            int height = item.getHeight() > 0 ?
                         item.getHeight() : bitmap.getHeight();
            MtkBitmapScreenNail mtkScreenNail = 
                new MtkBitmapScreenNail(bitmap, width, height); 
            mtkScreenNail.setSubType(subType);
            return mtkScreenNail;
        } else {
            return null;
        }
    }

    public static ScreenNail getMtkScreenNail(MediaItem item) {
        if (null == item) return null;
        //add Mediatek feature options...
        int subType = item.getSubType();
        if (needMtkScreenNail(subType)) {
            int width = item.getWidth();
            int height = item.getHeight();
            MtkBitmapScreenNail mtkScreenNail =
                      new MtkBitmapScreenNail(width, height);
            mtkScreenNail.setSubType(subType);
            return mtkScreenNail;
        } else {
            return null;
        }
    }

    public static MtkBitmapScreenNail toMtkScreenNail(ScreenNail screenNail) {
        if (null != screenNail && screenNail instanceof MtkBitmapScreenNail) {
            return (MtkBitmapScreenNail) screenNail;
        } else {
            return null;
        }
    }

    public static int getScreenNailSubType(ScreenNail screenNail) {
        if (null != screenNail && screenNail instanceof MtkBitmapScreenNail) {
            return ((MtkBitmapScreenNail) screenNail).getSubType();
        } else {
            return 0;
        }
    }

    public static void updateSizeForSubtype(Size size, ScreenNail screenNail) {
        if (null == size || null == screenNail) return;
        MtkBitmapScreenNail mtkScreenNail = toMtkScreenNail(screenNail);
        if (null == mtkScreenNail) return;
        int subType = mtkScreenNail.getSubType();
        if ((subType & MediaObject.SUBTYPE_ORIGIN_SIZE) != 0) {
            size.width = mtkScreenNail.getOriginWidth();
            size.height = mtkScreenNail.getOriginHeight();
        }
    }

    public static Size getSizeForSubtype(ScreenNail screenNail) {
        if (null == screenNail) return null;
        MtkBitmapScreenNail mtkScreenNail = toMtkScreenNail(screenNail);
        if (null == mtkScreenNail) return null;
        int subType = mtkScreenNail.getSubType();
        if ((subType & MediaObject.SUBTYPE_ORIGIN_SIZE) != 0 &&
            mtkScreenNail.getOriginWidth() > 0 &&
            mtkScreenNail.getOriginHeight() > 0) {
            Size size = new Size();
            size.width = mtkScreenNail.getOriginWidth();
            size.height = mtkScreenNail.getOriginHeight();
            return size;
        }
        return null;
    }

    public static boolean syncSubType(ScreenNail target, ScreenNail source) {
        if (null == target || null == source) {
           Log.e(TAG,"syncSubType:why we got null target or source");
           return false;
        }
        MtkBitmapScreenNail mtkTarget = toMtkScreenNail(target);
        MtkBitmapScreenNail mtkSource = toMtkScreenNail(source);
        if (null != mtkTarget && null != mtkSource) {
            mtkTarget.setSubType(mtkSource.getSubType());
            return true;
        }
        return false;
    }

    public static float getMinimalScale(float viewW, float viewH,
                                        int imageW, int imageH, int subType) {
        if (DrmHelper.showDrmMicroThumb(subType)) {
            //for special drm layout, show image round 40000 pixels
            double scale = Math.sqrt((double)DrmHelper.DRM_MICRO_THUMB_PIXEL_COUNT
                       / (double)imageW / (double)imageH);
            //Log.d(TAG,"getMinimalScale:scale="+scale);
            return (float) scale;
        }
        return Math.min(viewW / imageW, viewH / imageH);
    }

    public static boolean doesMaxEqualMin(int subType) {
        if (DrmHelper.showDrmMicroThumb(subType)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean preferDisplayOriginSize(int subType) {
        if (0 != (subType & MediaObject.SUBTYPE_ORIGIN_SIZE)) {
            return true;
        } else {
            return false;
        }
    }

    public static float minScaleLimit(int subType) {
        if (preferDisplayOriginSize(subType)) {
            return 1.0f;
        } else {
            return -1.0f;
        }
    }

    public static boolean showDrmMicroThumb(int subType) {
        return DrmHelper.showDrmMicroThumb(subType);
    }
    // M: added to support UI display end

    // M: added for stereo utils
    public static boolean isStereoImage(MediaObject obj) {
        if (!supportStereoDisplay || null == obj) return false;
        int operation = obj.getSupportedOperations();
        if (0 != (operation & MediaObject.SUPPORT_STEREO_DISPLAY) &&
            0 == (operation & MediaObject.SUPPORT_CONVERT_TO_3D)) {
            return true;
        } else {
            return false;
        }
    }
    // M: added for stereo utils end

    // M: added for picture quality enhancement
    public static void enablePictureQualityEnhance(Options options,
                           boolean suggestEnhance) {
        if (null == options) return;
        //we should consider both feature option and caller's suggestion
        if (supportPictureQualityEnhance && suggestEnhance) {
            options.inPostProc = true;
        } else {
            options.inPostProc = false;
        }
    }

    public static void enablePictureQualityEnhance(Params params,
                           boolean suggestEnhance) {
        if (null == params) return;
        //we should consider both feature option and caller's suggestion
        if (supportPictureQualityEnhance && suggestEnhance) {
            params.inPQEnhance = true;
        } else {
            params.inPQEnhance = false;
        }
    }

    // M: picture quality enhancement end

    public static int allStereoSubType() {
        return 
            MediaObject.SUBTYPE_MPO_3D |
            MediaObject.SUBTYPE_MPO_3D_PAN |
            MediaObject.SUBTYPE_STEREO_JPS |
            MediaObject.SUBTYPE_STEREO_VIDEO;
    }
    
    // M: for enabling video player list
    public static final String EXTRA_ENABLE_VIDEO_LIST =
                            "mediatek.intent.extra.ENABLE_VIDEO_LIST";

    public static boolean isOutOfLimitation(String mimeType, int width, int height) {
        if(mimeType.equals("image/jpeg")
                && (width > JPEG_LENGTH_MAX || height > JPEG_LENGTH_MAX)) {
            return true;
        }
        return false;
    }
    // M: add for pick=>crop image flow change to fix MTK UX issue:
    // we do not finish AlbumPage (Gallery) anymore when cropping
    // picked image, thus gives user a chance to back to AlbumPage
    // to change picked image
    public static final boolean MTK_CHANGE_PICK_CROP_FLOW = true;
    
    // the size is used for qhd mav(960 x 540)
    public static final long MEMORY_THRESHOLD_MAV_L1 = 100 * 1024 * 1024;
    public static final long MEMORY_THRESHOLD_MAV_L2 = 50 * 1024 * 1024;
    
    /**
     * Is there enough memory to support mav playback
     * @param mActivity
     * @return
     */
    public static long availableMemoryForMavPlayback(AbstractGalleryActivity mActivity) {
        ActivityManager am = (ActivityManager)(((Activity)mActivity).getSystemService(Context.ACTIVITY_SERVICE));
        android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long availableMemory = mi.availMem;
        MtkLog.d(TAG, "current available memory: " + availableMemory);
        return availableMemory;
    }
}
