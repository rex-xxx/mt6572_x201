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

package com.mediatek.videoplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Set;

public class CachedThumbnail {
    private static final String TAG = "CachedThumbnail";
    private static final boolean LOG = false;//for performance
    
    private final HashMap<Long, MyDrawable> mCachedPreview = new HashMap<Long, MyDrawable>();
    private final Context mContext;
    private final ContentResolver mCr;
    
    private final int mDefaultIconWidth;
    private final int mDefaultIconHeight;
    private final Bitmap mDefaultDrawable;
    private final ArrayList<DrawableStateListener> mListeners = new ArrayList<DrawableStateListener>();
    
    //asyc request media for fast the calling thread
    private Handler mTaskHandler;
    private static Looper sLooper;
    //priority queue for async request
    private static final PriorityQueue<TaskParams> TASK_QUEUE =
        new PriorityQueue<TaskParams>(10, TaskParams.getComparator());
    private static final int TASK_REQUEST_DONE = 1;
    private static final int TASK_REQUEST_NEW = 2;
    private static final int TASK_GROUP_ID = 1999;//just a number
    
    private Bitmap mDefaultDrawable3D;
    private final Bitmap mDefaultOverlay3D;
    
    private CachedThumbnail(final Context context, final Bitmap defaultDrawable, final Bitmap icon) {
        mContext = context;
        mCr = mContext.getContentResolver();
        
        mDefaultDrawable = defaultDrawable;
        mDefaultIconWidth = defaultDrawable.getWidth();
        mDefaultIconHeight = defaultDrawable.getHeight();
        
        mDefaultOverlay3D = icon;
        if (LOG) {
            MtkLog.v(TAG, "CachedPreview() mDefaultIconWidth=" + mDefaultIconWidth
                    + ", mDefaultIconHeight=" + mDefaultIconHeight);
        }
    }
    
