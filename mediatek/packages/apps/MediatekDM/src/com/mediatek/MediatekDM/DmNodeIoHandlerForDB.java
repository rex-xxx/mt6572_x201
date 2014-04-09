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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.NodeIoHandler;

import java.util.HashMap;
import java.util.Map;

public abstract class DmNodeIoHandlerForDB implements NodeIoHandler {

    protected Context mContext = null;
    protected Uri uri = null;
    protected String mccmnc = null;
    protected String recordToWrite = null;
    protected String recordToRead = null;

    protected Map map = new HashMap<String, String>();

    public int read(int arg0, byte[] arg1) throws MdmException {
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
            for (int i = 0; i < getItem().length; i++) {
                if (uri.getPath().contains(getItem()[i])) {
                    if ((String) map.get(getItem()[i]) != null) {
                        if (specificHandlingForRead(getItem()[i]) != null) {
                            recordToRead += specificHandlingForRead(getItem()[i]);
                            Log.d(TAG.NodeIOHandler, "Special read result = " + recordToRead);
                        }
                        if (recordToRead.length() == 0) {
                            String sqlString = buildSqlString(mccmnc);
                            Cursor cur = null;

                            Log.d(TAG.NodeIOHandler, "Normal read, sqlString = " + sqlString);

                            try {
                                cur = mContext.getContentResolver().query(getTableToBeQueryed(),
                                        getProjection(),
                                        sqlString, null, null);
                                if (cur != null && cur.moveToFirst()) {
                                    int col = cur.getColumnIndex((String) map.get(getItem()[i]));
                                    recordToRead = cur.getString(col);
                                }
                            } catch (Exception e) {
                                throw new MdmException(0xFFFF);
                            } finally {
                                if (cur != null) {
                                    cur.close();
                                }
                            }
                        }
                    } else {
                        recordToRead += getContentValue()[i];
                    }
                    break;
                }
            }
            DmService.mCCStoredParams.put(uriPath, recordToRead);
            Log.d(TAG.NodeIOHandler, "put valueToRead to mCCStoredParams, the value is "
                    + recordToRead);
        }
        Log.v(TAG.NodeIOHandler, "recordToRead = " + recordToRead);
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

        // if (recordToWrite == null) {
        // recordToWrite = new String();
        // }
        recordToWrite = new String(arg1);
        if (recordToWrite.length() == arg2) {
            // Modify: added************start
            String uriPath = uri.getPath();
            String uriPathName = "";
            if (uriPath != null) {
                int indexOfSlash = uriPath.lastIndexOf("/");
                if (indexOfSlash == -1) {
                    Log.e("DmService", "index of / of uri is null");
                    return;
                }
                uriPathName = uriPath.substring(indexOfSlash + 1);
            } else {
                Log.e("DmService", "uri.getPath is null!");
                return;
            }

            // Modify: added************end

            for (int i = 0; i < getItem().length; i++) {
                if (uriPathName.equals(getItem()[i])) {
                    ContentValues values = new ContentValues();
                    if ((String) map.get(getItem()[i]) != null) {
                        if (!specificHandlingForWrite(recordToWrite, values, getItem()[i])) {
                            values.put((String) map.get(getItem()[i]), recordToWrite);
                        }
                    } else {
                        recordToWrite = null;
                        break;
                    }
                    if (mContext.getContentResolver().update(getTableToBeQueryed(), values,
                            buildSqlString(mccmnc), null) == 0) {
                        if (getInsertUri() == null) {
                            setInsertUri(mContext.getContentResolver().insert(
                                    getTableToBeQueryed(), values));
                        } else {
                            if (mContext != null) {
                                mContext.getContentResolver().update(getInsertUri(), values,
                                        null, null);
                            }
                        }
                    }
                    recordToWrite = null;
                    break; // modify: added
                }
            }
        }
    }

    protected boolean specificHandlingForWrite(String str, ContentValues cv, String item) {
        return false;
    }

    protected String specificHandlingForRead(String item) {
        return null;
    }

    protected void setInsertUri(Uri uri) {
    };

    protected Uri getInsertUri() {
        return null;
    };

    protected String buildSqlString(String mccMnc) {
        return null;
    };

    protected abstract String[] getItem();

    protected abstract String[] getProjection();

    protected abstract Uri getTableToBeQueryed();

    protected abstract String[] getContentValue();
}
