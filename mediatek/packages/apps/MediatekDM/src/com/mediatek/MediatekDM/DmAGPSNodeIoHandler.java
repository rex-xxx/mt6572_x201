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
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.NodeIoHandler;
import com.mediatek.common.agps.MtkAgpsManager;
import com.mediatek.common.agps.MtkAgpsProfile;

import java.util.HashMap;
import java.util.Map;

public class DmAGPSNodeIoHandler implements NodeIoHandler {

    protected Context mContext = null;
    protected Uri uri = null;
    protected String mccmnc = null;
    protected String valueToRead = null;
    protected Map<String, String> map = new HashMap<String, String>();

    private String[] item = {
            "IAPID", "ProviderID", "Name", "PrefConRef", "ToConRef", "ConRef", "SLP", "port"
    };

    private String[] projection = {
            "appId", "providerId", "name", "defaultApn", "optionApn", "optionApn2", "addr",
            "addrType"
    };

    public DmAGPSNodeIoHandler(Context ctx, Uri treeUri, String profile) {
        Log.i(TAG.NodeIOHandler, "DmAGPSNodeIoHandler constructed");
        mContext = ctx;
        mccmnc = profile;
        uri = treeUri;

        for (int i = 0; i < item.length; i++) {
            map.put(item[i], projection[i]);
        }
    }

