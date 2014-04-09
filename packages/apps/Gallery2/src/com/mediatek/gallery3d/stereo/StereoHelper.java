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
package com.mediatek.gallery3d.stereo;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;
import android.widget.ShareActionProvider;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore;
import android.database.Cursor;
import android.util.Log;

import com.android.gallery3d.R;
import com.android.gallery3d.app.Gallery;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.Texture;
import com.android.gallery3d.ui.ResourceTexture;
import com.android.gallery3d.util.InterruptableOutputStream;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.android.gallery3d.util.ThreadPool.CancelListener;
import com.android.gallery3d.data.DecodeUtils;
import com.android.gallery3d.data.LocalAlbum;
import com.android.gallery3d.data.LocalMergeAlbum;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;

import com.mediatek.mpo.MpoDecoder;

import com.mediatek.gallery3d.data.DecodeHelper;
import com.mediatek.gallery3d.mpo.MpoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;
import com.mediatek.storage.StorageManagerEx;

//this class is add purely to support Mediatek Stereo Photo display feature.
//At present, there may be three kinds of stereo photo: MPO, JPS and PNS.
//For mpo image, different frames are stored in a series of JPEG sequence.
//For jps & pns, right and left eye frames are stored as right part and left
//part of a single frame.
//MPO images are roughly handled in MpoHelper.java and MpoDecoder.java.
//This class is mainly aimed for JPS & PNS: decode left and right eye frame,
//data base query clause, and so on

public class StereoHelper {
	
    private static final String TAG = "Gallery2/StereoHelper";

    public static final String JPS_EXTENSION = "jps";

    public static final String PNS_MIME_TYPE = "image/pns";
    public static final String JPS_MIME_TYPE = "image/x-jps";
    //this mime type is mainly to support un-common mime-types imposed by
    //other source
    public static final String JPS_MIME_TYPE2 = "image/jps";

    public static final int STEREO_INDEX_NONE = 0;
    public static final int STEREO_INDEX_FIRST = 1;
    public static final int STEREO_INDEX_SECOND = 2;

    public static final int MIN_STEREO_INPUT_WIDTH = 40;
    public static final int MIN_STEREO_INPUT_HEIGHT = 60;

    public static final int STEREO_LAYOUT_NONE = 0;
    public static final int STEREO_LAYOUT_FULL_FRAME = 1 << 0; 
    public static final int STEREO_LAYOUT_LEFT_AND_RIGHT = 1 << 1;
    public static final int STEREO_LAYOUT_TOP_AND_BOTTOM = 1 << 2;

    //invalid bucket is regarded as that bucket not within images
    //table. There are three candidate:
    //1, special value of String.hashCode(), we choose 0
    //2, hashCode of DCIM/.thumbnails 
    //3, hashCode of sdcard/.ConvertedTo2D
    //for simplicity, we choose 0 as invalide bucket id
    public static final int INVALID_BUCKET_ID = 
                                 StereoConvertor.INVALID_BUCKET_ID;
    public static final String INVALID_LOCAL_PATH_END =
                                 StereoConvertor.INVALID_LOCAL_PATH_END;

    //stereo signature in Bundle
    public static final String STEREO_EXTRA = "onlyStereoMedia";
    public static final String INCLUDED_STEREO_IMAGE = "includedSteroImage";
    public static final String KEY_GET_NO_STEREO_IMAGE = "get_no_stereo_image";
    public static final String ATTACH_WITHOUT_CONVERSION = "attachWithoutConversion";

    private static final boolean mIsMpoSupported = 
                                          MediatekFeature.isMpoSupported();
    private static final boolean mIsStereoDisplaySupported = 
                                          MediatekFeature.isStereoDisplaySupported();

    public static Texture sStereoIcon;

//    public static void initialize(Context context) {
//        sStereoIcon = new ResourceTexture(context, 
//                                 R.drawable.ic_stereo_overlay);
//    }

    public static boolean isStereoMediaFolder(MediaSet set) {
        if (null == set || !mIsStereoDisplaySupported) return false;
        //if bucket id is 0, we believe that this is the
        //Stereo Media folder, so be very careful when this logic changes
        Path path = set.getPath();
        if ((set instanceof LocalAlbum || set instanceof LocalMergeAlbum) && 
            null != path && path.toString().endsWith(INVALID_LOCAL_PATH_END)) {
            return true;
        } else {
            return false;
        }
    }

