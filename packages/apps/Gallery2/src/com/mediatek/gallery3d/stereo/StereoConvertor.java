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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContentValues;
import android.net.Uri;
import android.os.storage.StorageManager;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.database.Cursor;

import android.util.Log;

import com.mediatek.stereo3d.Stereo3DConversion;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.InterruptableOutputStream;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.android.gallery3d.util.ThreadPool.CancelListener;

import com.mediatek.gallery3d.data.DecodeHelper;
import com.mediatek.gallery3d.data.RequestHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;
import com.mediatek.storage.StorageManagerEx;
public class StereoConvertor {
	
    private static final String TAG = "Gallery2/StereoConvertor";

    public static final String STEREO_CONVERTED_TO_2D_FOLDER = "/.ConvertedTo2D/";
    public static final String STEREO_CONVERTED_TO_2D_FOLDER2 = "/.ConvertedTo2D";

    //invalid bucket is regarded as that bucket not within images
    //table. There are three candidate:
    //1, special value of String.hashCode(), we choose 0
    //2, hashCode of DCIM/.thumbnails 
    //3, hashCode of sdcard/.ConvertedTo2D
    //for simplicity, we choose 0 as invalide bucket id
    public static final int INVALID_BUCKET_ID = 0;
    public static final String INVALID_LOCAL_PATH_END = "/0";


    private static final boolean mConvertToJpeg = true;
    private static final String JPG_POST_FIX = ".jpg";

    private static long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000; 
    private static long INVALID_TIME_BEFORE = ONE_HOUR_IN_MILLIS * 24;//one day
    private static long INVALID_TIME_AFTER = 1000;//one second

    // Must preserve order between these indices and the order of the terms in
    // the following PROJECTION array.
    private static final int INDEX_ID = 0;
    private static final int INDEX_CAPTION = 1;
    private static final int INDEX_DISPLAY_NAME = 2;
    private static final int INDEX_MIME_TYPE = 3;
    private static final int INDEX_DATE_TAKEN = 4;
    private static final int INDEX_DATE_ADDED = 5;
    private static final int INDEX_DATE_MODIFIED = 6;
    private static final int INDEX_DATA = 7;
    private static final int INDEX_BUCKET_ID = 8;
    private static final int INDEX_SIZE_ID = 9;
    private static final int INDEX_WIDTH = 10;
    private static final int INDEX_HEIGHT = 11;

    static final String[] PROJECTION =  {
            ImageColumns._ID,           // 0
            ImageColumns.TITLE,         // 1
            Images.Media.DISPLAY_NAME,  // 2
            ImageColumns.MIME_TYPE,     // 3
            ImageColumns.DATE_TAKEN,    // 4
            ImageColumns.DATE_ADDED,    // 5
            ImageColumns.DATE_MODIFIED, // 6
            ImageColumns.DATA,          // 7
            ImageColumns.BUCKET_ID,     // 8
            ImageColumns.SIZE,          // 9
            // These should be changed to proper names after they are made public.
            "width", // ImageColumns.WIDTH,         // 10
            "height", // ImageColumns.HEIGHT         // 11
    };

    private static final String whereClause = 
                               Images.Media.DATA + " in (?,?) AND " + 
                               ImageColumns.DATE_MODIFIED + " between ? AND ?";

    private static final String whereClauseId = ImageColumns._ID + "=?";
    private static final String whereClausePath = ImageColumns.DATA + "=?";


    ////////////////////////////////////////////////////////////////////////////
    //  Convert 2D image to 3D image feature
    ////////////////////////////////////////////////////////////////////////////

    public static Bitmap convert2Dto3D(Bitmap bitmap) {
        if (null == bitmap) return null;
        try {
            return Stereo3DConversion.execute(bitmap);
        } catch (ExceptionInInitializerError e) {
            Log.e(TAG, "faile to convert be cause we got Exception:" + e);
            e.printStackTrace();
            return null;
        }
    }