    @Override
    public int read(int arg0, byte[] arg1) throws MdmException {

        if (uri == null) {
            throw new MdmException("AGPS read URI is null!");
        }

        String valueToRead = null;
        String uriPath = uri.getPath();
        Log.i(TAG.NodeIOHandler, "uri: " + uriPath);
        Log.i(TAG.NodeIOHandler, "arg0: " + arg0);
        if (DmService.mCCStoredParams.containsKey(uriPath)) {
            valueToRead = DmService.mCCStoredParams.get(uriPath);
            Log.d(TAG.NodeIOHandler, "get valueToRead from mCCStoredParams, the value is "
                    + valueToRead);
        } else {
            int leafIndex = uriPath.lastIndexOf("/");
            if (leafIndex == -1) {
                throw new MdmException("AGPS read URI is not valid, has no '/'!");
            }
            String leafValue = uriPath.substring(leafIndex + 1);

            String itemString = null;
            for (int i = 0; i < item.length; i++) {
                if (leafValue.equals(item[i])) {
                    itemString = item[i];
                    break;
                }
            }

            if (itemString == null) {
                return 0;
            }

            String profileString = map.get(itemString);
            if (profileString == null) {
                return 0;
            }

            MtkAgpsManager mAgpsMgr = (MtkAgpsManager) mContext
                    .getSystemService(Context.MTK_AGPS_SERVICE);
            MtkAgpsProfile agpsProfile = mAgpsMgr.getProfile();

            if (profileString.equals("appId")) {
                valueToRead = agpsProfile.appId;
            } else if (profileString.equals("providerId")) {
                valueToRead = agpsProfile.providerId;
            } else if (profileString.equals("name")) {
                valueToRead = agpsProfile.name;
            } else if (profileString.equals("defaultApn")) {
                valueToRead = agpsProfile.defaultApn;
            } else if (profileString.equals("optionApn")) {
                valueToRead = agpsProfile.optionApn;
            } else if (profileString.equals("optionApn2")) {
                valueToRead = agpsProfile.optionApn2;
            } else if (profileString.equals("addr")) {
                valueToRead = agpsProfile.addr + ":" + agpsProfile.port;
            } else if (profileString.equals("addrType")) {
                valueToRead = agpsProfile.addrType;
            }
            DmService.mCCStoredParams.put(uriPath, valueToRead);
            Log.d(TAG.NodeIOHandler, "put valueToRead to mCCStoredParams, the value is "
                    + valueToRead);
        }
        if (TextUtils.isEmpty(valueToRead)) {
            return 0;
        } else {
            byte[] temp = valueToRead.getBytes();
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
                valueToRead = null;
            } else if (numberRead < temp.length) {
                valueToRead = valueToRead.substring(arg1.length - arg0);
            }
            return numberRead;
        }
    }

    @Override
    public void write(int arg0, byte[] arg1, int arg2) throws MdmException {

        Log.i(TAG.NodeIOHandler, "uri: " + uri.getPath());
        Log.i(TAG.NodeIOHandler, "arg1: " + new String(arg1));
        Log.i(TAG.NodeIOHandler, "arg0: " + arg0);
        Log.i(TAG.NodeIOHandler, "arg2: " + arg2);

        String valueToWrite = new String(arg1);

        if (valueToWrite.length() != arg2) {
            Log.e(TAG.NodeIOHandler, "AGPS: arg1's length is not equals with arg2, do nothing.");
            return;
        }

        String uriPath = uri.toString();
        int leafIndex = uriPath.lastIndexOf("/");
        if (leafIndex == -1) {
            throw new MdmException("AGPS read URI is not valid, has no '/'!");
        }
        String leafValue = uriPath.substring(leafIndex + 1);

        String itemString = null;
        for (int i = 0; i < item.length; i++) {
            if (leafValue.equals(item[i])) {
                itemString = item[i];
                break;
            }
        }

        if (itemString == null) {
            Log.e(TAG.NodeIOHandler, "AGPS: itemString is null, do nothing.");
            return;
        }

        String profileString = map.get(itemString);
        if (profileString == null) {
            Log.e(TAG.NodeIOHandler, "AGPS: profileString is null, do nothing.");
            return;
        }

        MtkAgpsManager mAgpsMgr = (MtkAgpsManager) mContext
                .getSystemService(Context.MTK_AGPS_SERVICE);
        MtkAgpsProfile agpsProfile = mAgpsMgr.getProfile();

        if (profileString.equals("appId")) {
            agpsProfile.appId = valueToWrite;
        } else if (profileString.equals("providerId")) {
            agpsProfile.providerId = valueToWrite;
        } else if (profileString.equals("name")) {
            agpsProfile.name = valueToWrite;
        } else if (profileString.equals("defaultApn")) {
            agpsProfile.defaultApn = valueToWrite;
        } else if (profileString.equals("optionApn")) {
            agpsProfile.optionApn = valueToWrite;
        } else if (profileString.equals("optionApn2")) {
            agpsProfile.optionApn2 = valueToWrite;
        } else if (profileString.equals("addr")) {

            int indexOfColon = valueToWrite.indexOf(":");
            if (indexOfColon == -1) {
                Log.i(TAG.NodeIOHandler, "AGPS:the record to write for addr"
                        + " have not a : or have not port");
                agpsProfile.addr = valueToWrite;
            } else {
                agpsProfile.addr = valueToWrite.substring(0, indexOfColon);
                try {
                    agpsProfile.port = Integer.valueOf(valueToWrite.substring(indexOfColon + 1));
                } catch (NumberFormatException nfe) {
                    Log.e(TAG.NodeIOHandler, "AGPS:NumberFormatException,"
                            + "the record to write for addr have an invalid port:"
                            + valueToWrite.substring(indexOfColon + 1));
                }
            }
        } else if (profileString.equals("addrType")) {
            agpsProfile.addrType = valueToWrite;
        }

        Log.d(TAG.NodeIOHandler, "AGPS: previous code " + agpsProfile.code);

        String mcc = mccmnc.substring(0, 3);
        String mnc = mccmnc.substring(3);
        // for cmcc
        if (mcc.equals("460") && (mnc.equals("00") || mnc.equals("02") || mnc.equals("07"))) {
            agpsProfile.code = mccmnc;
        } else if (mcc.equals("460") && mnc.equals("01")) {
            agpsProfile.code = mccmnc;
        }

        mAgpsMgr.setProfile(agpsProfile);
    }

}
