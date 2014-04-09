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

package com.mediatek.connectivity;




import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.net.INetworkStatsService;
import android.net.NetworkStats;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;
import android.util.Log;

import com.mediatek.connectivity.R;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CdsPsControlActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "CdsPsControlActivity";

    private static final String INTERNET = "android.permission.INTERNET";
    
    private static final int MDLOGGER_PORT = 30017;
    private static final int ADB_PORT = 5037;

    private static INetworkStatsService sStatsService;

    private ArrayList<MyAppInfo> mAppList;
    private Button mEnableFwBtn = null;
    private Button mDisableFwBtn = null;
    private Context mContext;
    private INetworkManagementService mNetd;
    private ListView mAppListViw;
    private SimpleAdapter mAdapter;
    private TextView mFwStatus;
    private boolean mIsEnabled;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = this.getBaseContext();

        setContentView(R.layout.cds_ps_data);

        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNetd = INetworkManagementService.Stub.asInterface(b);

        mEnableFwBtn = (Button)this.findViewById(R.id.btn_enable_fw);
        if(mEnableFwBtn != null) {
            mEnableFwBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    setFirewallEnabled(true);
                }
            });
        }

        mDisableFwBtn = (Button)this.findViewById(R.id.btn_disable_fw);
        if(mDisableFwBtn != null) {
            mDisableFwBtn.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    setFirewallEnabled(false);
                }
            });
        }

        mFwStatus = (TextView) findViewById(R.id.ps_fw_rule_value);

        if(mNetd == null) {
            Xlog.e(TAG, "INetworkManagementService is null");
            return;
        }

        try {
            updateFwButton(mNetd.isFirewallEnabled());
        } catch(Exception e) {
            e.printStackTrace();
        }

        Xlog.i(TAG, "CdsPsControlActivity is started");
    }

    private void setFirewallEnabled(boolean enabled) {
        if(mNetd == null) {
            Xlog.e(TAG, "INetworkManagementService is null");
            return;
        }

        Xlog.i(TAG, "set firewall:" + enabled);

        if(mIsEnabled == enabled){
           Xlog.i(TAG, "No change");
           return;
        }

        try {
            if(enabled){
                mNetd.setFirewallEnabled(enabled);
            }

            mNetd.setFirewallEgressDestRule("", MDLOGGER_PORT, enabled);
            mNetd.setFirewallEgressDestRule("", ADB_PORT, enabled);

            //int uid = android.os.Process.getUidForName("root");
            //mNetd.setFirewallUidRule(uid, enabled);

            if(!enabled){
                mNetd.setFirewallEnabled(enabled);
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally{
            updateFwButton(enabled);
        }
    }

    private void updateAppList() {

    }

    private void updateFwButton(boolean enabled) {

        mIsEnabled = enabled;

        if(enabled) {
            mFwStatus.setText("enabled");
        } else {
            mFwStatus.setText("disabled");
        }
    }

    @Override
    protected void onResume() {
        try {
            updateFwButton(mNetd.isFirewallEnabled());
        } catch(Exception e) {
            e.printStackTrace();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    public void onClick(View v) {
        int buttonId = v.getId();
    }

    void getApps(Context context) {

        try {
            PackageManager packageManager = context.getPackageManager();
            List<ApplicationInfo> packages = packageManager.getInstalledApplications(0);
            mAppList = new ArrayList<MyAppInfo>(packages.size());
            Iterator<ApplicationInfo> apps = packages.iterator();
            MyAppInfo myApp;

            while (apps.hasNext()) {
                ApplicationInfo app = (ApplicationInfo) apps.next();
                String packageName = app.packageName;// package name
                if(packageManager.checkPermission(INTERNET, packageName) != 0) {
                    int uid = app.uid;
                    Xlog.d(TAG, "packageName:" + packageName + ":" + uid);

                    NetworkStats dataStats = getNetworkStatsForUid(uid);

                    myApp = new MyAppInfo(packageName, app);
                    mAppList.add(myApp);
                }
            }

            //Add pre-defined UID process
            ApplicationInfo appInfo = new ApplicationInfo();
            appInfo.uid = android.os.Process.getUidForName("root");
            appInfo.packageName = "root Applications";
            myApp = new MyAppInfo(appInfo.packageName, appInfo);
            mAppList.add(myApp);

            appInfo.uid = android.os.Process.getUidForName("media");
            appInfo.packageName = "Media server";
            myApp = new MyAppInfo(appInfo.packageName, appInfo);
            mAppList.add(myApp);

            appInfo.uid = android.os.Process.getUidForName("vpn");
            appInfo.packageName = "VPN networking";
            myApp = new MyAppInfo(appInfo.packageName, appInfo);
            mAppList.add(myApp);

            appInfo.uid = android.os.Process.getUidForName("shell");
            appInfo.packageName = "Linux shell";
            myApp = new MyAppInfo(appInfo.packageName, appInfo);
            mAppList.add(myApp);

            appInfo.uid = android.os.Process.getUidForName("gps");
            appInfo.packageName = "GPS";
            myApp = new MyAppInfo(appInfo.packageName, appInfo);
            mAppList.add(myApp);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private List<? extends Map<String, ?>> getData() {
        int i = 0;
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map;
        MyAppInfo myApp;

        for(i = 0; i < mAppList.size(); i++) {
            myApp = (MyAppInfo) mAppList.get(i);
            map = new HashMap<String, Object>();
            map.put("appText", myApp.getPackageName());
            list.add(map);
        }

        return list;
    }

    private synchronized static INetworkStatsService getStatsService() {
        if (sStatsService == null) {
            sStatsService = INetworkStatsService.Stub.asInterface(
                                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        }
        return sStatsService;
    }

    private static NetworkStats getNetworkStatsForUid(int uid) {
        try {
            return getStatsService().getDataLayerSnapshotForUid(uid);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }


    private class MyAppInfo {
        private String mPackageName;
        private ApplicationInfo mAppInfo;

        public MyAppInfo() {

        }

        public MyAppInfo(String name, ApplicationInfo info) {
            mPackageName = name;
            mAppInfo = info;
        }

        public String getPackageName() {
            return mPackageName;
        }

    }
}
