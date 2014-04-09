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
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.ext.MTKOptions;

public class DmMMSNodeIoHandler extends DmNodeIoHandlerForDB {

    private String[] item = {
            "Default", "MSCCenter"
    };
    private String[] contentValue = {
            null, null
    };
    // In buildsystem
    // String[] projection = {Telephony.Carriers.MMSC};
    // Uri table = Telephony.Carriers.CONTENT_URI;

    // in eclipse
    String[] projection = {
            "name", "mmsc"
    };

    int registeredSimId;
    Uri table = Uri.parse("content://telephony/carriers");
    Uri tableGemini1 = Uri.parse("content://telephony/carriers_sim1");
    Uri tableGemini2 = Uri.parse("content://telephony/carriers_sim2");

    public DmMMSNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        // Log.w(PACKAGENAME, "constructed");
        mContext = ctx;
        uri = treeUri;
        mccmnc = mccMnc;
        for (int i = 0; i < item.length; i++) {
            map.put(item[i], projection[i]);
        }

        registeredSimId = DmCommonFunction.getRegisteredSimId(ctx);
    }

    /*
     * public int read(int arg0, byte[] arg1) throws MdmException {
     * Log.w(PACKAGENAME, "read called"); Log.w("haman", "uri: " +
     * uri.getPath()); Log.w("haman", "arg0: " + arg0); if (recordToRead ==
     * null) { recordToRead = new String(); for (int i = 0; i < item.length;
     * i++) { if (uri.getPath().contains(item[i])) { if((String)map.get(item[i])
     * != null){ Cursor cur = mContext.getContentResolver().query(table,
     * projection, buildSqlString(mccmnc), null, null); int col =
     * cur.getColumnIndex((String) map.get(item[i])); cur.moveToFirst();
     * recordToRead = cur.getString(col); cur.close(); }else{ recordToRead +=
     * contentValue[i]; } break; } } } if(recordToRead == null){ throw new
     * MdmException(0); }else{ byte[] temp = recordToRead.getBytes(); if (arg1
     * == null) { return temp.length; } int numberRead = 0; for (; numberRead <
     * arg1.length - arg0; numberRead++) { if (numberRead < temp.length) {
     * arg1[numberRead] = temp[arg0 + numberRead]; }else{ break; } } if
     * (numberRead < arg1.length - arg0) { recordToRead = null; }else
     * if(numberRead < temp.length){ recordToRead =
     * recordToRead.substring(arg1.length - arg0); } return numberRead; } }
     * public void write(int arg0, byte[] arg1, int arg2) throws MdmException {
     * Log.w(PACKAGENAME, "write was called"); Log.w("haman", "uri: " +
     * uri.getPath()); Log.w("haman", "arg1: " + new String(arg1));
     * Log.w("haman", "arg0: " + arg0); Log.w("haman", "arg2: " + arg2); if
     * (recordToWrite == null) { recordToWrite = new String(); } recordToWrite
     * += new String(arg1); if (recordToWrite.length() == arg2) { for (int i =
     * 0; i < item.length; i++) { if (uri.getPath().contains(item[i])) {
     * ContentValues values = new ContentValues(); if (item[i].equals(item[2]))
     * { values.put(projection[0], recordToWrite); } else { recordToWrite =
     * null; break; // throw new MdmException(0); }
     * mContext.getContentResolver().update(table, values,
     * buildSqlString(mccmnc), null); recordToWrite = null; } } } }
     */
    protected String buildSqlString(String mccMnc) {
        String mcc = mccMnc.substring(0, 3);
        String mnc = mccMnc.substring(3);
        // for cmcc
        if (mcc.equals("460") && (mnc.equals("00") || mnc.equals("02") || mnc.equals("07"))) {
            return "mcc='460' AND type='mms' AND (mnc='" + mnc + "') AND (sourcetype='0')";
        } else if (mcc.equals("460") && mnc.equals("01")) {
            return "mcc='460' AND type='mms' AND mnc='01' AND (sourcetype='0')";
        }
        // need to add some code for other operator
        return null;
    }

    @Override
    protected String[] getContentValue() {
        return contentValue;
    }

    @Override
    protected String[] getItem() {
        // TODO Auto-generated method stub
        return item;
    }

    @Override
    protected String[] getProjection() {
        // TODO Auto-generated method stub
        return projection;
    }

    @Override
    protected Uri getTableToBeQueryed() {
        // TODO Auto-generated method stub
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