    private static Drawable sStereoOverlay = null;

    public static void drawImageTypeOverlay(Context context, Bitmap bitmap) {
        if (null == sStereoOverlay) {
            sStereoOverlay = context.getResources().getDrawable(R.drawable.ic_stereo_overlay);
        }
        int width = sStereoOverlay.getIntrinsicWidth();
        int height = sStereoOverlay.getIntrinsicHeight();
        Log.d(TAG, "original stereo overlay w=" + width + ", h=" + height);
        float aspectRatio = (float) width / (float) height;
        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();
        boolean heightSmaller = (bmpHeight < bmpWidth);
        int scaleResult = (heightSmaller ? bmpHeight : bmpWidth) / 5;
        if (heightSmaller) {
            height = scaleResult;
            width = (int)((float) scaleResult * aspectRatio);
        } else {
            width = scaleResult;
            height = (int)((float) width / aspectRatio);
        }
        Log.d(TAG, "scaled stereo overlay w=" + width + ", h=" + height);
        // 2 pixels' padding for both left and bottom
        int left = 2;   
        int bottom = bmpHeight - 2;
        int top = bottom - height;
        int right = width + left;
        Log.d(TAG, "stereo overlay drawing dimension=(" + left + ", " + top + ", " + right + ", " + bottom + ")");
        sStereoOverlay.setBounds(left, top, right, bottom);
        Canvas tmpCanvas = new Canvas(bitmap);
        sStereoOverlay.draw(tmpCanvas);
    }

    public static Texture getOverlay(int subType) {
        if (0 != (subType & MediaObject.SUBTYPE_MPO_3D) ||
            0 != (subType & MediaObject.SUBTYPE_STEREO_JPS)) {
            if (sStereoIcon == null) {
                sStereoIcon = new ResourceTexture(MediatekFeature.sContext, 
                        R.drawable.ic_stereo_overlay);
            }
            return sStereoIcon;
        } else {
            return null;
        }
    }

    public static void renderSubTypeOverlay(GLCanvas canvas,
                           int x, int y, int width, int height, int subType) {
        Texture overlay = getOverlay(subType);
        if (null == overlay) return;
        drawLeftBottom(canvas, overlay, x, y, width, height);
    }

    public static void drawLeftBottom(GLCanvas canvas, Texture tex, 
                                 int x, int y, int width, int height) {
        if (null == tex) return;
        int texWidth = tex.getWidth();
        int texHeight = tex.getHeight();
        tex.draw(canvas, x, y + height - texHeight, 
                 texWidth, texHeight);
    }

    public static boolean isStereoImage(MediaItem item) {
        if (null == item) return false;
        int support = item.getSupportedOperations();
        if ((support & MediaObject.SUPPORT_STEREO_DISPLAY) != 0 &&
            (support & MediaObject.SUPPORT_CONVERT_TO_3D) == 0 &&
            MediaObject.MEDIA_TYPE_IMAGE == item.getMediaType()) {
            return true;
        } else {
            return false;
        }
    }

    public static int convertToLocalLayout(int MediaStoreLayout) {
        switch (MediaStoreLayout) {
            case MediaStore.Video.Media.STEREO_TYPE_2D: 
                return STEREO_LAYOUT_NONE;
            case MediaStore.Video.Media.STEREO_TYPE_FRAME_SEQUENCE: 
                return STEREO_LAYOUT_FULL_FRAME; 
            case MediaStore.Video.Media.STEREO_TYPE_SIDE_BY_SIDE: 
                return STEREO_LAYOUT_LEFT_AND_RIGHT;
            case MediaStore.Video.Media.STEREO_TYPE_TOP_BOTTOM: 
                return STEREO_LAYOUT_TOP_AND_BOTTOM;
        }
        return STEREO_LAYOUT_NONE;
    }

