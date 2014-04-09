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

package mediatek.system.cts;


import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.test.AndroidTestCase;


import com.mediatek.common.featureoption.FeatureOption;

import android.server.BluetoothA2dpService;
import android.server.BluetoothService;
import android.server.BluetoothSocketService;
import android.server.BluetoothProfileManagerService;


public class SystemTest extends AndroidTestCase {

  
    protected void setUp() throws Exception {
        super.setUp();        
    }

    public void testServiceManager() throws Exception {
        
        assertNotNull(ServiceManager.getService("package"));
        assertNotNull(ServiceManager.getService("entropy"));
        assertNotNull(ServiceManager.getService(Context.POWER_SERVICE));
        assertNotNull(ServiceManager.getService("telephony.registry"));
        //assertNotNull(ServiceManager.getService("telephony.registry2"));
        assertNotNull(ServiceManager.getService(Context.ACCOUNT_SERVICE));
        assertNotNull(ServiceManager.getService("battery"));
        assertNotNull(ServiceManager.getService("hardware"));
        assertNotNull(ServiceManager.getService(Context.VIBRATOR_SERVICE));
        assertNotNull(ServiceManager.getService(Context.ALARM_SERVICE));
        assertNotNull(ServiceManager.getService(Context.WINDOW_SERVICE));
        assertNotNull(ServiceManager.getService(Context.ACTIVITY_SERVICE));

	if (SystemProperties.get("ro.kernel.qemu").equals("1")) {
		assertNull(ServiceManager.getService(BluetoothAdapter.BLUETOOTH_SERVICE));
	} else {
		assertNotNull(ServiceManager.getService(BluetoothAdapter.BLUETOOTH_SERVICE));
		assertNotNull(ServiceManager.getService(BluetoothA2dpService.BLUETOOTH_A2DP_SERVICE));
		if (true == FeatureOption.MTK_BT_PROFILE_SPP) {
	        	assertNotNull(ServiceManager.getService(BluetoothSocketService.BLUETOOTH_SOCKET_SERVICE));
		}
		if (true == FeatureOption.MTK_BT_PROFILE_MANAGER) {
		    assertNotNull(ServiceManager.getService(BluetoothProfileManagerService.BLUETOOTH_PROFILEMANAGER_SERVICE));		        
		}
	}
        assertNotNull(ServiceManager.getService(Context.DEVICE_POLICY_SERVICE));
        assertNotNull(ServiceManager.getService(Context.STATUS_BAR_SERVICE));        
        assertNotNull(ServiceManager.getService(Context.CLIPBOARD_SERVICE));
        assertNotNull(ServiceManager.getService(Context.INPUT_METHOD_SERVICE));
        assertNotNull(ServiceManager.getService("netstats"));
        assertNotNull(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        assertNotNull(ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
        assertNotNull(ServiceManager.getService(Context.THROTTLE_SERVICE));
        assertNotNull(ServiceManager.getService(Context.ACCESSIBILITY_SERVICE));
        assertNotNull(ServiceManager.getService("mount"));      
        assertNotNull(ServiceManager.getService(Context.NOTIFICATION_SERVICE));        
        assertNotNull(ServiceManager.getService(Context.LOCATION_SERVICE));
        assertNotNull(ServiceManager.getService(Context.SEARCH_SERVICE));
        assertNotNull(ServiceManager.getService(Context.DROPBOX_SERVICE));
        assertNotNull(ServiceManager.getService(Context.WALLPAPER_SERVICE));        
        assertNotNull(ServiceManager.getService(Context.AUDIO_SERVICE));
        assertNotNull(ServiceManager.getService(Context.USB_SERVICE));
        assertNotNull(ServiceManager.getService(Context.UI_MODE_SERVICE));
        assertNotNull(ServiceManager.getService(Context.BACKUP_SERVICE));
        assertNotNull(ServiceManager.getService(Context.APPWIDGET_SERVICE));

        if(true == FeatureOption.MTK_AGPS_APP && true == FeatureOption.MTK_GPS_SUPPORT){ 
            assertNotNull(ServiceManager.getService(Context.MTK_AGPS_SERVICE));
        }

        assertNotNull(ServiceManager.getService(Context.MTK_EPO_CLIENT_SERVICE));
        assertNotNull(ServiceManager.getService("diskstats"));        
    }

    public void testServiceManager2() throws Exception {
        assertNull(ServiceManager.getService("abc"));
    }
}
