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
package com.mediatek.gallery3d.mpo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.mediatek.common.mpodecoder.IMpoDecoder;
import com.mediatek.common.MediatekClassFactory;

import com.android.gallery3d.R;

import com.android.gallery3d.app.Gallery;
import com.android.gallery3d.ui.Texture;
import com.android.gallery3d.ui.ResourceTexture;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.mediatek.gallery3d.util.MediatekFeature;

public class MpoHelper {
	
    private static final String TAG = "Gallery2/MpoHelper";

    public static final String MPO_EXTENSION = "mpo";

    public static final String FILE_EXTENSION = "mpo";
    public static final String MIME_TYPE = "image/mpo";
    public static final String MPO_MIME_TYPE = "image/mpo";//this variable will be deleted later
    public static final String MPO_VIEW_ACTION = "com.mediatek.action.VIEW_MPO";

    private static Drawable sMavOverlay = null;

    public static IMpoDecoder createMpoDecoder(JobContext jc, String filePath) {
        try {
            Log.i(TAG,"createMpoDecoder:filepath:"+filePath);
            if (null == filePath) return null;
            return MediatekClassFactory.createInstance(IMpoDecoder.class,
                                IMpoDecoder.DECODE_FILE, filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static IMpoDecoder createMpoDecoder(JobContext jc, ContentResolver cr,
                                  Uri uri) {
        try {
            Log.i(TAG,"createMpoDecoder:uri:" + uri);
            if (null == cr || null == uri) return null;
            return MediatekClassFactory.createInstance(IMpoDecoder.class,
                                IMpoDecoder.DECODE_URI, cr, uri);
        } catch (Exception e)  {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String getMpoWhereClause(boolean showAllMpo) {
        String mpoFilter = null;
        if (!showAllMpo) {
            mpoFilter = FileColumns.MIME_TYPE + "!='" + MPO_MIME_TYPE + "'";
        }
        return mpoFilter;
    }

    public static String getWhereClause(int mtkInclusion) {
        if ((MediatekFeature.ALL_MPO_MEDIA & mtkInclusion) == 0) {
            return null;
        }
        String whereClause = null;
        String whereClauseEx = FileColumns.MIME_TYPE + "='" + 
                               MpoHelper.MPO_MIME_TYPE + "'";
        String whereClauseIn = FileColumns.MIME_TYPE + "='" + 
                               MpoHelper.MPO_MIME_TYPE + "'";
        String subWhereClause = null;

        if ((mtkInclusion & MediatekFeature.EXCLUDE_DEFAULT_MEDIA) != 0) {
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_MAV) != 0) {
                //Log.v(TAG,"getWhereClause:add where clause add mav");
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_MAV:
                          subWhereClause + " OR " + 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_MAV;
            }
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_3D) != 0) {
                //Log.v(TAG,"getWhereClause:add where clause add mpo 3d");
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_Stereo:
                          subWhereClause + " OR " + 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_Stereo;
            }
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_3D_PAN) != 0) {
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_3DPan:
                          subWhereClause + " OR " + 
                          Images.Media.MPO_TYPE + "=" + IMpoDecoder.MTK_TYPE_3DPan;
            }

            if (null != subWhereClause) {
                whereClause = whereClauseEx + " AND ( " + subWhereClause + " )";
            } //else {
                //whereClause = whereClauseEx + " AND ( " + subWhereClause + " )";
            //}
        } else {
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_MAV) == 0) {
                //Log.v(TAG,"getWhereClause2:add where clause remove mav");
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_MAV:
                          subWhereClause + " AND " + 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_MAV;
            }
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_3D) == 0) {
                //Log.v(TAG,"getWhereClause2:add where clause remove mpo 3d");
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_Stereo:
                          subWhereClause + " AND " + 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_Stereo;
            }
            if ((mtkInclusion & MediatekFeature.INCLUDE_MPO_3D_PAN) == 0) {
                subWhereClause = (null == subWhereClause) ? 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_3DPan:
                          subWhereClause + " AND " + 
                          Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_3DPan;
            }

            if (null != subWhereClause) {
                whereClause = whereClauseEx + " AND ( " + subWhereClause + " )";
            } else {
                whereClause = null;
            }

        }

        //if (null == subWhereClause) {
        //    Log.e(TAG,"getWhereClause:why got null subWhereClause?");
        //} else {
        //    whereClause = whereClause + " AND (" + subWhereClause + ")";
        //}
        //Log.i(TAG,"getWhereClause:whereClause="+whereClause);
        return whereClause;
    }
    
    public static void playMpo(Activity activity, Uri uri) {
        try {
            Intent i = new Intent(MPO_VIEW_ACTION);
            i.setDataAndType(uri, MPO_MIME_TYPE);
            activity.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Unable to open mpo file: ", e);
        }
    }

    public static void drawImageTypeOverlay(Context context, Bitmap bitmap) {
        if (null == sMavOverlay) {
            sMavOverlay = context.getResources().getDrawable(R.drawable.ic_mav_overlay);
        }
        int width = sMavOverlay.getIntrinsicWidth();
        int height = sMavOverlay.getIntrinsicHeight();
        float aspectRatio = (float) width / (float) height;
        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();
        boolean heightSmaller = (bmpHeight < bmpWidth);
        int scaleResult = (heightSmaller ? bmpHeight : bmpWidth) / 5;
        if (heightSmaller) {
            height = scaleResult;
            width = (int)(scaleResult * aspectRatio);
        } else {
            width = scaleResult;
            height = (int)(width / aspectRatio);
        }
        int left = (bmpWidth - width) / 2;
        int top = (bmpHeight - height) / 2;
        sMavOverlay.setBounds(left, top, left + width, top + height);
        Canvas tmpCanvas = new Canvas(bitmap);
        sMavOverlay.draw(tmpCanvas);
    }
    
    public static int getInclusionFromData(Bundle data) {
        return MediatekFeature.EXCLUDE_MPO_MAV;
    }
    
    public static String getMavWhereClause(int mavInclusion) {
        String whereClause = null;
        if ((mavInclusion & MediatekFeature.EXCLUDE_MPO_MAV) !=0) {
            whereClause =  Images.Media.MPO_TYPE + "!=" + IMpoDecoder.MTK_TYPE_MAV;
        }
        return whereClause;
    }
}
