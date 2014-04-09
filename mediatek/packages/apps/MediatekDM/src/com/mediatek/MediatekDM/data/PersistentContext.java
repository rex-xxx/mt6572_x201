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

package com.mediatek.MediatekDM.data;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.DownloadDescriptor;

import java.io.File;

public class PersistentContext implements IDmPersistentValues {
    private static final Object LOCK_OBJ = new Object();
    private static PersistentContext sInstance = null;

    private final Context fContext;

    // use 2 files to make recoverable from power lost.
    private final StateValues fValues;
    private final StateValues fValuesBak;

    private PersistentContext(Context context) {
        fContext = context;
        fValues = new StateValues(context, "dm_values");
        fValues.load();
        fValuesBak = new StateValues(context, "dm_values_bak");
        fValuesBak.load();
        // modify for CR ALPS002821226 (need to enable)
        int state = getDLSessionStatus();
        if (state == STATE_DOWNLOADING || state == STATE_RESUMEDOWNLOAD) {
            setDLSessionStatus(STATE_PAUSEDOWNLOAD);
        }
        Log.v(TAG.Common, "--- [PersistentContext] loading, DL state is " + state + " ---");
        // end modify
    }

    public static PersistentContext getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LOCK_OBJ) {
                if (sInstance == null) {
                    sInstance = new PersistentContext(context);
                }
            }
        }
        return sInstance;
    }

    @Override
    public long getMaxSize() {
        long maxSize = getTotalInternalMemorySize();
        Log.d(TAG.Common, "[persistent]:MAX-SIZE=" + maxSize);
        return maxSize;
    }

    @Override
    public long getDownloadedSize() {
        long dlSize = 0L;

        fValues.load();
        String value = fValues.get(StateValues.ST_DOWNLOADED_SIZE);
        if (!TextUtils.isEmpty(value)) {
            dlSize = Long.valueOf(value);
        } else {
            fValuesBak.load();
            value = fValuesBak.get(StateValues.ST_DOWNLOADED_SIZE);
            if (!TextUtils.isEmpty(value)) {
                dlSize = Long.valueOf(value);
            }
        }

        Log.d(TAG.Common, "[persistent]:DOWNLOADED_SIZE->get=" + dlSize);
        return dlSize;
    }

    @Override
    public void setDownloadedSize(long size) {
        String dlSize = Long.toString(size);
        Log.d(TAG.Common, "[persistent]:DOWNLOADED_SIZE->set=" + dlSize);

        fValues.put(StateValues.ST_DOWNLOADED_SIZE, dlSize);
        fValues.commit();

        fValuesBak.put(StateValues.ST_DOWNLOADED_SIZE, dlSize);
        fValuesBak.commit();
    }

    @Override
    public long getSize() {
        long size = 0L;

        fValues.load();
        String value = fValues.get(StateValues.DD_SIZE);
        if (!TextUtils.isEmpty(value)) {
            size = Long.valueOf(value);
        } else {
            fValuesBak.load();
            value = fValuesBak.get(StateValues.DD_SIZE);
            if (!TextUtils.isEmpty(value)) {
                size = Long.valueOf(value);
            }
        }
        Log.d(TAG.Common, "[persistent]:DD_SIZE->get=" + size);
        return size;
    }

    @Override
    public int getDLSessionStatus() {
        // default session state value for DM, will check network in first step.
        int state = IDmPersistentValues.MSG_NETWORKERROR;

        fValues.load();
        String value = fValues.get(StateValues.ST_STATE);
        if (!TextUtils.isEmpty(value)) {
            state = Integer.valueOf(value);
        } else {
            fValuesBak.load();
            value = fValuesBak.get(StateValues.ST_STATE);
            if (!TextUtils.isEmpty(value)) {
                state = Integer.valueOf(value);
            } else {
                Log.w(TAG.Common, "[persistent]:DL_STATE->DEFAULT(network)");
            }
        }
        Log.d(TAG.Common, "[persistent]:DL_STATE->get=" + state);
        return state;
    }

    @Override
    public void setDLSessionStatus(int status) {
        String state = Integer.toString(status);
        Log.d(TAG.Common, "[persistent]:DL_STATE->set=" + state);

        fValues.put(StateValues.ST_STATE, state);
        fValues.commit();

        fValuesBak.put(StateValues.ST_STATE, state);
        fValuesBak.commit();
    }

    @Override
    public DownloadDescriptor getDownloadDescriptor() {
        DownloadDescriptor dd = new DownloadDescriptor();

        fValues.load();
        dd.field[0] = fValues.get(StateValues.DD_FIELD0);
        dd.field[1] = fValues.get(StateValues.DD_FIELD1);
        dd.field[2] = fValues.get(StateValues.DD_FIELD2);
        dd.field[3] = fValues.get(StateValues.DD_FIELD3);
        dd.field[4] = fValues.get(StateValues.DD_FIELD4);
        dd.field[5] = fValues.get(StateValues.DD_FIELD5);
        dd.field[6] = fValues.get(StateValues.DD_FIELD6);
        dd.field[7] = fValues.get(StateValues.DD_FIELD7);
        dd.field[8] = fValues.get(StateValues.DD_FIELD8);
        dd.field[9] = fValues.get(StateValues.DD_FIELD9);
        dd.field[10] = fValues.get(StateValues.DD_FIELD10);
        dd.field[11] = fValues.get(StateValues.DD_FIELD11);

        String value = fValues.get(StateValues.DD_SIZE);
        if (!TextUtils.isEmpty(value)) {
            dd.size = Long.valueOf(value);
        } else {
            fValuesBak.load();
            dd.field[0] = fValuesBak.get(StateValues.DD_FIELD0);
            dd.field[1] = fValuesBak.get(StateValues.DD_FIELD1);
            dd.field[2] = fValuesBak.get(StateValues.DD_FIELD2);
            dd.field[3] = fValuesBak.get(StateValues.DD_FIELD3);
            dd.field[4] = fValuesBak.get(StateValues.DD_FIELD4);
            dd.field[5] = fValuesBak.get(StateValues.DD_FIELD5);
            dd.field[6] = fValuesBak.get(StateValues.DD_FIELD6);
            dd.field[7] = fValuesBak.get(StateValues.DD_FIELD7);
            dd.field[8] = fValuesBak.get(StateValues.DD_FIELD8);
            dd.field[9] = fValuesBak.get(StateValues.DD_FIELD9);
            dd.field[10] = fValuesBak.get(StateValues.DD_FIELD10);
            dd.field[11] = fValuesBak.get(StateValues.DD_FIELD11);

            value = fValuesBak.get(StateValues.DD_SIZE);
            if (!TextUtils.isEmpty(value)) {
                dd.size = Long.valueOf(value);
            }
        }

        return dd;
    }

    @Override
    public void setDownloadDescriptor(DownloadDescriptor dd) {
        if (dd == null) {
            throw new RuntimeException("You can't save an empty DD.");
        }
        fValues.put(StateValues.DD_FIELD0, dd.field[0]);
        fValues.put(StateValues.DD_FIELD1, dd.field[1]);
        fValues.put(StateValues.DD_FIELD2, dd.field[2]);
        fValues.put(StateValues.DD_FIELD3, dd.field[3]);
        fValues.put(StateValues.DD_FIELD4, dd.field[4]);
        fValues.put(StateValues.DD_FIELD5, dd.field[5]);
        fValues.put(StateValues.DD_FIELD6, dd.field[6]);
        fValues.put(StateValues.DD_FIELD7, dd.field[7]);
        fValues.put(StateValues.DD_FIELD8, dd.field[8]);
        fValues.put(StateValues.DD_FIELD9, dd.field[9]);
        fValues.put(StateValues.DD_FIELD10, dd.field[10]);
        fValues.put(StateValues.DD_FIELD11, dd.field[11]);
        fValues.put(StateValues.DD_SIZE, Long.toString(dd.size));

        fValues.commit();

        fValuesBak.put(StateValues.DD_FIELD0, dd.field[0]);
        fValuesBak.put(StateValues.DD_FIELD1, dd.field[1]);
        fValuesBak.put(StateValues.DD_FIELD2, dd.field[2]);
        fValuesBak.put(StateValues.DD_FIELD3, dd.field[3]);
        fValuesBak.put(StateValues.DD_FIELD4, dd.field[4]);
        fValuesBak.put(StateValues.DD_FIELD5, dd.field[5]);
        fValuesBak.put(StateValues.DD_FIELD6, dd.field[6]);
        fValuesBak.put(StateValues.DD_FIELD7, dd.field[7]);
        fValuesBak.put(StateValues.DD_FIELD8, dd.field[8]);
        fValuesBak.put(StateValues.DD_FIELD9, dd.field[9]);
        fValuesBak.put(StateValues.DD_FIELD10, dd.field[10]);
        fValuesBak.put(StateValues.DD_FIELD11, dd.field[11]);
        fValuesBak.put(StateValues.DD_SIZE, Long.toString(dd.size));

        fValuesBak.commit();
        Log.d(TAG.Common, "[persistent]: dd saved.");
    }

    @Override
    public void deleteDeltaPackage() {
        Log.d(TAG.Common, "[persistent]: delete package.");
        fContext.deleteFile(deltaFileName);
        fContext.deleteFile(resumeFileName);

        String state = Integer.toString(IDmPersistentValues.MSG_NETWORKERROR);
        fValues.put(StateValues.ST_DOWNLOADED_SIZE, "");
        fValues.put(StateValues.ST_STATE, state);
        fValues.commit();

        fValuesBak.put(StateValues.ST_DOWNLOADED_SIZE, "");
        fValues.put(StateValues.ST_STATE, state);
        fValuesBak.commit();
    }

    @Override
    public DownloadInfo getDownloadInfo() {
        DownloadInfo info = new DownloadInfo();

        fValues.load();
        info.url = fValues.get(StateValues.DD_FIELD1);
        info.version = fValues.get(StateValues.DD_FIELD4);

        if (TextUtils.isEmpty(info.url)) {
            fValuesBak.load();
            info.url = fValuesBak.get(StateValues.DD_FIELD1);
            info.version = fValuesBak.get(StateValues.DD_FIELD4);
        }

        return info;
    }

    private static long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlock = stat.getAvailableBlocks();
        long availableSize = (long) (availableBlock * blockSize) - 1000000;
        return availableSize;
    }

}
