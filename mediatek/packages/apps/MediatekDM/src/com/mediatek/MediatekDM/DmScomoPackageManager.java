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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.ext.MTKMediaContainer;
import com.mediatek.MediatekDM.ext.MTKPackageManager;
import com.mediatek.MediatekDM.mdm.MdmTree;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomo;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomoDc;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DmScomoPackageManager {

    public static final int STATUS_OK = 0;
    public static final int STATUS_FAILED_UPDATE = 1;
    public static final int STATUS_FAILED = 2;

    // FIXME!
    // imcs.getMinimalPackageInfo has an extra argument(shreshold) on ICS.
    // hard coded as 1024*1024 to pass build.
    private static final long DEF_CONTAINER_SHRESHOLD = 1024 * 1024;

    static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(
            "com.android.defcontainer",
            "com.android.defcontainer.DefaultContainerService");

    private PackageManager mPackageManager;
    private Context mContext;
    private HandlerThread mThread;
    private static DmScomoPackageManager INSTANCE = null;

    class ScomoPackageInfo {
        String name;
        String label;
        String version;
        String description;
        Drawable icon;
    }

    private MTKMediaContainer mMtkContainer;
    private ServiceConnection mContainerServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            mMtkContainer = new MTKMediaContainer(service);
        }

        public void onServiceDisconnected(ComponentName name) {
            mMtkContainer.finish();
            mMtkContainer = null;
        }

    };

    public static DmScomoPackageManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DmScomoPackageManager();
        }
        return INSTANCE;
    }

    public DmScomoPackageManager() {
        mContext = DmApplication.getInstance();

        mPackageManager = mContext.getPackageManager();

        Intent intent = new Intent();
        intent.setComponent(DEFAULT_CONTAINER_COMPONENT);
        mContext.bindService(intent, mContainerServiceConnection, Context.BIND_AUTO_CREATE);

        mThread = new HandlerThread("scomo_thread");
        mThread.start();
    }

    public HandlerThread getThread() {
        return mThread;
    }

    public boolean checkSpace(String archiveFilePath) {
        if (archiveFilePath == null) {
            return false;
        }
        if (mMtkContainer == null) { // not bounded yet
            return true;
        }
        return mMtkContainer.checkSpace(archiveFilePath, DEF_CONTAINER_SHRESHOLD);
    }

    public ScomoPackageInfo getPackageInfo(String pkgName) {
        if (pkgName == null || pkgName.length() == 0) {
            return null;
        }
        ScomoPackageInfo ret = new ScomoPackageInfo();
        ret.name = pkgName;
        try {
            PackageInfo info = mPackageManager.getPackageInfo(pkgName, 0);
            ret.version = info.versionName;
            ret.description = "test";
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public synchronized void install(final String absPkgPath,
            final ScomoPackageInstallObserver observer,
            final boolean shouldUpdate) {
        Log.i(TAG.Scomo, "pkgPath is " + absPkgPath + " should update " + shouldUpdate);
        if (absPkgPath == null) {
            new Handler(mContext.getMainLooper()).post(new Runnable() {
                public void run() {
                    if (observer != null) {
                        observer.packageInstalled(absPkgPath, STATUS_FAILED);
                    }
                }
            });
            return;
        }
        PackageManager pm = mContext.getPackageManager();
        int installFlag = 0;
        if (shouldUpdate) {
            installFlag |= MTKPackageManager.INSTALL_REPLACE_EXISTING;
        }
        Log.v(TAG.Scomo, "PM: about to install, install Flag is " + installFlag);
        File file = new File(absPkgPath);
        file.setReadable(true, false);
        file.setWritable(true, false);
        file.setExecutable(true, false);
        Log.v(TAG.Scomo, "open permission, file name is " + file.getAbsolutePath());
        MTKPackageManager.installPackage(pm, Uri.parse(absPkgPath), installFlag,
                new MTKPackageManager.InstallListener() {
                    @Override
                    public void packageInstalled(final String name, final int status) {
                        new Handler(mContext.getMainLooper()).post(new Runnable() {
                            public void run() {
                                Log.i(TAG.Scomo, "PM: package installed, status: " + status
                                        + " packageName: " + name);
                                int ret = STATUS_OK;
                                if (status == MTKPackageManager.INSTALL_SUCCEEDED) {
                                    ret = STATUS_OK;
                                } else if (!shouldUpdate
                                        && status == MTKPackageManager.INSTALL_FAILED_ALREADY_EXISTS) {
                                    ret = STATUS_FAILED_UPDATE;
                                } else {
                                    ret = STATUS_FAILED;
                                }
                                if (observer != null) {
                                    observer.packageInstalled(name, ret);
                                }
                            }
                        });
                    }
                });

    }

    interface ScomoPackageInstallObserver {
        void packageInstalled(String name, int status);
    }

    public void scanPackage() {
        Log.i(TAG.Scomo, "scanPackage: begin");
        if (DmService.getInstance() == null || !DmService.getInstance().IsInitDmController()) {
            Log.w(TAG.Scomo, "scanPackage: DmService or DmController is not ready, return");
            return;
        }
        new Handler(mThread.getLooper()).post(new Runnable() {
            public void run() {
                DmScomoPackageManager.this.scanPackageInternal();
            }
        });
        Log.i(TAG.Scomo, "scanPackage: end");
    }

    private void scanPackageInternal() {
        try {
            List<ApplicationInfo> installedList = mPackageManager.getInstalledApplications(0);
            List<MdmScomoDc> dcs = MdmScomo.getInstance(DmConst.NodeUri.ScomoRootUri,
                    DmScomoHandler.getInstance())
                    .getDcs();
            Set<String> dcsNames = new HashSet<String>();
            for (MdmScomoDc dc : dcs) {
                dcsNames.add(dc.getName());
            }
            Set<String> appNames = new HashSet<String>();
            for (ApplicationInfo appInfo : installedList) {
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                    continue;
                }
                appNames.add(appInfo.packageName);
            }

            Set<String> dcsNamesTmp = new HashSet<String>(dcsNames);
            Set<String> appNamesTmp = new HashSet<String>(appNames);

            dcsNamesTmp.removeAll(appNames); // dcsNamesTmp now contains pkg
                                             // need to be removed from dcs
            appNamesTmp.removeAll(dcsNames); // appNamesTMp now contains pkg
                                             // need to be added to dcs

            MdmScomo scomo = MdmScomo.getInstance(DmConst.NodeUri.ScomoRootUri,
                    DmScomoHandler.getInstance());
            for (String pkgName : dcsNamesTmp) {
                Log.i(TAG.Scomo, "scanPackage: remove " + pkgName);
                MdmScomoDc dc = scomo.createDC(pkgName, DmScomoDcHandler.getInstance(),
                        DmPLInventory.getInstance());
                dc.deleteFromInventory();
                dc.destroy();
            }
            for (String pkgName : appNamesTmp) {
                Log.i(TAG.Scomo, "scanPackage: add " + pkgName);
                MdmScomoDc dc = scomo.createDC(pkgName, DmScomoDcHandler.getInstance(),
                        DmPLInventory.getInstance());
                dc.addToInventory(pkgName, pkgName, null, null, null, null, true);
            }
            new MdmTree().writeToPersistentStorage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ScomoPackageInfo getMinimalPackageInfo(String archiveFilePath) {
        if (archiveFilePath == null) {
            Log.w(TAG.Scomo, "getMinimalPackageInfo fail for archiveFilePath is null");
            return null;
        }
        ScomoPackageInfo ret = new ScomoPackageInfo();
        MTKPackageManager.PackageInfo packInfo = MTKPackageManager.getPackageInfo(mPackageManager,
                mContext.getResources(), archiveFilePath);

        if (packInfo == null) {
            Log.w(TAG.Scomo, "get Package Info fail, the archiveFile Path is " + archiveFilePath);
            return null;
        }
        ret.version = packInfo.version;
        ret.name = packInfo.name;
        ret.label = packInfo.label;
        ret.description = packInfo.description;
        ret.icon = packInfo.icon;
        // ret.pkg = packInfo.pkg;

        return ret;
    }

    public boolean isPackageInstalled(String pkgName) {
        if (pkgName == null) {
            return false;
        }
        try {
            PackageInfo pi = mPackageManager.getPackageInfo(pkgName, 0);
            return (pi != null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Drawable getDefaultActivityIcon() {
        return mPackageManager.getDefaultActivityIcon();
    }
}
