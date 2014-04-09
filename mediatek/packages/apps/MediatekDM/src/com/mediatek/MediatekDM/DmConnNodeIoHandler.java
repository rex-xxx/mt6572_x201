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
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.ext.MTKOptions;
import com.mediatek.MediatekDM.mdm.MdmException;

public class DmConnNodeIoHandler extends DmNodeIoHandlerForDB {
    private String[] item = {
            "Cmnet/Name", "Cmnet/apn", "Cmwap/Name", "Cmwap/apn", "gprs/Addr", "PortNbr",
            "csd0/Addr", "apn",
            "ProxyAddr", "ProxyPort"
    };
    private String[] contentValue = {
            null, null, null, null, null, null, null
    };
    String[] projection = {
            "apn", "name", "name", "apn", "proxy", "port", "csdnum", "apn", "proxy", "port"
    };

    Uri table = Uri.parse("content://telephony/carriers");
    Uri tableGemini1 = Uri.parse("content://telephony/carriers_sim1");
    Uri tableGemini2 = Uri.parse("content://telephony/carriers_sim2");
    int registeredSimId;

    public DmConnNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        Log.i(TAG.NodeIOHandler, "DmConnNodeIoHandler constructed");

        mContext = ctx;
        uri = treeUri;
        mccmnc = mccMnc;
        for (int i = 0; i < item.length; i++) {
            map.put(item[i], projection[i]);
        }
        registeredSimId = DmCommonFunction.getRegisteredSimId(ctx);
        if (registeredSimId == -1) {
            Log.e(TAG.NodeIOHandler, "No registered SIM ID found");
        }
    }

    public void write(int arg0, byte[] arg1, int arg2) throws MdmException {

        if (recordToWrite == null) {
            recordToWrite = new String();
        }
        recordToWrite = new String(arg1);
        if (recordToWrite.length() == arg2) {
            String uriPath = uri.getPath();
            String uriPathName = "";
            if (uriPath != null) {
                if (uriPath.indexOf("Conn") != -1) {
                    int indexOfConn = uriPath.lastIndexOf("Conn");
                    if (indexOfConn == -1) {
                        Log.e("DmService", "index of Conn of uri is null");
                        return;
                    }
                    uriPathName = uriPath.substring(indexOfConn + 5);
                } else {
                    int indexOfSlash = uriPath.lastIndexOf("/");
                    if (indexOfSlash == -1) {
                        Log.e("DmService", "index of / of uri is null");
                        return;
                    }
                    uriPathName = uriPath.substring(indexOfSlash + 1);
                }

            } else {
                Log.e("DmService", "uri.getPath is null!");
                return;
            }

            Log.i(TAG.NodeIOHandler, "uriPathName = " + uriPathName + " value = " + recordToWrite);

            for (int i = 0; i < item.length; i++) {
                if (uriPathName.equals(item[i])) {
                    ContentValues values = new ContentValues();
                    if ((String) map.get(item[i]) != null) {
                        values.put((String) map.get(item[i]), recordToWrite);
                        mContext.getContentResolver().update(getTableToBeQueryed(), values,
                                buildSqlString(mccmnc),
                                null);
                    }
                    recordToWrite = null;
                    break;
                }
            }
        }
    }

    protected String buildSqlString(String mccMnc) {
        String mcc = mccMnc.substring(0, 3);
        String mnc = mccMnc.substring(3);
        // for cmcc
        if (mcc.equals("460") && (mnc.equals("00") || mnc.equals("02") || mnc.equals("07"))) {
            if (uri.getPath().contains(item[0]) || uri.getPath().contains(item[1])) {
                return "mcc='460' AND type='default,supl,net' AND (mnc='" + mnc + "')";
            } else {
                return "mcc='460' AND (type = '' OR type is null) AND (sourcetype='0') AND (mnc='"
                        + mnc + "')";
            }
        } else if (mcc.equalsIgnoreCase("460") && mnc.equals("01")) {
            return "mcc='460' AND (type is null) AND mnc='01' AND (sourcetype='0')";
        }
        // need to add some code for other operator
        return "mcc='" + mcc + "' AND mnc='" + mnc + "'";
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
        if (MTKOptions.MTK_GEMINI_SUPPORT == true) {
            if (registeredSimId == 0) {
                Log.d(TAG.NodeIOHandler, "[GEMINI]getTableToBeQueryed() returns " + tableGemini1);
                return tableGemini1;
            } else if (registeredSimId == 1) {
                Log.d(TAG.NodeIOHandler, "[GEMINI]getTableToBeQueryed() returns " + tableGemini2);
                return tableGemini2;
            } else {
                Log.e(TAG.NodeIOHandler, "[GEMINI]getTableToBeQueryed() returns null");
                return null;
            }
        } else {
            Log.d(TAG.NodeIOHandler, "getTableToBeQueryed() returns " + table);
            return table;
        }
    }

}
