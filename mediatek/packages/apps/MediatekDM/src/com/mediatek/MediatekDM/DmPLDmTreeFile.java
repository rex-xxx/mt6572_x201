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

package com.mediatek.MediatekDM;

import android.content.Context;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.PLFile;
import com.mediatek.MediatekDM.mdm.PLStorage.AccessMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class DmPLDmTreeFile implements PLFile {

    private DmPLDmTreeFile(String fileName, Context context, AccessMode accessMode)
            throws FileNotFoundException {
        mDmTreeContext = context;
        dmTreeFileName = fileName;
        tempTreeFileName = "temp_" + dmTreeFileName;
        mAccessMode = accessMode;

        switch (accessMode) {
            case READ:
                mInputStream = mDmTreeContext.openFileInput(dmTreeFileName);
                Log.i(TAG.PL, "Dm tree file opened");
                break;
            case WRITE:
                mOutputStream = mDmTreeContext.openFileOutput(tempTreeFileName, 0);
                Log.i(TAG.PL, "Dm tree file opened");
                break;
            default:
                break;
        }
    }

    public static synchronized DmPLDmTreeFile getInstance(String fileName, Context context,
            AccessMode accessMode) {
        if (plTreeinstance == null) {
            try {
                plTreeinstance = new DmPLDmTreeFile(fileName, context, accessMode);
            } catch (Exception e) {
                Log.e(TAG.PL, e.getMessage());
            }
        }

        return plTreeinstance;
    }

    public synchronized void close(boolean commitClose) throws IOException {
        try {
            if (commitClose == false && mAccessMode == AccessMode.READ && mInputStream != null) {
                mInputStream.close();
                Log.i(TAG.PL, "Dm tree file closed");
            } else if (commitClose == true && mAccessMode == AccessMode.WRITE
                    && mOutputStream != null) {
                try {
                    mOutputStream.getFD().sync();
                    File tempTree = mDmTreeContext.getFileStreamPath(tempTreeFileName);
                    File tree = mDmTreeContext.getFileStreamPath(dmTreeFileName);
                    Log.d(TAG.PL, "about to rename tree.xml");
                    boolean isOk = tempTree.renameTo(tree);
                    if (!isOk) {
                        Log.e(TAG.PL,
                                "Could not rename " + tempTree.getName() + " to " + tree.getName()
                                        + "!!!");
                    }
                } catch (Exception e) {
                    Log.e(TAG.PL, "mOutputStream sync fail");
                    e.printStackTrace();
                }
                mOutputStream.close();
                Log.i(TAG.PL, "Dm tree file closed");
            } else {
                Log.e(TAG.PL, "There are something wrong in closing delta file");
            }
        } catch (IOException e) {
            Log.e(TAG.PL, "There is IOException in close dm tree file");
        } finally {
            mOutputStream = null;
            mInputStream = null;
            plTreeinstance = null;
        }

    }

    public synchronized int read(byte[] buffer) throws IOException {
        int ret = 0;
        // only input stream can read
        if (plTreeinstance == null) {
            Log.w(TAG.PL, "read plTreeinstance is null,create new");
            try {
                plTreeinstance = new DmPLDmTreeFile(dmTreeFileName, mDmTreeContext, mAccessMode);
            } catch (Exception e) {
                Log.e(TAG.PL, e.getMessage());
            }
        }
        if (mAccessMode == AccessMode.WRITE || mInputStream == null) {
            throw new IOException("Attempt to read from an output stream");
        }
        try {
            ret = mInputStream.read(buffer);
        } catch (IOException e) {
            Log.e(TAG.PL, "Read dm tree file error!");
        }
        return ret;
    }

    public synchronized void write(byte[] data) throws IOException {
        // only output stream can write
        if (plTreeinstance == null) {
            Log.w(TAG.PL, "read plTreeinstance is null,create new");
            try {
                plTreeinstance = new DmPLDmTreeFile(dmTreeFileName, mDmTreeContext, mAccessMode);
            } catch (Exception e) {
                Log.e(TAG.PL, e.getMessage());
            }
        }
        if (mAccessMode == AccessMode.READ || mOutputStream == null) {
            throw new IOException("Attempt to write to an input stream");
        }
        try {
            mOutputStream.write(data);
        } catch (IOException e) {
            Log.e(TAG.PL, "Write dm tree file error!");
        }
        return;
    }

    private static FileInputStream mInputStream = null;
    private static FileOutputStream mOutputStream = null;
    private static Context mDmTreeContext;
    private static AccessMode mAccessMode;
    private static String dmTreeFileName = null;
    private static String tempTreeFileName = null;
    // private static final String TAG = "DmServiceS::DmPLDmTreeFile";
    // private static final String TAGS="DmServiceS::DmPLDmTreeFile";
    private static DmPLDmTreeFile plTreeinstance = null;
}
