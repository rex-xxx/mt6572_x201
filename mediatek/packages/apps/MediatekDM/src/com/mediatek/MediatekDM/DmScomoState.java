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

package com.mediatek.MediatekDM;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.ext.MTKFileUtil;
import com.mediatek.MediatekDM.mdm.DownloadDescriptor;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomo;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomoDp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class DmScomoState implements Serializable {
    private static final String SCOMO_STATE_FILE = "scomo_state";

    public static final int IDLE = 0;
    public static final int DOWNLOAD_VALUE_BEGIN = 1;
    public static final int DOWNLOADING_STARTED = 2;
    public static final int DOWNLOADING = 3;
    public static final int PAUSED = 4;
    public static final int CONFIRM_DOWNLOAD = 5;
    public static final int DOWNLOAD_FAILED = 6;
    public static final int ABORTED = 7;
    // these two states are used to fix the problem that `engine will download
    // some more packages after DL session is cancelled`
    // public static final int STARTED = 8;
    public static final int RESUMED = 9;
    public static final int WRONG_PACKAGE_FORMAT = 10;
    public static final int DOWNLOAD_VALUE_END = 20;

    public static final int INSTALLING = 21;
    public static final int UPDATING = 22;
    public static final int CONFIRM_INSTALL = 23;
    public static final int CONFIRM_UPDATE = 24;
    public static final int INSTALL_FAILED = 25;
    public static final int INSTALL_OK = 26;

    public static final int GENERIC_ERROR = 27;
    public String errorMsg;

    // TODO: journal file is needed to fix problems like power off, reboot, ...
    // if rebooted during installing, state should be set to CONFIRM_INSTALLED
    // if rebooted during downloading, state should be set to PAUSED

    public int state = IDLE;
    public int currentSize = 0;
    public int totalSize = 0;
    public MdmScomoDp currentDp;
    public DownloadDescriptor currentDd;
    public DmScomoPackageManager.ScomoPackageInfo pkgInfo;
    public String archiveFilePath = "";

    /**
     * mVerbose is used by SCOMO listeners to decide whether to interact with
     * user. mVerbose will be set in MmiConfirmation (ALERT 1101), and be reset
     * after DM session.
     */
    public boolean mVerbose = false;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(this.archiveFilePath);
        out.writeInt(this.state);
        out.writeInt(this.currentSize);
        out.writeInt(this.totalSize);
        out.writeBoolean(mVerbose);
        if (this.currentDp != null && this.currentDp.getName() != null) {
            out.writeUTF(this.currentDp.getName());
        } else {
            out.writeUTF("");
        }

        // write dd
        if (currentDd == null) {
            out.writeLong(0);
        } else {
            out.writeLong(currentDd.size);
            out.writeObject(currentDd.field);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.archiveFilePath = in.readUTF();
        this.state = in.readInt();
        this.currentSize = in.readInt();
        this.totalSize = in.readInt();
        this.mVerbose = in.readBoolean();
        // reload dp
        String dpName = in.readUTF();
        if (!dpName.equals("")) {
            try {
                currentDp = MdmScomo.getInstance(DmConst.NodeUri.ScomoRootUri,
                        DmScomoHandler.getInstance()).createDP(
                        dpName, DmScomoDpHandler.getInstance());
            } catch (Exception e) {
                e.printStackTrace();
                currentDp = null;
            }
        }

        // reload dd
        long ddSize = in.readLong();
        if (ddSize == 0) {
            currentDd = null;
        } else {
            this.currentDd = new DownloadDescriptor();
            this.currentDd.size = ddSize;
            this.currentDd.field = (String[]) in.readObject();
        }

        if (this.state == PAUSED) {

        } else if (this.state == INSTALLING || this.state == UPDATING
                || this.state == CONFIRM_INSTALL
                || this.state == CONFIRM_UPDATE) {
            this.state = CONFIRM_INSTALL;
            // reload pkgInfo from archivePath;
            this.setArchivePath(this.archiveFilePath);
        } else if (state == DOWNLOADING || state == DOWNLOADING_STARTED || state == RESUMED) {
            this.state = PAUSED;
        } else {
            // reset state
            Log.e(TAG.Scomo, "abnormal exit, delete delta files");
            resetState();
        }
    }

    private void resetState() {
        DmService.getInstance().deleteScomoFile();
        this.state = IDLE;
        this.currentDd = null;
        this.currentDp = null;
        this.pkgInfo = null;
        this.currentSize = 0;
        this.totalSize = 0;
    }

    public void setArchivePath(String path) {
        this.archiveFilePath = path;
        this.pkgInfo = DmScomoPackageManager.getInstance().getMinimalPackageInfo(path);
    }

    public static DmScomoState load(Context context) {
        Object obj = null;
        try {
            obj = MTKFileUtil.atomicRead(context.getFileStreamPath(SCOMO_STATE_FILE));
        } catch (Exception e) {
            Log.e(TAG.Scomo, SCOMO_STATE_FILE + " not found, create one");
        }

        if (obj != null) {
            DmScomoState ret = (DmScomoState) obj;
            Log.i(TAG.Scomo, "DmScomoState: state loaded: state= " + ret.state);
            return ret;
        }
        return new DmScomoState();
    }

    public static void store(Context context, DmScomoState state) {
        if (state == null) {
            return;
        }
        if (state.state == IDLE) {
            Log.i(TAG.Scomo, "state is IDLE, reset state");
            state.resetState();
        }
        MTKFileUtil.atomicWrite(context.getFileStreamPath(SCOMO_STATE_FILE), state);
    }

    // ////////////////// name,icon,version,description may from different
    // source,e.g. PM > dd > dp ////////////////
    public String getName() {
        String ret = "";
        if (pkgInfo != null && pkgInfo.label != null) {
            ret = pkgInfo.label;
        } else if (currentDd != null && currentDd.getField(DownloadDescriptor.Field.NAME) != null) {
            ret = currentDd.getField(DownloadDescriptor.Field.NAME);
        } else if (currentDp != null && currentDp.getName() != null) {
            ret = "";
        }
        return ret;
    }

    public String getVersion() {
        String ret = "";
        if (pkgInfo != null && pkgInfo.version != null) {
            ret = pkgInfo.version;
        } else if (currentDd != null
                && currentDd.getField(DownloadDescriptor.Field.VERSION) != null) { //
            ret = currentDd.getField(DownloadDescriptor.Field.VERSION);
        }
        return ret;
    }

    public Drawable getIcon() {
        Drawable ret;
        if (pkgInfo != null) {
            // pkgInfo.icon is assured to be not-null
            ret = pkgInfo.icon;
        } else {
            ret = DmScomoPackageManager.getInstance().getDefaultActivityIcon();
        }
        return ret;
    }

    public int getSize() {
        int ret = -1;
        if (this.totalSize != -1 && this.totalSize != 0) {
            ret = this.totalSize;
        } else if (currentDd != null) {
            ret = (int) currentDd.size;
        }
        return ret;

    }

    public CharSequence getDescription() {
        Log.i(TAG.Scomo, "getdescription begin");
        String ret = DmService.getInstance().getString(R.string.default_scomo_description);
        if (pkgInfo != null && pkgInfo.description != null) {
            ret = pkgInfo.description;
        } else if (currentDd != null
                && currentDd.getField(DownloadDescriptor.Field.DESCRIPTION) != null) {
            ret = currentDd.getField(DownloadDescriptor.Field.DESCRIPTION);
        }
        Log.i(TAG.Scomo, "getdescription end");
        return ret;

    }

    public String getPackageName() {
        String ret = "";
        try {
            if (pkgInfo != null && pkgInfo.name != null) {
                ret = pkgInfo.name;
            } else if (currentDp != null && currentDp.getPkgName() != null) {
                ret = currentDp.getPkgName();
            }
        } catch (Exception e) {
        }
        return ret;
    }

    public boolean isAboutDownload() {
        return state > DOWNLOAD_VALUE_BEGIN && state < DOWNLOAD_VALUE_END;
    }
}
