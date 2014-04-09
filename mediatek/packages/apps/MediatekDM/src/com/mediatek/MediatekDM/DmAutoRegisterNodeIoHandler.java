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
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.NodeIoHandler;
import com.mediatek.MediatekDM.xml.DmXMLParser;

import java.util.HashMap;
import java.util.Map;

public class DmAutoRegisterNodeIoHandler implements NodeIoHandler {

    private Context mContext = null;
    private Uri uri = null;
    private String mccmnc = null;
    private String recordToWrite = null;
    private DmXMLParser configReader = null; // modify:added

    // private final String PACKAGENAME =
    // this.getClass().getPackage().getName();
    private String[] item = {
            "AutoRegSMSC", "AutoRegSMSport"
    };
    private String[] contentValue = {
            null, null
    };
    String[] projection = {
            "AutoRegSMSC", "AutoRegSMSport"
    };

    private final String PREFS_NAME = "SmsRegSelf";

    private Map map = new HashMap<String, String>();

    public DmAutoRegisterNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        // Log.w(PACKAGENAME, "constructed");
        mContext = ctx;
        uri = treeUri;
        mccmnc = mccMnc;
        for (int i = 0; i < item.length; i++) {
            map.put(item[i], projection[i]);
        }
    }

    public int read(int arg0, byte[] arg1) throws MdmException {
        // Log.w(PACKAGENAME, "read called");
        String recordToRead = null;
        String uriPath = uri.getPath();
        Log.i(TAG.NodeIOHandler, "uri: " + uriPath);
        Log.i(TAG.NodeIOHandler, "arg0: " + arg0);

        if (DmService.mCCStoredParams.containsKey(uriPath)) {
            recordToRead = DmService.mCCStoredParams.get(uriPath);
            Log.d(TAG.NodeIOHandler, "get valueToRead from mCCStoredParams, the value is "
                    + recordToRead);
        } else {
            recordToRead = new String();
            for (int i = 0; i < item.length; i++) {
                if (uri.getPath().contains(item[i])) {
                    if ((String) map.get(item[i]) != null) {

                        // modify:added start
                        String defaultValue = "";
                        if (item[i].equals("AutoRegSMSC")) {
                            defaultValue = getDefaultValue("smsnumber");
                        } else if (item[i].equals("AutoRegSMSport")) {
                            defaultValue = getDefaultValue("smsport");
                            defaultValue = Integer.toHexString(new Integer(defaultValue));
                        }
                        // modify:added end

                        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 2);
                        recordToRead += settings.getString(item[i], defaultValue); // modify:0-->defaultValue
                    } else {
                        recordToRead += contentValue[i];
                    }
                    break;
                }
                DmService.mCCStoredParams.put(uriPath, recordToRead);
                Log.d(TAG.NodeIOHandler, "put valueToRead to mCCStoredParams, the value is "
                        + recordToRead);
            }
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
        // Log.w(PACKAGENAME, "write was called");
        Log.i(TAG.NodeIOHandler, "uri: " + uri.getPath());
        Log.i(TAG.NodeIOHandler, "arg1: " + new String(arg1));
        Log.i(TAG.NodeIOHandler, "arg0: " + arg0);
        Log.i(TAG.NodeIOHandler, "arg2: " + arg2);

        // if (recordToWrite == null) {
        // recordToWrite = new String();
        // }
        recordToWrite = new String(arg1);
        if (recordToWrite.length() == arg2) {
            for (int i = 0; i < item.length; i++) {
                if (uri.getPath().contains(item[i])) {
                    SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(item[i], recordToWrite);
                    editor.commit();

                }
            }
        }
    }

    private String buildSqlString(String mccMnc) {
        String mcc = mccMnc.substring(0, 3);
        String mnc = mccMnc.substring(3);
        // for cmcc
        if (mcc.equals("460") && (mnc.equals("00") || mnc.equals("02") || mnc.equals("07"))) {
            return "mcc='460' AND type='mms' AND (mnc='00' OR mnc='02' OR mnc='07')";
        } else if (mcc.equals("460") && mnc.equals("01")) {
            return "mcc='460' AND type='mms' AND mnc='01'";
        }
        // need to add some code for other operator
        return null;
    }

    private String getDefaultValue(String key) {
        if (configReader == null) {
            configReader = new DmXMLParser(DmConst.Path.pathSmsRegConfig);
        }
        String value = configReader.getValByTagName(key);
        return value;
    }

}
