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

package com.mediatek.cmmb.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.mbbms.ServerStatus;
import com.mediatek.mbbms.protocol.AccountResponse;
import com.mediatek.mbbms.protocol.LocationInfo;
import com.mediatek.mbbms.protocol.LocationInfo.City;
import com.mediatek.mbbms.protocol.LocationInfo.County;
import com.mediatek.mbbms.protocol.LocationInfo.Province;
import com.mediatek.mbbms.service.MBBMSService;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public class ServiceManager {
    private static final String TAG = "ServiceManager";
    private static final boolean LOG = true;
    private static final boolean DEBUG = true;

    private static ServiceManager sManager;// singleton
    private volatile MBBMSService mMBBMSService;
    private Context mContext;
    private ReentrantLock mBindingDog = new ReentrantLock();
    private MyServiceConnection mServiceConnection;

    private ServiceManager(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Get the instance of ServiceManager. it just init an instance which
     * manages the real service.
     * 
     * @param context
     * @return
     */
    public static ServiceManager getServiceManager(Context context) {
        if (sManager == null) {
            sManager = new ServiceManager(context);
        }
        return sManager;
    }

    /**
     * Should be called if app do not use service. You may not have to call this
     * function if your activity do not need service.
     */
    public void closeService() {
        if (LOG) {
            Log.v(TAG, "close() mMBBMSService=" + mMBBMSService + ", mServiceConnection=" + mServiceConnection
                    + getThreadInfo());
        }
        if (mMBBMSService != null) {
            mContext.unbindService(mServiceConnection);
            mMBBMSService = null;
        }
    }

    public MBBMSService getService() {
        if (mMBBMSService == null) {
            mBindingDog.lock();// lock it
            if (LOG) {
                Log.v(TAG, "getService() " + getThreadInfo() + " enter lock. mMBBMSService=" + mMBBMSService);
            }
            if (mMBBMSService == null) {
                // not bind the service
                CountDownLatch lock = new CountDownLatch(1);
                Intent service = new Intent(mContext, MBBMSService.class);
                boolean isGemini = false;
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    isGemini = true;
                }
                service.putExtra(MBBMSService.CONFIGURATION_RESOURCE_ID, R.raw.configurations);
                service.putExtra(MBBMSService.IS_GEMINI_PROJ, isGemini);
                /*
                 * if you config User agent here, the <userAgent> config in
                 * packages\CMMB\res\raw\configurations.xml would't work
                 * anymore!
                 */
                String userAgent = null;
                service.putExtra(MBBMSService.CONFIG_USER_AGENT, userAgent);
                mServiceConnection = new MyServiceConnection(lock);
                boolean result = mContext.bindService(service, mServiceConnection, Context.BIND_AUTO_CREATE);
                if (!result) {
                    Log.e(TAG, "getService: Can not bind to service!");
                    return null;
                }
                try {
                    if (LOG) {
                        Log.v(TAG, "getService() " + getThreadInfo() + " await " + lock);
                    }
                    lock.await();
                    if (LOG) {
                        Log.v(TAG, "getService() " + getThreadInfo() + " waked " + lock);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mBindingDog.unlock();// release it
            if (LOG) {
                Log.v(TAG, "getService() " + getThreadInfo() + " outer lock. mMBBMSService=" + mMBBMSService);
            }
        }
        if (LOG) {
            Log.v(TAG, "getService() return " + mMBBMSService);
        }
        return mMBBMSService;
    }

    /**
     * Should be called in Non-UI thread. it may consume lots of time.
     * 
     * @param purchaseItemGlobalId
     * @param purchaseDataGlobalId
     * @return
     */
    public ServerStatus subscribe(String purchaseItemGlobalId, String purchaseDataGlobalId) {
        if (LOG) {
            Log.v(TAG, "subscribe: (" + purchaseItemGlobalId + "," + purchaseDataGlobalId + ")");
        }
        MBBMSService service = getService();
        if (service != null) {
            ServerStatus status = service.subscribe(purchaseItemGlobalId, purchaseDataGlobalId);
            if (LOG) {
                Log.v(TAG, "subscribe: 1(" + status.code + ")");
            }
            if (MBBMSService.STATUS_FAIL_UAM_AUTH.equalsIgnoreCase(status.code)) {
                try {
                    status = service.processGBAInitialization();
                    if (LOG) {
                        Log.v(TAG, "subscribe: 2(" + status.code + ")");
                    }
                    if (MBBMSService.STATUS_SUCCEED.equalsIgnoreCase(status.code)) {
                        status = service.subscribe(purchaseItemGlobalId, purchaseDataGlobalId);
                        if (LOG) {
                            Log.v(TAG, "subscribe: 3(" + status.code + ")");
                        }
                    }
                } catch (InterruptedException e) {
                    service.stopDataConnect();
                    e.printStackTrace();
                }
            }
            service.stopDataConnect();
            return status;
        }
        return null;
    }

    /**
     * Should be called in Non-UI thread. it may consume lots of time.
     * 
     * @param purchaseItemGlobalId
     * @param purchaseDataGlobalId
     * @return
     */
    public ServerStatus unsubscribe(String purchaseItemGlobalId, String purchaseDataGlobalId) {
        if (LOG) {
            Log.v(TAG, "unsubscribe: (" + purchaseItemGlobalId + "," + purchaseDataGlobalId + ")");
        }
        MBBMSService service = getService();
        if (service != null) {
            ServerStatus status = service.unsubscribe(purchaseItemGlobalId, purchaseDataGlobalId);
            if (LOG) {
                Log.v(TAG, "unsubscribe: 1(" + status.code + ")");
            }
            if (MBBMSService.STATUS_FAIL_UAM_AUTH.equalsIgnoreCase(status.code)) {
                try {
                    status = service.processGBAInitialization();
                    if (LOG) {
                        Log.v(TAG, "unsubscribe: 2(" + status.code + ")");
                    }
                    if (MBBMSService.STATUS_SUCCEED.equalsIgnoreCase(status.code)) {
                        status = service.unsubscribe(purchaseItemGlobalId, purchaseDataGlobalId);
                        if (LOG) {
                            Log.v(TAG, "unsubscribe: 3(" + status.code + ")");
                        }
                    }
                } catch (InterruptedException e) {
                    service.stopDataConnect();
                    e.printStackTrace();
                }
            }
            service.stopDataConnect();
            return status;
        }
        return null;
    }

    /**
     * Should be called in Non-UI thread. it may consume lots of time.
     * 
     * @param purchaseItemGlobalId
     * @param purchaseDataGlobalId
     * @return
     */
    public ServerStatus unsubscribeAll() {
        if (LOG) {
            Log.v(TAG, "unsubscribeAll");
        }
        MBBMSService service = getService();
        if (service != null) {
            ServerStatus status = service.unsubscribeAll();
            if (LOG) {
                Log.v(TAG, "unsubscribeAll: 1(" + status.code + ")");
            }
            if (MBBMSService.STATUS_FAIL_UAM_AUTH.equalsIgnoreCase(status.code)) {
                try {
                    status = service.processGBAInitialization();
                    if (LOG) {
                        Log.v(TAG, "unsubscribeAll: 2(" + status.code + ")");
                    }
                    if (MBBMSService.STATUS_SUCCEED.equalsIgnoreCase(status.code)) {
                        status = service.unsubscribeAll();
                        if (LOG) {
                            Log.v(TAG, "unsubscribeAll: 3(" + status.code + ")");
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    service.stopDataConnect();
                }
            }
            service.stopDataConnect();
            return status;
        }
        return null;
    }

    /**
     * Should be called in Non-UI thread. it may consume lots of time.
     * 
     * @return
     */
    public ServerStatus processAccountInquiry() {
        MBBMSService service = getService();
        if (service != null) {
            ServerStatus status = service.processAccountInquiry();
            service.stopDataConnect();
            return status;
        }
        return null;
    }

    /**
     * Should be called in Non-UI thread. it may consume lots of time.
     * 
     * @return
     */
    public AccountResponse getAccountInfo() {
        MBBMSService service = getService();
        if (service != null) {
            return service.getAccountInfo();
        }
        return null;
    }

    private String getThreadInfo() {
        Thread t = Thread.currentThread();
        return "T(" + t.getId() + ", " + t.getName() + ")";
    }

    /* package */class MyServiceConnection implements ServiceConnection {
        private CountDownLatch mLock;

        public MyServiceConnection(CountDownLatch lock) {
            mLock = lock;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (LOG) {
                Log.v(TAG, "onServiceConnected() " + getThreadInfo() + " count down " + mLock);
            }
            mMBBMSService = ((MBBMSService.LocalBinder) service).getService();
            mLock.countDown();// notify the wait thread
        }

        public void onServiceDisconnected(ComponentName name) {
            if (LOG) {
                Log.v(TAG, "onServiceDisconnected() " + getThreadInfo());
            }
            mMBBMSService = null;
        }
    }

    // -------------------------just for test
    // ----------------------------------------
    private LocationInfo testPickSuggestion(boolean pick, boolean suggestion) {
        LocationInfo mLocation = new LocationInfo();
        mLocation.candidateArea = new ArrayList<City>();
        for (int i = 0; i < 3; i++) {
            City city = new City("Suggestion" + i);
            mLocation.candidateArea.add(city);
        }
        mLocation.provinces = new ArrayList<Province>();
        for (int i = 0; i < 3; i++) {
            Province province = new Province("Province" + i);
            province.cities = new ArrayList<City>();
            if (i < 2) {
                for (int j = 0; j < 3; j++) {
                    City city = new City("City" + j);
                    city.counties = new ArrayList<County>();
                    if (j < 1) {
                        for (int k = 0; k < 3; k++) {
                            County county = new County("county" + k);
                            city.counties.add(county);
                        }
                    }
                    province.cities.add(city);
                }
            }
            mLocation.provinces.add(province);
        }
        // Intent intent = new Intent();
        // if (pick) {
        // intent.setAction(Intent.ACTION_PICK);
        // }
        // if (suggestion) {
        // intent.putExtra(Utils.EXTRA_LOCATION_MODE,
        // Utils.LOCATION_MODE_NORMAL);
        // }
        // setIntent(intent);
        return mLocation;
    }
}
