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

/* //device/content/providers/telephony/TelephonyProvider.java
 **
 ** Copyright 2006, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.mediatek.MediatekDM.conn;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.MediatekDM.DmCommonFunction;
import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.ext.MTKOptions;
import com.mediatek.MediatekDM.ext.MTKPhone;
import com.mediatek.MediatekDM.xml.DmXMLParser;
import com.mediatek.telephony.TelephonyManagerEx;

import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DmDatabase {
    public static final int GEMINI_SIM_1 = 0;
    public static final int GEMINI_SIM_2 = 1;

    private static final String DM_APNS_PATH = "/system/etc/dm/DmApnInfo.xml";

    private TelephonyManagerEx mTelephonyManager;

    private ContentResolver mContentResolver;
    private Cursor mCursor;
    private static final Uri dmUri = MTKPhone.CONTENT_URI_DM;
    private static final Uri dmUriGemini = MTKPhone.CONTENT_URI_DM_GEMINI;
    private static final String defaultProxyAddr = "10.0.0.172";
    private static final int defaultProxyPort = 80;

    /**
     * Information of apn for DM.
     */
    public static class DmApnInfo {
        public Integer id;
        public String name;
        public String numeric;
        public String mcc;
        public String mnc;
        public String apn;
        public String user;
        public String password;
        public String server;
        public String proxy;
        public String port;
        public String type;
    }

    private ArrayList<DmApnInfo> mApnInfo;
    private static int mRegisterSimId;
    private Context mContext;
    private Builder builder;
    private String mccMncRegistered = null;

    public DmDatabase(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mTelephonyManager = TelephonyManagerEx.getDefault();
    }

    public boolean updateDmServer(int simId, String serverAddr) {

        String mccMnc = null;
        if (MTKOptions.MTK_GEMINI_SUPPORT == true) {
            mccMnc = MTKPhone.getSimOperatorGemini(mTelephonyManager, simId);
        } else {
            mccMnc = mTelephonyManager.getNetworkOperator(0);
        }
        Log.i(TAG.Database, "The mccmnc is = " + mccMnc + " and the register mccmnc is "
                + mccMncRegistered);
        if (mccMnc.equals(mccMncRegistered) || serverAddr == null) {
            Log.e(TAG.Database, "It is not the right mccmnc or server address is null!");
            return false;
        }
        Log.i(TAG.Database, "The sim id from intent is = " + simId
                + " and the register sin id  is " + mRegisterSimId);
        if (simId != mRegisterSimId || serverAddr == null) {
            Log.e(TAG.Database,
                    "It is not the right sim card or server address is null!");
            return false;
        }
        if (mContentResolver == null) {
            Log.e(TAG.Database, "mContentResolver is null!!");
            return false;
        }

        Log.i(TAG.Database, "before update");
        ContentValues v = new ContentValues();
        v.put("server", serverAddr);
        if (MTKOptions.MTK_GEMINI_SUPPORT == true) {
            if (simId == GEMINI_SIM_1) {
                mContentResolver.update(dmUri, v, null, null);
            } else if (mRegisterSimId == GEMINI_SIM_2) {
                mContentResolver.update(dmUriGemini, v, null, null);
            }

        } else {
            mContentResolver.update(dmUri, v, null, null);
        }
        Log.i(TAG.Database, "Update server addr finished");
        return true;
    }

    public boolean DmApnReady(int simId) {
        if (simId != GEMINI_SIM_1 && simId != GEMINI_SIM_2) {
            Log.e(TAG.Database, "simId = [" + simId + "]is error! ");
            return false;
        }
        Log.i(TAG.Database, "Sim Id = " + simId);
        mRegisterSimId = simId;
        Cursor cursor = null;
        boolean ret = false;
        int count = 0;
        try {
            if (MTKOptions.MTK_GEMINI_SUPPORT == true) {
                if (simId == GEMINI_SIM_1) {
                    cursor = mContentResolver.query(dmUri, null, null, null, null);
                } else if (simId == GEMINI_SIM_2) {
                    cursor = mContentResolver.query(dmUriGemini, null, null, null, null);
                }
            } else {
                cursor = mContentResolver.query(dmUri, null, null, null, null);
            }

            if (cursor != null && cursor.getCount() > 0) {
                Log.w(TAG.Database,
                        "There are apn data in dm apn table, the record is " + cursor.getCount());
                return true;
            }
            count = cursor.getCount();
            Log.w(TAG.Database, "There is no data in dm apn table");
        } catch (Exception e) {
            // TODO: handle exception
            Log.e(TAG.Database, "Try to get data form dm apn table error!");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (count < 0) {
                Log.e(TAG.Database, "cursor count = " + ret);
                return false;
            }
        }
        getApnValuesFromConfFile();
        ret = InitDmApnTable(mApnInfo);
        if (ret == false) {
            Log.e(TAG.Database, "Init Apn table error!");
        }
        return ret;
    }

    // read config file to get the apn values
    private void getApnValuesFromConfFile() {
        // init mApnInfo
        Log.i(TAG.Database, "getApnValuesFromConfFile");
        TelephonyManagerEx teleMgr = TelephonyManagerEx.getDefault();

        String operatorName = null;
        if (MTKOptions.MTK_GEMINI_SUPPORT == true) {
            operatorName = MTKPhone.getSimOperatorNameGemini(teleMgr, mRegisterSimId);
        } else {
            operatorName = teleMgr.getSimOperatorName(0);
        }

        Log.i(TAG.Database, "getApnValuesFromConfFile():operatorName = " + operatorName);

        File dmApnFile = new File(DM_APNS_PATH);
        if (dmApnFile == null || (!dmApnFile.exists())) {
            Log.e(TAG.Database, "Apn file is not exists or dmApnFile is null");
            return;
        }
        DmXMLParser xmlParser = new DmXMLParser(DM_APNS_PATH);
        List<Node> nodeList = new ArrayList<Node>();
        xmlParser.getChildNode(nodeList, "dmapn");
        if (nodeList != null && nodeList.size() > 0) {
            Log.i(TAG.Database, "dmapn node list size = " + nodeList.size());
            operatorName = DmCommonFunction.getOperatorName();
            if (operatorName == null) {
                Log.e(TAG.Database, "Get operator name from config file is null");
                return;
            }

            Log.i(TAG.Database, "Operator  = " + operatorName);
            Node node = nodeList.get(0);
            List<Node> operatorList = new ArrayList<Node>();
            // xmlParser.getChildNode(node, operatorList, operatorName);
            xmlParser.getChildNode(node, operatorList, operatorName);
            int operatorSize = operatorList.size();
            Log.i(TAG.Database, "OperatorList size  =  " + operatorSize);
            mApnInfo = new ArrayList<DmApnInfo>(operatorSize);
            for (int i = 0; i < operatorSize; i++) {
                DmApnInfo mDmApnInfo = new DmApnInfo();
                Log.i(TAG.Database, "this is the [" + i + "] operator apn");
                Node operatorNode = operatorList.get(i);
                List<Node> operatorLeafNodeList = new ArrayList<Node>();
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "id");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList size  =  " + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node str id = " + nodeStr);
                    mDmApnInfo.id = Integer.parseInt(nodeStr);
                }

                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "name");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList name size  =  " + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node nodeStr name  = " + nodeStr);
                    mDmApnInfo.name = nodeStr;

                }
                // numberic
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "numeric");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList numberic size  =  " + operatorLeafSize);
                    String nodeStr = null;
                    nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node node numberic  = " + nodeStr);
                    mDmApnInfo.numeric = nodeStr;

                }
                // mcc
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "mcc");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList mcc size  =  " + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node node mcc  = " + nodeStr);
                    mDmApnInfo.mcc = nodeStr;

                }
                // mnc
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "mnc");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList mnc size  =  " + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node node mnc  = " + nodeStr);
                    mDmApnInfo.mnc = nodeStr;

                }

                // apn
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "apn");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList apn size  =  " + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node node apn  = " + nodeStr);
                    mDmApnInfo.apn = nodeStr;

                }

                // type
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "type");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList type size  =  " + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node type   = " + nodeStr);
                    mDmApnInfo.type = nodeStr;

                }

                // user
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "user");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList user size  =  " + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node user   = " + nodeStr);
                    mDmApnInfo.user = nodeStr;

                }
                // password
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "password");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList password size  =  " + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node password   = " + nodeStr);
                    mDmApnInfo.password = nodeStr;

                }
                // server
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "server");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList server size  =  " + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node server   = " + nodeStr);
                    mDmApnInfo.server = nodeStr;

                }
                // proxy
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "proxy");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList proxy size  =  " + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node proxy   = " + nodeStr);
                    mDmApnInfo.proxy = nodeStr;

                }

                // port
                xmlParser.getLeafNode(operatorNode, operatorLeafNodeList, "port");
                if (operatorLeafNodeList != null && operatorLeafNodeList.size() > 0) {
                    int operatorLeafSize = operatorLeafNodeList.size();
                    Log.i(TAG.Database, "OperatorLeafList port size  =  " + operatorLeafSize);
                    String nodeStr = operatorLeafNodeList.get(operatorLeafSize - 1).getFirstChild()
                            .getNodeValue();
                    Log.i(TAG.Database, "node port   = " + nodeStr);
                    mDmApnInfo.port = nodeStr;

                }

                Log.i(TAG.Database, "Before add to array mDmApnInfo[" + i + "] = " + mDmApnInfo.id);
                mApnInfo.add(mDmApnInfo);

            }// for(int i=0; i < operatorSize; i++)
        }
    }

    // init the table if need
    private boolean InitDmApnTable(ArrayList<DmApnInfo> apnInfo) {
        Log.i(TAG.Database, "Enter init Dm Apn Table");
        if (apnInfo == null || apnInfo.size() <= 0) {
            Log.e(TAG.Database, "Apn that read from apn configure file is null");
            return false;
        }

        int size = apnInfo.size();
        Log.i(TAG.Database, "apnInfo size = " + size);
        ArrayList<ContentValues> apnInfo_insert = new ArrayList<ContentValues>(size);

        for (int i = 0; i < size; i++) {
            Log.i(TAG.Database, "insert i = " + i);
            Log.i(TAG.Database, "apnInfo.get(" + i + ").id = " + apnInfo.get(i).id);
            ContentValues v = new ContentValues();
            if (apnInfo.get(i) == null || apnInfo.get(i).id == null) {
                Log.w(TAG.Database, "before continue apnInfo.get.id " + apnInfo.get(i).id);
                continue;
            }

            v.put("_id", apnInfo.get(i).id);
            if (apnInfo.get(i).name != null) {
                v.put("name", apnInfo.get(i).name);
            }
            if (apnInfo.get(i).numeric != null) {
                v.put("numeric", apnInfo.get(i).numeric);
            }
            if (apnInfo.get(i).mcc != null) {
                v.put("mcc", apnInfo.get(i).mcc);
            }
            if (apnInfo.get(i).mnc != null) {
                v.put("mnc", apnInfo.get(i).mnc);
            }

            if (apnInfo.get(i).apn != null) {
                v.put("apn", apnInfo.get(i).apn);
            }
            if (apnInfo.get(i).type != null) {
                v.put("type", apnInfo.get(i).type);
            }

            if (apnInfo.get(i).user != null) {
                v.put("user", apnInfo.get(i).user);
            }
            if (apnInfo.get(i).server != null) {
                v.put("server", apnInfo.get(i).server);
            }
            if (apnInfo.get(i).password != null) {
                v.put("password", apnInfo.get(i).password);
            }
            if (apnInfo.get(i).proxy != null) {
                v.put("proxy", apnInfo.get(i).proxy);
            }
            if (apnInfo.get(i).port != null) {
                v.put("port", apnInfo.get(i).port);
            }

            apnInfo_insert.add(v);

        }
        int insertSize = apnInfo_insert.size();
        Log.i(TAG.Database, "insert size = " + insertSize);
        if (insertSize > 0) {

            if (MTKOptions.MTK_GEMINI_SUPPORT == true) {
                if (mRegisterSimId == GEMINI_SIM_1) {
                    builder = dmUri.buildUpon();
                } else if (mRegisterSimId == GEMINI_SIM_2) {
                    builder = dmUriGemini.buildUpon();
                }

            } else {
                builder = dmUri.buildUpon();
            }
            ContentValues[] values = new ContentValues[apnInfo_insert.size()];
            for (int i = 0; i < insertSize; i++) {
                Log.i(TAG.Database, "insert to values i = [" + i + "]");
                values[i] = apnInfo_insert.get(i);
            }
            // bulk insert
            mContentResolver.bulkInsert(builder.build(), values);
        }

        Log.i(TAG.Database, "Init Dm database finish");
        return true;
    }

    public String getApnProxyFromSettings() {
        // waiting for Yuhui's interface
        String proxyAddr = null;
        String simOperator = null;
        String mcc = null;
        String mnc = null;
        String where = null;
        int simId = mRegisterSimId;
        if (simId == -1) {
            Log.e(TAG.Database, "Get Register SIM ID error");
            return null;
        }

        try {
            Log.i(TAG.Database, "simId = " + simId);
            // where = "numeric =" + simOperator;
            if (MTKOptions.MTK_GEMINI_SUPPORT == true) {
                // for gemini
                simOperator = MTKPhone.getSimOperatorGemini(mTelephonyManager, simId);
                Log.i(TAG.Database, "simOperator numberic = " + simOperator);
                if (simOperator == null || simOperator.equals("")) {
                    Log.e(TAG.Database, "Get sim operator wrong ");
                    return defaultProxyAddr;
                }
                where = "numeric =" + simOperator;
                if (simId == 1) {
                    // sim card in the second is the right sim card
                    mCursor = mContentResolver.query(dmUriGemini, null, where, null, null);
                } else if (simId == 0) {
                    mCursor = mContentResolver.query(dmUri, null, where, null, null);
                } else {
                    Log.e(TAG.Database, "There is no right the sim card");
                    return null;
                }
            } else {
                simOperator = mTelephonyManager.getSimOperator(0);
                Log.i(TAG.Database, "simOperator numberic = " + simOperator);
                if (simOperator == null || simOperator.equals("")) {
                    Log.e(TAG.Database, "Get sim operator wrong ");
                    return defaultProxyAddr;
                }
                where = "numeric =" + simOperator;
                mCursor = mContentResolver.query(dmUri, null, where, null, null);
            }
            if (mCursor == null || mCursor.getCount() <= 0) {
                Log.e(TAG.Database, "Get cursor error or cursor is no record");
                if (mCursor != null) {
                    mCursor.close();
                }
                return null;
            }
            mCursor.moveToFirst();
            // int serverAddrID = mCursor.getColumnIndex("server");
            int proxyAddrID = mCursor.getColumnIndex("proxy");
            proxyAddr = mCursor.getString(proxyAddrID);
        } catch (Exception e) {
            Log.e(TAG.Database, "There are some exception accured during finding proxy address");
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        // Log.w(TAG.Database,"server address = " + serverAddr);
        Log.i(TAG.Database, "proxy address = " + proxyAddr);
        // return lookupHost(serverAddr);
        return proxyAddr;
    }

    public int getApnProxyPortFromSettings() {
        // waiting for Yuhui's interface
        String port = null;
        String simOperator = null;
        String mcc = null;
        String mnc = null;
        String where = null;
        int simId = mRegisterSimId;
        if (simId == -1) {
            Log.e(TAG.Database, "Get Register SIM ID error");
            return -1;
        }
        try {

            Log.i(TAG.Database, "Sim Id = " + simId);

            if (MTKOptions.MTK_GEMINI_SUPPORT == true) {
                // for gemini
                simOperator = MTKPhone.getSimOperatorGemini(mTelephonyManager, simId);
                Log.i(TAG.Database, "simOperator numberic = " + simOperator);
                if (simOperator == null || simOperator.equals("")) {
                    Log.e(TAG.Database, "Get sim operator wrong");
                    return defaultProxyPort;
                }
                where = "numeric =" + simOperator;
                if (simId == 1) {
                    // sim card in the second is the right sim card
                    mCursor = mContentResolver.query(dmUriGemini, null, where, null, null);
                } else if (simId == 0) {
                    mCursor = mContentResolver.query(dmUri, null, where, null, null);
                } else {
                    Log.e(TAG.Database, "There is no right the sim card");
                    return -1;
                }
            } else {
                simOperator = mTelephonyManager.getSimOperator(0);
                Log.i(TAG.Database, "simOperator numberic = " + simOperator);
                if (simOperator == null || simOperator.equals("")) {
                    Log.e(TAG.Database, "Get sim operator wrong");
                    return defaultProxyPort;
                }
                where = "numeric =" + simOperator;
                mCursor = mContentResolver.query(dmUri, null, where, null, null);
            }
            if (mCursor == null || mCursor.getCount() <= 0) {
                Log.e(TAG.Database, "Get cursor error or cursor is no record");
                if (mCursor != null) {
                    mCursor.close();
                }
                return -1;
            }
            mCursor.moveToFirst();
            // int serverAddrID = mCursor.getColumnIndex("server");
            int portId = mCursor.getColumnIndex("port");
            port = mCursor.getString(portId);
        } catch (Exception e) {
            Log.e(TAG.Database, "There are some exception accured during finding serveraddr");
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        // Log.w(TAG,"server address = " + serverAddr);
        Log.i(TAG.Database, "proxy port = " + port);
        // return lookupHost(serverAddr);
        return (Integer.parseInt(port));
    }

    public String getDmAddressFromSettings() {
        // waiting for Yuhui's interface
        String serverAddr = null;
        String simOperator = null;
        String mcc = null;
        String mnc = null;
        String where = null;
        int simId = mRegisterSimId;
        if (simId == -1) {
            Log.e(TAG.Database, "Get Register SIM ID error");
            return null;
        }

        try {
            Log.i(TAG.Database, "Sim Id register = " + mRegisterSimId);
            if (MTKOptions.MTK_GEMINI_SUPPORT == true) {
                // for gemini
                simOperator = MTKPhone.getSimOperatorGemini(mTelephonyManager, simId);
                Log.i(TAG.Database, "simOperator numberic = " + simOperator);
                if (simOperator == null || simOperator.equals("")) {
                    Log.e(TAG.Database, "Get sim operator wrong");
                    return null;
                }
                where = "numeric =" + simOperator;
                if (simId == 1) {
                    // sim card in the second is the right sim card
                    mCursor = mContentResolver.query(dmUriGemini, null, where, null, null);
                } else if (simId == 0) {
                    mCursor = mContentResolver.query(dmUri, null, where, null, null);
                } else {
                    Log.e(TAG.Database, "There is no right the sim card");
                    return null;
                }
            } else {
                simOperator = mTelephonyManager.getSimOperator(0);
                Log.i(TAG.Database, "simOperator numberic = " + simOperator);
                if (simOperator == null || simOperator.equals("")) {
                    Log.e(TAG.Database, "Get sim operator wrong");
                    return null;
                }
                where = "numeric =" + simOperator;
                mCursor = mContentResolver.query(dmUri, null, where, null, null);
            }
            if (mCursor == null || mCursor.getCount() <= 0) {
                Log.e(TAG.Database, "Get cursor error or cursor is no record");
                if (mCursor != null) {
                    mCursor.close();
                }
                return null;
            }
            mCursor.moveToFirst();
            int serverAddrID = mCursor.getColumnIndex("server");
            serverAddr = mCursor.getString(serverAddrID);
        } catch (Exception e) {
            Log.e(TAG.Database, "There are some exception accured during finding serveraddr");
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        Log.i(TAG.Database, "server address = " + serverAddr);
        return serverAddr;
    }

}
