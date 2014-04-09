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
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.NodeIoHandler;
import com.scan.pimcontentprovider.PimContentProvider;

import java.util.List;

public class DmPIMNodeIoHandler implements NodeIoHandler {
    private static final String TAG = "DmPIMNodeIoHandler";
    private String[] mItem = {
            "DefProfile", "ConnProfile", "Addr", "AddressBookURI"
    };
    private static final String databaseUri_DefProfile = "content://com.scan.cmpim/DevDetail/Ext/Conf/DataSync/DefaultProfile";
    private static final String databaseUri_ConnProfile = "content://com.scan.cmpim/DevDetail/Ext/Conf/DataSync/SyncML/Profiles/CMCC/Conn";
    private static final String databaseUri_Addr = "content://com.scan.cmpim/DevDetail/Ext/Conf/DataSync/SyncML/Profiles/CMCC/URL";
    private static final String databaseUri_Contact = "content://com.scan.cmpim/DevDetail/Ext/Conf/DataSync/SyncML/Profiles/CMCC/DataStores/contact/ServerPath";
    protected Context mContext;
    protected Uri mUri;
    private String mNode;

    public DmPIMNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        mContext = ctx;
        mUri = treeUri;
        List<String> uList = treeUri.getPathSegments();
        mNode = uList.get(uList.size() - 1);
    }

    public int read(int arg0, byte[] arg1) throws MdmException {
        String recordToRead = null;
        String uriPath = mUri.getPath();
        Log.i(TAG, "uri: " + uriPath);
        if (DmService.mCCStoredParams.containsKey(uriPath)) {
            recordToRead = DmService.mCCStoredParams.get(uriPath);
            Log.d(TAG, "get valueToRead from mCCStoredParams, the value is " + recordToRead);
        } else {
            if (mNode.equalsIgnoreCase(mItem[0])) {
                Log.d(TAG, "read uri is " + databaseUri_DefProfile);
                Uri uri = Uri.parse(databaseUri_DefProfile);
                try {
                    recordToRead = PimContentProvider.ipth_HALGetDefaultPIMProfile(uri, mContext);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new MdmException(0xFFFE);
                }
            } else if (mNode.equalsIgnoreCase(mItem[1])) {
                Log.d(TAG, "read uri is " + databaseUri_ConnProfile);
                Uri uri = Uri.parse(databaseUri_ConnProfile);
                try {
                    recordToRead = PimContentProvider.ipth_HALGetCMCCPIMConnectionProfile(
                            uri, mContext);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new MdmException(0xFFFE);
                }
            } else if (mNode.equalsIgnoreCase(mItem[2])) {
                Log.d(TAG, "read uri is " + databaseUri_Addr);
                Uri uri = Uri.parse(databaseUri_Addr);
                try {
                    recordToRead = PimContentProvider.ipth_HALGetCMCCPIMServerAddr(uri, mContext);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new MdmException(0xFFFE);
                }
            } else if (mNode.equalsIgnoreCase(mItem[3])) {
                Log.d(TAG, "read uri is " + databaseUri_Contact);
                Uri uri = Uri.parse(databaseUri_Contact);
                try {
                    recordToRead = PimContentProvider.ipth_HALGetCMCCPIMContactPath(uri, mContext);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new MdmException(0xFFFE);
                }
            }
            DmService.mCCStoredParams.put(uriPath, recordToRead);
            Log.d(TAG, "put valueToRead to mCCStoredParams, the value is " + recordToRead);
        }

        if (TextUtils.isEmpty(recordToRead)) {
            return 0;
        }
        Log.d(TAG, "read result is " + recordToRead);
        if (arg1 == null) {
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
        String value = new String(arg1);

        if (mNode.equalsIgnoreCase(mItem[0])) {
            Log.d(TAG, "write uri is " + databaseUri_DefProfile);
            Uri uri = Uri.parse(databaseUri_DefProfile);
            try {
                PimContentProvider.ipth_HALSetDefaultPIMProfile(uri, mContext, value);
            } catch (Exception e) {
                e.printStackTrace();
                throw new MdmException(0xFFFE);
            }
        } else if (mNode.equalsIgnoreCase(mItem[1])) {
            Log.d(TAG, "write uri is " + databaseUri_ConnProfile);
            Uri uri = Uri.parse(databaseUri_ConnProfile);
            try {
                PimContentProvider.ipth_HALSetCMCCPIMConnectionProfile(uri, mContext, value);
            } catch (Exception e) {
                e.printStackTrace();
                throw new MdmException(0xFFFE);
            }
        } else if (mNode.equalsIgnoreCase(mItem[2])) {
            Log.d(TAG, "write uri is " + databaseUri_Addr);
            Uri uri = Uri.parse(databaseUri_Addr);
            try {
                PimContentProvider.ipth_HALSetCMCCPIMServerAddr(uri, mContext, value);
            } catch (Exception e) {
                e.printStackTrace();
                throw new MdmException(0xFFFE);
            }
        } else if (mNode.equalsIgnoreCase(mItem[3])) {
            Log.d(TAG, "write uri is " + databaseUri_Contact);
            Uri uri = Uri.parse(databaseUri_Contact);
            try {
                PimContentProvider.ipth_HALSetCMCCPIMContactPath(uri, mContext, value);
            } catch (Exception e) {
                e.printStackTrace();
                throw new MdmException(0xFFFE);
            }
        }
    }

}
