/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.beam;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Map.Entry;
import java.util.Set;

public class BeamShareTask {

    public static final int ID_NULL = -1;

    public static final int TYPE_BLUETOOTH_INCOMING = 0;

    public static final int TYPE_BLUETOOTH_OUTGOING = TYPE_BLUETOOTH_INCOMING + 1;

    public static final int TYPE_WIFI_DIRECT_INCOMING = TYPE_BLUETOOTH_INCOMING + 2;

    public static final int TYPE_WIFI_DIRECT_OUTGOING = TYPE_BLUETOOTH_INCOMING + 3;
    
    //Beam state

    public static final int STATE_FAILURE = 0; // finish - failure

    public static final int STATE_SUCCESS = STATE_FAILURE + 1; // finish - success

    // incoming task: Wi-Fi incoming + BT incoming
    public static final String SC_INCOMING_TASK = BeamShareTaskMetaData.TASK_TYPE + " in ("
            + BeamShareTask.TYPE_BLUETOOTH_INCOMING + "," + BeamShareTask.TYPE_WIFI_DIRECT_INCOMING + ")";

    // outgoing task:  Wi-Fi outgoing + BT outgoing
    public static final String SC_OUTGOING_TASK = BeamShareTaskMetaData.TASK_TYPE + " in ("
            + BeamShareTask.TYPE_BLUETOOTH_OUTGOING + "," + BeamShareTask.TYPE_WIFI_DIRECT_OUTGOING + ")";

    /**
     * Beam Share Task Metadata
     */
    public interface BeamShareTaskMetaData extends BaseColumns {

        String TABLE_NAME = "share_tasks";

        Uri CONTENT_URI = Uri.parse("content://" + BeamShareProvider.AUTHORITY + "/" + TABLE_NAME);

        String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mtkbeam.share.task";

        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mtkbeam.share.task";

        String DEFAULT_SORT_ORDER = "modified DESC";

        // the record type:such as Wifi Direct incoming ...
        String TASK_TYPE = "type";
        //the fail or success transfer
        String TASK_STATE = "state";

        // the transfer file 
        String TASK_OBJECT_FILE = "data";
        // the transfer file type
        String TASK_MIMETYPE = "mime";

        // the total and done transfer file bytes
        String TASK_TOTAL_BYTES = "total";

        String TASK_DONE_BYTES = "done";

        // finished transfer time
        String TASK_MODIFIED_DATE = "modified";
    }

    public Uri getTaskUri() {

        if (mId == ID_NULL) {

            throw new IllegalStateException("null id task can't get uri");
        } else {
            return Uri.withAppendedPath(BeamShareTaskMetaData.CONTENT_URI, Integer.toString(mId));
        }
    }

    // metadata
    private int mId = ID_NULL;
    private int mType;
    private int mState;
    private String mData;
    private String mMimeType;
    private long mTotalBytes;
    private long mDoneBytes;
    private long mModifiedDate = 0;

    public BeamShareTask(int type) {

        mType = type;
    }

    public BeamShareTask(Cursor cursor) {

        mId = cursor.getInt(cursor.getColumnIndexOrThrow(BeamShareTaskMetaData._ID));
        mType = cursor.getInt(cursor.getColumnIndexOrThrow(BeamShareTaskMetaData.TASK_TYPE));
        mState = cursor.getInt(cursor.getColumnIndexOrThrow(BeamShareTaskMetaData.TASK_STATE));

        mData = cursor.getString(cursor.getColumnIndexOrThrow(BeamShareTaskMetaData.TASK_OBJECT_FILE));
        mMimeType = cursor.getString(cursor.getColumnIndexOrThrow(BeamShareTaskMetaData.TASK_MIMETYPE));

        mTotalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(BeamShareTaskMetaData.TASK_TOTAL_BYTES));
        mDoneBytes = cursor.getLong(cursor.getColumnIndexOrThrow(BeamShareTaskMetaData.TASK_DONE_BYTES));

        mModifiedDate = cursor.getLong(cursor.getColumnIndexOrThrow(BeamShareTaskMetaData.TASK_MODIFIED_DATE));
    }

    /**
     * create ContentValues for ContentProvider operations
     *
     * @return
     */
    public ContentValues getContentValues() {

        ContentValues values = new ContentValues();

        // existing record
        if (mId != ID_NULL) {
            values.put(BeamShareTaskMetaData._ID, mId);
        }
        if (mModifiedDate != 0) {
            values.put(BeamShareTaskMetaData.TASK_MODIFIED_DATE, mModifiedDate);
        }
        values.put(BeamShareTaskMetaData.TASK_TYPE, mType);
        values.put(BeamShareTaskMetaData.TASK_STATE, mState);

        values.put(BeamShareTaskMetaData.TASK_OBJECT_FILE, mData);
        values.put(BeamShareTaskMetaData.TASK_MIMETYPE, mMimeType);
        
        values.put(BeamShareTaskMetaData.TASK_TOTAL_BYTES, mTotalBytes);
        values.put(BeamShareTaskMetaData.TASK_DONE_BYTES, mDoneBytes);

        return values;
    }

    public String getPrintableString() {

        StringBuilder res = new StringBuilder();
        ContentValues cv = getContentValues();
        Set<Entry<String, Object>> set = cv.valueSet();
        for (Entry<String, Object> e : set) {

            res.append("[").append(e.getKey()).append("=").append(e.getValue()).append("]");
        }
        return res.toString();
    }

    public static enum Direction {
        in, out
    };

    public Direction getDirection() {
        switch (mType) {
            case BeamShareTask.TYPE_BLUETOOTH_INCOMING:
            case BeamShareTask.TYPE_WIFI_DIRECT_INCOMING:
                return Direction.in; 
            case BeamShareTask.TYPE_BLUETOOTH_OUTGOING:
            case BeamShareTask.TYPE_WIFI_DIRECT_OUTGOING:
                return Direction.out;
            default:
                return Direction.out;
        }
    }

    /**********************************************************************************************************
     * Getter / Setter
     **********************************************************************************************************/
    public int getState() {
        return mState;
    }

    public void setState(int state) {
        mState = state;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public void setMimeType(String mimeType) {

        if (mimeType != null) {

            // MIME type matching in the Android framework is case-sensitive
            // (unlike formal RFC MIME types).
            // As a result, you should always specify MIME types using lowercase
            // letters.
            mMimeType = mimeType.toLowerCase();
        } else {
            mMimeType = mimeType;
        }
    }

    public long getTotalBytes() {
        return this.mTotalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.mTotalBytes = totalBytes;
    }

    public long getDoneBytes() {
        return this.mDoneBytes;
    }

    public void setDoneBytes(long doneBytes) {
        this.mDoneBytes = doneBytes;
    }

    public long getModifiedDate() {
        return this.mModifiedDate;
    }

    public void setModifiedDate(long modifiedDate) {
        this.mModifiedDate = modifiedDate;
    }

    public int getId() {
        return this.mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public int getType() {
        return this.mType;
    }

    public String getData() {
        return this.mData;
    }

    public void setData(String data) {
        this.mData = data;
    }
}