    private final Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            if (LOG) {
                MtkLog.v(TAG, "mCallingHandler.handleMessage(" + msg + ")");
            } 
            if (msg.what == TASK_REQUEST_DONE && (msg.obj instanceof TaskParams)) {
                final TaskParams task = (TaskParams)msg.obj;
                final MyDrawable drawable = task.mDrawable;
                for (final DrawableStateListener listener : mListeners) {
                    listener.onChanged(task.mRowId, drawable.mType, drawable.mDrawable);
                }
            }
        }
    };
    
    private boolean mInitedTask;
    private long mPrioritySeed;
    private TaskParams mCurrentRequest;
    private void initTask() {
        if (mInitedTask) {
            return;
        }
        if (LOG) {
            MtkLog.v(TAG, "initTask() prioritySeed=" + mPrioritySeed);
        }
        mPrioritySeed = 0;
        synchronized (CachedThumbnail.class) {
            if (sLooper == null) {
                final HandlerThread t = new HandlerThread("cached-thumbnail-thread",
                        android.os.Process.THREAD_PRIORITY_BACKGROUND);
                t.start();
                sLooper = t.getLooper();
            }
        }
        mTaskHandler = new Handler(sLooper) {
            @Override
            public void handleMessage(final Message msg) {
                if (LOG) {
                    MtkLog.v(TAG, "mTaskHandler.handleMessage(" + msg + ") this=" + this);
                } 
                if (msg.what == TASK_REQUEST_NEW) {
                    synchronized (TASK_QUEUE) {
                        mCurrentRequest = TASK_QUEUE.poll();
                    }
                    if (mCurrentRequest == null) {
                        MtkLog.w(TAG, "wrong request, has request but no task params.");
                        return;
                    }
                    final TaskParams task = mCurrentRequest;//currentRequest may be cleared by other thread.
                    //recheck the drawable is exists or not.
                    final long id = task.mRowId;
                    MyDrawable cachedDrawable = null;
                    synchronized (mCachedPreview) {
                        cachedDrawable = mCachedPreview.get(id);
                    }
                    if (cachedDrawable == null) {
                        MtkLog.w(TAG, "cached drawable was delete. may for clear.");
                        return;
                    }
                    //load or reload the preview
                    if (cachedDrawable.mType == TYPE_NEED_LOAD) {
                        Bitmap tempBitmap = getThumbnail(id);
                        if (tempBitmap != null) {
                            tempBitmap = Bitmap.createScaledBitmap(tempBitmap, mDefaultIconWidth, mDefaultIconHeight, true);
                            if (cachedDrawable.mSupport3D) {
                                tempBitmap = overlay3DImpl(tempBitmap);
                            }
                            cachedDrawable.set(tempBitmap, TYPE_LOADED_HAS_PREVIEW);
                        } else {
                            cachedDrawable.set(null, TYPE_LOADED_NO_PREVIEW);
                        }
                    }
                    if (task != mCurrentRequest) {
                        MtkLog.w(TAG, "current request was changed by other thread. task=" + task
                                + ", currentRequest=" + mCurrentRequest);
                        return;
                    }
                    task.mDrawable = cachedDrawable;
                    final Message done = mUiHandler.obtainMessage(TASK_REQUEST_DONE);
                    done.obj = task;
                    done.sendToTarget();
                    if (LOG) {
                        MtkLog.v(TAG, "mTaskHandler.handleMessage() send done. " + mCurrentRequest + " this=" + this);
                    }
                    synchronized (TASK_QUEUE) {
                    	mCurrentRequest = null;
                    }
                }
            }
        };
        mInitedTask = true;
    }
    
    private Bitmap getDefaultBitmap(final boolean support3D) {
        if (MtkUtils.isSupport3d() && support3D) {
            if (mDefaultDrawable3D == null) {
                mDefaultDrawable3D = mDefaultDrawable.copy(Bitmap.Config.ARGB_8888, true);
                mDefaultDrawable3D = overlay3DImpl(mDefaultDrawable3D);
            }
            return mDefaultDrawable3D;
        } else {
            return mDefaultDrawable;
        }
    }
    
    private Bitmap overlay3DImpl(final Bitmap bitmap) {
        final Canvas overlayCanvas = new Canvas(bitmap);
        final int overlayWidth = mDefaultOverlay3D.getWidth();
        final int overlayHeight = mDefaultOverlay3D.getHeight();
        final int left = 0;//bitmap.getWidth() - overlayWidth;
        final int top = bitmap.getHeight() - overlayHeight;
        final Rect newBounds = new Rect(left, top, left + overlayWidth, top + overlayHeight);
        overlayCanvas.drawBitmap(mDefaultOverlay3D, null, newBounds, null);
        return bitmap;
    }
    
    private void clearTask() {
        if (LOG) {
            MtkLog.v(TAG, "clearTask() initTask=" + mInitedTask);
        }
        if (mInitedTask) {
            mPrioritySeed = 0;
            mUiHandler.removeMessages(TASK_REQUEST_DONE);
            synchronized (TASK_QUEUE) {
                TASK_QUEUE.clear();
            }
            mTaskHandler.removeMessages(TASK_REQUEST_NEW);
            mTaskHandler = null;
            mInitedTask = false;
            cancelThumbnail();
        }
        if (sLooper != null) {
            sLooper.quit();
            sLooper = null;
        }
    }
    
    //will be loaded if sdcard is in phone
    public Bitmap getCachedPreview(final long id, final long dateModified,
            final boolean support3D, final boolean request) {
        if (LOG) {
            MtkLog.v(TAG, "getCachedPreview(" + id + ", " + dateModified + ", " + request + ")");
        }
        initTask();
        MyDrawable cachedDrawable = null;
        synchronized (mCachedPreview) {
            cachedDrawable = mCachedPreview.get(id);
        }
        if (request) {
            if (cachedDrawable != null && cachedDrawable.mDateModified != dateModified) {
                //video was updated, reload its thumbnail
                cachedDrawable.mType = TYPE_NEED_LOAD;
            }
            if (cachedDrawable == null || cachedDrawable.mType == TYPE_NEED_LOAD) {
                mPrioritySeed++;
                synchronized (TASK_QUEUE) {
                    //current request is not in queue.
                    //check is processing or not
                    if (!isProcessing(id, dateModified)) {
                        //check whether it is in request queue or not.
                        TaskParams oldRequest = getOldRequest(id);
                        if (oldRequest == null) { //not in cache and not in request
                            cachedDrawable = createNewRequest(id, dateModified, support3D);
                            //check whether to refresh it or not
                            if (cachedDrawable.mType != TYPE_LOADED_HAS_PREVIEW) {
                                final TaskParams task = new TaskParams(id, -mPrioritySeed, cachedDrawable);
                                TASK_QUEUE.add(task);
                                mTaskHandler.sendEmptyMessage(TASK_REQUEST_NEW);
                            }
                        } else { //not in cache, but in request queue
                            //just update priority and dateModified
                            oldRequest.mPriority = -mPrioritySeed;
                            oldRequest.mDrawable.mDateModified = dateModified;
                            if (TASK_QUEUE.remove(oldRequest)) {
                                TASK_QUEUE.add(oldRequest);//re-order the queue
                            }
                        }
                    }
                }
                if (LOG) {
                    MtkLog.v(TAG, "getCachedPreview() async load the drawable for " + id + " size()=" + TASK_QUEUE.size());
                }
            }
        }
        Bitmap result = null;
        if (cachedDrawable == null || cachedDrawable.mDrawable == null) {
            result = getDefaultBitmap(support3D);
        } else {
            result = cachedDrawable.mDrawable;
        }
        if (LOG) {
            MtkLog.v(TAG, "getCachedPreview() cachedDrawable=" + cachedDrawable + ", return " + result);
        }
        return result;
    }

    private MyDrawable createNewRequest(final long id, final long dateModified, final boolean support3D) {
        MyDrawable cachedDrawable = null;
        synchronized (mCachedPreview) { //double check it
            cachedDrawable = mCachedPreview.get(id);
            if (cachedDrawable == null) {
                final MyDrawable temp = new MyDrawable(getDefaultBitmap(support3D),
                        TYPE_NEED_LOAD, dateModified, support3D);
                mCachedPreview.put(id, temp);
                cachedDrawable = temp;
            } else if (cachedDrawable.mDateModified != dateModified) {
                //reload it too
                cachedDrawable.mDateModified = dateModified;
                cachedDrawable.mType = TYPE_NEED_LOAD;
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "createNewRequest(" + id + ", " + dateModified + ", " + support3D + ") return " + cachedDrawable);
        }
        return cachedDrawable;
    }

    private TaskParams getOldRequest(final long id) {
        TaskParams oldRequest = null;
        for (final TaskParams one : TASK_QUEUE) {
            if (one.mRowId == id) {
                oldRequest = one;
                break;
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "getOldRequest(" + id + ") return " + oldRequest);
        }
        return oldRequest;
    }

    private boolean isProcessing(final long id, final long dateModified) {
        boolean processing = false;
        if (mCurrentRequest != null) {
            synchronized (mCurrentRequest) {
                if (mCurrentRequest.mRowId == id && mCurrentRequest.mDrawable.mDateModified == dateModified) {
                    processing = true;
                }
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "isProcessing(" + id + ", " +  dateModified + ") return " + processing);
        }
        return processing;
    }
    
    private Bitmap getThumbnail(final long id) {
//        mUiHandler.postDelayed(new Runnable() {
//            
//            @Override
//            public void run() {
//                MediaStore.Video.Thumbnails.cancelThumbnailRequest(mCr, _id, TASK_GROUP_ID);
//            }
//        }, 5000);
        final Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(mCr,
                id,
                TASK_GROUP_ID,
                MediaStore.Video.Thumbnails.MICRO_KIND,
                null);
        Bitmap newBitmap = bitmap;
        /*
         * Workaround for MediaStore.Video.Thumbnails.getThumbnail().
         * This function works well in ICS version, but not work in JB version.
         * The issue is: isMutable = false, but "true" is expected.
         * Should be removed after fix MediaStore's bug. @{
         */
        if (bitmap != null) {
            newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            bitmap.recycle();
        }
        /* @} */
        if (LOG) {
            MtkLog.v(TAG, "getPreview() return " + newBitmap);
        }
        return newBitmap;
    }
    
    private void cancelThumbnail() {
        MediaStore.Video.Thumbnails.cancelThumbnailRequest(mCr, -1, TASK_GROUP_ID);
    }
    
    public void clearCachedPreview() {
        if (LOG) {
            MtkLog.v(TAG, "clearCachedPreview()");
        }
        clearTask();
        mListeners.clear();
        synchronized (mCachedPreview) {
            final Set<Long> keys = mCachedPreview.keySet();
            for (final Long key : keys) {
                final MyDrawable drawable = mCachedPreview.get(key);
                if (drawable != null && drawable.mDrawable != null && drawable.mDrawable != mDefaultDrawable
                        && drawable.mDrawable != mDefaultDrawable3D) {
                    drawable.mDrawable.recycle();
                }
            }
            mCachedPreview.clear();
        }
        if (LOG) {
            MtkLog.v(TAG, "clearCachedPreview() finished");
        }
    }
    
    public boolean addListener(final DrawableStateListener listener) {
        return mListeners.add(listener);
    }
    
    public boolean removeListener(final DrawableStateListener listener) {
        return mListeners.remove(listener);
    }
    
    private static CachedThumbnail sCachedManager;
    public static CachedThumbnail getCachedManager(final Context context, final Bitmap defaultBitmap, final Bitmap icon3d) {
        if (sCachedManager == null) {
            sCachedManager = new CachedThumbnail(context, defaultBitmap, icon3d);
        }
        return sCachedManager;
    }
    
    public static void releaseCachedManager() {
        if (sCachedManager != null) {
            sCachedManager.clearCachedPreview();
        }
        sCachedManager = null;
    }
    
    public static final int TYPE_NEED_LOAD = 0;
    public static final int TYPE_LOADED_NO_PREVIEW = 1;
    public static final int TYPE_LOADED_HAS_PREVIEW = 2;

    public class MyDrawable {
        private long mDateModified;
        private int mType;
        private Bitmap mDrawable;
        private boolean mSupport3D;
        
        public MyDrawable(final Bitmap idrawable, final int itype, final long idateModified, final boolean isupport3D) {
            mType = itype;
            mDrawable = idrawable;
            mDateModified = idateModified;
            mSupport3D = isupport3D;
        }
        
        public void set(final Bitmap idrawable, final int itype) {
            mType = itype;
            mDrawable = idrawable;
        }

        @Override
        public String toString() {
            return new StringBuilder()
            .append("MyDrawable(type=")
            .append(mType)
            .append(", drawable=")
            .append(mDrawable)
            .append(")")
            .toString();
        }
        
    }
    
    public interface DrawableStateListener {
        //will be called if requesgted drawable state is changed.
        void onChanged(long rowId, int type, Bitmap drawable);
    }
    
    public static class TaskParams {
        long mRowId; //thumbnail _id
        long mPriority;
        MyDrawable mDrawable;//final drawable
        
        public TaskParams(final long rowId, final long priority, final MyDrawable drawable) {
            this.mRowId = rowId;
            this.mPriority = priority;
            this.mDrawable = drawable;
        }

        @Override
        public String toString() {
            return new StringBuilder()
            .append("TaskInput(rowId=")
            .append(mRowId)
            .append(", drawable=")
            .append(mDrawable)
            .append(")")
            .toString();
        }
        
        static Comparator<TaskParams> getComparator() {
            return new Comparator<TaskParams>() {

                public int compare(final TaskParams r1, final TaskParams r2) {
                    if (r1.mPriority != r2.mPriority) {
                        return (r1.mPriority < r2.mPriority) ? -1 : 1;
                    }
                    return 0;
                }
                
            };
        }
    }
}
