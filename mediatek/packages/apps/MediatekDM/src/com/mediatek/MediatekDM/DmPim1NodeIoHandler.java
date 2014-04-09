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
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.NodeIoHandler;

import java.util.HashMap;
import java.util.Map;

public class DmPim1NodeIoHandler implements NodeIoHandler {

    private String[] mItem = {
            "Addr", "ConnProfile", "AddressBookURI", "CalendarURI"
    };
    // private String mBase =
    // "content://com.scan.cmpim/DevDetail/Ext/Conf/DataSync/";
    private String[] mProjection = {
            "PIM_SERVER", "PIM_APN", "PIM_CONTACT", "PIM_CALENDAR"
    };
    protected Context mContext;
    protected Uri mUri;
    protected Map<String, String> mMap = new HashMap<String, String>();
    protected String recordToRead = null;

    public DmPim1NodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {

        mContext = ctx;
        mUri = treeUri;
        for (int i = 0; i < mItem.length; i++) {
            mMap.put(mItem[i], mProjection[i]);
        }
    }

    public int read(int arg0, byte[] arg1) throws MdmException {
        String recordToRead = null;
        String uriPath = mUri.getPath();
        Log.i(TAG.NodeIOHandler, "uri: " + uriPath);
        Log.i(TAG.NodeIOHandler, "arg0: " + arg0);
        if (DmService.mCCStoredParams.containsKey(uriPath)) {
            recordToRead = DmService.mCCStoredParams.get(uriPath);
            Log.d(TAG.NodeIOHandler, "get valueToRead from mCCStoredParams, the value is "
                    + recordToRead);
        } else {
            for (int i = 0; i < mItem.length; i++) {
                if (mUri.getPath().contains(mItem[i])) {
                    if ((String) mMap.get(mItem[i]) != null) {
                        String dbKey = mMap.get(mItem[i]);
                        try {
                            recordToRead = Settings.System.getString(mContext.getContentResolver(),
                                    dbKey);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new MdmException(0xFFFE);
                        }
                        break;
                    }
                }
            }
            DmService.mCCStoredParams.put(uriPath, recordToRead);
            Log.d(TAG.NodeIOHandler, "put valueToRead to mCCStoredParams, the value is "
                    + recordToRead);
        }

        if (TextUtils.isEmpty(recordToRead)) {
            return 0;
        } else if (arg1 == null) {
            return recordToRead.length();
        } else {
            byte[] temp = recordToRead.getBytes();
            for (int j = 0; j < recordToRead.length(); j++) {
                arg1[j] = temp[j];
            }
            return recordToRead.length();
        }
    }

    public void write(int arg0, byte[] arg1, int arg2) throws MdmException {
        for (int i = 0; i < mItem.length; i++) {
            if (mUri.getPath().contains(mItem[i])) {
                String dbKey = mMap.get(mItem[i]);
                String value = new String(arg1);
                // if((String)mMap.get(mItem[i]) != null){
                try {
                    Settings.System.putString(mContext.getContentResolver(), dbKey, value);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new MdmException(0xFFFE);
                }
                // }
            }
        }

    }

}