    //this function is used to for migration as a stub
    public static Bitmap fake2dto3d(Bitmap input, boolean drawColor) {
        DecodeHelper.showBitmapInfo(input);
        //stub for 2d to 3d 
        Bitmap stereo = Bitmap.createBitmap(input.getWidth()*2,
                            input.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(stereo);
        Rect src = new Rect(0, 0, input.getWidth(), input.getHeight());
        RectF dst = new RectF(0, 0, stereo.getWidth()/2,stereo.getHeight());
        canvas.drawBitmap(input, src, dst, null);
        if (drawColor) {
            //draw green
            Bitmap tempOverlay;
            int colorint = android.graphics.Color.argb(100, 0, 255, 0);
            int[] colorArray = new int[4]; 
            for (int i = 0; i < 4; ++i) {
                colorArray[i] = colorint;
            }
            tempOverlay = Bitmap.createBitmap(colorArray, 2, 2, Bitmap.Config.ARGB_8888);
            src = new Rect(0, 0, tempOverlay.getWidth(), tempOverlay.getHeight());
            canvas.drawBitmap(tempOverlay, src, dst, null);
            tempOverlay.recycle();
        }

        src = new Rect(0, 0, input.getWidth(), input.getHeight());
        dst = new RectF(stereo.getWidth()/2, 0, stereo.getWidth(),stereo.getHeight());
        canvas.drawBitmap(input, src, dst, null);
        if (drawColor) {
            //draw red
            Bitmap tempOverlay;
            int colorint = android.graphics.Color.argb(100, 255, 0, 0);
            int[] colorArray = new int[4]; 
            for (int i = 0; i < 4; ++i) {
                colorArray[i] = colorint;
            }
            tempOverlay = Bitmap.createBitmap(colorArray, 2, 2, Bitmap.Config.ARGB_8888);
            src = new Rect(0, 0, tempOverlay.getWidth(), tempOverlay.getHeight());
            canvas.drawBitmap(tempOverlay, src, dst, null); 
            tempOverlay.recycle();
        }
        return stereo;
    }

    ////////////////////////////////////////////////////////////////////////////
    //  Convert 3D image to 2D image feature
    ////////////////////////////////////////////////////////////////////////////

    // Convert 3D to 2D feature is mainly for sharing stereo image. Of course other
    // purpose such as Bluetooth print and edit can also take advantage of this 
    // function.
    // In order to maximize other app's effort for sharing 3D image as 2D or 3D, we
    // integrate the converting function in Gallery. This can transparently do
    // converting for other app.

    // Convert 3D image to 2D image consists the following procedure
    // 1, decode the left eye image into Bitmap
    // 2, compress the Bitmap to SD card
    // 3, insert a record into database.
    // There first two procedures are easy and straight forward, The third procedure
    // is tricky, and has to use some strategy to make sure it work well

    // As we want to make convertion transparently for other app, we have to pass
    // other app the same kind of uri: context://media from MediaProvider.
    // And, we don't want media scanner scan the file into database automatically.
    // Never do we want user to see the converted temp 2D files in Gallery, because
    // it may make the user confused where these files came from!

    // The above requests make us to consider a update-delete strategy to maintain
    // the temp 2D files.
    // The strategy is: we only perceive the temp files that converted within 1 day
    // as valid temp file; files created before 1 day are perceived as invalid files
    // When a file is reused, its last modified time is updated to current time
    // Each time we convert, we wil first find all the files that can be reused
    // and update their last modified time, and then delete all the files that are
    // 1 day before or in the future.

    // -----before----->][<----24 hours---->][<-----future------>
    //                  ^                   ^   
    //                  1 day before        now

    // Note: some files may not exist in database base, for example, user reseted
    // device which will erase databse, but scanner will never scanned the converted
    // files to database. So we will first delete the files in database, then delete
    // remaining files on SD card.
    // Note: we also have to take care of multi-storage suitation, the converted file
    // will be stored to currently default directory.

    //we use java.lang.System.currentTimeMillis() to retrieve current time stamp.
    // 1 millisecond is 1/1000 second. and is 1/1000/60/60 hour
    // 1 hour is 60*60*1000 millisecond.

    //init for where clause
    private static String HIDE_FOLDER_WHERE_CLAUSE = null;
    private static boolean sIsInitialized = false;

    private static void initializeHiddenFolders() {
        if (null == MediatekFeature.sContext || sIsInitialized) {
            Log.e(TAG,"initialize: invalid context");
            return;
        }
        StorageManager storageManager = 
            (StorageManager) MediatekFeature.sContext.getSystemService(Context.STORAGE_SERVICE);
        Log.d(TAG,"initialize:StorageManagerEx.getDefaultPath()="+StorageManagerEx.getDefaultPath());
        ArrayList<String> hidePaths = new ArrayList<String>();
        String[] volums = storageManager.getVolumePaths();
        for (String volum : volums) {
            //load all files under
            Log.v(TAG,"initialize:volum="+volum);
            hidePaths.add(volum + STEREO_CONVERTED_TO_2D_FOLDER2);
        }
        HIDE_FOLDER_WHERE_CLAUSE = getBucketIdNotIn(hidePaths);
        sIsInitialized = true;
    }
    
    public static String getHideFolderWhereClause() {
        if (!sIsInitialized) {
            initializeHiddenFolders();
        }
        //Log.d(TAG, "getHideFolderWhereClause: " + HIDE_FOLDER_WHERE_CLAUSE);
        return HIDE_FOLDER_WHERE_CLAUSE;
    }

    private static String getBucketIdNotIn(ArrayList<String> hidePaths) {
        if (null == hidePaths || 0 == hidePaths.size()) return null;
        String whereClause = null;
        String whereClauseWithin = null;
        String hidePathId = null;
        for (String hidePath : hidePaths) {
            //load all files under
            if (null == hidePath) {
                Log.e(TAG, "getBucketIdNotIn:why we got null hidePath!");
                continue;
            }
            hidePathId = String.valueOf(hidePath.toLowerCase().hashCode());
            if (null == whereClauseWithin) {
                whereClauseWithin = hidePathId;
            } else {
                whereClauseWithin = whereClauseWithin + "," + hidePathId;
            }
        }
        whereClause = ImageColumns.BUCKET_ID + " NOT IN ("+whereClauseWithin+")";
        Log.i(TAG,"getBucketIdNotIn:whereClause="+whereClause);
        return whereClause;
    }

    private static class FileInfo {
        private File mFile;

        public FileInfo(File file) {
            if (file == null){
                Log.e(TAG, "FileInfo:got null file");
                return;
            }
            Log.v(TAG, "FileInfo:got file " + file);
            mFile = file;
        }

        public boolean equalByName(String fileName) {
            if (null == mFile || null == fileName) {
                Log.w(TAG, "FileInfo: equalByName: invalid params");
                return false;
            }
            return fileName.equals(mFile.getName());
        }

        public long lastModified() {
            if (null == mFile) {
                Log.w(TAG, "FileInfo: lastModified: null mFile!");
                return 0;
            }
            return mFile.lastModified();
        }

        public boolean setLastModified(long time) {
            if (null == mFile) {
                Log.w(TAG, "FileInfo: setLastModified: null mFile!");
                return false;
            }
            return mFile.setLastModified(time);
        }

        public String getPath() {
            if (null == mFile) {
                Log.w(TAG, "FileInfo: getPath: null mFile!");
                return null;
            }
            return mFile.getPath();
        }

        public String getName() {
            if (null == mFile) {
                Log.w(TAG, "FileInfo: getName: null mFile!");
                return null;
            }
            return mFile.getName();
        }

        public File getFile() {
            return mFile;
        }
    }

    private static boolean inInvalidTimeRange(FileInfo fileInfo,
                                              long triggeredTime) {
        if (null == fileInfo) {
            Log.e(TAG, "inInvalidTimeRange:got null fileInfo!");
            return false;
        }
        long lastModified = fileInfo.lastModified();
        if (lastModified < triggeredTime - INVALID_TIME_BEFORE ||
            lastModified > triggeredTime + INVALID_TIME_AFTER) {
            return true;
        } else {
            return false;
        }
    }

    private static void deleteRecord(Context context, String filePath) {
        if (null == context || null == filePath) {
            Log.e(TAG, "deleteRecord:got null param");
            return;
        }
        ContentResolver cr = context.getContentResolver();
        int count = cr.delete(Images.Media.EXTERNAL_CONTENT_URI, 
                              whereClausePath, new String[]{filePath});
        Log.d(TAG, "deleteRecord: "+count+" record delete for "+filePath);
    }

    private static void deleteLocalFile(String filePath) {
        if (null == filePath) {
            Log.e(TAG, "deleteLocalFile:got null filePath");
            return;
        }
        File file = new File(filePath);
        if (null != file && file.exists()) {
            Log.w(TAG, "deleteLocalFile: delete "+file+" returns "+file.delete());
        }
    }

    private static void deleteRecordAndFile(Context context, FileInfo fileInfo) {
        if (null == context || null == fileInfo) {
            Log.i(TAG, "deleteRecordAndFile()");
            return;
        }
        String filePath = fileInfo.getPath();
        //when we delete a file, we first delete the record in database,
        //and local file may be deleted automatically
        deleteRecord(context, filePath);
        //there is a chance that the file is not recorded in database or
        //the file fails to be delete by database, so we have to check and
        //delete local file if needed.
        deleteLocalFile(filePath);
    }

    private static void triggerClearInvalidCache(JobContext jc, Context context, 
                            ArrayList<FileInfo> fileInfoList, long triggeredTime) {
        Log.i(TAG, "triggerClearInvalidCache()");
        if (null == fileInfoList) return;
        for (FileInfo fileInfo : fileInfoList) {
            if (inInvalidTimeRange(fileInfo, triggeredTime)) {
                if (jc != null && jc.isCancelled()) return;
                Log.i(TAG,"triggerClearInvalidCache: clear file "+fileInfo.getPath());
                //delete record in database and delete local file if needed
                deleteRecordAndFile(context, fileInfo);
            }
        }
    }

    private static FileInfo findFile(ArrayList<FileInfo> fileInfoList,
                                     String fileName) {
        if (null == fileInfoList || null == fileName) return null;
        Log.v(TAG,"findFile("+fileName+") //fileInfoList.size()="+fileInfoList.size());
        FileInfo targetFileInfo = null;
        for (FileInfo fileInfo : fileInfoList) {
            boolean equal = fileInfo.equalByName(fileName);
            if (equal) {
                targetFileInfo = fileInfo;
                Log.d(TAG, "findFile: found file " + fileName);
                break;
            }
        }
        return targetFileInfo;
    }

    private static void loadFileInfoList(
                            ArrayList<FileInfo> fileInfoList, File[] files) {
        if (null == fileInfoList || null == files) return;
        Log.i(TAG,"loadFileInfoList() //files.length = "+files.length);
        // load the file info first.
        for (File file : files) {
            FileInfo fileInfo = new FileInfo(file);
            fileInfoList.add(fileInfo);
        }
    }

    private static void loadSubFiles(ArrayList<FileInfo> fileInfoList, String volum) {
        if (null == fileInfoList || null == volum) return;
        Log.i(TAG,"loadFileInfoList() //volum="+volum);
        String directPath = volum + STEREO_CONVERTED_TO_2D_FOLDER;
        File[] files = null;
        File dir = new File(directPath);
        files = dir.listFiles();
        if (files != null) {
            loadFileInfoList(fileInfoList, files);
        }
    }

    private static void loadAllTempFiles(JobContext jc, Context context, 
                                         ArrayList<FileInfo> fileInfoList) {
        if (null == context || null == fileInfoList) {
            Log.e(TAG,"loadAllTempFiles: invalid params");
            return;
        }
        StorageManager storageManager = (StorageManager) context.getSystemService(
                                                           Context.STORAGE_SERVICE);
        String[] volums = storageManager.getVolumePaths();
        for (String volum : volums) {
            if (null != jc && jc.isCancelled()) return;
            //load all files under
            loadSubFiles(fileInfoList, volum);
        }
    }

    //change original uri into hashcode
    private static String createFileName(Uri origUri) {
        if (null == origUri) return null;

        //for all kinds of uri other than content://media/..., we replace the 
        //original converted image if any and update the data base
        //
        //for content://media/... kind uri, we create a new file the first
        //time we convert, insert a record in media database including 
        //date_modified. next time we want to convert, we can firstly check 
        //if we can find a previously converted image:
        //    If we can, use it directly;
        //    If we can not, convert a image and insert database
        //The criteria we say we find a previously converted image is that:
        //    1,the image title matches the calculated new image title according
        //      to original uri's hashCode.
        //    2,bucket_id matches (saved in special directory)
        //    3,date_modified matches the original image's date_modified
        return String.valueOf(origUri.hashCode()); 
    }

    private static void updateDatabase(JobContext jc, ContentResolver cr, int id,
                                       long triggeredTime) {
        if (jc != null && jc.isCancelled()) return;
        if (null == cr) {
            Log.e(TAG, "updateDatabase:why we got null content resolver");
            return;
        }

        //we update database record that can be defined by id
        ContentValues values = new ContentValues();
        values.put(Images.Media.DATE_TAKEN, triggeredTime);
        values.put(Images.Media.DATE_ADDED, triggeredTime);
        values.put(Images.Media.DATE_MODIFIED, triggeredTime);
        int count = cr.update(Images.Media.EXTERNAL_CONTENT_URI, values,
                        whereClauseId, new String[] {String.valueOf(id)});
        Log.d(TAG, "updateDatabase: " + count + " rows updated for id=" + id);
    }

    private static Cursor queryFilePath(JobContext jc, ContentResolver cr,
                                        FileInfo targetFileInfo) {
        if (jc != null && jc.isCancelled()) return null;
        if (null == cr || null == targetFileInfo) {
            Log.e(TAG, "queryFilePath:why got null params!");
            return null;
        }
        String filePath = targetFileInfo.getPath();
        if (null == filePath) {
            Log.e(TAG, "queryFilePath:why we got null path for "+targetFileInfo);
            return null;
        }
        Cursor targetCursor = cr.query(Images.Media.EXTERNAL_CONTENT_URI, 
                                       PROJECTION, Images.Media.DATA +
                                       "= ?", new String[]{ filePath } ,null);
        if (null != targetCursor && !targetCursor.moveToNext()) {
            Log.w(TAG,"queryFilePath:did not queried any data for "+filePath);
            targetCursor.close();
            targetCursor = null;
        } else {
            Log.i(TAG,"queryFilePath:got record for " + filePath);
        }
        return targetCursor;
    }

    private static Uri updateDatabase(JobContext jc, Context context,
                                  FileInfo targetFileInfo, long triggeredTime) {
        //we query the database with image name as file name
        //if exists, we update date_modified, if not, we insert a new one
        Uri imageUri = null;
        ContentResolver cr = context.getContentResolver();
        Cursor targetCursor = null;
        try {
            targetCursor = queryFilePath(jc, cr, targetFileInfo);
            if (jc != null && jc.isCancelled()) return null;
            if (targetCursor != null) {
                int id = targetCursor.getInt(INDEX_ID);
                updateDatabase(jc, cr, id, triggeredTime);
                //create final image uri
                imageUri = Uri.parse(Images.Media.EXTERNAL_CONTENT_URI + "/" + id);
            } else {
                imageUri = insertLocalImage(jc, cr, targetFileInfo.getFile(), 
                                            triggeredTime);
            }
        } finally {
            if (null != targetCursor) {
                targetCursor.close();
                targetCursor = null;
            }
        }
        return imageUri;
    }

    private static Uri updateExisting(JobContext jc, Context context,
                                     FileInfo targetFileInfo, String fileNameWithExt,
                                     long triggeredTime) {
        //update file last modified time
        boolean suc = targetFileInfo.setLastModified(triggeredTime);
        Log.v(TAG, "updateExisting: setLastModified returns " + suc);
        //update Database
        Uri imageUri = null;
        imageUri = updateDatabase(jc, context, targetFileInfo, triggeredTime);
        return imageUri;
    }

    private static Uri createAndInsert(JobContext jc, Context context,Uri origUri,
                              String mimeType, String fileName, long triggeredTime) {
        if (null == context || null == origUri || null == mimeType ||
            null == fileName) {
            Log.e(TAG, "createAndInsert:got invalid params!");
            return null;
        }
        //we want to save file to the current default path
        String displayName = fileName + JPG_POST_FIX;
        String directPath = StorageManagerEx.getDefaultPath() +
                                STEREO_CONVERTED_TO_2D_FOLDER;
        Log.d(TAG, "createAndInsert:target file path:" + directPath + displayName);
        return saveAndInsertLocalImage(jc, context, origUri, mimeType, 0,
                                       directPath, fileName, displayName);
    }

    private static Uri findOrCreate(JobContext jc, Context context, Uri origUri,
                              String mimeType, String fileName, 
                              ArrayList<FileInfo> fileInfoList, long triggeredTime) {
        if (null == context || null == fileName || null == fileInfoList) {
            Log.e(TAG,"findOrCreate: invalid parameters");
            return null;
        }
        //check if the fileName exists within the fileInfoList
        String fileNameWithExt = fileName + JPG_POST_FIX;
        FileInfo targetFileInfo = findFile(fileInfoList, fileNameWithExt);
        if (jc != null && jc.isCancelled()) return null;

        Uri imageUri = null;
        if (null != targetFileInfo) {
            //we found a existing file, we should update file modified time
            //and update/insert record in database, and return the very Uri
            imageUri = updateExisting(jc, context, targetFileInfo,
                                      fileNameWithExt,triggeredTime);
        } else {
            //we don't find a existing file, we should create a new file and insert
            //a record in database
            imageUri = createAndInsert(jc, context, origUri,
                           mimeType, fileName, triggeredTime);
        }
        return imageUri;
    }

    public static Uri convertSingle(JobContext jc, Context context, Uri origUri,
                                       String mimeType) {
        long triggeredTime = System.currentTimeMillis();
        Log.i(TAG, "convertSingle:triggeredTime="+triggeredTime);

        ArrayList<FileInfo> fileInfoList = new ArrayList<FileInfo>();
        loadAllTempFiles(jc, context, fileInfoList);
        //create target file name
        String fileName = createFileName(origUri);
        Log.d(TAG, "convertSingle:created file name:"+fileName);

        Uri imageUri = findOrCreate(jc, context, origUri, mimeType, fileName, 
                                    fileInfoList, triggeredTime);
        Log.i(TAG, "convertSingle:imageUri="+imageUri);

        //trigger clear cache operation
        triggerClearInvalidCache(jc, context, fileInfoList, triggeredTime);

        return imageUri;
    }

    public static Uri convertSingle(JobContext jc, Context context, MediaItem item) {
        if (null == jc || null == context || null == item) {
            Log.e(TAG,"convertSingle: Got null parameters");
            return null;
        }
        if (jc.isCancelled()) return null;

        return convertSingle(jc, context, item.getContentUri(), item.getMimeType());
    }

    public static ArrayList<Uri> convertMultiple(JobContext jc, 
                                     AbstractGalleryActivity activity, ArrayList<Uri> uris) {
        long triggeredTime = System.currentTimeMillis();
        Log.i(TAG, "convertMultiple:triggeredTime="+triggeredTime);

        ArrayList<FileInfo> fileInfoList = new ArrayList<FileInfo>();
        Context context = (Context)activity;
        loadAllTempFiles(jc, context, fileInfoList);

        DataManager manager = activity.getDataManager();
        Path itemPath = null;
        MediaItem item = null;

        Uri imageUri = null;
        if (jc != null && jc.isCancelled()) return null;
        final ArrayList<Uri> newUris = new ArrayList<Uri>();
        for (Uri uri : uris) {
            itemPath = manager.findPathByUri(uri, null);
            item = (MediaItem) manager.getMediaObject(itemPath);
            if (StereoHelper.isStereoImage(item)) {
                if (jc != null && jc.isCancelled()) return null;
                //create target file name
                String fileName = createFileName(uri);
Log.v(TAG, "convertMultiple:origin uri: "+uri);
                Log.d(TAG, "convertMultiple:created file name:"+fileName);
                imageUri = findOrCreate(jc, context, uri, item.getMimeType(),
                                        fileName, fileInfoList, triggeredTime);
                Log.i(TAG,"convertMultiple:got new Uri="+imageUri);
                if (null == imageUri) {
                    Log.e(TAG,"convertMultiple:convert failed, insert " + uri);
                    imageUri = uri;
                }
                //save new uri to new array list
                newUris.add(imageUri);
            } else {
                newUris.add(uri);
            }
            //check if the job is cancelled
            if (jc.isCancelled()) break;
        }
        if (jc.isCancelled()) return null;

        //after we successfully finished converting (without interrupt),
        //we want to check if there are out dated temp files to delete
        triggerClearInvalidCache(jc, context, fileInfoList, triggeredTime);

        return newUris;
    }

    private static Bitmap getFlattened(JobContext jc, Context context,
                                       Uri origUri, String mimeType) {
        Params params = new Params();
        params.inOriginalFrame = true;
        params.inOriginalTargetSize = 2048;
        DataBundle dataBundle = RequestHelper.requestDataBundle(jc, params,
                                    context, origUri, mimeType, false);
        return dataBundle != null ? dataBundle.originalFrame : null;
    }

    private static Uri saveAndInsertLocalImage(JobContext jc, Context context,
            Uri origUri, String mimeType, int targetSize, String directPath,
            String fileName, String displayName) {
        Uri imageUri = null;
        Bitmap image = getFlattened(jc, context, origUri, mimeType);

        //Log.v(TAG,"convertedTo2D:got image "+image);
if (image!=null) Log.e(TAG,"convertedTo2D:saveAndInsertLocalImage:got ["+image.getWidth()+"x"+image.getHeight()+"]");
        if (null != image) {
            ContentResolver cr = context.getContentResolver();
            File directory = new File(directPath);
            File saved = saveLocalImage(jc, image, directory, displayName);
            if (null != saved) {
                imageUri = insertLocalImage(jc, saved, fileName, cr,
                                            image.getWidth(), image.getHeight());
            } else {
                Log.w(TAG,"convertedTo2D:failed to save local image");
            }
            image.recycle();
        }
        return imageUri;
    }

    private static Uri insertLocalImage(JobContext jc, ContentResolver cr,
                                        File image, long lastModified) {
        lastModified /= 1000;
        ContentValues values = new ContentValues();
        values.put(Images.Media.TITLE, image.getName());
        values.put(Images.Media.DISPLAY_NAME, image.getName());
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.DATA, image.getAbsolutePath());
        values.put(Images.Media.DATE_TAKEN, lastModified);
        values.put(Images.Media.DATE_ADDED, lastModified);
        values.put(Images.Media.DATE_MODIFIED, lastModified);
        values.put(Images.Media.SIZE, image.length());
        Log.i(TAG,"insertLocalImage:values="+values);
        return cr.insert(
                Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private static Uri insertLocalImage(JobContext jc, File image, String fileName,
                                ContentResolver cr, int width, int height) {
        long now = System.currentTimeMillis() / 1000;
        ContentValues values = new ContentValues();
        values.put(Images.Media.TITLE, fileName);
        values.put(Images.Media.DISPLAY_NAME, fileName + JPG_POST_FIX);
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        values.put(Images.Media.DATA, image.getAbsolutePath());
        values.put(Images.Media.DATE_TAKEN, now);
        values.put(Images.Media.DATE_ADDED, now);
        values.put(Images.Media.DATE_MODIFIED, now);
        values.put("width", width);
        values.put("height", height);
        values.put(Images.Media.SIZE, image.length());
        Log.i(TAG,"insertLocalImage:values="+values);
        return cr.insert(
                Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private static boolean saveBitmapToOutputStream(
            JobContext jc, Bitmap bitmap, CompressFormat format, OutputStream os) {
        // We wrap the OutputStream so that it can be interrupted.
        final InterruptableOutputStream ios = new InterruptableOutputStream(os);
        if (null != jc) {
            jc.setCancelListener(new CancelListener() {
                    public void onCancel() {
                        ios.interrupt();
                    }
                });
        }
        try {
            bitmap.compress(format, 100, os);
            return !jc.isCancelled();
        } finally {
            jc.setCancelListener(null);
            Utils.closeSilently(os);
        }
    }

    private static File saveLocalImage(
            JobContext jc, Bitmap bitmap, File directory, String filename) {
        if ((!directory.exists()) && (!directory.mkdir())) {
            Log.e(TAG, "saveLocalImage: create directory file: " + directory.getPath()
                    + " failed");
            return null;
        }

        File candidate = null;
        candidate = new File(directory, filename);
        try {
            boolean created = candidate.createNewFile();
            if (!created) {
                Log.w(TAG,"saveLocalImage: error may happen when create new file");
            }
        } catch (IOException e) {
            Log.e(TAG, "saveLocalImage: fail to create new file: "
                    + candidate.getAbsolutePath(), e);
            return null;
        }

        if (!candidate.exists() || !candidate.isFile()) {
            throw new RuntimeException("cannot create file: " + filename);
        }

        candidate.setReadable(true, false);
        candidate.setWritable(true, false);

        try {
            FileOutputStream fos = new FileOutputStream(candidate);
            try {
                saveBitmapToOutputStream(jc, bitmap, CompressFormat.JPEG, fos);
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "saveLocalImage: fail to save image: "
                    + candidate.getAbsolutePath(), e);
            candidate.delete();
            return null;
        }

        if (null != jc && jc.isCancelled()) {
            candidate.delete();
            return null;
        }

        return candidate;
    }

}
