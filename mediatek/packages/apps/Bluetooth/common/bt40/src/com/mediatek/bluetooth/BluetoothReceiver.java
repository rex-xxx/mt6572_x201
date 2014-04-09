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

package com.mediatek.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfileManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.mediatek.bluetooth.util.BtLog;
import com.mediatek.common.featureoption.FeatureOption;

import java.util.HashSet;

public class BluetoothReceiver extends BroadcastReceiver {

    // used to keep the un-mounting sdcard
    public static HashSet<String> sUnmountingStorageSet = new HashSet<String>(2);

    // constants for show-toast feature
    public static final String ACTION_SHOW_TOAST = "com.mediatek.bluetooth.receiver.action.SHOW_TOAST";

    public static final String EXTRA_TEXT = "com.mediatek.bluetooth.receiver.extra.TEXT";

    // service class
    private static final String OPP_SERVICE_CLASS = "com.mediatek.bluetooth.opp.adp.OppService";

    private static final String FTP_SERVICE_CLASS = "com.mediatek.bluetooth.ftp.BluetoothFtpService";

    private static final String PBAP_SERVICE_CLASS = "com.mediatek.bluetooth.pbap.BluetoothPbapService";

    private static final String AVRCP_SERVICE_CLASS = "com.mediatek.bluetooth.avrcp.BluetoothAvrcpService";

    private static final String HID_SERVICE_CLASS = "com.mediatek.bluetooth.hid.BluetoothHidService";

    private static final String DUN_SERVICE_CLASS = "com.mediatek.bluetooth.dun.BluetoothDunService";

    private static final String BIP_SERVICE_CLASS = "com.mediatek.bluetooth.bip.BipService";

    private static final String PAN_SERVICE_CLASS = "com.mediatek.bluetooth.pan.BluetoothPanService";

    private static final String MAP_SERVICE_CLASS = "com.mediatek.bluetooth.map.BluetoothMapServerService";


    public static boolean isPathMounted(String path){
         boolean ret;
         synchronized(sUnmountingStorageSet){
             ret = !BluetoothReceiver.sUnmountingStorageSet.contains(path);
         }
         return ret;
    }


    @Override
    public void onReceive(Context context, Intent intent) {

        int btState;
        String action = intent.getAction();
        BtLog.i("BluetoothReceiver receive action:" + action);
        // Bluetooth On or Boot Completed => start profile services
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

            btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (btState == BluetoothAdapter.STATE_ON) {

                this.startProfileServices(context);
            } else if (btState == BluetoothAdapter.STATE_TURNING_OFF) {

                this.stopProfileServices(context);
            }
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {


            this.startProfileServices(context);
        } else if (action.equals(BluetoothProfileManager.ACTION_DISABLE_PROFILES)) {
            // ProfileManager notify to stop profile services
            BtLog.i("BluetoothProfileManaher disable profile");
            // this.stopProfileServices( context );
        } else if (ACTION_SHOW_TOAST.equals(action)) { // show toast

            String text = intent.getStringExtra(EXTRA_TEXT);
            if (text != null) {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        } else if (Intent.ACTION_MEDIA_EJECT.equals(action)) {

            Uri path = intent.getData();
            if (path != null) {
                if (Options.LL_DEBUG) {
                    BtLog.d("BluetoothReceiver: add un-mounting path[" + path.getPath() + "] for " + action);
                }
                synchronized(sUnmountingStorageSet) {
                    sUnmountingStorageSet.add(path.getPath());
                }
            }
        } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action) || Intent.ACTION_MEDIA_MOUNTED.equals(action)) {

            Uri path = intent.getData();
            if (path != null) {
                if (Options.LL_DEBUG) {
                    BtLog.d("BluetoothReceiver: del un-mounting path[" + path.getPath() + "] for " + action);
                }
                synchronized(sUnmountingStorageSet) {
                    sUnmountingStorageSet.remove(path.getPath());
                }
            }
        }
    }

    private void startProfileServices(Context context) {

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter != null && btAdapter.isEnabled()) {
            this.startService(context, FTP_SERVICE_CLASS);
        }
        if (btAdapter != null && btAdapter.isEnabled()) {
            this.startService(context, PBAP_SERVICE_CLASS);
        }
        //if (btAdapter != null && btAdapter.isEnabled()) {
        //    this.startService(context, AVRCP_SERVICE_CLASS);
        //}
        if (btAdapter != null && btAdapter.isEnabled()) {
            this.startService(context, HID_SERVICE_CLASS);
        }
        // if (FeatureOption.MTK_BT_PROFILE_OPP && btAdapter != null &&
        // btAdapter.isEnabled())
        // this.startService( context, OPP_SERVICE_CLASS );
        // if (btAdapter != null && btAdapter.isEnabled())
        // this.startService( context, DUN_SERVICE_CLASS );
        if (FeatureOption.MTK_BT_PROFILE_BIP && btAdapter != null && btAdapter.isEnabled()) {
            this.startService(context, BIP_SERVICE_CLASS);
        }
        // if (FeatureOption.MTK_BT_PROFILE_PAN && btAdapter != null &&
        // btAdapter.isEnabled())
        // this.startService( context, PAN_SERVICE_CLASS);//, "BtTethering",
        // (Parcelable)mBTtethering );
        if (FeatureOption.MTK_BT_PROFILE_MAPS && btAdapter != null && btAdapter.isEnabled()) {
            this.startService(context, MAP_SERVICE_CLASS);
        }
    }

    private void stopProfileServices(Context context) {

        this.stopService(context, PBAP_SERVICE_CLASS);
        //this.stopService(context, AVRCP_SERVICE_CLASS);
        if (FeatureOption.MTK_BT_PROFILE_OPP) {
            this.stopService(context, OPP_SERVICE_CLASS);
        }
        this.startService(context, BIP_SERVICE_CLASS, "action", "com.mediatek.bluetooth.bipiservice.action.BIP_DISABLE");
        this.stopService(context, HID_SERVICE_CLASS);
        this.stopService(context, DUN_SERVICE_CLASS);
        if (FeatureOption.MTK_BT_PROFILE_MAPS) {
            this.stopService(context, MAP_SERVICE_CLASS);
        }
        if (FeatureOption.MTK_BT_PROFILE_PAN) {
            this.stopService(context, PAN_SERVICE_CLASS);
        }
    }

    private void startService(Context context, String serviceClass) {

        try {
            context.startService(new Intent(context, Class.forName(serviceClass)));
        } catch (ClassNotFoundException ex) {
            BtLog.e("start service for class[" + serviceClass + "] fail:", ex);
        }
    }

    private void stopService(Context context, String serviceClass) {

        try {
            context.stopService(new Intent(context, Class.forName(serviceClass)));
        } catch (ClassNotFoundException ex) {
            BtLog.e("stop service for class[" + serviceClass + "] fail:", ex);
        }
    }

    private void startService(Context context, String serviceClass, String extraName, String extraValue) {

        try {
            context.startService(new Intent(context, Class.forName(serviceClass)).putExtra(extraName, extraValue));
        } catch (ClassNotFoundException ex) {
            BtLog.e("start service for class[" + serviceClass + "] fail:", ex);
        }
    }
}