    public static int getInclusionFromData(Bundle data) {
        //Log.v(TAG,"getInclusionFromData(data="+data+")");
        if (!mIsStereoDisplaySupported || null == data) {
            if (mIsMpoSupported) {
                return MediatekFeature.INCLUDE_MPO_MAV;
            }
            return 0;
        }
        int stereoInclusion = MediatekFeature.INCLUDE_STEREO_JPS;
        // stereoInclusion |= MediatekFeature.INCLUDE_STEREO_PNS;
        stereoInclusion |= MediatekFeature.INCLUDE_STEREO_VIDEO;
        boolean onlyStereoMedia = data.getBoolean(STEREO_EXTRA, false);
        boolean getAlbum = data.getBoolean(Gallery.KEY_GET_ALBUM, false);
        if (mIsMpoSupported) {
            if (onlyStereoMedia) {
                stereoInclusion |= MediatekFeature.INCLUDE_MPO_3D;
                stereoInclusion |= MediatekFeature.INCLUDE_MPO_3D_PAN;
                //exclude normal images/videos
                stereoInclusion |= MediatekFeature.EXCLUDE_DEFAULT_MEDIA;
            } else {
                stereoInclusion |= MediatekFeature.ALL_MPO_MEDIA;
            }
        }
        //if (!getAlbum && !onlyStereoMedia) {
        if (!onlyStereoMedia) {
            //Log.e(TAG,"getInclusionFromData:create virtual folder...");
            //create a virtual folder for stereo feature
            stereoInclusion |= MediatekFeature.INCLUDE_STEREO_FOLDER;
        }
        return stereoInclusion;
    }

    public static void makeShareProviderIgnorAction(Intent intent) {
        if (null == intent) return;
        Log.v(TAG, "make share provider ignor action!");
        intent.putExtra(
            ShareActionProvider.SHARE_TARGET_SELECTION_IGNORE_ACTION, true);
    }

    public static String getWhereClause(int mtkInclusion) {
        //Log.i(TAG,"getWhereClause(mtkInclusion="+mtkInclusion+")");
        String mpoWhereClause = null;//MpoHelper.getWhereClause(mtkInclusion);
        String whereClause = null;

        if ((mtkInclusion & MediatekFeature.EXCLUDE_DEFAULT_MEDIA) != 0) {
            //add mpo inclusion
            if (null != mpoWhereClause) {
                //Log.v(TAG,"getWhereClause:add where clause add mpo");
                whereClause = (null == whereClause) ? 
                          mpoWhereClause : whereClause + " OR " + mpoWhereClause;
            }
            if ((mtkInclusion & MediatekFeature.INCLUDE_STEREO_VIDEO) != 0) {
                //Log.v(TAG,"getWhereClause:add where clause add stereo video");
                whereClause = (null == whereClause) ? 
                          "("+MediaStore.Video.Media.STEREO_TYPE + "!=" + 
                          MediaStore.Video.Media.STEREO_TYPE_2D + " AND " +
                          MediaStore.Video.Media.STEREO_TYPE + " IS NOT NULL)" :
                          whereClause + " OR " + 
                          "("+MediaStore.Video.Media.STEREO_TYPE + "!=" + 
                          MediaStore.Video.Media.STEREO_TYPE_2D + " AND " +
                          MediaStore.Video.Media.STEREO_TYPE + " IS NOT NULL)";
            }
        } else {
            if (null != mpoWhereClause) {
                //Log.v(TAG,"getWhereClaus2:add where clause remove mpo");
                whereClause = (null == whereClause) ? 
                          mpoWhereClause : whereClause + " AND " + mpoWhereClause;
            }
            if ((mtkInclusion & MediatekFeature.INCLUDE_STEREO_VIDEO) == 0) {
                //Log.v(TAG,"getWhereClause:add where clause add stereo video");
                whereClause = (null == whereClause) ? 
                          "("+MediaStore.Video.Media.STEREO_TYPE + "=" + 
                          MediaStore.Video.Media.STEREO_TYPE_2D + " OR " +
                          MediaStore.Video.Media.STEREO_TYPE + " IS NULL)":
                          whereClause + " AND " + 
                          "("+MediaStore.Video.Media.STEREO_TYPE + "=" + 
                          MediaStore.Video.Media.STEREO_TYPE_2D + " OR " +
                          MediaStore.Video.Media.STEREO_TYPE + " IS NULL)";
            }
        }

        //String directPath = StorageManagerEx.getDefaultPath() +
        //                    StereoConvertor.STEREO_CONVERTED_TO_2D_FOLDER2;
        //String dirWhereClause = ImageColumns.BUCKET_ID + " != " + 
        //                        directPath.toLowerCase().hashCode();
        whereClause = (null == whereClause) ?
                      StereoConvertor.getHideFolderWhereClause() :
                      "(" + whereClause +  ") AND " + 
                      StereoConvertor.getHideFolderWhereClause();
        //Log.v(TAG,"getWhereClause:whereClause = "+whereClause);
        return whereClause;
    }

