/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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
 */

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

public class DMOperatorFile {
    private static final String TAG = "DMOpFile";
    public int mNumberOfDMOpInfo; // Number of entries
    public ArrayList<DMOperatorInfo> mDMOperatorInfoArray; //
    private boolean isDMOpFileLoaded;
    private XmlPullParser mDMOpFileParser;
    private Resources r;
    private static DMOperatorFile instance = null;

    private DMOperatorFile() {
        Log.d(TAG, "[DM create DMOperatorFile");
        this.mNumberOfDMOpInfo = 0;
        this.mDMOperatorInfoArray = new ArrayList<DMOperatorInfo>();
        this.isDMOpFileLoaded = false;
        this.mDMOpFileParser = null;
        this.r = null;
    }

    public static DMOperatorFile getInstance() {
        if (instance == null) {
            instance = new DMOperatorFile();
        }
        return instance;
    }

    public void initFromRes(Context mContext) {
        Log.d(TAG, "loadDMOperatorFileFromXml: open normal file");
        r = mContext.getResources();
        // TEMP mDMOpFileParser =
        // r.getXml(com.mediatek.internal.R.xml.dm_operator_info);
        try {
            if (mDMOpFileParser != null) {
                XmlUtils.beginDocument(mDMOpFileParser, "DMOperatorFile");
            } else {
                Log.d(TAG, "Fail: mDMOpFileParser is null");
                return;
            }
            this.mNumberOfDMOpInfo = Integer.parseInt(mDMOpFileParser.getAttributeValue(null,
                    "NumberOfDMOpInfo"));
            boolean MTK_DM_REGISTER_SUPPORT = mDMOpFileParser.getAttributeValue(null,
                    "DMOpInfoFilterSupport").equals("on");
            if (!MTK_DM_REGISTER_SUPPORT) {
                Log.d(TAG, "DMOpInfoFilterSupport : off");
                isDMOpFileLoaded = false;
            } else {
                Log.d(TAG, "DMOpInfoFilterSupport : on");
                int parsedDMOpInfo = 0;
                while (true) {
                    XmlUtils.nextElement(mDMOpFileParser);
                    String name = mDMOpFileParser.getName();
                    if (name == null) {
                        if (parsedDMOpInfo != this.mNumberOfDMOpInfo)
                            Log.e(TAG, "Error Parsing DMOperator file: " + this.mNumberOfDMOpInfo
                                    + " defined, " + parsedDMOpInfo + " parsed!");
                        break;
                    } else if (name.equals("DMOperatorInfo")) {
                        String opName = mDMOpFileParser.getAttributeValue(null, "OpName");
                        String DMNum = mDMOpFileParser.getAttributeValue(null, "DMNum");
                        int DMPort = Integer.parseInt(mDMOpFileParser.getAttributeValue(null,
                                "DMPort"));
                        parsedDMOpInfo++;
                        this.mDMOperatorInfoArray.add(new DMOperatorInfo(opName, DMNum, DMPort));
                    }
                }
                Log.d(TAG, "loadDMOpFileFromXml: DMOpFile parsing successful, file loaded");
                isDMOpFileLoaded = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception while loading DMOperatorInfo file.", e);
        } finally {
            if (mDMOpFileParser instanceof XmlResourceParser) {
                ((XmlResourceParser) mDMOpFileParser).close();
            }
        }
    }

    public boolean searchMatchOp(String DMNum, int DMPort) {
        if (!isDMOpFileLoaded) {
            Log.d(TAG, "DM Operator File hasn't been load From xml");
            return false;
        } else {
            Log.d(TAG, "DMNum : " + DMNum + " DMport : " + DMPort);
            if (DMNum == null) {
                return false;
            }
            for (DMOperatorInfo EachOpInfo : this.mDMOperatorInfoArray) {
                if (DMPort == EachOpInfo.mDMport &&
                        DMNum.equals(EachOpInfo.mDMnum)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void dump() {
        Log.d(TAG, "[xj dump operator info");
        for (DMOperatorInfo e : mDMOperatorInfoArray) {
            Log.d(TAG, "operator info(port/num/name): "
                    + e.mDMport + "/" + e.mDMnum + "/" + e.mOpname);
        }
    }
}
