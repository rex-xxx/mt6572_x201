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

package com.mediatek.MediatekDM.data;

import com.mediatek.MediatekDM.mdm.DownloadDescriptor;

public interface IDmPersistentValues {
    public long getMaxSize();

    public long getDownloadedSize();

    public void setDownloadedSize(long size);

    public long getSize();

    public int getDLSessionStatus();

    public void setDLSessionStatus(int status);

    public DownloadDescriptor getDownloadDescriptor();

    public void setDownloadDescriptor(DownloadDescriptor dd);

    // public void deleteDlInfo();
    public void deleteDeltaPackage();

    public DownloadInfo getDownloadInfo();

    public static final int MSG_NETWORKERROR = 0;
    public static final int MSG_NEWVERSIONDETECTED = 1;
    public static final int MSG_NONEWVERSIONDETECTED = 2;
    public static final int MSG_DLPKGCONFIRMED = 3;
    public static final int MSG_DLPKGUPGRADE = 4;
    public static final int MSG_DLPKGCOMPLETE = 5;
    public static final int MSG_DLPKGCANCELLED = 6;
    public static final int MSG_DLPKGPAUSED = 7;
    public static final int MSG_DLPKGRESUME = 8;
    public static final int MSG_NIARECIEVED = 9;
    public static final int MSG_NIACONFIRMED = 10;
    public static final int MSG_DMSESSIONCOMPLETED = 11;
    public static final int MSG_DMSESSIONABORTED = 12;
    public static final int MSG_DLSESSIONABORTED = 13;
    public static final int MSG_CONNECTTIMEOUT = 16;
    public static final int MSG_AUTHENTICATION = 14;
    public static final int MSG_OTHERERROR = 15;
    public static final int MSG_DLPKGSTARTED = 16;

    public static final int MSG_SCOMO_CONFIRM_DOWNLOAD = 200;
    public static final int MSG_SCOMO_CONFIRM_INSTALL = 201;
    public static final int MSG_SCOMO_EXEC_INSTALL = 202;
    public static final int MSG_DM_SESSION_COMPLETED = 203;
    public static final int MSG_DM_SESSION_ABORTED = 204;
    public static final int MSG_SCOMO_DL_SESSION_COMPLETED = 205;
    public static final int MSG_SCOMO_DL_SESSION_ABORTED = 206;
    public static final int MSG_SCOMO_DL_PKG_UPGRADE = 207;
    public static final int MSG_SCOMO_DL_SESSION_START = 208;
    // public static final int MSG_SCOMO_DL_SESSION_PAUSED = 209;
    // public static final int MSG_SCOMO_DL_SESSION_RESUMED = 210;

    public static final int MSG_USERMODE_VISIBLE = 17;
    public static final int MSG_USERMODE_INTERACT = 18;
    public static final int MSG_NIASESSION_START = 19;
    public static final int MSG_NIASESSION_CANCLE = 20;
    public static final int MSG_DMSESSION_START = 21;
    public static final int MSG_DMSESSION_CANCLE = 22;
    public static final int MSG_NIASESSION_INVALID = 23;
    public static final int MSG_USERMODE_INVISIBLE=24;
    public static final int MSG_CHECK_NIA=25;

    public static final int MSG_WAP_CONNECTION_ALREADY_EXIST = 100;
    public static final int MSG_WAP_CONNECTION_FAILED = 101;
    public static final int MSG_WAP_CONNECTION_APN_TYPE_NOT_AVAILABLE = 102;
    public static final int MSG_WAP_CONNECTION_SUCCESS = 103;
    public static final int MSG_WAP_CONNECTION_TIMEOUT = 104;

    public static final int STATE_QUERYNEWVERSION = 0;
    public static final int STATE_DOWNLOADING = 1;
    public static final int STATE_DLPKGCOMPLETE = 3;
    public static final int STATE_CANCELDOWNLOAD = 4;
    public static final int STATE_PAUSEDOWNLOAD = 5;
    public static final int STATE_RESUMEDOWNLOAD = 6;
    public static final int STATE_UPDATECOMPLETE = 7;
    public static final int STATE_NEWVERSIONDETECTED = 9;
    public static final int STATE_NOTDOWNLOAD = 10;
    public static final int STATE_NIARECEIVED = 11;
    public static final int SERVER = 0;
    public static final int CLIENT_PULL = 1; // foreground
    public static final int CLIENT_POLLING = 2; // background

    public static final int STATE_USERMODE_INVISIBLE = 16;
    public static final int STATE_USERMODE_VISIBLE = 17;
    public static final int STATE_USERMODE_INTERACT = 18;
    public static final int STATE_NIASESSION_START = 19;
    public static final int STATE_NIASESSION_CANCLE = 20;
    public static final int STATE_DMSESSION_START = 21;
    public static final int STATE_DMSESSION_CANCLE = 22;
    public static final int STATE_USREMODE_CANCLE = 23;
    public static final int STATE_DETECT_WAP = 24;
    public static final int STATE_NIASESSION_COMPLETE = 25;
    public static final int STATE_WAPCONNECT_SUCCESS = 26;
    public static final int STATE_WAPCONNECT_TIMEOUT = 27;

    public static final String deltaFileName = "delta.zip";
    public static final String scomoFileName = "scomo.zip";
    public static final String resumeFileName = "dlresume.dat";
    public static final String resumeScomoFileName = "scomoresume.dat";
}