    public static String getWhereClause(int mtkInclusion, boolean queryVideo) {
        //Log.i(TAG,"getWhereClause(mtkInclusion="+mtkInclusion+",queryVideo="+queryVideo+")");
        String mpoWhereClause = MpoHelper.getWhereClause(mtkInclusion);
        String whereClause = null;

        if ((mtkInclusion & MediatekFeature.EXCLUDE_DEFAULT_MEDIA) != 0) {
            if (!queryVideo) {
                //add mpo inclusion
                if (null != mpoWhereClause) {
                    //Log.v(TAG,"getWhereClause:add where clause add mpo");
                    whereClause = (null == whereClause) ? 
                          mpoWhereClause : whereClause + " OR " + mpoWhereClause;
                }
                if ((mtkInclusion & MediatekFeature.INCLUDE_STEREO_JPS) != 0) {
                    //Log.v(TAG,"getWhereClause:add where clause add jps");
                    whereClause = (null == whereClause) ? 
                          FileColumns.MIME_TYPE + "='" + JPS_MIME_TYPE + "'" :
                          whereClause + " OR " + 
                          FileColumns.MIME_TYPE + "='" + JPS_MIME_TYPE + "'";
                }
            } else {
                if ((mtkInclusion & MediatekFeature.INCLUDE_STEREO_VIDEO) != 0) {
                    //Log.v(TAG,"getWhereClause:add where clause add stereo video");
                    whereClause = (null == whereClause) ? 
                          "("+MediaStore.Video.Media.STEREO_TYPE + "!=" + 
                          MediaStore.Video.Media.STEREO_TYPE_2D + " AND " +
                          MediaStore.Video.Media.STEREO_TYPE + " IS NOT NULL)" :
                          whereClause + " OR " + 
                          "("+MediaStore.Video.Media.STEREO_TYPE + "!=" + 
                          MediaStore.Video.Media.STEREO_TYPE_2D + " AND " +
                          MediaStore.Video.Media.STEREO_TYPE + " IS NOT NULL)";
                }
            }
        } else {
            if (!queryVideo) {
                if (null != mpoWhereClause) {
                    //Log.v(TAG,"getWhereClaus2:add where clause remove mpo");
                    whereClause = (null == whereClause) ? 
                          mpoWhereClause : whereClause + " AND " + mpoWhereClause;
                }
                if ((mtkInclusion & MediatekFeature.INCLUDE_STEREO_JPS) == 0) {
                    //Log.v(TAG,"getWhereClause2:add where clause remove jps");
                    whereClause = (null == whereClause) ? 
                          FileColumns.MIME_TYPE + "!='" + JPS_MIME_TYPE + "'" :
                          whereClause + " AND " + 
                          FileColumns.MIME_TYPE + "!='" + JPS_MIME_TYPE + "'";
                }
            } else {
                if ((mtkInclusion & MediatekFeature.INCLUDE_STEREO_VIDEO) != 0) {
                    //Log.v(TAG,"getWhereClause:add where clause add stereo video");
                    whereClause = (null == whereClause) ? 
                          "("+MediaStore.Video.Media.STEREO_TYPE + "=" + 
                          MediaStore.Video.Media.STEREO_TYPE_2D + " OR " +
                          MediaStore.Video.Media.STEREO_TYPE + " IS NULL)":
                          whereClause + " AND " + 
                          "("+MediaStore.Video.Media.STEREO_TYPE + "=" + 
                          MediaStore.Video.Media.STEREO_TYPE_2D + " OR " +
                          MediaStore.Video.Media.STEREO_TYPE + " IS NULL)";
                }
            }
        }

        //String directPath = StorageManagerEx.getDefaultPath() + 
        //                    StereoConvertor.STEREO_CONVERTED_TO_2D_FOLDER2;
        //String dirWhereClause = ImageColumns.BUCKET_ID + " != " + 
        //                        directPath.toLowerCase().hashCode();
        whereClause = (null == whereClause) ? 
                      StereoConvertor.getHideFolderWhereClause() :
                      "(" + whereClause +  ") AND " + 
                      StereoConvertor.getHideFolderWhereClause();
        //Log.d(TAG,"getWhereClause:whereClause = "+whereClause);
        return whereClause;
    }

