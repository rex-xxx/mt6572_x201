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

package com.mediatek.MediatekDM.mdm.fumo;

import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.MdmException.MdmError;

import junit.framework.Assert;

public class MdmFumo {
    public static enum ClientType {
        DEVICE,
        USER,
    }

    public static enum FwUpdateStatus {
        DONE,
        START_POSTPONED,
        STARTED,
    }

    private static MdmFumo sInstance;

    public MdmFumo(String fumoRootURI, FumoHandler h) throws MdmException {
        // TODO
        synchronized (MdmFumo.class) {
            Assert.assertEquals(null, sInstance);

            if (0 != _create()) {
                throw new MdmException(MdmError.INTERNAL);
            }

            sInstance = this;
        }
    }

    public void destroy() {
        // TODO
    }

    public void triggerSession(byte[] message, MdmFumo.ClientType clientType) throws MdmException {
        // TODO
    }

    public void triggerSession(byte[] message, MdmFumo.ClientType clientType, String account)
            throws MdmException {
        // TODO
    }

    public void resumeDLSession() throws MdmException {
        // TODO
    }

    public String getPkgName() throws MdmException {
        // TODO
        return null;
    }

    public String getPkgVersion() throws MdmException {
        // TODO
        return null;
    }

    public FumoState getState() throws MdmException {
        // TODO
        return null;
    }

    public String getUpdatePkgPath() throws MdmException {
        // TODO
        return null;
    }

    public void executeFwUpdate() throws MdmException {
        // TODO
    }

    public void triggerReportSession(MdmFumoUpdateResult.ResultCode resultCode) throws MdmException {
        // TODO
    }

    public FwUpdateStatus getUpdateStatus() throws MdmException {
        // TODO
        return null;
    }

    public int querySessionActions() {
        // TODO
        return 0;
    }

    public void setIsReportLocUriRoot(boolean isReportLocUriRoot) throws MdmException {
        // TODO
    }

    public boolean getIsReportLocUriRoot() throws MdmException {
        // TODO
        return false;
    }

    public void setIsConfirmDownloadCalledInResume(boolean isConfirmDownloadCalledInResume)
            throws MdmException {
        // TODO
    }

    public boolean getIsConfirmDownloadCalledInResume() throws MdmException {
        // TODO
        return false;
    }

    // native interfaces
    private native int _create();

    private native int _destroy();
}
