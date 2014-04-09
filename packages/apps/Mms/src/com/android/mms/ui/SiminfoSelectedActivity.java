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
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
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
package com.android.mms.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.os.ServiceManager;
import android.app.ActionBar;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.android.internal.telephony.ITelephony;
import com.mediatek.encapsulation.MmsLog;
import com.android.mms.R;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** M:
 * SiminfoSelectedActivity
 */
public class SiminfoSelectedActivity extends Activity{
    private static final String TAG = "SiminfoSelectedActivity";
    private String where;
    private List<SIMInfo> mSimInfoList;
    private int mSimCount = 0;
    private static final String VIEW_ITEM_KEY_IMAGE     = "simCardPicid";
    private static final String VIEW_ITEM_KEY_SIMNAME   = "simCardTexid";
    private static final String VIEW_ITEM_KEY_SELECT    = "selectedid"; 
    private static final String VIEW_ITEM_KEY           = "simcardkey";
    private ListView listview;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.siminfo_seleted_framelayout);
        listview = (ListView)findViewById(R.id.siminfolist);
        LayoutInflater inflater = getLayoutInflater();
        View header = inflater.inflate(R.layout.select_siminfo_header, null);
        listview.addHeaderView(header, null, false);
        getSimInfoList();
        initListAdapter();

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                String where = null;     
                Map<String, Object> map = (Map<String, Object>) listview.getItemAtPosition(position);
                int mindex = Integer.parseInt(map.get(VIEW_ITEM_KEY).toString());
                if (mindex == 0) {
                    where = null;
                } else if (mindex > 0) {
                        SIMInfo si = SIMInfo.getSIMInfoBySlot(SiminfoSelectedActivity.this, (mindex - 1));
                    if (si == null) {
                        finish();
                    } else {
                        where = "sim_id = " + (int) si.getSimId();
                    }
                }
                MmsConfig.setSimCardInfo(mindex);
                Intent mIntent = new Intent();
                mIntent.putExtra("sim_id", where);
                setResult(RESULT_OK, mIntent);
                finish();
            }
        });
        
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                   finish();
                return true;
            case KeyEvent.KEYCODE_SEARCH:
                return true;
            case KeyEvent.KEYCODE_MENU:
                return true;
            case KeyEvent.KEYCODE_HOME:
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initListAdapter() {
        SimpleAdapter mSimAdapter = new SimpleAdapter(this, getSimData(),
            R.layout.select_simcard_dialog_view,
            new String[] {"simCardPicid", "simCardTexid","selectedid"},
            new int[] { R.id.simCardPicid, R.id.simCardTexid, R.id.selectedid});
        listview.setAdapter(mSimAdapter);
    }
    
    private void getSimInfoList() {
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            mSimInfoList = SIMInfo.getInsertedSIMList(this);
            mSimCount = mSimInfoList.isEmpty()? 0: mSimInfoList.size();
        } 
    }

    private List<Map<String, Object>> getSimData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        Resources res = getResources();
        int msimindex = MmsConfig.getSimCardInfo();
        //map.put(VIEW_ITEM_KEY_IMAGE, R.drawable.ic_launcher_smsmms);
        map.put(VIEW_ITEM_KEY_SIMNAME, res.getText(R.string.allmessage));
        if (msimindex == 0) {
            map.put(VIEW_ITEM_KEY_SELECT, true);
        } else {
            map.put(VIEW_ITEM_KEY_SELECT, false);
        }
        map.put(VIEW_ITEM_KEY, "0");

        list.add(map);
        if (mSimCount == 2) {
            map = new HashMap<String, Object>();
            if (mSimInfoList.get(0).getSlot() == 0) {
                map.put(VIEW_ITEM_KEY_IMAGE, mSimInfoList.get(0).getSimBackgroundLightRes());
                map.put(VIEW_ITEM_KEY_SIMNAME, mSimInfoList.get(0).getDisplayName());
            } else {
                map.put(VIEW_ITEM_KEY_IMAGE, mSimInfoList.get(1).getSimBackgroundLightRes());
                map.put(VIEW_ITEM_KEY_SIMNAME, mSimInfoList.get(1).getDisplayName());
            }
            if (msimindex == 1) {
                map.put(VIEW_ITEM_KEY_SELECT, true);
            } else {
                map.put(VIEW_ITEM_KEY_SELECT, false);
        }
            map.put(VIEW_ITEM_KEY, "1");
            list.add(map);
        
            map = new HashMap<String, Object>();
            if (mSimInfoList.get(0).getSlot() == 1) {
                map.put(VIEW_ITEM_KEY_IMAGE, mSimInfoList.get(0).getSimBackgroundLightRes());
                map.put(VIEW_ITEM_KEY_SIMNAME, mSimInfoList.get(0).getDisplayName());
            } else {
                map.put(VIEW_ITEM_KEY_IMAGE, mSimInfoList.get(1).getSimBackgroundLightRes());
                map.put(VIEW_ITEM_KEY_SIMNAME, mSimInfoList.get(1).getDisplayName());
            }
            if (msimindex == 2) {
                map.put(VIEW_ITEM_KEY_SELECT, true);
            } else {
                map.put(VIEW_ITEM_KEY_SELECT, false);
            }
            map.put(VIEW_ITEM_KEY, "2");
            list.add(map);
        } else if (mSimCount == 1) {
            int slotId = 0;
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                slotId = mSimInfoList.get(0).getSlot();
                Log.d(TAG, "MTK_GEMINI_SUPPORT slotId =" + slotId);
                if (slotId == 0) {
                    map = new HashMap<String, Object>();
                    map.put(VIEW_ITEM_KEY_IMAGE, mSimInfoList.get(0).getSimBackgroundLightRes());
                    map.put(VIEW_ITEM_KEY_SIMNAME, mSimInfoList.get(0).getDisplayName());
                    if (msimindex == 1) {
                        map.put(VIEW_ITEM_KEY_SELECT, true);
                    } else {
                        map.put(VIEW_ITEM_KEY_SELECT, false);
                    }
                    map.put(VIEW_ITEM_KEY, "1");
                    list.add(map);
                } else {
                    map = new HashMap<String, Object>();
                    map.put(VIEW_ITEM_KEY_IMAGE, mSimInfoList.get(0).getSimBackgroundLightRes());
                    map.put(VIEW_ITEM_KEY_SIMNAME, mSimInfoList.get(0).getDisplayName());
                    if (msimindex == 2) {
                        map.put(VIEW_ITEM_KEY_SELECT, true);
                    } else {
                        map.put(VIEW_ITEM_KEY_SELECT, false);
                    }
                    map.put(VIEW_ITEM_KEY, "2");
                    list.add(map);
        }
            }
        }

        return list;
    }  
}