    public static int getMpoFrameIndex(boolean leftFrame, int frameCount, boolean isMav) {
        int frameIndex = 0;
        if(isMav) { // if mpo type is frame, return middle index
            frameIndex = frameCount / 2;
            if(!leftFrame) {
                frameIndex++;
                if(frameIndex >= frameCount) {
                    frameIndex = frameCount - 1;
                }
            }
        } else {
            frameIndex = leftFrame ? 0 : 1;
            if (!leftFrame && 4 == frameCount) {
                //sepecial workaround for sony 3d panorama, because it
                //contains 4 image, first two are left eyed, the last
                //two are right eyed.
                frameIndex = 2;
            }
        }
        return frameIndex;
    }

    public static DataBundle
        generateSecondImage(JobContext jc, Bitmap bitmap, Params params,
                            boolean recyleBitmap) {
        if (null == bitmap) return null;

        if (MIN_STEREO_INPUT_WIDTH > bitmap.getWidth() ||
            MIN_STEREO_INPUT_HEIGHT > bitmap.getHeight()) {
            Log.i(TAG,"generateSecondImage:image dimension too small");
            return null;
        }

        int originBitmapW = bitmap.getWidth();
        int originBitmapH = bitmap.getHeight();

        //be careful if one dimension is not even.
        int increaseX = originBitmapW % 2;
        int increaseY = originBitmapH % 2;

        Bitmap input = null;
        if (increaseX + increaseY > 0) {
            Log.d(TAG, "generateSecondImage:resize before convert");
            input = Bitmap.createBitmap(originBitmapW + increaseX, 
                                        originBitmapH + increaseY,
                                        Bitmap.Config.ARGB_8888);
            //draw original bitmap to input Bitmap
            Canvas tempCanvas = new Canvas(input);
            Rect src = new Rect(0, 0, originBitmapW, originBitmapH);
            RectF dst = new RectF(0, 0, originBitmapW, originBitmapH);
            tempCanvas.drawBitmap(bitmap, src, dst, null);
        } else {
            input = bitmap;
        }

        //To save memory usage,recycel bitmap if a new bitmap is created
        if (recyleBitmap && bitmap != input) {
            bitmap.recycle();
        }

        //dumpBitmap(input,"mnt/sdcard2/DCIM/Input["
                //+ android.os.SystemClock.uptimeMillis() + "].png");
        Bitmap stereo = StereoConvertor.convert2Dto3D(input);
        //Bitmap stereo = StereoConvertor.fake2dto3d(input, true);//stub
        //DecodeHelper.dumpBitmap(stereo);

        if (recyleBitmap) {
            input.recycle();
        }

        if (null == stereo) return null;

        if (jc.isCancelled()) return null;

        DataBundle dataBundle =retrieveDataBundle(jc, stereo, params,
                         originBitmapW, originBitmapH, increaseX, increaseY);
        stereo.recycle();
        return dataBundle;
    }

    private static DataBundle
        retrieveDataBundle(JobContext jc, Bitmap stereo, Params params,
            int originBitmapW, int originBitmapH, int increaseX, int increaseY) {
        if (null == stereo || null == params) {
            Log.w(TAG, "retrieveDataBundle: got null stereo or params");
            return null;
        }

        DataBundle dataBundle = new DataBundle();

        //crop left part of the image to create a new Bitmap
        Bitmap firstFrame = retrieveStereoImage(stereo, true,
                        originBitmapW, originBitmapH, increaseX, increaseY);

        if (jc.isCancelled()) return null;

        if (params.inFirstFrame) {
            dataBundle.firstFrame = firstFrame;
        } else if (params.inFirstFullFrame) {
            //compress the first frame to buffer
            dataBundle.firstFullFrame =
                DecodeHelper.getRegionDecoder(jc, firstFrame, true);
        }

        if (jc.isCancelled()) {
            dataBundle.recycle();
            return null;
        }

        //crop left part of the image to create a new Bitmap
        Bitmap secondFrame = retrieveStereoImage(stereo, false,
                        originBitmapW, originBitmapH, increaseX, increaseY);
        if (params.inSecondFrame) {
            dataBundle.secondFrame = secondFrame;
        } else if (params.inSecondFullFrame) {
            //compress the first frame to buffer
            dataBundle.secondFullFrame =
                DecodeHelper.getRegionDecoder(jc, secondFrame, true);
        }

        if (jc.isCancelled()) {
            dataBundle.recycle();
            return null;
        }

        return dataBundle;
    }

