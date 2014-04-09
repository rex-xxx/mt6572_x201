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
import android.net.Uri;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.NodeIoHandler;
import com.mediatek.custom.CustomProperties;

public class DmModNodeIoHandler implements NodeIoHandler {

    protected Context mContext = null;
    protected Uri uri = null;
    public static final String uStr = "./DevInfo/Mod";

    public DmModNodeIoHandler(Context ctx, Uri treeUri) {

        Log.i(TAG.NodeIOHandler, "DmModNodeIoHanlder constructed..");

        this.mContext = ctx;
        this.uri = treeUri;
    }

    @Override
    public int read(int offset, byte[] data) throws MdmException {

        if (uri == null) {
            throw new MdmException("MODEL read URI is null!");
        }

        Log.i(TAG.NodeIOHandler, "DmManNodeIoHandler.read Uri:" + uri + ", offset:" + offset);

        String mod = CustomProperties.getString(CustomProperties.MODULE_DM, CustomProperties.MODEL,
                "MTK");
        byte[] src = mod.getBytes();

        if (data == null) {
            return src.length;
        } else {
            for (int i = 0; i < src.length; i++) {
                data[offset + i] = src[i];
            }
        }

        return src.length;
    }

    @Override
    public void write(int offset, byte[] data, int totalSize) throws MdmException {
        // TODO Auto-generated method stub

    }

}
