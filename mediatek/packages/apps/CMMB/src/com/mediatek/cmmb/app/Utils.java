/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.cmmb.app;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.mbbms.MBBMSStore;
import com.mediatek.mbbms.ServerStatus;
import com.mediatek.mbbms.service.MBBMSService;

import java.text.DateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Set;

public class Utils {
    private static final String TAG = "Utils";
    private static final boolean LOG = true;
    /**
     * Extra name for passing service row id. <br/>
     * Type: long
     */
    public static final String EXTRA_SERVICE_ROW_ID = "ServiceRowID";
    /**
     * Extra name for passing service id. <br/>
     * Type: String
     */
    public static final String EXTRA_SERVICE_ID = "ServiceID";
    /**
     * Extra name for passing content id. <br/>
     * Type: String
     */
    public static final String EXTRA_CONTENT_ID = "ContentID";
    /**
     * Extra name for passing interactivity data id. <br/>
     * Type: String
     */
    public static final String EXTRA_INTERACTIVITY_DATA_ID = "InteractivityDataID";
    /**
     * Row id of interactivity data. <br/>
     * Type: long
     */
    public static final String EXTRA_INTERACTIVITY_DATA_ROW_ID = "InteractivityDataRowID";
    /**
     * Interactivity from SG or not <br/>
     * Type: boolean
     */
    public static final String EXTRA_INTERACTIVITY_FROM_SG = "FromSG";
    /**
     * IMD group id <br/>
     * Type: string
     */
    public static final String EXTRA_INTERACTIVITY_GROUP_ID = "GroupID";
    /**
     * IMD group position <br/>
     * Type: int
     */
    public static final String EXTRA_INTERACTIVITY_GROUP_POSITION = "GroupPosition";

    /**
     * Extra name for passing service name. <br/>
     * Type: String
     */
    public static final String EXTRA_SERVICE_NAME = "ServiceName";
    /**
     * Extra name for passing service frequency no. <br/>
     * Type: int
     */
    public static final String EXTRA_FREQUENCY_NO = "FrequencyNO";
    /**
     * Extra name for passing current service is favorited or not. <br/>
     * Type: int
     */
    public static final String EXTRA_IS_FAVORITE = "IsFavorite";
    /**
     * Extra name for passing current service is for free or not. <br/>
     * Type: int
     */
    public static final String EXTRA_FOR_FREE = "ForFree";
    /**
     * Extra name for half screen, used by player. <br/>
     * Type: boolean
     */
    public static final String EXTRA_HALF_SCREEN = "HalfScreen";
    /**
     * Int type
     */
    public static final String EXTRA_LOCATION_MODE = "mode";
    public static final int LOCATION_MODE_NORMAL = 0;
    public static final int LOCATION_MODE_UNKNOWN = 1;
    public static final String LOCATION_MIME_TYPE = "location/mediatek-cmmb";
    /**
     * String type
     */
    public static final String RESULT_EXTRA_LOCATION_SUGGESTION = "suggestion";
    /**
     * String type
     */
    public static final String RESULT_EXTRA_LOCATION_PROVINCE = "province";
    /**
     * String type
     */
    public static final String RESULT_EXTRA_LOCATION_CITY = "city";
    /**
     * String type
     */
    public static final String RESULT_EXTRA_LOCATION_COUNTY = "county";
    /**
     * Boolean type
     */
    public static final String RESULT_EXTRA_LOCATION_PERSIST = "persist";

    /**
     * Parent activity requests to finish him.
     */
    public static final int REQUEST_CODE_FOR_FINISHED = 0;

    private Utils() {
        // no instance
    }

