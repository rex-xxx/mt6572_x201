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

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.MdmException;

public class DmStreamingNodeIoHandler extends DmNodeIoHandlerForDB {
    // private File StreamingCfg;
    // final private String cfgFilePath = "/data/data/Streaming.cfg";
    private final String[] item = {
            "Name", "To-Proxy", "To-NapID", "NetInfo", "MIN-UDP-PORT", "MAX-UDP-PORT"
    };
    private final String[] mContentItem = new String[item.length];

    private String[] contentValue = {
            null
    };
    // In buildsystem
    // String[] projection = {Telephony.Carriers.MMSC};
    // Uri table = Telephony.Carriers.CONTENT_URI;

    // in eclipse

    String[] projection = {
            "mtk_rtsp_name", "mtk_rtsp_to_proxy", "mtk_rtsp_to_napid", "mtk_rtsp_netinfo",
            "mtk_rtsp_min_udp_port",
            "mtk_rtsp_max_udp_port"
    };

    Uri table = Uri.parse("content://media/internal/streaming/omartspsetting");

    public DmStreamingNodeIoHandler(Context context, Uri StreamingUri) {
        mContext = context;
        uri = StreamingUri;
        for (int i = 0; i < item.length; i++) {
            map.put(item[i], projection[i]);
        }
    }

    public int read(int arg0, byte[] arg1) throws MdmException {

        String uriKey = uri.getPath();
        Log.i(TAG.NodeIOHandler, "uri: " + uriKey);
        Log.i(TAG.NodeIOHandler, "arg0: " + arg0);
        if (DmService.mCCStoredParams.containsKey(uriKey)) {
            recordToRead = DmService.mCCStoredParams.get(uriKey);
            Log.d(TAG.NodeIOHandler, "get valueToRead from mCCStoredParams, the value is "
                    + recordToRead);
        } else {
            recordToRead = new String();
            for (int i = 0; i < getItem().length; i++) {
                if (uri.getPath().contains(getItem()[i])) {
                    if ((String) map.get(getItem()[i]) != null) {
                        if (recordToRead.length() == 0) {
                            recordToRead = Settings.System.getString(mContext.getContentResolver(),
                                    (String) map.get(getItem()[i]));
                        }
                    } else {
                        recordToRead += getContentValue()[i];
                    }
                    break;
                }
            }
            DmService.mCCStoredParams.put(uriKey, recordToRead);
            Log.d(TAG.NodeIOHandler, "put valueToRead to mCCStoredParams, the value is "
                    + recordToRead);
        }
        if (TextUtils.isEmpty(recordToRead)) {
            return 0;
        } else {
            byte[] temp = recordToRead.getBytes();
            if (arg1 == null) {
                return temp.length;
            }
            int numberRead = 0;
            for (; numberRead < arg1.length - arg0; numberRead++) {
                if (numberRead < temp.length) {
                    arg1[numberRead] = temp[arg0 + numberRead];
                } else {
                    break;
                }
            }
            if (numberRead < arg1.length - arg0) {
                recordToRead = null;
            } else if (numberRead < temp.length) {
                recordToRead = recordToRead.substring(arg1.length - arg0);
            }
            return numberRead;
        }
    }

    public void write(int arg0, byte[] arg1, int arg2) throws MdmException {

        Log.i(TAG.NodeIOHandler, "uri: " + uri.getPath());
        Log.i(TAG.NodeIOHandler, "arg1: " + new String(arg1));
        Log.i(TAG.NodeIOHandler, "arg0: " + arg0);
        Log.i(TAG.NodeIOHandler, "arg2: " + arg2);

        recordToWrite = new String(arg1);
        if (recordToWrite.length() == arg2) {
            for (int i = 0; i < getItem().length; i++) {
                if (uri.getPath().contains(getItem()[i])) {
                    ContentValues values = new ContentValues();
                    if ((String) map.get(getItem()[i]) != null) {
                        if (!specificHandlingForWrite(recordToWrite, values, getItem()[i])) {
                            Settings.System.putString(mContext.getContentResolver(),
                                    (String) map.get(getItem()[i]),
                                    recordToWrite);
                        }
                    }
                    recordToWrite = null;
                    break;
                }
            }
        }
    }

    @Override
    protected String[] getContentValue() {
        return contentValue;
    }

    @Override
    protected String[] getItem() {
        return item;
    }

    @Override
    protected String[] getProjection() {
        return projection;
    }

    @Override
    protected Uri getTableToBeQueryed() {
        return table;
    }

}