    private static Bitmap retrieveStereoImage(Bitmap stereo, boolean first,
            int originBitmapW, int originBitmapH, int increaseX, int increaseY) {
        Bitmap temp = Bitmap.createBitmap(originBitmapW, 
                                          originBitmapH,
                                          Bitmap.Config.ARGB_8888);

        //draw first/second screen nail onto it
        Canvas canvas = new Canvas(temp);
        int x = first ?
                0 : stereo.getWidth()/2;
        int right = first ?
                stereo.getWidth()/2 : stereo.getWidth();
        Rect src = new Rect(x, 0, right - increaseX, 
                            stereo.getHeight() - increaseY);
        RectF dst = new RectF(0, 0, originBitmapW, originBitmapH);
        canvas.drawBitmap(stereo, src, dst, null);
        return temp;
    }

    //Stretch half of original video frame as a whole video frame
    public static Bitmap getStereoVideoImage(JobContext jc, Bitmap originFrame, 
                                        boolean firstFrame, int mediaStoreLayout) {
        if (null == originFrame || originFrame.getWidth() <= 0 ||
            originFrame.getHeight() <= 0) {
            Log.e(TAG, "getStereoVideoImage:got invalid original frame");
            return null;
        }
        int localLayout = convertToLocalLayout(mediaStoreLayout);
        //Log.i(TAG,"getStereoVideoImage:localLayout="+localLayout);

        if (STEREO_LAYOUT_NONE == localLayout ||
            STEREO_LAYOUT_FULL_FRAME == localLayout && !firstFrame) {
            Log.e(TAG, "getStereoVideoImage:can not retrieve second image!");
            return null;
        }

        boolean isLeftRight = (STEREO_LAYOUT_LEFT_AND_RIGHT == localLayout);
        Rect src = new Rect(0, 0, originFrame.getWidth(), originFrame.getHeight());
        //Log.v(TAG,"getStereoVideoImage:src="+src);
        //create a new bitmap that has the same dimension as the original video
        Bitmap bitmap = Bitmap.createBitmap(src.right - src.left, 
                                           src.bottom - src.top,
                                           Bitmap.Config.ARGB_8888);
        adjustRect(isLeftRight, firstFrame, src);
        //Log.d(TAG,"getStereoVideoImage:src="+src);
        Canvas canvas = new Canvas(bitmap);
        RectF dst = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        canvas.drawBitmap(originFrame, src, dst, null);

        if (null != bitmap) {
            Log.i(TAG,"getStereoVideoImage:["+bitmap.getWidth()+"x"+bitmap.getHeight()+"]");
        }
        return bitmap;
    }

    private static void adjustRect(boolean isLeftRight,boolean firstFrame, 
                                  Rect imageRect) {
        //Log.i(TAG,"adjustRect:got imageRect: "+imageRect);
        if (null == imageRect) {
            Log.e(TAG,"adjustRect:got null image rect");
            return;
        }
        if (isLeftRight) {
            if (firstFrame) {
                imageRect.set(imageRect.left, imageRect.top,
                              (imageRect.left + imageRect.right) / 2, 
                              imageRect.bottom);
            } else {
                imageRect.set((imageRect.left + imageRect.right) / 2, 
                              imageRect.top, imageRect.right, imageRect.bottom);
            }
        } else {
            if (firstFrame) {
                imageRect.set(imageRect.left, imageRect.top,
                              imageRect.right, 
                              (imageRect.top + imageRect.bottom) / 2);
            } else {
                imageRect.set(imageRect.left, 
                              (imageRect.top + imageRect.bottom) / 2, 
                              imageRect.right, imageRect.bottom);
            }
        }
        //Log.d(TAG,"adjustRect:adjusted imageRect: "+imageRect);
    }

    public static int adjustDim(boolean dimX, int layout, int length) {
        if (dimX) {
            //for dimension x, we have to check left-and-right layout
            if (STEREO_LAYOUT_LEFT_AND_RIGHT == layout) {
                return length / 2;
            } else {
                return length;
            }
        } else {
            if (STEREO_LAYOUT_TOP_AND_BOTTOM == layout) {
                return length / 2;
            } else {
                return length;
            }
        }
    }
}