    /* package */static class CachedPreview extends Handler {
        private final HashMap<Integer, MyDrawable> mCachedPreview = new HashMap<Integer, MyDrawable>();
        private Context mContext;
        private ContentResolver mCr;
        private BitmapDrawable mDefaultDrawable;
        private boolean mIsSGMode;
        private Uri mUri;
        private int mUsage;
        private boolean mMounted;
        private int mAllPreviewStatus = TYPE_NEED_LOAD;
        private Boolean mRegisted = false;
        private MyContentObserver mContentObserver;
        private MyBroadcastReceiver mSdCardReceiver;
        private int mIconWidth;
        private int mIconHeight;
        private DrawableStateListener mListener;

        // asyc request media for fast the calling thread
        private Handler mTaskHandler;
        private static Looper sLooper;
        // priority queue for async request
        private static final PriorityQueue<TaskParams> TASK_QUEUE = new PriorityQueue<TaskParams>(10, TaskParams
                .getComparator());
        private static final int TASK_REQUEST_DONE = 1;
        private static final int TASK_REQUEST_NEW = 2;

        public CachedPreview(Context context, BitmapDrawable defaultDrawable, boolean isSGMode, int usage,
                DrawableStateListener listener) {
            mContext = context;
            mCr = mContext.getContentResolver();
            mDefaultDrawable = defaultDrawable;
            Bitmap icon = mDefaultDrawable.getBitmap();
            mIconWidth = icon.getWidth();
            mIconHeight = icon.getHeight();
            mUsage = usage;
            mIsSGMode = isSGMode;
            if (mIsSGMode) {
                mUri = MBBMSStore.SG.PreviewData.CONTENT_URI;
            } else {
                mUri = MBBMSStore.ESG.Media.CONTENT_URI;
            }
            mListener = listener;
            if (LOG) {
                Log.v(TAG, "CachedPreview(" + isSGMode + ", " + usage + ")");
            }
        }

        private void registerObserver() {
            // Here I register the ContentObserver and BroadcasetReceiver.
            // These can check the database's update and sdcard's state.
            // So, when there's no sdcard or database has no preview infos,
            // getCachedPreview() will be faster.
            synchronized (mRegisted) {
                if (!mRegisted) {
                    mContentObserver = new MyContentObserver(null);
                    mSdCardReceiver = new MyBroadcastReceiver();

                    mCr.registerContentObserver(mUri, true, mContentObserver);

                    IntentFilter filter1 = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
                    filter1.addDataScheme("file");
                    mContext.registerReceiver(mSdCardReceiver, filter1);

                    IntentFilter filter2 = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
                    filter2.addDataScheme("file");
                    mContext.registerReceiver(mSdCardReceiver, filter2);
                    mRegisted = true;

                    mMounted = isExternalStorageReady();
                    if (LOG) {
                        Log.v(TAG, "registerObserver() regist observers! mMounted=" + mMounted);
                    }
                }
            }
            if (LOG) {
                Log.v(TAG, "registerObserver() mRegisted=" + mRegisted + ", mMounted=" + mMounted);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            if (LOG) {
                Log.v(TAG, "mCallingHandler.handleMessage(" + msg + ")");
            }
            if (msg.what == TASK_REQUEST_DONE && (msg.obj instanceof TaskParams)) {
                TaskParams result = (TaskParams) msg.obj;
                if (result.listener != null) {
                    result.listener.onChanged(result.rowId, result.drawable);
                }
            }
        }

        private boolean mInitTask;
        private long mPrioritySeed;
        private TaskParams mCurrentRequest;

        private void initTask() {
            if (LOG) {
                Log.v(TAG, "initTask() initTask=" + mInitTask + ", prioritySeed=" + mPrioritySeed);
            }
            if (mInitTask) {
                return;
            }
            mPrioritySeed = 0;
            synchronized (CachedPreview.class) {
                if (sLooper == null) {
                    HandlerThread t = new HandlerThread("cached-preview-thread",
                            android.os.Process.THREAD_PRIORITY_BACKGROUND);
                    t.start();
                    sLooper = t.getLooper();
                }
            }
            mTaskHandler = new Handler(sLooper) {
                @Override
                public void handleMessage(Message msg) {
                    if (LOG) {
                        Log.v(TAG, "mTaskHandler.handleMessage(" + msg + ")");
                    }
                    if (msg.what == TASK_REQUEST_NEW) {
                        synchronized (TASK_QUEUE) {
                            mCurrentRequest = TASK_QUEUE.poll();
                        }
                        if (mCurrentRequest == null) {
                            Log.w(TAG, "wrong request, has request but no task params.");
                            return;
                        }
                        // recheck the drawable is exists or not.
                        int contentRowID = mCurrentRequest.rowId;
                        MyDrawable cachedDrawable = null;
                        synchronized (mCachedPreview) {
                            cachedDrawable = mCachedPreview.get(contentRowID);
                        }
                        if (cachedDrawable == null) {
                            Log.w(TAG, "cached drawable was delete. may for clear.");
                            return;
                        }
                        // when sdcard exists, load or reload the preview
                        if (mMounted && cachedDrawable.type == TYPE_NEED_LOAD) {
                            Bitmap tempBitmap = getPreview(contentRowID);
                            if (tempBitmap != null) {
                                tempBitmap = Bitmap.createScaledBitmap(tempBitmap, mIconWidth, mIconHeight, true);
                                cachedDrawable.set(new FastBitmapDrawable(tempBitmap), TYPE_LOADED_HAS_PREVIEW);
                            } else {
                                cachedDrawable.set(null, TYPE_LOADED_NO_PREVIEW);
                            }
                        }
                        mCurrentRequest.drawable = cachedDrawable;
                        Message done = CachedPreview.this.obtainMessage(TASK_REQUEST_DONE);
                        done.obj = mCurrentRequest;
                        done.sendToTarget();
                        if (LOG) {
                            Log.v(TAG, "mTaskHandler.handleMessage() send done. " + mCurrentRequest);
                        }
                    }
                }
            };
            mInitTask = true;
        }

        private void clearTask() {
            if (LOG) {
                Log.v(TAG, "clearTask() initTask=" + mInitTask);
            }
            if (mInitTask) {
                mPrioritySeed = 0;
                removeMessages(TASK_REQUEST_DONE);
                synchronized (TASK_QUEUE) {
                    TASK_QUEUE.clear();
                }
                mTaskHandler.removeMessages(TASK_REQUEST_NEW);
                mTaskHandler = null;
            }
            mInitTask = false;
        }

        public Drawable getCachedPreview(int contentRowID) {
            if (LOG) {
                Log.v(TAG, "getCachedPreview(" + contentRowID + ") mPreviewStatus=" + mAllPreviewStatus);
            }
            initTask();
            registerObserver();
            if (mAllPreviewStatus == TYPE_NEED_LOAD) {
                loadPreviewStatus();
            }
            if (mAllPreviewStatus == TYPE_LOADED_NO_PREVIEW) {
                return mDefaultDrawable;
            }
            //
            MyDrawable cachedDrawable = null;
            synchronized (mCachedPreview) {
                cachedDrawable = mCachedPreview.get(contentRowID);
            }
            // when sdcard exists, load or reload the preview
            if (mMounted && (cachedDrawable == null || cachedDrawable.type == TYPE_NEED_LOAD)) {
                // add priority
                mPrioritySeed++;
                synchronized (TASK_QUEUE) {
                    // check is processing or not
                    // current request is not in queue.
                    boolean isProcessing = false;
                    if (mCurrentRequest != null) {
                        synchronized (mCurrentRequest) {
                            if (mCurrentRequest.rowId == contentRowID) {
                                isProcessing = true;
                            }
                        }
                    }
                    if (LOG) {
                        Log.v(TAG, "getCachedPreview() isProcessing=" + isProcessing);
                    }
                    if (!isProcessing) {
                        // check is in request queue or not.
                        TaskParams oldRequest = null;
                        for (TaskParams one : TASK_QUEUE) {
                            if (one.rowId == contentRowID) {
                                oldRequest = one;
                                break;
                            }
                        }
                        if (LOG) {
                            Log.i(TAG, "getCachedPreview() oldRequest=" + oldRequest);
                        }
                        if (oldRequest == null) {
                            // not in cache and not in request
                            MyDrawable temp = new MyDrawable(null, TYPE_NEED_LOAD);
                            synchronized (mCachedPreview) {
                                cachedDrawable = mCachedPreview.get(contentRowID);
                                if (cachedDrawable == null) {
                                    mCachedPreview.put(contentRowID, temp);
                                }
                                cachedDrawable = temp;
                            }
                            TaskParams task = new TaskParams(contentRowID, mListener, -mPrioritySeed);
                            TASK_QUEUE.add(task);
                            mTaskHandler.sendEmptyMessage(TASK_REQUEST_NEW);
                        } else {
                            // not in cache, but in request, just update priority
                            oldRequest.priority = -mPrioritySeed;
                            if (TASK_QUEUE.remove(oldRequest)) {
                                TASK_QUEUE.add(oldRequest);// re-order the queue
                            }
                        }
                    }
                }
                if (LOG) {
                    Log.v(TAG, "getCachedPreview() async load the drawable for " + contentRowID);
                }
            }
            Drawable result = null;
            if (cachedDrawable == null || cachedDrawable.type != TYPE_LOADED_HAS_PREVIEW) {
                result = mDefaultDrawable;
            } else {
                result = cachedDrawable.drawable;
            }
            if (LOG) {
                Log.v(TAG, "getCachedPreview() mPreviewStatus=" + mAllPreviewStatus + ", cachedDrawable=" + cachedDrawable
                        + ", return " + result);
            }
            return result;
        }

        private Bitmap getPreview(int contentRowID) {
            Bitmap bitmap = null;
            if (mIsSGMode) {
                bitmap = MBBMSStore.SG.ContentPreviewData.getPreviewBitmap(mCr, contentRowID, mUsage, null);
            } else {
                bitmap = MBBMSStore.ESG.ContentMedia.getMediaBitmap(mCr, contentRowID, mUsage, null);
            }
            if (LOG) {
                Log.v(TAG, "getPreview() bitmap=" + bitmap);
            }
            return bitmap;
        }

        public void clearCachedPreview() {
            synchronized (mRegisted) {
                if (mRegisted) {
                    mCr.unregisterContentObserver(mContentObserver);
                    mContentObserver = null;
                    mContext.unregisterReceiver(mSdCardReceiver);
                    mSdCardReceiver = null;
                    mAllPreviewStatus = TYPE_NEED_LOAD;
                    mRegisted = false;
                }
            }
            synchronized (mCachedPreview) {
                mCachedPreview.clear();
            }
            clearTask();
        }

        private void loadPreviewStatus() {
            Cursor cursor = mCr.query(mUri, new String[] {
                        "count(*)"
                }, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                if (count > 0) {
                    mAllPreviewStatus = TYPE_LOADED_HAS_PREVIEW;
                } else {
                    mAllPreviewStatus = TYPE_LOADED_NO_PREVIEW;
                }
            }
            if (cursor != null) {
                cursor.close();
            }

            if (LOG) {
                Log.v(TAG, "loadPreviewStatus() mPreviewStatus=" + mAllPreviewStatus);
            }
        }

        private class MyContentObserver extends ContentObserver {

            public MyContentObserver(Handler handler) {
                super(handler);
            }

            @Override
            public void onChange(boolean selfChange) {
                if (LOG) {
                    Log.v(TAG, "mContentObserver.onChange(" + selfChange + ")");
                }
                if (mAllPreviewStatus != TYPE_NEED_LOAD) {
                    // we do not delete drawable until we refresh the whole SG
                    // or ESG info
                    // which is be operated in MainScreen Activity.
                    // So here do not need to consider delete case.
                    Set<Integer> keys = mCachedPreview.keySet();
                    for (Integer key : keys) {
                        MyDrawable drawable = mCachedPreview.get(key);
                        if (drawable.type == TYPE_LOADED_NO_PREVIEW) {
                            drawable.type = TYPE_NEED_LOAD;
                        }
                    }
                    mAllPreviewStatus = TYPE_NEED_LOAD;
                }
            }

        }

        private class MyBroadcastReceiver extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String path = intent.getData().getPath();
                // only observe sdcard with default path: "/mnt/sdcard"
                String externalPath = Environment.getExternalStorageDirectory().getPath();
                if (Intent.ACTION_MEDIA_EJECT.equals(action) && externalPath.equalsIgnoreCase(path)) {
                    mMounted = false;
                } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action) && externalPath.equalsIgnoreCase(path)) {
                    mMounted = true;
                }
                if (LOG) {
                    Log.v(TAG, "onReceive(" + intent + ") mMounted=" + mMounted);
                }
            }

        };

        /* package */static final int TYPE_NEED_LOAD = 0;
        /* package */static final int TYPE_LOADED_NO_PREVIEW = 1;
        /* package */static final int TYPE_LOADED_HAS_PREVIEW = 2;

        /* package */class MyDrawable {
            public int type;
            public Drawable drawable;

            public MyDrawable(Drawable idrawable, int itype) {
                type = itype;
                drawable = idrawable;
            }

            public void set(Drawable idrawable, int itype) {
                type = itype;
                drawable = idrawable;
            }

            @Override
            public String toString() {
                return new StringBuilder().append("MyDrawable(type=").append(type).append(", drawable=").append(drawable)
                        .append(")").toString();
            }

        }

        public interface DrawableStateListener {
            void onChanged(int rowId, MyDrawable drawable);
        }

        private static class TaskParams {
            public int rowId;
            public MyDrawable drawable;
            public DrawableStateListener listener;
            public long priority;

            public TaskParams(int rId, DrawableStateListener lisr, long pri) {
                this.rowId = rId;
                this.listener = lisr;
                this.priority = pri;
            }

            @Override
            public String toString() {
                return new StringBuilder().append("TaskInput(rowId=").append(rowId).append(", listener=").append(listener)
                        .append(", drawable=").append(drawable).append(")").toString();
            }

            static Comparator<TaskParams> getComparator() {
                return new Comparator<TaskParams>() {

                    public int compare(TaskParams r1, TaskParams r2) {
                        if (r1.priority != r2.priority) {
                            return (r1.priority < r2.priority) ? -1 : 1;
                        }
                        return 0;
                    }

                };
            }
        }

    }

    // copied from com.android.music.MusicUtils.java
    // A really simple BitmapDrawable-like class, that doesn't do
    // scaling, dithering or filtering.
    /* package */static class FastBitmapDrawable extends Drawable {
        private Bitmap mBitmap;

        public FastBitmapDrawable(Bitmap b) {
            mBitmap = b;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }
    }

    /* package */static boolean isExternalStorageReady() {
        String state = Environment.getExternalStorageState();
        boolean isReady = (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
        if (LOG) {
            Log.v(TAG, "external storage state = " + state);
            Log.v(TAG, "external storage is ready = " + String.valueOf(isReady));
        }
        return isReady;
    }

    // need recheck
    public static String getCurrentLang() {
        return "zho";
    }

    public static void showToast(Context context, int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    public static String getEBLevelString(Context context, int level) {
        if (LOG) {
            Log.v(TAG, "getEBLevelString(" + level + ")");
        }
        return String.format(context.getResources().getString(R.string.ebm_level), level);
    }

    public static int getEBLevelIcon(int level) {
        if (LOG) {
            Log.v(TAG, "getEBLevelIcon(" + level + ")");
        }
        switch (level) {
            case 1:
                return R.drawable.ebm_level1;
            case 2:
                return R.drawable.ebm_level2;
            case 3:
                return R.drawable.ebm_level3;
            case 4:
                return R.drawable.ebm_level4;
            default:
                return R.drawable.ebm_level4;
        }
    }

    // should add time
    public static String getFormatDate(Context context, long time) {
        Date mDate = new Date();
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        mDate.setTime(time);
        return dateFormat.format(mDate);
    }

    public static String getFormatTime(Context context, long time) {
        Date mDate = new Date();
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        mDate.setTime(time);
        return timeFormat.format(mDate);
    }

    public static String getFormatDateTime(Context context, long time) {
        Date mDate = new Date();
        mDate.setTime(time);
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        String strDate = dateFormat.format(mDate);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        String strTime = timeFormat.format(mDate);
        return strDate + " " + strTime;
    }

    // don't cancel notification
    public static void readBroadcast(Context context, Uri uri) {
        if (LOG) {
            Log.v(TAG, "readBroadcast(" + uri + ")");
        }
        ContentValues values = new ContentValues();
        values.put(MBBMSStore.EB.Broadcast.HAS_READ, 1);
        context.getContentResolver().update(uri, values, null, null);
    }

    public static long getNow() {
        return System.currentTimeMillis();
    }

    public static boolean checkLocalTime() {
        return true;
    }

    // Here use static variable to transfer all purchase items
    // including not local purchase items.
    private static Cursor sSearchCursor;

    public static Cursor getSearchCursor() {
        if (LOG) {
            Log.i(TAG, "getSearchCursor() return " + sSearchCursor);
        }
        return sSearchCursor;
    }

    public static void setSearchCursor(Cursor cursor) {
        if (LOG) {
            Log.i(TAG, "setSearchCursor(" + cursor + ")");
        }
        sSearchCursor = cursor;
    }

    private static HashMap<String, String> sErrorDescriptions;

    public static String getErrorDescription(Resources res, ServerStatus status, String defaultDescription) {
        if (LOG) {
            Log.v(TAG, "parseStatus(" + status + ", " + defaultDescription + ")");
        }
        if (status == null) {
            // always wrong
            return defaultDescription;
        }
        // if server description is not null, return server description.
        if (status.description != null) {
            return status.description;
        } else if (isSuccess(status)) {
            // if success, not return client error
            return defaultDescription;
        }
        if (sErrorDescriptions == null) {
            String[] descriptions = res.getStringArray(R.array.mbbms_description_text);
            String[] desIds = res.getStringArray(R.array.mbbms_description_id);
            if (descriptions == null || desIds == null) {
                return defaultDescription;
            }
            int len = descriptions.length < desIds.length ? descriptions.length : desIds.length;
            sErrorDescriptions = new HashMap<String, String>(len);
            for (int i = 0; i < len; i++) {
                sErrorDescriptions.put(desIds[i], descriptions[i]);
            }
        }
        String client = sErrorDescriptions.get(status.code);
        if (client == null) {
            return defaultDescription;
        } else {
            return client;
        }
    }

    public static boolean isSuccess(ServerStatus status) {
        if (status == null) {
            return false;
        }
        return MBBMSService.STATUS_SUCCEED.equalsIgnoreCase(status.code);
    }

    public static SavedLocation parseSavedLocation(String saved) {
        if (LOG) {
            Log.v(TAG, "parseSavedLocation(" + saved + ")");
        }
        if (saved != null && !saved.trim().equals("")) {
            String[] temp = saved.split("-");
            if (temp != null) {
                SavedLocation location = new SavedLocation();
                for (int i = 0, len = temp.length; i < len; i++) {
                    if (i == 0) {
                        location.province = temp[i];
                    } else if (i == 1) {
                        location.city = temp[i];
                    } else if (i == 2) {
                        location.county = temp[i];
                    }
                }
                return location;
            }
        }
        return null;
    }

    public static class SavedLocation {
        /**
         * Hold for unknown province. if user select suggestion, then province will be this value.
         */
        public static final String SUGGESTION_HOLDER = "|";
        public String province;
        public String city;
        public String county;

        /**
         * Get the readable diaplay name for final user.
         * 
         * @param savedLocation
         * @return
         */
        public static String getDisplayName(String savedLocation) {
            String name = null;
            SavedLocation saved = Utils.parseSavedLocation(savedLocation);
            if (saved != null && SavedLocation.SUGGESTION_HOLDER.equals(saved.province)) {
                name = saved.city;
            } else {
                name = savedLocation;
            }
            return name;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("SavedLocation(province=").append(province).append(", city=").append(city)
                    .append(", county=").append(county).append(")").toString();
        }
    }
}
