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

/**
 * Copyright (c) 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net;

import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkQuotaInfo;
import android.net.NetworkState;
import android.net.ProxyProperties;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;

/**
 * Interface that answers queries about, and allows changing, the
 * state of network connectivity.
 */
/** {@hide} */
interface IConnectivityManager
{
    void setNetworkPreference(int pref);

    int getNetworkPreference();

    NetworkInfo getActiveNetworkInfo();
    NetworkInfo getActiveNetworkInfoForUid(int uid);
    NetworkInfo getNetworkInfo(int networkType);
    NetworkInfo[] getAllNetworkInfo();

    boolean isNetworkSupported(int networkType);

    LinkProperties getActiveLinkProperties();
    LinkProperties getLinkProperties(int networkType);

    NetworkState[] getAllNetworkState();

    NetworkQuotaInfo getActiveNetworkQuotaInfo();
    boolean isActiveNetworkMetered();

    boolean setRadios(boolean onOff);

    boolean setRadio(int networkType, boolean turnOn);

    int startUsingNetworkFeature(int networkType, in String feature,
            in IBinder binder);

    int stopUsingNetworkFeature(int networkType, in String feature);

    boolean requestRouteToHost(int networkType, int hostAddress);

    boolean requestRouteToHostAddress(int networkType, in byte[] hostAddress);

    boolean getMobileDataEnabled();
    void setMobileDataEnabled(boolean enabled);

    /** Policy control over specific {@link NetworkStateTracker}. */
    void setPolicyDataEnable(int networkType, boolean enabled);

    int tether(String iface);

    int untether(String iface);

    int getLastTetherError(String iface);

    boolean isTetheringSupported();

    String[] getTetherableIfaces();

    String[] getTetheredIfaces();

    /**
     * Return list of interface pairs that are actively tethered.  Even indexes are
     * remote interface, and odd indexes are corresponding local interfaces.
     */
    String[] getTetheredIfacePairs();

    String[] getTetheringErroredIfaces();

    String[] getTetherableUsbRegexs();

    String[] getTetherableWifiRegexs();

    String[] getTetherableBluetoothRegexs();

    int setUsbTethering(boolean enable);

    void requestNetworkTransitionWakelock(in String forWhom);

    void reportInetCondition(int networkType, int percentage);

    ProxyProperties getGlobalProxy();

    void setGlobalProxy(in ProxyProperties p);

    ProxyProperties getProxy();

    void setDataDependency(int networkType, boolean met);

    boolean protectVpn(in ParcelFileDescriptor socket);

    boolean prepareVpn(String oldPackage, String newPackage);

    ParcelFileDescriptor establishVpn(in VpnConfig config);

    void startLegacyVpn(in VpnProfile profile);

    LegacyVpnInfo getLegacyVpnInfo();

    boolean updateLockdownVpn();

    void captivePortalCheckComplete(in NetworkInfo info);
    /** M: support Tether dediated APN feature */
    boolean isTetheringChangeDone();

    //MTK-START [mtk04070][111128][ALPS00093395]MTK proprietary methods
    /**
     * @hide
     */
    int startUsingNetworkFeatureGemini(int networkType, in String feature, in IBinder binder , int radioNum);

    /**
     * @hide
     */
    int stopUsingNetworkFeatureGemini(int networkType, in String feature, int radioNum);

    //MTK-END [mtk04070][111128][ALPS00093395]MTK proprietary methods

    /** M: support data usage function in Gemini feature */
    boolean getMobileDataEnabledGemini(int slotId);



    /** M: support stopUsingNetworkFeature in Gemini feature */
    boolean setMobileDataEnabledGemini(int slotId);

    /**
     * M: ipv6 tethering
     * @hide
     */
    void setTetheringIpv6Enable(boolean enable);

    /**
     * M: ipv6 tethering
     * @hide
     */
    boolean getTetheringIpv6Enable();

    /**
     * M: Hotspot Manager - USB Internet
     * @hide
     */
    boolean setUsbInternet(boolean enable);

    /**
     * M: Hotspot Manager - USB Internet
     * @param enable  enable or disable UsbInternet
     * @param system_type  to determine different IP of Usb interface for WinXP/WinVista/Win7/Win8
     * @hide
     */
    boolean setUsbInternetWithType(boolean enable, int system_type);
}
