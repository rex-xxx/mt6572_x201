/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.server.connectivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.INetworkStatsService;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/** M: MediaTek imports @{ */
import android.os.Bundle;
import android.os.SystemProperties;
import android.net.wifi.IWifiManager;
import android.net.wifi.WifiConfiguration;
import android.telephony.TelephonyManager;
import com.google.android.collect.Lists;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;
import android.os.SystemClock;
import com.android.internal.telephony.DefaultPhoneNotifier;
import android.net.wifi.HotspotClient;
import android.net.wifi.WifiManager;
import android.net.INetworkManagementIpv6EventObserver;
import java.util.List;
import android.net.RouteInfo;
/** @} */

/**
 * @hide
 *
 * Timeout
 *
 * TODO - look for parent classes and code sharing
 */
public class Tethering extends INetworkManagementEventObserver.Stub {

    private Context mContext;
    private final static String TAG = "Tethering";
    private final static boolean DBG = true;
    private final static boolean VDBG = true;

    // TODO - remove both of these - should be part of interface inspection/selection stuff
    private String[] mTetherableUsbRegexs;
    private String[] mTetherableWifiRegexs;
    private String[] mTetherableBluetoothRegexs;
    private Collection<Integer> mUpstreamIfaceTypes;

    // used to synchronize public access to members
    private Object mPublicSync;

    private static final Integer MOBILE_TYPE = new Integer(ConnectivityManager.TYPE_MOBILE);
    private static final Integer HIPRI_TYPE = new Integer(ConnectivityManager.TYPE_MOBILE_HIPRI);
    private static final Integer DUN_TYPE = new Integer(ConnectivityManager.TYPE_MOBILE_DUN);

    // if we have to connect to mobile, what APN type should we use?  Calculated by examining the
    // upstream type list and the DUN_REQUIRED secure-setting
    private int mPreferredUpstreamMobileApn = ConnectivityManager.TYPE_NONE;

    private final INetworkManagementService mNMService;
    private final INetworkStatsService mStatsService;
    private final IConnectivityManager mConnService;
    private Looper mLooper;
    private HandlerThread mThread;

    private HashMap<String, TetherInterfaceSM> mIfaces; // all tethered/tetherable ifaces

    private BroadcastReceiver mStateReceiver;

    private static final String USB_NEAR_IFACE_ADDR      = "192.168.42.129";
    private static final int USB_PREFIX_LENGTH        = 24;

    // USB is  192.168.42.1 and 255.255.255.0
    // Wifi is 192.168.43.1 and 255.255.255.0
    // BT is limited to max default of 5 connections. 192.168.44.1 to 192.168.48.1
    // with 255.255.255.0

    private String[] mDhcpRange;
    private static final String[] DHCP_DEFAULT_RANGE = {
        "192.168.42.2", "192.168.42.254", "192.168.43.2", "192.168.43.254",
        "192.168.44.2", "192.168.44.254", "192.168.45.2", "192.168.45.254",
        "192.168.46.2", "192.168.46.254", "192.168.47.2", "192.168.47.254",
        "192.168.48.2", "192.168.48.254",
    };

    private String[] mDefaultDnsServers;
    private static final String DNS_DEFAULT_SERVER1 = "8.8.8.8";
    private static final String DNS_DEFAULT_SERVER2 = "8.8.4.4";

    private StateMachine mTetherMasterSM;

    /** M: for bug solving, ALPS00331223 */
    private boolean mUnTetherDone = true;
    private boolean mTetherDone = true;
    private boolean mTetheredFail = false;

    private Notification mTetheredNotification;

    private boolean mRndisEnabled;       // track the RNDIS function enabled state
    private boolean mUsbTetherRequested; // true if USB tethering should be started
                                         // when RNDIS is enabled
    /** M: ALPS00233672 track the UI USB Tethering State (record) */
    private boolean mUsbTetherEnabled = false;

    /** M: dedicated apn feature @{ */
    private String mWifiIface;
    private boolean mIsTetheringChangeDone = true;
    /** @} */

    /** M: Hotspot Manager @{ */
    private boolean mUsbInternetEnable;
    private int     mUsbInternetState;
    ///M: ALPS00433208 JE due to race condition
    private Object  mNotificationSync;
    private static final int USB_INTERNET_SYSTEM_DEFAULT  = ConnectivityManager.USB_INTERNET_SYSTEM_WINXP;
    private String[] mUsbInternetDnsServers;
    private static final String USB_INTERNET_DNS_SERVER1[] = {"192.168.0.1", "192.168.137.1"};
    private static final String USB_INTERNET_DNS_SERVER2 = "168.95.1.1";
    private String mUsbInternetGateway;
    private int    mUsbInternetSystemType;
    private String mUsbIface;
    private static final int USB_INTERNET_DISCONNECTED = 0;
    private static final int USB_INTERNET_CONNECTING = 1;
    private static final int USB_INTERNET_CONNECTED = 2;
    private static final int USB_INTERNET_SUSPENDED = 3;
    /** @} */

    /** M: ipv6 tethering @{ */
    private StateMachine mIpv6TetherMasterSM;
    private boolean mIpv6FeatureEnable;
    private static final String MASTERSM_IPV4 = "TetherMaster";
    private static final String MASTERSM_IPV6 = "Ipv6TetherMaster";
    /** @} */

    public Tethering(Context context, INetworkManagementService nmService,
            INetworkStatsService statsService, IConnectivityManager connService, Looper looper) {
        mContext = context;
        mNMService = nmService;
        mStatsService = statsService;
        mConnService = connService;
        mLooper = looper;

        mPublicSync = new Object();

        mIfaces = new HashMap<String, TetherInterfaceSM>();

        // make our own thread so we don't anr the system
        mThread = new HandlerThread("Tethering");
        mThread.start();
        mLooper = mThread.getLooper();
        mTetherMasterSM = new TetherMasterSM("TetherMaster", mLooper);
        mTetherMasterSM.start();

        /** M: ipv6 tethering @{ */
        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
              mIpv6TetherMasterSM = new TetherMasterSM(MASTERSM_IPV6, mLooper);
              mIpv6TetherMasterSM.start();
        }
        /** @} */

        mStateReceiver = new StateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_STATE);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        /** M: Hotspot Manager */
        filter.addAction(WifiManager.WIFI_HOTSPOT_CLIENTS_CHANGED_ACTION);
        mContext.registerReceiver(mStateReceiver, filter);
        mUsbInternetState = USB_INTERNET_DISCONNECTED;
        mNotificationSync = new Object();

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        filter.addDataScheme("file");
        mContext.registerReceiver(mStateReceiver, filter);

        mDhcpRange = context.getResources().getStringArray(
                com.android.internal.R.array.config_tether_dhcp_range);
        if ((mDhcpRange.length == 0) || (mDhcpRange.length % 2 ==1)) {
            mDhcpRange = DHCP_DEFAULT_RANGE;
        }

        // load device config info
        updateConfiguration();

        // TODO - remove and rely on real notifications of the current iface
        mDefaultDnsServers = new String[2];
        mDefaultDnsServers[0] = DNS_DEFAULT_SERVER1;
        mDefaultDnsServers[1] = DNS_DEFAULT_SERVER2;

        mWifiIface = SystemProperties.get("wifi.interface", "wlan0");

        /// M: Hotspot Manager
        mUsbInternetEnable = false;
        mUsbInternetSystemType = USB_INTERNET_SYSTEM_DEFAULT;
        mUsbInternetGateway = USB_INTERNET_DNS_SERVER1[mUsbInternetSystemType];
        mUsbInternetDnsServers = new String[2];
        mUsbInternetDnsServers[0] = USB_INTERNET_DNS_SERVER1[mUsbInternetSystemType];
        mUsbInternetDnsServers[1] = USB_INTERNET_DNS_SERVER2;

        /** M: ipv6 tethering @{ */
        try {
            nmService.registerIpv6Observer(mIpv6IfaceObserver);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        /* @} */
    }

    void updateConfiguration() {
        String[] tetherableUsbRegexs = mContext.getResources().getStringArray(
                com.android.internal.R.array.config_tether_usb_regexs);
        String[] tetherableWifiRegexs = mContext.getResources().getStringArray(
                com.android.internal.R.array.config_tether_wifi_regexs);
        String[] tetherableBluetoothRegexs = mContext.getResources().getStringArray(
                com.android.internal.R.array.config_tether_bluetooth_regexs);

        int ifaceTypes[] = mContext.getResources().getIntArray(
                com.android.internal.R.array.config_tether_upstream_types);
        Collection<Integer> upstreamIfaceTypes = new ArrayList();
        for (int i : ifaceTypes) {
            if (VDBG) Log.d(TAG, "upstreamIfaceTypes.add:" + i);
            upstreamIfaceTypes.add(new Integer(i));
        }

        synchronized (mPublicSync) {
            mTetherableUsbRegexs = tetherableUsbRegexs;
            mTetherableWifiRegexs = tetherableWifiRegexs;
            mTetherableBluetoothRegexs = tetherableBluetoothRegexs;
            mUpstreamIfaceTypes = upstreamIfaceTypes;
        }

        // check if the upstream type list needs to be modified due to secure-settings
        checkDunRequired();

        /** M: ipv6 tethering @{ */
        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
            mIpv6FeatureEnable = readIpv6FeatureEnable();
        }
        /** @} */
    }

    public void interfaceStatusChanged(String iface, boolean up) {
        if (VDBG) Log.d(TAG, "interfaceStatusChanged " + iface + ", " + up);
        boolean found = false;
        boolean usb = false;
        synchronized (mPublicSync) {
            if (isWifi(iface)) {
                found = true;
            } else if (isUsb(iface)) {
                found = true;
                usb = true;
            } else if (isBluetooth(iface)) {
                found = true;
            }
            if (found == false) return;

            TetherInterfaceSM sm = mIfaces.get(iface);
            if (up) {
                if (sm == null) {
                    sm = new TetherInterfaceSM(iface, mLooper, usb);
                    mIfaces.put(iface, sm);
                    sm.start();
                }
            } else {
                /** M: ignore btn0 down event as well */
                if (isUsb(iface) || isBluetooth(iface)) {
                    // ignore usb0 down after enabling RNDIS
                    // we will handle disconnect in interfaceRemoved instead
                    if (VDBG) Log.d(TAG, "ignore interface down for " + iface);
                } else if (sm != null) {
                    Xlog.d(TAG,"interfaceLinkStatusChanged, sm!=null, sendMessage:CMD_INTERFACE_DOWN");
                    sm.sendMessage(TetherInterfaceSM.CMD_INTERFACE_DOWN);
                    mIfaces.remove(iface);
                }
            }
        }
    }

    public void interfaceLinkStateChanged(String iface, boolean up) {
        if (VDBG) Log.d(TAG, "interfaceLinkStateChanged " + iface + ", " + up);
        interfaceStatusChanged(iface, up);
    }

    private boolean isUsb(String iface) {
        synchronized (mPublicSync) {
            for (String regex : mTetherableUsbRegexs) {
                if (iface.matches(regex)) return true;
            }
            return false;
        }
    }

    public boolean isWifi(String iface) {
        synchronized (mPublicSync) {
            for (String regex : mTetherableWifiRegexs) {
                if (iface.matches(regex)) return true;
            }
            return false;
        }
    }

    public boolean isBluetooth(String iface) {
        synchronized (mPublicSync) {
            for (String regex : mTetherableBluetoothRegexs) {
                if (iface.matches(regex)) return true;
            }
            return false;
        }
    }

    public void interfaceAdded(String iface) {
        if (VDBG) Log.d(TAG, "interfaceAdded " + iface);
        boolean found = false;
        boolean usb = false;
        synchronized (mPublicSync) {
            if (isWifi(iface)) {
                found = true;
            }
            if (isUsb(iface)) {
                found = true;
                usb = true;
            }
            if (isBluetooth(iface)) {
                found = true;
            }
            if (found == false) {
                if (VDBG) Log.d(TAG, iface + " is not a tetherable iface, ignoring");
                return;
            }

            TetherInterfaceSM sm = mIfaces.get(iface);
            if (sm != null) {
                if (VDBG) Log.d(TAG, "active iface (" + iface + ") reported as added, ignoring");
                return;
            }

            /** M: for bug solving @{ */
            /*if (isWifi(iface)) {
                IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
                final INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);
                IBinder binder = ServiceManager.getService(Context.WIFI_SERVICE);
                final IWifiManager wifiService = IWifiManager.Stub.asInterface(binder);
                if (service != null && wifiService != null) {
                    new Thread() {
                        public void run() {
                            try {
                                if (!service.getStartRequest()) {
                                    WifiConfiguration config = wifiService.getWifiApConfiguration();
                                    service.restartAccessPoint(config, SystemProperties.get("wifi.interface", "wlan0"), "ap0");
                                }
                            } catch (Exception e) {
                                Xlog.e(TAG, "restartAccessPoint failed!");
                            }
                        }
                    }.start();
                } else {
                    Xlog.e(TAG, "Failed to get the NetworkManagementService or WifiService!");
                }
            }/** @} */

            sm = new TetherInterfaceSM(iface, mLooper, usb);
            mIfaces.put(iface, sm);
            sm.start();
        }
        Xlog.d(TAG, "interfaceAdded :" + iface);
    }

    public void interfaceRemoved(String iface) {
        if (VDBG) Log.d(TAG, "interfaceRemoved " + iface);
        synchronized (mPublicSync) {
            TetherInterfaceSM sm = mIfaces.get(iface);
            if (sm == null) {
                if (VDBG) {
                    Log.e(TAG, "attempting to remove unknown iface (" + iface + "), ignoring");
                }
                return;
            }
            Xlog.d(TAG, "interfaceRemoved, iface=" + iface + ", sendMessage:CMD_INTERFACE_DOWN");
            sm.sendMessage(TetherInterfaceSM.CMD_INTERFACE_DOWN);
            mIfaces.remove(iface);
            /** M: for bug solving @{ */
            if (isWifi(iface)) {
                IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
                INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);
                if (null == service) {
                    Xlog.e(TAG, "Failed to get the NetworkManagementService!");
                    return;
                }
                try {
                    service.setStartRequest(false);
                } catch (Exception e) {
                    Xlog.e(TAG, "setStartRequest failed!");
                }
            }/** @} */
        }
    }

    public void limitReached(String limitName, String iface) {}

    public void interfaceClassDataActivityChanged(String label, boolean active) {}

    public int tether(String iface) {
        if (DBG) Log.d(TAG, "Tethering " + iface);
        TetherInterfaceSM sm = null;
        synchronized (mPublicSync) {
            sm = mIfaces.get(iface);
        }
        if (sm == null) {
            Log.e(TAG, "Tried to Tether an unknown iface :" + iface + ", ignoring");
            return ConnectivityManager.TETHER_ERROR_UNKNOWN_IFACE;
        }
        if (!sm.isAvailable() && !sm.isErrored()) {
            Log.e(TAG, "Tried to Tether an unavailable iface :" + iface + ", ignoring");
            return ConnectivityManager.TETHER_ERROR_UNAVAIL_IFACE;
        }
        sm.sendMessage(TetherInterfaceSM.CMD_TETHER_REQUESTED);
        return ConnectivityManager.TETHER_ERROR_NO_ERROR;
    }

    public int untether(String iface) {
        if (DBG) Log.d(TAG, "Untethering " + iface);
        TetherInterfaceSM sm = null;
        synchronized (mPublicSync) {
            sm = mIfaces.get(iface);
        }
        if (sm == null) {
            Log.e(TAG, "Tried to Untether an unknown iface :" + iface + ", ignoring");
            return ConnectivityManager.TETHER_ERROR_UNKNOWN_IFACE;
        }
        if (sm.isErrored()) {
            Log.e(TAG, "Tried to Untethered an errored iface :" + iface + ", ignoring");
            return ConnectivityManager.TETHER_ERROR_UNAVAIL_IFACE;
        }
        sm.sendMessage(TetherInterfaceSM.CMD_TETHER_UNREQUESTED);
        return ConnectivityManager.TETHER_ERROR_NO_ERROR;
    }

    public int getLastTetherError(String iface) {
        TetherInterfaceSM sm = null;
        synchronized (mPublicSync) {
            sm = mIfaces.get(iface);
            if (sm == null) {
                Log.e(TAG, "Tried to getLastTetherError on an unknown iface :" + iface +
                        ", ignoring");
                return ConnectivityManager.TETHER_ERROR_UNKNOWN_IFACE;
            }
            return sm.getLastError();
        }
    }

    // TODO - move all private methods used only by the state machine into the state machine
    // to clarify what needs synchronized protection.
    private void sendTetherStateChangedBroadcast() {
        try {
            if (!mConnService.isTetheringSupported()) return;
        } catch (RemoteException e) {
            return;
        }
        Xlog.d(TAG, "sendTetherStateChangedBroadcast");

        ArrayList<String> availableList = new ArrayList<String>();
        ArrayList<String> activeList = new ArrayList<String>();
        ArrayList<String> erroredList = new ArrayList<String>();

        boolean wifiTethered = false;
        boolean usbTethered = false;
        boolean bluetoothTethered = false;

        synchronized (mPublicSync) {
            Set ifaces = mIfaces.keySet();
            for (Object iface : ifaces) {
                TetherInterfaceSM sm = mIfaces.get(iface);
                if (sm != null) {
                    if (sm.isErrored()) {
                        Xlog.d(TAG, "add err");
                        erroredList.add((String)iface);
                    } else if (sm.isAvailable()) {
                        Xlog.d(TAG, "add avai");
                        availableList.add((String)iface);
                    } else if (sm.isTethered()) {
                        if (isUsb((String)iface)) {
                             Xlog.d(TAG, "usb isTethered");
                            mUsbIface = (String) iface;
                            usbTethered = true;
                        } else if (isWifi((String)iface)) {
                            Xlog.d(TAG, "wifi isTethered");
                            wifiTethered = true;
                        } else if (isBluetooth((String)iface)) {
                            Xlog.d(TAG, "bt isTethered");
                            bluetoothTethered = true;
                        }
                        activeList.add((String)iface);
                    }
                }
            }
        }
        Intent broadcast = new Intent(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        broadcast.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING |
                Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        broadcast.putStringArrayListExtra(ConnectivityManager.EXTRA_AVAILABLE_TETHER,
                availableList);
        broadcast.putStringArrayListExtra(ConnectivityManager.EXTRA_ACTIVE_TETHER, activeList);
        broadcast.putStringArrayListExtra(ConnectivityManager.EXTRA_ERRORED_TETHER,
                erroredList);
        /** M: for bug solving, ALPS00331223 */
        broadcast.putExtra("UnTetherDone", mUnTetherDone);
        broadcast.putExtra("TetherDone", mTetherDone);
        broadcast.putExtra("TetherFail", mTetheredFail);

        mContext.sendStickyBroadcastAsUser(broadcast, UserHandle.ALL);
        if (DBG) {
            Log.d(TAG, "sendTetherStateChangedBroadcast " + availableList.size() + ", " +
                    activeList.size() + ", " + erroredList.size());
        }

        /** M: Modified by Hotspot Manager @{ */
        if (usbTethered) {
            if (wifiTethered || bluetoothTethered) {
                showTetheredNotification(com.android.internal.R.drawable.stat_sys_tether_general, "comb");
            } else {
                showTetheredNotification(com.android.internal.R.drawable.stat_sys_tether_usb, "usb");
            }
            broadcastDataConnectionStateChanged(USB_INTERNET_CONNECTED);
        } else if (wifiTethered) {
            if (bluetoothTethered) {
                showTetheredNotification(com.android.internal.R.drawable.stat_sys_tether_general, "comb");
            } else {
                showTetheredNotification(com.android.internal.R.drawable.stat_sys_tether_wifi, "wifi");
            }
            broadcastDataConnectionStateChanged(USB_INTERNET_DISCONNECTED);
        } else if (bluetoothTethered) {
            showTetheredNotification(com.android.internal.R.drawable.stat_sys_tether_bluetooth, "bt");
            broadcastDataConnectionStateChanged(USB_INTERNET_DISCONNECTED);
        } else {
            clearTetheredNotification();
            broadcastDataConnectionStateChanged(USB_INTERNET_DISCONNECTED);
        }
    }   /** @} */

    private void showTetheredNotification(int icon, String type) {

        synchronized (Tethering.this.mNotificationSync) {
             NotificationManager notificationManager =
                     (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
             if (notificationManager == null) {
                 return;
             }
             /** M: Hotspot Manager @{ */
             WifiManager mgr = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
             if (mgr == null) {
                 return;
             }
             /** @} */
             if (mTetheredNotification != null) {
                 if (mTetheredNotification.icon == icon) {
                     return;
                 }
                 notificationManager.cancelAsUser(null, mTetheredNotification.icon,
                         UserHandle.ALL);
             }

             Intent intent = new Intent();
             /** M: Modified by Hotspot Manager @{ */
             if (FeatureOption.MTK_BSP_PACKAGE) {
                 intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
             } else {
                 if (mUsbInternetEnable) {
                     intent.setClassName("com.android.settings", "com.android.settings.Settings$WirelessSettingsActivity");
                 } else if ("wifi".equals(type)) {
                     intent.setClassName("com.android.settings", "com.android.settings.wifi.hotspot.TetherWifiSettings");
                 } else {
                     intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
                 }
             }
             /** @} */
             intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

             PendingIntent pi = PendingIntent.getActivityAsUser(mContext, 0, intent, 0,
                     null, UserHandle.CURRENT);

             Resources r = Resources.getSystem();
             CharSequence title = r.getText(com.android.internal.R.string.tethered_notification_title);
             /** M: Modified by Hotspot Manager @{ */
             CharSequence message = null;
             if (mUsbInternetEnable) {
                 title = r.getText(com.mediatek.internal.R.string.usbint_notification_title);
             }
             if (FeatureOption.MTK_BSP_PACKAGE) {
                 message = r.getString(com.android.internal.R.string.tethered_notification_message);
             } else {
                 if ("wifi".equals(type) || "comb".equals(type)) {
                     List<HotspotClient> clients = mgr.getHotspotClients();
                     int connected = 0;
                     int blocked = 0;
                     if (clients != null) {
                         for (HotspotClient client : clients) {
                             if (client.isBlocked) {
                                 blocked++;
                             }
                         }
                         connected = clients.size() - blocked;
                     }
                     message = r.getString(com.mediatek.internal.R.string.tethered_notification_message_for_hotspot, connected, blocked);
         
                 } else {
                     message = r.getString(com.android.internal.R.string.tethered_notification_message);
                 }
             }
             /** @} */
             if (mTetheredNotification == null) {
                 mTetheredNotification = new Notification();
                 mTetheredNotification.when = 0;
             }
             mTetheredNotification.icon = icon;
             mTetheredNotification.defaults &= ~Notification.DEFAULT_SOUND;
             mTetheredNotification.flags = Notification.FLAG_ONGOING_EVENT;
             mTetheredNotification.tickerText = title;
             mTetheredNotification.setLatestEventInfo(mContext, title, message, pi);

             notificationManager.notifyAsUser(null, mTetheredNotification.icon,
                     mTetheredNotification, UserHandle.ALL);
        }
    }

    /** M: Hotspot Manager @{ */
    private void updateTetheredNotification() {
        synchronized (Tethering.this.mNotificationSync) {
              NotificationManager notificationManager =
                      (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
              WifiManager mgr = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
              if (notificationManager == null || mgr == null || mTetheredNotification == null) {
                  return;
              }
              List<HotspotClient> clients = mgr.getHotspotClients();
              int connected = 0;
              int blocked = 0;
              if (clients != null) {
                  for (HotspotClient client : clients) {
                      if (client.isBlocked) {
                          blocked++;
                      }
                  }
                  connected = clients.size() - blocked;
              }
              Resources r = Resources.getSystem();
              CharSequence title = r.getText(com.android.internal.R.string.tethered_notification_title);
              /** M: Modified by Hotspot Manager @{ */
              String message = null;
              if (FeatureOption.MTK_BSP_PACKAGE){
                  message = r.getString(com.android.internal.R.string.tethered_notification_message);
              } else {
                  message = r.getString(com.mediatek.internal.R.string.tethered_notification_message_for_hotspot, connected, blocked);
              }
              /** @} */
              mTetheredNotification.setLatestEventInfo(mContext, title, message, mTetheredNotification.contentIntent);
              notificationManager.notify(mTetheredNotification.icon, mTetheredNotification);
        }
    }
    /** @} */

    private void clearTetheredNotification() {
        synchronized (Tethering.this.mNotificationSync) {
           NotificationManager notificationManager =
               (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
           if (notificationManager != null && mTetheredNotification != null) {
               notificationManager.cancelAsUser(null, mTetheredNotification.icon,
                       UserHandle.ALL);
               mTetheredNotification = null;
           }
        }
    }

    private class StateReceiver extends BroadcastReceiver {
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            Xlog.d(TAG, "StateReceiver onReceive action:" + action);
            if (action.equals(UsbManager.ACTION_USB_STATE)) {
                synchronized (Tethering.this.mPublicSync) {
                    /** M: for bug solving, ALPS00331223 */
                    boolean usbConfigured = intent.getBooleanExtra(UsbManager.USB_CONFIGURED, false);

                    boolean usbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
                    /** M: Hotspot Manager @{ */
                    if (!usbConnected) {
                        Xlog.d(TAG, "set mUsbInternetEnable false due to usb disconnect");
                        mUsbInternetEnable = false;
                    }
                    /** @} */
                    /** M: for bug solving, ALPS00233672 */
                    boolean oriRndisEnabled = mRndisEnabled;

                    /** EEM Support */
                    //mRndisEnabled = intent.getBooleanExtra(UsbManager.USB_FUNCTION_RNDIS, false);
                    /// M: @{
                    mRndisEnabled = intent.getBooleanExtra(UsbManager.USB_FUNCTION_RNDIS, false) || intent.getBooleanExtra(UsbManager.USB_FUNCTION_EEM, false) ;
                    /// @}

                    /** M: for bug solving, ALPS00233672 @{ */
                    Xlog.d(TAG, "StateReceiver onReceive action synchronized: usbConnected = " + usbConnected +
                        " usbConfigured = " + usbConfigured + ", mRndisEnabled = " + mRndisEnabled +
                        ", mUsbTetherRequested = " + mUsbTetherRequested);

                    Xlog.d(TAG, "StateReceiver onReceive action synchronized: mUsbTetherEnabled = " + mUsbTetherEnabled);
                    //check that if the UI state is sync with mRndisEnabled state
                    if(!mUsbTetherEnabled)
                    {
                        if(mRndisEnabled && (mRndisEnabled!=oriRndisEnabled))
                        {
                            //The state of UI and USB is not synced
                            //The USB tethering is enabled without UI checked
                            //disable the rndis function
                            Xlog.d(TAG, "StateReceiver onReceive action synchronized: mUsbTetherEnabled = " + mUsbTetherEnabled +
                            ", mRndisEnabled = " + mRndisEnabled + ", oriRndisEnabled = " + oriRndisEnabled);
                            tetherUsb(false);
                            UsbManager usbManager = (UsbManager)mContext.getSystemService(Context.USB_SERVICE);
                            usbManager.setCurrentFunction(null, false);

                            mUsbTetherRequested = false;
                        }
                    } /** @} */
                    // start tethering if we have a request pending
                    /** M: for bug solving, ALPS00331223 */
                    if (usbConnected && mRndisEnabled && mUsbTetherRequested && usbConfigured) {
                        Xlog.d(TAG, "StateReceiver onReceive action synchronized: usbConnected && mRndisEnabled && mUsbTetherRequested, tetherUsb!! ");
                        tetherUsb(true);
                        /** M: for bug solving, ALPS00233672 */
                        mUsbTetherRequested = false;
                    }
                    /** M: for bug solving, ALPS00233672 */
                    //mUsbTetherRequested = false;
                }
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra(
                        ConnectivityManager.EXTRA_NETWORK_INFO);
                if (VDBG) Log.d(TAG, "Tethering got CONNECTIVITY_ACTION, networkInfo:" + networkInfo);
                if (networkInfo != null &&
                        networkInfo.getDetailedState() != NetworkInfo.DetailedState.FAILED) {
                   if (VDBG) Log.d(TAG, "Tethering got CONNECTIVITY_ACTION");
                   mTetherMasterSM.sendMessage(TetherMasterSM.CMD_UPSTREAM_CHANGED);
                   /** M: ipv6 tethering @{ */
                   if (isIpv6MasterSmOn()) {
                       mIpv6TetherMasterSM.sendMessage(TetherMasterSM.CMD_UPSTREAM_CHANGED);
                   }
                }

                /** @} */
            /** M: Hotspot Manager @{ */
            } else if (action.equals(WifiManager.WIFI_HOTSPOT_CLIENTS_CHANGED_ACTION)) {
                updateTetheredNotification();
            }
            /** @} */
        }
    }

    private void tetherUsb(boolean enable) {
        if (VDBG) Log.d(TAG, "tetherUsb " + enable);

        //mTetheredFail = false ;
        String[] ifaces = new String[0];
        try {
            ifaces = mNMService.listInterfaces();
            for (String iface : ifaces) {
               if (isUsb(iface)) {
                    int result = (enable ? tether(iface) : untether(iface));
                    if (result == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error listing Interfaces", e);
            //return;
        }


        /** M: for bug solving, ALPS00331223 */
        mTetheredFail = true ;
        SystemClock.sleep(500);
        sendTetherStateChangedBroadcast() ;

        Log.e(TAG, "unable start or stop USB tethering");
    }

    // configured when we start tethering and unconfig'd on error or conclusion
    private boolean configureUsbIface(boolean enabled) {
        if (VDBG) Log.d(TAG, "configureUsbIface(" + enabled + ")");

        // toggle the USB interfaces
        String[] ifaces = new String[0];
        try {
            ifaces = mNMService.listInterfaces();
        } catch (Exception e) {
            Log.e(TAG, "Error listing Interfaces", e);
            return false;
        }
        for (String iface : ifaces) {
            if (isUsb(iface)) {
                InterfaceConfiguration ifcg = null;
                try {
                    ifcg = mNMService.getInterfaceConfig(iface);
                    if (ifcg != null) {
                        InetAddress addr = NetworkUtils.numericToInetAddress(USB_NEAR_IFACE_ADDR);
                        ifcg.setLinkAddress(new LinkAddress(addr, USB_PREFIX_LENGTH));
                        if (enabled) {
                            ifcg.setInterfaceUp();
                        } else {
                            ifcg.setInterfaceDown();
                        }
                        ifcg.clearFlag("running");
                        mNMService.setInterfaceConfig(iface, ifcg);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error configuring interface " + iface, e);
                    return false;
                }
            }
         }

        return true;
    }

    // TODO - return copies so people can't tamper
    public String[] getTetherableUsbRegexs() {
        return mTetherableUsbRegexs;
    }

    public String[] getTetherableWifiRegexs() {
        return mTetherableWifiRegexs;
    }

    public String[] getTetherableBluetoothRegexs() {
        return mTetherableBluetoothRegexs;
    }

    public int setUsbTethering(boolean enable) {
        if (VDBG) Log.d(TAG, "setUsbTethering(" + enable + ")");
         UsbManager usbManager = (UsbManager)mContext.getSystemService(Context.USB_SERVICE);
        int value ;

        /** M: ALPS00233672 */
        mUsbTetherEnabled = enable;

        synchronized (mPublicSync) {
            /** M: for bug solving, ALPS00331223 */
            mTetheredFail = false ;
            if (enable) {
                mTetherDone = false ;
                if (mRndisEnabled) {
                    tetherUsb(true);
                } else {
                    mUsbTetherRequested = true;
                    /// M: @{
                    /** EEM Support */
                    //usbManager.setCurrentFunction(UsbManager.USB_FUNCTION_RNDIS, false);
                    value = Settings.System.getInt(mContext.getContentResolver(), Settings.System.USB_TETHERING_TYPE, Settings.System.USB_TETHERING_TYPE_DEFAULT);
                    if ((value == Settings.System.USB_TETHERING_TYPE_EEM) && FeatureOption.MTK_TETHERING_EEM_SUPPORT){
                            Xlog.d(TAG, "The MTK_TETHERING_EEM_SUPPORT is True");
                            usbManager.setCurrentFunction(UsbManager.USB_FUNCTION_EEM, false);
                    }else{
                            Xlog.d(TAG, "The MTK_TETHERING_RNDIS only");
                            usbManager.setCurrentFunction(UsbManager.USB_FUNCTION_RNDIS, false);
                    }
                    /// @}
                }
            } else {
                mUnTetherDone = false ;
                tetherUsb(false);
                if (mRndisEnabled) {
                    usbManager.setCurrentFunction(null, false);
                }
                mUsbTetherRequested = false;
            }
        }
        return ConnectivityManager.TETHER_ERROR_NO_ERROR;
    }

    public int[] getUpstreamIfaceTypes() {
        int values[];
        synchronized (mPublicSync) {
            updateConfiguration();
            values = new int[mUpstreamIfaceTypes.size()];
            Iterator<Integer> iterator = mUpstreamIfaceTypes.iterator();
            for (int i=0; i < mUpstreamIfaceTypes.size(); i++) {
                values[i] = iterator.next();
            }
        }
        return values;
    }

    public void checkDunRequired() {
        int secureSetting = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.TETHER_DUN_REQUIRED, 2);
        synchronized (mPublicSync) {
            // 2 = not set, 0 = DUN not required, 1 = DUN required
            if (VDBG) Log.d(TAG, "checkDunRequired:" + secureSetting);
            if (secureSetting != 2) {
                int requiredApn = (secureSetting == 1 ?
                        ConnectivityManager.TYPE_MOBILE_DUN :
                        ConnectivityManager.TYPE_MOBILE_HIPRI);
                if (requiredApn == ConnectivityManager.TYPE_MOBILE_DUN) {
                    while (mUpstreamIfaceTypes.contains(MOBILE_TYPE)) {
                        mUpstreamIfaceTypes.remove(MOBILE_TYPE);
                    }
                    while (mUpstreamIfaceTypes.contains(HIPRI_TYPE)) {
                        mUpstreamIfaceTypes.remove(HIPRI_TYPE);
                    }
                    if (mUpstreamIfaceTypes.contains(DUN_TYPE) == false) {
                        mUpstreamIfaceTypes.add(DUN_TYPE);
                    }
                } else {
                    while (mUpstreamIfaceTypes.contains(DUN_TYPE)) {
                        mUpstreamIfaceTypes.remove(DUN_TYPE);
                    }
                    if (mUpstreamIfaceTypes.contains(MOBILE_TYPE) == false) {
                        mUpstreamIfaceTypes.add(MOBILE_TYPE);
                    }
                    if (mUpstreamIfaceTypes.contains(HIPRI_TYPE) == false) {
                        mUpstreamIfaceTypes.add(HIPRI_TYPE);
                    }
                }
            }
            if (mUpstreamIfaceTypes.contains(DUN_TYPE)) {
                mPreferredUpstreamMobileApn = ConnectivityManager.TYPE_MOBILE_DUN;
            } else {
                mPreferredUpstreamMobileApn = ConnectivityManager.TYPE_MOBILE_HIPRI;
            }
        }
    }

    // TODO review API - maybe return ArrayList<String> here and below?
    public String[] getTetheredIfaces() {
        ArrayList<String> list = new ArrayList<String>();
        synchronized (mPublicSync) {
            Set keys = mIfaces.keySet();
            for (Object key : keys) {
                TetherInterfaceSM sm = mIfaces.get(key);
                if (sm.isTethered()) {
                    list.add((String)key);
                }
            }
        }
        String[] retVal = new String[list.size()];
        for (int i=0; i < list.size(); i++) {
            retVal[i] = list.get(i);
        }
        return retVal;
    }

    public String[] getTetheredIfacePairs() {
        final ArrayList<String> list = Lists.newArrayList();
        synchronized (mPublicSync) {
            for (TetherInterfaceSM sm : mIfaces.values()) {
                if (sm.isTethered()) {
                    list.add(sm.mMyUpstreamIfaceName);
                    list.add(sm.mIfaceName);
                    Xlog.d(TAG, "getTetheredIfacePairs:" + sm.mMyUpstreamIfaceName + ", " + sm.mIfaceName);
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }

    public String[] getTetherableIfaces() {
        ArrayList<String> list = new ArrayList<String>();
        synchronized (mPublicSync) {
            Set keys = mIfaces.keySet();
            for (Object key : keys) {
                TetherInterfaceSM sm = mIfaces.get(key);
                if (sm.isAvailable()) {
                    list.add((String)key);
                }
            }
        }
        String[] retVal = new String[list.size()];
        for (int i=0; i < list.size(); i++) {
            retVal[i] = list.get(i);
        }
        return retVal;
    }

    public String[] getErroredIfaces() {
        ArrayList<String> list = new ArrayList<String>();
        synchronized (mPublicSync) {
            Set keys = mIfaces.keySet();
            for (Object key : keys) {
                TetherInterfaceSM sm = mIfaces.get(key);
                if (sm.isErrored()) {
                    list.add((String)key);
                }
            }
        }
        String[] retVal = new String[list.size()];
        for (int i= 0; i< list.size(); i++) {
            retVal[i] = list.get(i);
        }
        return retVal;
    }

    //TODO: Temporary handling upstream change triggered without
    //      CONNECTIVITY_ACTION. Only to accomodate interface
    //      switch during HO.
    //      @see bug/4455071
    public void handleTetherIfaceChange() {
        mTetherMasterSM.sendMessage(TetherMasterSM.CMD_UPSTREAM_CHANGED);
        /** M: ipv6 tethering @{ */
        if (isIpv6MasterSmOn()) {
            mIpv6TetherMasterSM.sendMessage(TetherMasterSM.CMD_UPSTREAM_CHANGED); }
        /** @} */
    }

    /** M: dedicated apn feature
     * @hide
     */
    public boolean isTetheringChangeDone() {
        return mIsTetheringChangeDone;
    }

    /** M: for bug solving @{ */
    private void updateDns() {
        String newdns = SystemProperties.get("net.dns1", DNS_DEFAULT_SERVER2);
        Xlog.d(TAG, "updateDns net.dns1:" + newdns);
        if (null != newdns && !newdns.isEmpty()) {
            try {
                InetAddress address = InetAddress.getByName(newdns);
                if (address instanceof Inet4Address) {
                    mDefaultDnsServers[1] = newdns;
                } else {
                    newdns = SystemProperties.get("net.dns2", DNS_DEFAULT_SERVER2);
                    Xlog.d(TAG, "net.dns2:" + newdns);
                    if (null != newdns && !newdns.isEmpty()) {
                        address = InetAddress.getByName(newdns);
                        if (address instanceof Inet4Address) {
                            mDefaultDnsServers[1] = newdns;
                        } else {
                            Xlog.d(TAG, "Not support IPv6 address or else!");
                        }
                    }
                }
            } catch (Exception e) {
                Xlog.e(TAG, "Exception:" + e.toString());
            }
        }
    }
    /** }@ */

    /** M: ipv6 tethering @{ */
    private boolean isIpv6MasterSmOn() {
        return (FeatureOption.MTK_TETHERINGIPV6_SUPPORT && mIpv6FeatureEnable && !mUsbInternetEnable);
    }

    private boolean readIpv6FeatureEnable() {
        int value = Settings.System.getInt(mContext.getContentResolver(), Settings.System.TETHER_IPV6_FEATURE, 0);
        Xlog.d(TAG, "getIpv6FeatureEnable:" + value);
        return (value == 1);
    }

    public boolean getIpv6FeatureEnable() {
        return mIpv6FeatureEnable;
    }

    public void setIpv6FeatureEnable(boolean enable) {
        Xlog.d(TAG, "setIpv6FeatureEnable:" + enable + " old:" + mIpv6FeatureEnable);
        int value = (enable ? 1 : 0);
        if (mIpv6FeatureEnable != enable) {
            mIpv6FeatureEnable = enable;
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.TETHER_IPV6_FEATURE, value);
        }
    }

    private boolean hasIpv6Address(int networkType) {
        if (ConnectivityManager.TYPE_NONE == networkType)
            return false;

        try {
            LinkProperties netProperties = mConnService.getLinkProperties(networkType);
            String iface = netProperties.getInterfaceName();
            String propertyName = "net.ipv6." + iface + ".prefix";
            String value = SystemProperties.get(propertyName);
            if ( value == null || value.length() == 0){
                Log.d(TAG, "This is No IPv6 prefix!");
                return false;
            } else {
                Log.d(TAG, "This is IPv6 prefix: " + value);
                return true;
            }
        } catch (RemoteException e) {
            Xlog.e(TAG, "[MSM_TetherModeAlive] hasIpv6Address: " + e.toString());
            return false;
        }
    }

    private boolean hasIpv4Address(int networkType) {

        if (ConnectivityManager.TYPE_NONE == networkType)
            return false;

        try {
            LinkProperties netProperties = mConnService.getLinkProperties(networkType);
            for (LinkAddress l : netProperties.getLinkAddresses()) {
                if (l.getAddress() instanceof Inet4Address) {
                    Log.d(TAG, "This is v4 address:" + l.getAddress());
                    return true;
                } else {
                    Log.d(TAG, "address:" + l.getAddress());
                }
            }
        } catch (RemoteException e) {
            Xlog.e(TAG, "[MSM_TetherModeAlive] hasIpv4Address: " + e.toString());
        }


        return false;
    }

    public String[] getTetheredIfacePairsIpv6() {
        final ArrayList<String> list = Lists.newArrayList();
        synchronized (mPublicSync) {
            for (TetherInterfaceSM sm : mIfaces.values()) {
                if (sm.isTethered()) {
                    list.add(sm.mMyUpstreamIfaceNameIpv6);
                    list.add(sm.mIfaceName);
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /*
    * Callback handler implementations.
    */
    private INetworkManagementIpv6EventObserver mIpv6IfaceObserver =
        new INetworkManagementIpv6EventObserver.Stub() {

        public void interfaceStatusChangedIpv6(String iface, boolean up) {
            if (VDBG) Log.d(TAG, "interfaceStatusChangedIpv6 " + iface + ", " + up);
            if (isIpv6MasterSmOn()) {
                mIpv6TetherMasterSM.sendMessage(TetherMasterSM.CMD_UPSTREAM_CHANGED); }
        }
    };

    /** @} */


    class TetherInterfaceSM extends StateMachine {
        // notification from the master SM that it's not in tether mode
        static final int CMD_TETHER_MODE_DEAD            =  1;
        // request from the user that it wants to tether
        static final int CMD_TETHER_REQUESTED            =  2;
        // request from the user that it wants to untether
        static final int CMD_TETHER_UNREQUESTED          =  3;
        // notification that this interface is down
        static final int CMD_INTERFACE_DOWN              =  4;
        // notification that this interface is up
        static final int CMD_INTERFACE_UP                =  5;
        // notification from the master SM that it had an error turning on cellular dun
        static final int CMD_CELL_DUN_ERROR              =  6;
        // notification from the master SM that it had trouble enabling IP Forwarding
        static final int CMD_IP_FORWARDING_ENABLE_ERROR  =  7;
        // notification from the master SM that it had trouble disabling IP Forwarding
        static final int CMD_IP_FORWARDING_DISABLE_ERROR =  8;
        // notification from the master SM that it had trouble staring tethering
        static final int CMD_START_TETHERING_ERROR       =  9;
        // notification from the master SM that it had trouble stopping tethering
        static final int CMD_STOP_TETHERING_ERROR        = 10;
        // notification from the master SM that it had trouble setting the DNS forwarders
        static final int CMD_SET_DNS_FORWARDERS_ERROR    = 11;
        // the upstream connection has changed
        static final int CMD_TETHER_CONNECTION_CHANGED   = 12;

        private State mDefaultState;

        private State mInitialState;
        private State mStartingState;
        private State mTetheredState;

        private State mUnavailableState;

        private boolean mAvailable;
        private boolean mTethered;
        int mLastError;

        String mIfaceName;
        String mMyUpstreamIfaceName;  // may change over time

        /** M: ipv6 tethering */
        String mMyUpstreamIfaceNameIpv6;

        boolean mUsb;

        TetherInterfaceSM(String name, Looper looper, boolean usb) {
            super(name, looper);
            mIfaceName = name;
            mUsb = usb;
            setLastError(ConnectivityManager.TETHER_ERROR_NO_ERROR);

            /** M: ipv6 tethering @{ */
            if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT)
                setLastError(ConnectivityManager.TETHER_ERROR_IPV6_NO_ERROR);
            /** @} */
            mInitialState = new InitialState();
            addState(mInitialState);
            mStartingState = new StartingState();
            addState(mStartingState);
            mTetheredState = new TetheredState();
            addState(mTetheredState);
            mUnavailableState = new UnavailableState();
            addState(mUnavailableState);

            setInitialState(mInitialState);
        }

        public String toString() {
            String res = new String();
            res += mIfaceName + " - ";
            IState current = getCurrentState();
            if (current == mInitialState) res += "InitialState";
            if (current == mStartingState) res += "StartingState";
            if (current == mTetheredState) res += "TetheredState";
            if (current == mUnavailableState) res += "UnavailableState";
            if (mAvailable) res += " - Available";
            if (mTethered) res += " - Tethered";
            res += " - lastError =" + mLastError;
            return res;
        }

        public int getLastError() {
            synchronized (Tethering.this.mPublicSync) {
                Xlog.d(TAG, "getLastError:" + mLastError);
                return mLastError;
            }
        }

        private void setLastError(int error) {
            synchronized (Tethering.this.mPublicSync) {
                /** M: ipv6 tethering @{ */
                if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                    if (error >= ConnectivityManager.TETHER_ERROR_IPV6_NO_ERROR) {
                        //set error for ipv6 status
                        mLastError &= 0x0f;
                        mLastError |= error;
                    } else {
                        //set error for ipv4 status
                        mLastError &= 0xf0;
                        mLastError |= error;
                    }
                } else {
                /** @} */
                    mLastError = error; }

                Xlog.d(TAG, "setLastError: "+ mLastError);
                if (isErrored()) {
                    if (mUsb) {
                        // note everything's been unwound by this point so nothing to do on
                        // further error..
                        Tethering.this.configureUsbIface(false);
                    }
                }
            }
        }

        public boolean isAvailable() {
            synchronized (Tethering.this.mPublicSync) {
                return mAvailable;
            }
        }

        private void setAvailable(boolean available) {
            synchronized (Tethering.this.mPublicSync) {
                mAvailable = available;
            }
        }

        public boolean isTethered() {
            synchronized (Tethering.this.mPublicSync) {
                return mTethered;
            }
        }

        private void setTethered(boolean tethered) {
            synchronized (Tethering.this.mPublicSync) {
                mTethered = tethered;
            }
        }

        public boolean isErrored() {
            synchronized (Tethering.this.mPublicSync) {
                /** M: ipv6 tethering @{ */
                if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                    boolean ret = ((mLastError & 0x0f) != ConnectivityManager.TETHER_ERROR_NO_ERROR) &&
                                  ((mLastError & 0xf0) != ConnectivityManager.TETHER_ERROR_IPV6_NO_ERROR);
                    return ret;
                }
                /** @} */

                return (mLastError != ConnectivityManager.TETHER_ERROR_NO_ERROR);
            }
        }

        class InitialState extends State {
            @Override
            public void enter() {
                Xlog.d(TAG, "[ISM_Initial] enter, sendTetherStateChangedBroadcast");
                setAvailable(true);
                setTethered(false);
                sendTetherStateChangedBroadcast();
            }

            @Override
            public boolean processMessage(Message message) {
                if (DBG) Log.d(TAG, "[ISM_Initial] processMessage what=" + message.what);
                boolean retValue = true;
                switch (message.what) {
                    case CMD_TETHER_REQUESTED:
                        setLastError(ConnectivityManager.TETHER_ERROR_NO_ERROR);
                        mTetherMasterSM.sendMessage(TetherMasterSM.CMD_TETHER_MODE_REQUESTED,
                                TetherInterfaceSM.this);
                        /** M: ipv6 tethering @{ */
                        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                            setLastError(ConnectivityManager.TETHER_ERROR_IPV6_NO_ERROR);
                            if (mIpv6FeatureEnable) {
                                mIpv6TetherMasterSM.sendMessage(TetherMasterSM.CMD_TETHER_MODE_REQUESTED,
                                        TetherInterfaceSM.this);
                             }
                        }
                        /** @} */
                        transitionTo(mStartingState);
                        break;
                    case CMD_INTERFACE_DOWN:
                        transitionTo(mUnavailableState);
                        break;
                    default:
                        retValue = false;
                        break;
                }
                return retValue;
            }
        }

        class StartingState extends State {
            @Override
            public void enter() {
                Xlog.d(TAG, "[ISM_Starting] enter");
                setAvailable(false);
                if (mUsb) {
                    if (!Tethering.this.configureUsbIface(true)) {
                        mTetherMasterSM.sendMessage(TetherMasterSM.CMD_TETHER_MODE_UNREQUESTED,
                                TetherInterfaceSM.this);
                        setLastError(ConnectivityManager.TETHER_ERROR_IFACE_CFG_ERROR);

                        /** M: ipv6 tethering @{ */
                        if (isIpv6MasterSmOn()) {
                            mIpv6TetherMasterSM.sendMessage(TetherMasterSM.CMD_TETHER_MODE_UNREQUESTED,
                                    TetherInterfaceSM.this);
                            setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                        }
                        /** @} */
                        transitionTo(mInitialState);
                        /** M: for bug solving, ALPS00331223 */
                        mTetherDone = true;
                        sendTetherStateChangedBroadcast();
                        return;
                    }
                }
                Xlog.d(TAG, "[ISM_Starting] sendTetherStateChangedBroadcast");
                sendTetherStateChangedBroadcast();

                // Skipping StartingState
                transitionTo(mTetheredState);
            }
            @Override
            public boolean processMessage(Message message) {
                if (DBG) Log.d(TAG, "[ISM_Starting] processMessage what=" + message.what);
                boolean retValue = true;
                switch (message.what) {
                    // maybe a parent class?
                    case CMD_TETHER_UNREQUESTED:
                        mTetherMasterSM.sendMessage(TetherMasterSM.CMD_TETHER_MODE_UNREQUESTED,
                                TetherInterfaceSM.this);
                        /** M: ipv6 tethering @{ */
                        if (isIpv6MasterSmOn()) {
                            mIpv6TetherMasterSM.sendMessage(TetherMasterSM.CMD_TETHER_MODE_UNREQUESTED,
                                TetherInterfaceSM.this);
                        }
                        /** @} */
                        if (mUsb) {
                            if (!Tethering.this.configureUsbIface(false)) {
                                setLastErrorAndTransitionToInitialState(
                                    ConnectivityManager.TETHER_ERROR_IFACE_CFG_ERROR);
                                /** M: ipv6 tethering @{ */
                                if (isIpv6MasterSmOn())
                                    setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                                /** @} */
                                break;
                            }
                        }
                        transitionTo(mInitialState);
                        break;
                    case CMD_CELL_DUN_ERROR:
                    case CMD_IP_FORWARDING_ENABLE_ERROR:
                    case CMD_IP_FORWARDING_DISABLE_ERROR:
                    case CMD_START_TETHERING_ERROR:
                    case CMD_STOP_TETHERING_ERROR:
                    case CMD_SET_DNS_FORWARDERS_ERROR:
                        setLastErrorAndTransitionToInitialState(
                                ConnectivityManager.TETHER_ERROR_MASTER_ERROR);
                        /** M: ipv6 tethering @{ */
                        if (isIpv6MasterSmOn())
                            setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                        /** @} */
                        break;
                    case CMD_INTERFACE_DOWN:
                        mTetherMasterSM.sendMessage(TetherMasterSM.CMD_TETHER_MODE_UNREQUESTED,
                                TetherInterfaceSM.this);
                        /** M: ipv6 tethering @{ */
                        if (isIpv6MasterSmOn()) {
                            mIpv6TetherMasterSM.sendMessage(TetherMasterSM.CMD_TETHER_MODE_UNREQUESTED,
                                TetherInterfaceSM.this);
                        }
                        /** @} */
                        transitionTo(mUnavailableState);
                        break;
                    default:
                        retValue = false;
                }
                return retValue;
            }
        }

        class TetheredState extends State {
            @Override
            public void enter() {
                Xlog.d(TAG, "[ISM_Tethered] enter");
                try {
                    mNMService.tetherInterface(mIfaceName);
                    /** M: enable dhcpv6 @{ */
                    if(isIpv6MasterSmOn()) {
                        mNMService.setDhcpv6Enabled(true, mIfaceName);
                    }
                    /** @} */
                } catch (Exception e) {
                    Log.e(TAG, "[ISM_Tethered] Error Tethering: " + e.toString());
                    setLastError(ConnectivityManager.TETHER_ERROR_TETHER_IFACE_ERROR);
                    /** M: ipv6 tethering @{ */
                    if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT)
                            setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                    /** @} */

                    transitionTo(mInitialState);
                    return;
                }
                if (DBG) Log.d(TAG, "[ISM_Tethered] Tethered " + mIfaceName);
                setAvailable(false);
                setTethered(true);
                /** M: for bug solving, ALPS00331223 */
                mTetherDone = true ;
                Xlog.d(TAG,"[ISM_Tethered] sendTetherStateChangedBroadcast");
                sendTetherStateChangedBroadcast();
            }

            private void cleanupUpstream() {
                if (mMyUpstreamIfaceName != null) {
                    // note that we don't care about errors here.
                    // sometimes interfaces are gone before we get
                    // to remove their rules, which generates errors.
                    // just do the best we can.
                    try {
                        // about to tear down NAT; gather remaining statistics
                        mStatsService.forceUpdate();
                    } catch (Exception e) {
                        if (VDBG) Log.e(TAG, "[ISM_Tethered] Exception in forceUpdate: " + e.toString());
                    }
                    try {
                        mNMService.disableNat(mIfaceName, mMyUpstreamIfaceName);
                        Xlog.d(TAG, "[ISM_Tethered] cleanupUpstream disableNat(" + mIfaceName + ", " + mMyUpstreamIfaceName + ")");
                    } catch (Exception e) {
                        if (VDBG) Log.e(TAG, "[ISM_Tethered] Exception in disableNat: " + e.toString());
                    }
                    mMyUpstreamIfaceName = null;
                }
                return;
            }

            /** M: ipv6 tethering @{ */
            private void cleanupUpstreamIpv6() {
                if (mMyUpstreamIfaceNameIpv6 != null) {

                    try {
                            mNMService.disableNatIpv6(mIfaceName, mMyUpstreamIfaceNameIpv6);
                            Xlog.d(TAG, "[ISM_Tethered] cleanupUpstream disableNatIpv6("+ mIfaceName + ", " + mMyUpstreamIfaceNameIpv6 + ")");
                    } catch (Exception e) {
                        if (VDBG) Log.e(TAG, "[ISM_Tethered] Exception in disableNatIpv6: " + e.toString());
                    }
                    mMyUpstreamIfaceNameIpv6 = null;
                }
                return;
            }
            /** @} */

            @Override
            public boolean processMessage(Message message) {
                if (DBG) Log.d(TAG, "[ISM_Tethered] processMessage what=" + message.what);
                boolean retValue = true;
                boolean error = false;
                switch (message.what) {
                    case CMD_TETHER_UNREQUESTED:
                    case CMD_INTERFACE_DOWN:
                        Xlog.d(TAG, "[ISM_Tethered] mMyUpstreamIfaceName: " + mMyUpstreamIfaceName);
                        cleanupUpstream();
                        /** M: ipv6 tethering @{ */
                        if (isIpv6MasterSmOn())
                            cleanupUpstreamIpv6();
                        /** @} */
                        try {
                            mNMService.untetherInterface(mIfaceName);
                            /** M: disable dhcpv6 @{ */
                            if(isIpv6MasterSmOn()) {
                                mNMService.setDhcpv6Enabled(false, mIfaceName);
                            }
                            /** @} */
                        } catch (Exception e) {
                            setLastErrorAndTransitionToInitialState(
                                    ConnectivityManager.TETHER_ERROR_UNTETHER_IFACE_ERROR);
                            /** M: ipv6 tethering @{ */
                            if (isIpv6MasterSmOn())
                                setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                            /** @} */
                            break;
                        }
                        mTetherMasterSM.sendMessage(TetherMasterSM.CMD_TETHER_MODE_UNREQUESTED,
                                TetherInterfaceSM.this);
                        /** M: ipv6 tethering @{ */
                        if (isIpv6MasterSmOn()) {
                            mIpv6TetherMasterSM.sendMessage(TetherMasterSM.CMD_TETHER_MODE_UNREQUESTED,
                                TetherInterfaceSM.this);
                        }
                        /** @} */
                        if (message.what == CMD_TETHER_UNREQUESTED) {
                            if (mUsb) {
                                if (!Tethering.this.configureUsbIface(false)) {
                                    setLastError(
                                            ConnectivityManager.TETHER_ERROR_IFACE_CFG_ERROR);
                                    /** M: ipv6 tethering @{ */
                                    if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT)
                                        setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                                    /** @} */
                                }
                            }
                            transitionTo(mInitialState);
                        } else if (message.what == CMD_INTERFACE_DOWN) {
                            transitionTo(mUnavailableState);
                        }
                        if (DBG) Log.d(TAG, "[ISM_Tethered] Untethered " + mIfaceName);
                        break;
                    case CMD_TETHER_CONNECTION_CHANGED:
                        /** M: ipv6 tethering @{ */
                        String s = (String)(message.obj);
                        String newUpstreamIfaceName = null;
                        String smName = null;
                        if (isIpv6MasterSmOn()) {
                            if (s != null) {
                                Xlog.d(TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED s:" + s);
                                String [] IfaceNameSmNames = s.split(",");
                                if (IfaceNameSmNames.length > 1) {
                                    Xlog.d(TAG, "[ISM_Tethered] IfaceNameSmNames[0]:" + IfaceNameSmNames[0] + " IfaceNameSmNames[1]:" + IfaceNameSmNames[1]);
                                    newUpstreamIfaceName = IfaceNameSmNames[0];
                                    smName = IfaceNameSmNames[1];
                                    if ("empty".equals(newUpstreamIfaceName))
                                        newUpstreamIfaceName = null;
                                }
                            }
                        } else {
                            newUpstreamIfaceName = s;
                        }
                        Xlog.d(TAG, "[ISM_Tethered:"+ smName +"] CMD_TETHER_CONNECTION_CHANGED mMyUpstreamIfaceName: " +mMyUpstreamIfaceName +
                            ", mMyUpstreamIfaceNameIpv6:" + mMyUpstreamIfaceNameIpv6 +
                            ", newUpstreamIfaceName: " + newUpstreamIfaceName);

                        if (newUpstreamIfaceName == null &&
                            isIpv6MasterSmOn() &&
                            MASTERSM_IPV6.equals(smName)) {
                                setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                        }

                        if (smName == null || MASTERSM_IPV4.equals(smName)) {
                            if ((mMyUpstreamIfaceName == null && newUpstreamIfaceName == null) ||
                                    (mMyUpstreamIfaceName != null &&
                                    mMyUpstreamIfaceName.equals(newUpstreamIfaceName))) {
                                if (VDBG) Log.d(TAG, "[ISM_Tethered] Connection changed noop - dropping");
                                break;
                            }
                        } else if (MASTERSM_IPV6.equals(smName)) {
                            if ((mMyUpstreamIfaceNameIpv6 == null && newUpstreamIfaceName == null) ||
                                    (mMyUpstreamIfaceNameIpv6 != null &&
                                    mMyUpstreamIfaceNameIpv6.equals(newUpstreamIfaceName))) {
                                if (VDBG) Log.d(TAG, "[ISM_Tethered] Connection changed noop - dropping ipv6");
                                break;
                            }
                        }
                        /** @} */
                        /** M: dedicate apn feature */
                        mIsTetheringChangeDone = false;
                        /** M: ipv6 tethering @{ */
                        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                            if (smName == null || MASTERSM_IPV4.equals(smName))
                                cleanupUpstream();
                            else if(MASTERSM_IPV6.equals(smName))
                                cleanupUpstreamIpv6();
                        } else {
                            cleanupUpstream();
                        }
                        /** @} */
                        if (newUpstreamIfaceName != null) {
                            try {
                                /** M: ipv6 tethering @{ */
                                if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                                    if (smName == null || MASTERSM_IPV4.equals(smName)) {
                                        mNMService.enableNat(mIfaceName, newUpstreamIfaceName);
                                        mNMService.setIpForwardingEnabled(true);
                                    } else if (mIpv6FeatureEnable && MASTERSM_IPV6.equals(smName)) {
                                        mNMService.enableNatIpv6(mIfaceName, newUpstreamIfaceName);
                                        mNMService.setIpv6ForwardingEnabled(true);
                                    }
                                    Xlog.d(TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED enableNat for:" + smName + "(" + mIfaceName + ", " + newUpstreamIfaceName + ")");
                                } else {
                                    mNMService.enableNat(mIfaceName, newUpstreamIfaceName);
                                    mNMService.setIpForwardingEnabled(true);
                                    Xlog.d(TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED enableNat(" + mIfaceName + ", " + newUpstreamIfaceName + ")");
                                }/** @} */

                                /** M: dedicate apn feature @{ */
                                if (FeatureOption.MTK_DEDICATEDAPN_SUPPORT) {
                                    InterfaceConfiguration ifcg = mNMService.getInterfaceConfig(newUpstreamIfaceName);
                                    if (ifcg != null && ifcg.isActive()) {
                                        Xlog.d(TAG, "[ISM_Tethered] " + newUpstreamIfaceName + " is up!");
                                    } else {
                                        Xlog.d(TAG, "[ISM_Tethered] " + newUpstreamIfaceName + " is down!");
                                        /** M: ipv6 tethering @{ */
                                        if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT) {
                                            if (smName == null || MASTERSM_IPV4.equals(smName)) {
                                                Xlog.d(TAG, "[ISM_Tethered] " + "disableNatIpv4(" + mIfaceName + ", " + newUpstreamIfaceName + ")");
                                                mNMService.disableNat(mIfaceName, newUpstreamIfaceName);
                                            } else if (mIpv6FeatureEnable && MASTERSM_IPV6.equals(smName)) {
                                                Xlog.d(TAG, "[ISM_Tethered] " + "disableNatIpv6(" + mIfaceName + ", " + newUpstreamIfaceName + ")");
                                                mNMService.disableNatIpv6(mIfaceName, newUpstreamIfaceName);
                                            }
                                        } else { /** @} */
                                            mNMService.disableNat(mIfaceName, newUpstreamIfaceName);
                                            Xlog.d(TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED disableNat(" + mIfaceName + ", " + newUpstreamIfaceName + ")");
                                        }

                                        newUpstreamIfaceName = null;
                                        if (FeatureOption.MTK_DEDICATEDAPN_SUPPORT) {
                                            mContext.sendBroadcast(new Intent(ConnectivityManager.TETHER_CHANGED_DONE_ACTION));
                                        }
                                        mIsTetheringChangeDone = true;
                                        break;
                                    }
                                }
                                /** @} */
                                /** M: for bug solving @{ */
                                Xlog.d(TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED updateDns");
                                updateDns();
                                /** M: Hotspot Manager @{ */
                                if (mUsbInternetEnable) {
                                    mNMService.setDnsForwarders(mUsbInternetDnsServers);
                                /** @} */
                                } else {
                                    mNMService.setDnsForwarders(mDefaultDnsServers);
                                }
                                /** @} */
                            } catch (Exception e) {
                                Log.e(TAG, "[ISM_Tethered] Exception enabling Nat: " + e.toString());
                                try {
                                    mNMService.untetherInterface(mIfaceName);
                                    /** M: disabke dhcpv6 @{ */
                                    if(isIpv6MasterSmOn()) {
                                        mNMService.setDhcpv6Enabled(false, mIfaceName);
                                    }
                                    /** @} */
                                } catch (Exception ee) {
                                    Xlog.e(TAG, "[ISM_Tethered] untetherInterface failed, exception: " + ee);
                                }
                                /** M: ipv6 tethering @{ */
                                if (isIpv6MasterSmOn()) {
                                    setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                                }
                                /// M: fix bug, enableNat ok but enableNatIpv6 fail case (vice versa)
                                cleanupUpstream();
                                cleanupUpstreamIpv6();
                                /** @} */
                                setLastError(ConnectivityManager.TETHER_ERROR_ENABLE_NAT_ERROR);
                                transitionTo(mInitialState);
                                /** M: dedicated apn feature @{ */
                                if (FeatureOption.MTK_DEDICATEDAPN_SUPPORT) {
                                    mContext.sendBroadcast(new Intent(ConnectivityManager.TETHER_CHANGED_DONE_ACTION));
                                }
                                mIsTetheringChangeDone = true;
                                /** @} */
                                return true;
                            }
                        }else{
                            try{
                                /** M: ipv6 tethering @{ */
                                if (isIpv6MasterSmOn()) {
                                    if (MASTERSM_IPV4.equals(smName)) {
                                        mNMService.setIpForwardingEnabled(false); }
                                    if (MASTERSM_IPV6.equals(smName)) {
                                        mNMService.setIpv6ForwardingEnabled(false); }
                                } else {
                                /** @} */
                                mNMService.setIpForwardingEnabled(false);
                                }
                            } catch (Exception eee) {
                                Xlog.e(TAG, "[ISM_Tethered] untetherInterface failed, exception: " + eee);
                            }
                        }
                        /** M: ipv6 tethering @{ */
                        if (isIpv6MasterSmOn()) {
                            if (smName == null || MASTERSM_IPV4.equals(smName)) {
                            if (newUpstreamIfaceName == null) {
                                    Xlog.d(TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED mMyUpstreamIfaceName = null");
                                mMyUpstreamIfaceName = null;
                                } else {
                                mMyUpstreamIfaceName = newUpstreamIfaceName;
                                }
                            } else if (MASTERSM_IPV6.equals(smName)) {
                                if (newUpstreamIfaceName == null) {
                                    Xlog.d(TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED mMyUpstreamIfaceNameIpv6 = null");
                                    mMyUpstreamIfaceNameIpv6 = null;
                                } else {
                                mMyUpstreamIfaceNameIpv6 = newUpstreamIfaceName;
                                }
                            }
                        } else {/** @} */
                            mMyUpstreamIfaceName = newUpstreamIfaceName;
                        }
                        Xlog.d(TAG, "[ISM_Tethered] CMD_TETHER_CONNECTION_CHANGED finished!"+smName);
                        /** M: dedicated apn feature @{ */
                        if (FeatureOption.MTK_DEDICATEDAPN_SUPPORT) {
                            mContext.sendBroadcast(new Intent(ConnectivityManager.TETHER_CHANGED_DONE_ACTION));
                        }
                        mIsTetheringChangeDone = true;
                        /** @} */

                        /** M: Hotspot Manager */
                        broadcastReadyforUsbInternetConfig();

                        /** M: ipv6 tethering @{ */
                        if (isIpv6MasterSmOn() && MASTERSM_IPV6.equals(smName)) {
                            if (mMyUpstreamIfaceNameIpv6 != null) {
                                setLastError(ConnectivityManager.TETHER_ERROR_IPV6_AVAIABLE);
                            } else {
                                setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                            }
                                sendTetherStateChangedBroadcast();
                        }
                        /** @} */
                        /** M: fix ALPS00609719 */
                        if (isWifi(mIfaceName)) {
                            boolean enable = Settings.Secure.getInt(mContext.getContentResolver(),
                                             Settings.Secure.INTERFACE_THROTTLE, 0) == 1;
                            if (enable) {
                                try {
                                    int rx = mNMService.getBandwidthRxKbps();
                                    int tx = mNMService.getBandwidthTxKbps();
                                    mNMService.setInterfaceThrottle(mIfaceName, rx, tx);
                                    Xlog.d(TAG, "[ISM_Tethered] wifi hotspot bandwidth is enable:" + rx + "/" + tx);
                                } catch (Exception e) {
                                    Xlog.d(TAG, "setInterfaceThrottle failed:" + e);
                                }
                            }
                        }
                        /** @} */
                        break;
                    case CMD_CELL_DUN_ERROR:
                    case CMD_IP_FORWARDING_ENABLE_ERROR:
                    case CMD_IP_FORWARDING_DISABLE_ERROR:
                    case CMD_START_TETHERING_ERROR:
                    case CMD_STOP_TETHERING_ERROR:
                    case CMD_SET_DNS_FORWARDERS_ERROR:
                        error = true;
                        // fall through
                    case CMD_TETHER_MODE_DEAD:
                        Xlog.d(TAG, "[ISM_Tethered] CMD_TETHER_MODE_DEAD, mMyUpstreamIfaceName: " + mMyUpstreamIfaceName);
                        Xlog.d(TAG, "[ISM_Tethered] CMD_TETHER_MODE_DEAD, mMyUpstreamIfaceNameIpv6: " + mMyUpstreamIfaceNameIpv6);
                        cleanupUpstream();
                        /** M: ipv6 tethering @{ */
                        if (isIpv6MasterSmOn())
                            cleanupUpstreamIpv6();
                        /** @} */
                        try {
                            mNMService.untetherInterface(mIfaceName);
                            /** M: disable dhcpv6 @{ */
                            if(isIpv6MasterSmOn()) {
                                mNMService.setDhcpv6Enabled(false, mIfaceName);
                            }
                            /** @} */
                        } catch (Exception e) {
                            /** M: ipv6 tethering @{ */
                            if (isIpv6MasterSmOn())
                                setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                            /** @} */
                            setLastErrorAndTransitionToInitialState(
                                    ConnectivityManager.TETHER_ERROR_UNTETHER_IFACE_ERROR);
                            break;
                        }
                        if (error) {
                            /** M: ipv6 tethering @{ */
                            if (isIpv6MasterSmOn())
                                setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                            /** @} */
                            setLastErrorAndTransitionToInitialState(
                                    ConnectivityManager.TETHER_ERROR_MASTER_ERROR);
                            break;
                        }
                        if (DBG) Log.d(TAG, "[ISM_Tethered] Tether lost upstream connection " + mIfaceName);
                        Xlog.d(TAG, "[ISM_Tethered] sendTetherStateChangedBroadcast in CMD_TETHER_MODE_DEAD of TetheredState");
                        sendTetherStateChangedBroadcast();

                        if (mUsb) {
                            if (!Tethering.this.configureUsbIface(false)) {
                                setLastError(ConnectivityManager.TETHER_ERROR_IFACE_CFG_ERROR);
                                /** M: ipv6 tethering @{ */
                                if (isIpv6MasterSmOn())
                                    setLastError(ConnectivityManager.TETHER_ERROR_IPV6_UNAVAIABLE);
                                /** @} */
                            }
                        }
                        transitionTo(mInitialState);
                        break;
                    default:
                        retValue = false;
                        break;
                }
                return retValue;
            }
        }

        class UnavailableState extends State {
            @Override
            public void enter() {
                Xlog.d(TAG, "[ISM_Unavailable] enter, sendTetherStateChangedBroadcast");
                setAvailable(false);
                setLastError(ConnectivityManager.TETHER_ERROR_NO_ERROR);
                /** M: ipv6 tethering @{ */
                if (isIpv6MasterSmOn())
                    setLastError(ConnectivityManager.TETHER_ERROR_IPV6_NO_ERROR);
                /** @} */
                setTethered(false);
                /** M: for bug solving, ALPS00331223,ALPS00361177 */
                mTetherDone = true ;
                mTetheredFail = true;
                sendTetherStateChangedBroadcast();
                /** @} */
            }
            @Override
            public boolean processMessage(Message message) {
                Xlog.d(TAG, "[ISM_Unavailable] processMessage what=" + message.what);
                boolean retValue = true;
                switch (message.what) {
                    case CMD_INTERFACE_UP:
                        transitionTo(mInitialState);
                        break;
                    default:
                        retValue = false;
                        break;
                }
                return retValue;
            }
        }

        void setLastErrorAndTransitionToInitialState(int error) {
            setLastError(error);
            transitionTo(mInitialState);
        }

    }

    class TetherMasterSM extends StateMachine {
        // an interface SM has requested Tethering
        static final int CMD_TETHER_MODE_REQUESTED   = 1;
        // an interface SM has unrequested Tethering
        static final int CMD_TETHER_MODE_UNREQUESTED = 2;
        // upstream connection change - do the right thing
        static final int CMD_UPSTREAM_CHANGED        = 3;
        // we received notice that the cellular DUN connection is up
        static final int CMD_CELL_CONNECTION_RENEW   = 4;
        // we don't have a valid upstream conn, check again after a delay
        static final int CMD_RETRY_UPSTREAM          = 5;

        // This indicates what a timeout event relates to.  A state that
        // sends itself a delayed timeout event and handles incoming timeout events
        // should inc this when it is entered and whenever it sends a new timeout event.
        // We do not flush the old ones.
        private int mSequenceNumber;

        private State mInitialState;
        private State mTetherModeAliveState;

        private State mSetIpForwardingEnabledErrorState;
        private State mSetIpForwardingDisabledErrorState;
        private State mStartTetheringErrorState;
        private State mStopTetheringErrorState;
        private State mSetDnsForwardersErrorState;

        private ArrayList<TetherInterfaceSM> mNotifyList;

        private int mCurrentConnectionSequence;
        private int mMobileApnReserved = ConnectivityManager.TYPE_NONE;

        private String mUpstreamIfaceName = null;

        private static final int UPSTREAM_SETTLE_TIME_MS     = 10000;
        private static final int CELL_CONNECTION_RENEW_MS    = 40000;
        /** M: ipv6 tethering */
        private String mName;

        TetherMasterSM(String name, Looper looper) {
            super(name, looper);
            /** M: ipv6 tethering */
            mName = name;

            //Add states
            mInitialState = new InitialState();
            addState(mInitialState);
            mTetherModeAliveState = new TetherModeAliveState();
            addState(mTetherModeAliveState);

            mSetIpForwardingEnabledErrorState = new SetIpForwardingEnabledErrorState();
            addState(mSetIpForwardingEnabledErrorState);
            mSetIpForwardingDisabledErrorState = new SetIpForwardingDisabledErrorState();
            addState(mSetIpForwardingDisabledErrorState);
            mStartTetheringErrorState = new StartTetheringErrorState();
            addState(mStartTetheringErrorState);
            mStopTetheringErrorState = new StopTetheringErrorState();
            addState(mStopTetheringErrorState);
            mSetDnsForwardersErrorState = new SetDnsForwardersErrorState();
            addState(mSetDnsForwardersErrorState);

            mNotifyList = new ArrayList<TetherInterfaceSM>();
            setInitialState(mInitialState);
        }

        class TetherMasterUtilState extends State {
            protected final static boolean TRY_TO_SETUP_MOBILE_CONNECTION = false;
            protected final static boolean WAIT_FOR_NETWORK_TO_SETTLE     = false;

            @Override
            public boolean processMessage(Message m) {
                return false;
            }
            protected String enableString(int apnType) {
                switch (apnType) {
                case ConnectivityManager.TYPE_MOBILE_DUN:
                    return Phone.FEATURE_ENABLE_DUN_ALWAYS;
                case ConnectivityManager.TYPE_MOBILE:
                case ConnectivityManager.TYPE_MOBILE_HIPRI:
                    return Phone.FEATURE_ENABLE_HIPRI;
                }
                return null;
            }
            protected boolean turnOnUpstreamMobileConnection(int apnType) {
                boolean retValue = true;
                if (apnType == ConnectivityManager.TYPE_NONE) return false;
                if (apnType != mMobileApnReserved) turnOffUpstreamMobileConnection();
                int result = PhoneConstants.APN_REQUEST_FAILED;
                String enableString = enableString(apnType);
                if (enableString == null) return false;
                try {
                    Xlog.d(TAG, "[MSM_TetherModeAlive]["+mName+"] Try to startUsingNetworkFeature" );
                    result = mConnService.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                            enableString, new Binder());
                } catch (Exception e) {
                    Xlog.e(TAG, "[MSM_TetherModeAlive]["+mName+"] Failed to startUsingNetworkFeature: " + e.toString());
                }
                Xlog.d(TAG, "[MSM_TetherModeAlive]["+mName+"] startUsingNetworkFeature result=" + result);
                switch (result) {
                case PhoneConstants.APN_ALREADY_ACTIVE:
                case PhoneConstants.APN_REQUEST_STARTED:
                    mMobileApnReserved = apnType;
                    /** M: no need to renew connection due to feature expire
                     * only effect mms connection by MTK design.
                     */
                    //Message m = obtainMessage(CMD_CELL_CONNECTION_RENEW);
                    //m.arg1 = ++mCurrentConnectionSequence;
                    //sendMessageDelayed(m, CELL_CONNECTION_RENEW_MS);
                    break;
                case PhoneConstants.APN_REQUEST_FAILED:
                default:
                    retValue = false;
                    break;
                }

                return retValue;
            }
            protected boolean turnOffUpstreamMobileConnection() {
                // ignore pending renewal requests
                ++mCurrentConnectionSequence;
                if (mMobileApnReserved != ConnectivityManager.TYPE_NONE) {
                    try {
                        Xlog.d(TAG, "[MSM_TetherModeAlive]["+mName+"] Try to stopUsingNetworkFeature" );
                        mConnService.stopUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                                enableString(mMobileApnReserved));
                    } catch (Exception e) {
                        return false;
                    }
                    mMobileApnReserved = ConnectivityManager.TYPE_NONE;
                }
                return true;
            }
            protected boolean turnOnMasterTetherSettings() {
                try {
                    /** M: ipv6 tethering @{ */
                    if (isIpv6MasterSmOn()) {
                        if (MASTERSM_IPV4.equals(mName)) {
                            ///M: Fix for CR ALPS00382764
                            //mNMService.setIpForwardingEnabled(true);
                        }
                        else if (MASTERSM_IPV6.equals(mName)) {
                            mNMService.setIpv6ForwardingEnabled(true); }
                    } else {
                    /** @} */
                        ///M: Fix for CR ALPS00382764
                        //mNMService.setIpForwardingEnabled(true);
                    }
                } catch (Exception e) {
                    transitionTo(mSetIpForwardingEnabledErrorState);
                    return false;
                }
                try {
                    mNMService.startTethering(mDhcpRange);
                } catch (Exception e) {
                    try {
                        mNMService.stopTethering();
                        mNMService.startTethering(mDhcpRange);
                    } catch (Exception ee) {
                        transitionTo(mStartTetheringErrorState);
                        return false;
                    }
                }
                try {
                    /** M: for bug solving @{ */
                    Xlog.d(TAG, "[MSM_TetherModeAlive] turnOnMasterTetherSettings updateDns");
                    updateDns();
                    /** @} */
                    /** M: Hotspot Manager @{ */
                    if (mUsbInternetEnable) {
                        mNMService.setDnsForwarders(mUsbInternetDnsServers);
                    } else {
                    /** @} */
                        mNMService.setDnsForwarders(mDefaultDnsServers);
                    }
                } catch (Exception e) {
                    transitionTo(mSetDnsForwardersErrorState);
                    return false;
                }
                return true;
            }
            protected boolean turnOffMasterTetherSettings() {
                try {
                    mNMService.stopTethering();
                } catch (Exception e) {
                    transitionTo(mStopTetheringErrorState);
                    return false;
                }
                try {
                    /** M: ipv6 tethering @{ */
                    if (isIpv6MasterSmOn()) {
                        if (MASTERSM_IPV4.equals(mName)) {
                            mNMService.setIpForwardingEnabled(false); }
                        else if (MASTERSM_IPV6.equals(mName)) {
                            mNMService.setIpv6ForwardingEnabled(false); }
                    } else {
                    /** @} */
                        mNMService.setIpForwardingEnabled(false);
                    }
                } catch (Exception e) {
                    transitionTo(mSetIpForwardingDisabledErrorState);
                    return false;
                }
                /** M: ATT throughput test @{ */
                if (mUpstreamIfaceName != null && !mUpstreamIfaceName.equals(mWifiIface)) {
                    int mtu = mContext.getResources().getInteger(com.mediatek.internal.R.integer.config_mobile_mtu_default_size);
                    Xlog.d(TAG, "[MSM_TetherUtil] setMtuByInterface mtu:" + mtu);
                    NetworkUtils.setMtuByInterface(mUpstreamIfaceName, mtu);
                }
                /** @} */
                transitionTo(mInitialState);
                return true;
            }

            /** M: dedicated apn feature @{ */
            private int chooseDedicatedType() {
                TelephonyManager tm = TelephonyManager.getDefault();
                boolean isSimAbsent = false;
                boolean isGPRSDisabled = false;
                boolean dataEnabled = false;

                try {
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        final long simId = Settings.System.getLong(mContext.getContentResolver(),
                                       Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
                        dataEnabled = !(Settings.System.DEFAULT_SIM_NOT_SET == simId || 0 == simId);
                        Xlog.d(TAG, "SettingsGemini:" + dataEnabled);
                    } else {
                        dataEnabled = mConnService.getMobileDataEnabled();
                        Xlog.d(TAG, "Settings:" + dataEnabled);
                    }

                    final NetworkInfo wifiInfo = mConnService.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (wifiInfo != null && wifiInfo.isConnected()) {
                        Xlog.d(TAG, "trun off dedicated pdp due to wifi connected");
                        turnOffUpstreamMobileConnection();
                        return ConnectivityManager.TYPE_WIFI;
                    }

                    final NetworkInfo dunInfo = mConnService.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_DUN);
                    if (dunInfo != null && dataEnabled) {
                        return ConnectivityManager.TYPE_MOBILE_DUN;
                    }

                    if (!dataEnabled) {
                        Xlog.d(TAG, "trunOffUpstreamMobileConnection due to data disabled");
                        turnOffUpstreamMobileConnection();
                    }
                } catch (RemoteException e) {
                    Xlog.e(TAG, "RemoteException " + e);
                }

                return ConnectivityManager.TYPE_NONE;
            }
            /** @} */

            protected void chooseUpstreamType(boolean tryCell) {
                int upType = ConnectivityManager.TYPE_NONE;
                String iface = null;

                updateConfiguration();

                synchronized (mPublicSync) {
                    if (VDBG) {
                        Log.d(TAG, "["+mName+"]chooseUpstreamType has upstream iface types:");
                        for (Integer netType : mUpstreamIfaceTypes) {
                            Log.d(TAG, " " + netType);
                        }
                    }
                    /** M: Hotspot Manager @{ */
                    if (mUsbInternetEnable) {
                        if (isIpv6MasterSmOn()) {
                            Log.d(TAG, "["+mName+"] UsbInternet is ON, transitionTo ErrorState");
                            transitionTo(mStartTetheringErrorState);
                            return;
                        }
                        upType = ConnectivityManager.TYPE_USB;
                    /** @} */
                    /** M: dedicated apn feature @{ */
                    } else if (FeatureOption.MTK_DEDICATEDAPN_SUPPORT) {
                        upType = chooseDedicatedType();
                        Xlog.d(TAG, "chooseDedicatedType:" + upType);
                    } else {
                    /** @} */
                        for (Integer netType : mUpstreamIfaceTypes) {
                            NetworkInfo info = null;
                            try {
                                info = mConnService.getNetworkInfo(netType.intValue());
                            } catch (RemoteException e) { }
                            if ((info != null) && info.isConnected()) {
                                upType = netType.intValue();
                                /** M: ipv6 tethering @{ */
                                if (FeatureOption.MTK_TETHERINGIPV6_SUPPORT && mName.equals(MASTERSM_IPV6)) {
                                    if (mIpv6FeatureEnable && !hasIpv6Address(upType)) {
                                        Xlog.d(TAG, "we have no ipv6 address, upType:" + upType);
                                        upType = ConnectivityManager.TYPE_NONE;
                                    }
                                }
                                /** @} */
                                break;
                            }
                        }
                    }
                }

                if (DBG) {
                    Log.d(TAG, "["+mName+"]chooseUpstreamType(" + tryCell + "), preferredApn ="
                            + mPreferredUpstreamMobileApn + ", got type=" + upType);
                }

                // if we're on DUN, put our own grab on it
                if (upType == ConnectivityManager.TYPE_MOBILE_DUN ||
                        upType == ConnectivityManager.TYPE_MOBILE_HIPRI) {
                    turnOnUpstreamMobileConnection(upType);
                } else if (upType != ConnectivityManager.TYPE_NONE) {
                    /* If we've found an active upstream connection that's not DUN/HIPRI
                     * we should stop any outstanding DUN/HIPRI start requests.
                     *
                     * If we found NONE we don't want to do this as we want any previous
                     * requests to keep trying to bring up something we can use.
                     */
                    turnOffUpstreamMobileConnection();
                }

                if (upType == ConnectivityManager.TYPE_NONE) {
                    /** M: change policy don't enable data connection if no data connection is exist */
                    boolean tryAgainLater = false;
                    if ((tryCell == TRY_TO_SETUP_MOBILE_CONNECTION) &&
                            (turnOnUpstreamMobileConnection(mPreferredUpstreamMobileApn) == true)) {
                        // we think mobile should be coming up - don't set a retry
                        tryAgainLater = false;
                    }
                    if (tryAgainLater) {
                        sendMessageDelayed(CMD_RETRY_UPSTREAM, UPSTREAM_SETTLE_TIME_MS);
                    }
                /** M: Hotspot Manager @{ */
                } else if (upType == ConnectivityManager.TYPE_USB) {
                    String[] ifaces = new String[0];
                    try {
                        ifaces = mNMService.listInterfaces();
                    } catch (Exception e) {
                        Log.e(TAG, "Error listing Usb Internet Interfaces", e);
                    }
                    for (String usbiface : ifaces) {
                        if (isUsb(usbiface)) iface = usbiface;
                    }
                /** @} */
                } else {
                    LinkProperties linkProperties = null;
                    try {
                        linkProperties = mConnService.getLinkProperties(upType);
                    } catch (RemoteException e) { }
                    if (linkProperties != null) {
                        iface = linkProperties.getInterfaceName();
                        String[] dnsServers = mDefaultDnsServers;
                        Collection<InetAddress> dnses = linkProperties.getDnses();
                        if (dnses != null) {
                            // we currently only handle IPv4
                            ArrayList<InetAddress> v4Dnses =
                                    new ArrayList<InetAddress>(dnses.size());
                            for (InetAddress dnsAddress : dnses) {
                                if (dnsAddress instanceof Inet4Address) {
                                    v4Dnses.add(dnsAddress);
                                }
                            }
                            if (v4Dnses.size() > 0) {
                                dnsServers = NetworkUtils.makeStrings(v4Dnses);
                            }
                        }
                        try {
                            mNMService.setDnsForwarders(dnsServers);
                        } catch (Exception e) {
                            transitionTo(mSetDnsForwardersErrorState);
                        }
                    }
                }
                notifyTetheredOfNewUpstreamIface(iface);
            }

            protected void notifyTetheredOfNewUpstreamIface(String ifaceName) {
                if (DBG) Log.d(TAG, "[MSM_TetherModeAlive]["+mName+"] notifying tethered with iface =" + ifaceName);
                mUpstreamIfaceName = ifaceName;
                /** M: ATT throughput test @{ */
                if (ifaceName != null && !ifaceName.equals(mWifiIface)) {
                    Xlog.d(TAG, "[MSM_TetherUtil] setMtuByInterface 1500:" + mUpstreamIfaceName);
                    NetworkUtils.setMtuByInterface(mUpstreamIfaceName, 1500);
                }
                /** @} */
                /** M: for bug solving @{ */
                for (Object o : mNotifyList) {
                    TetherInterfaceSM sm = (TetherInterfaceSM)o;
                    if (ifaceName != null && sm.mMyUpstreamIfaceName != null && !ifaceName.equals(sm.mMyUpstreamIfaceName)) {
                        sm.sendMessage(TetherInterfaceSM.CMD_TETHER_CONNECTION_CHANGED, null);
                    }
                }
                /** @} */
                /** M: ipv6 tethering @{ */
                if (isIpv6MasterSmOn()) {
                    if (ifaceName != null) {
                        ifaceName = ifaceName + "," + mName;
                    } else {
                        ifaceName = "empty," + mName;
                    }
                    Xlog.d(TAG, "notifying tethered with change iface =" + ifaceName);
                }
                /** @} */
                for (TetherInterfaceSM sm : mNotifyList) {
                    sm.sendMessage(TetherInterfaceSM.CMD_TETHER_CONNECTION_CHANGED,
                            ifaceName);
                }
            }
        }

        class InitialState extends TetherMasterUtilState {
            @Override
            public void enter() {
                Xlog.d(TAG, "[MSM_Initial]["+mName+"] enter");
            }
            @Override
            public boolean processMessage(Message message) {
                if (DBG) Log.d(TAG, "[MSM_Initial]["+mName+"] processMessage what=" + message.what);
                boolean retValue = true;
                switch (message.what) {
                    case CMD_TETHER_MODE_REQUESTED:
                        TetherInterfaceSM who = (TetherInterfaceSM)message.obj;
                        if (VDBG) Log.d(TAG, "[MSM_Initial]["+mName+"] Tether Mode requested by " + who);
                        mNotifyList.add(who);
                        transitionTo(mTetherModeAliveState);
                        break;
                    case CMD_TETHER_MODE_UNREQUESTED:
                        who = (TetherInterfaceSM)message.obj;
                        Xlog.d(TAG, "[MSM_Initial] CMD_TETHER_MODE_UNREQUESTED ===========>");
                        if (VDBG) Log.d(TAG, "[MSM_Initial]["+mName+"] Tether Mode unrequested by " + who);
                        int index = mNotifyList.indexOf(who);
                        if (index != -1) {
                            mNotifyList.remove(who);
                        }
						                        /** M: for bug solving, ALPS00331223 */
                        if (who.mUsb){
                            mUnTetherDone = true;
                            Xlog.d(TAG,"[MSM_Initial] sendTetherStateChangedBroadcast");
                            sendTetherStateChangedBroadcast();
                        }
                        Xlog.d(TAG, "[MSM_Initial] CMD_TETHER_MODE_UNREQUESTED <===========");
                        break;
                    default:
                        retValue = false;
                        break;
                }
                return retValue;
            }
        }

        class TetherModeAliveState extends TetherMasterUtilState {
            boolean mTryCell = !WAIT_FOR_NETWORK_TO_SETTLE;
            @Override
            public void enter() {
                Xlog.d(TAG, "[MSM_TetherModeAlive]["+mName+"] enter");
                turnOnMasterTetherSettings(); // may transition us out

                mTryCell = !WAIT_FOR_NETWORK_TO_SETTLE; // better try something first pass
                                                        // or crazy tests cases will fail
                chooseUpstreamType(mTryCell);
                mTryCell = !mTryCell;
            }
            @Override
            public void exit() {
                /** M: ipv6 tethering @{ */
                Xlog.d(TAG, "[MSM_TetherModeAlive]["+mName+"] exit");
                if (isIpv6MasterSmOn()) {
                    if (mName.equals(MASTERSM_IPV4)) {
                        turnOffUpstreamMobileConnection();
                        notifyTetheredOfNewUpstreamIface(null);
                    } else {
                        Xlog.d(TAG, "[MSM_TetherModeAlive]["+mName+"] skip actions when exit");
                    }
                } else {
                /** @} */
                    turnOffUpstreamMobileConnection();
                    notifyTetheredOfNewUpstreamIface(null);
                }
            }
            @Override
            public boolean processMessage(Message message) {
                if (DBG) Log.d(TAG, "[MSM_TetherModeAlive]["+mName+"] processMessage what=" + message.what);
                boolean retValue = true;
                switch (message.what) {
                    case CMD_TETHER_MODE_REQUESTED:
                        TetherInterfaceSM who = (TetherInterfaceSM)message.obj;
                        if (VDBG) Log.d(TAG, "Tether Mode requested by " + who);
                        mNotifyList.add(who);
                        /** M: ipv6 tethering @{ */
                        String ifaceName = mUpstreamIfaceName;
                        if (isIpv6MasterSmOn()) {
                            if (ifaceName != null) {
                                ifaceName = ifaceName + "," + mName;
                            } else {
                                ifaceName = "empty," + mName;
                            }
                            Xlog.d(TAG, "CMD_TETHER_MODE_REQUESTED with change iface =" + ifaceName);
                        }
                        who.sendMessage(TetherInterfaceSM.CMD_TETHER_CONNECTION_CHANGED, ifaceName);
                        /** @} */
                        break;
                    case CMD_TETHER_MODE_UNREQUESTED:
                        who = (TetherInterfaceSM)message.obj;
                        if (VDBG) Log.d(TAG, "Tether Mode unrequested by " + who);
                        Xlog.d(TAG, "[MSM_TetherModeAlive] CMD_TETHER_MODE_UNREQUESTED ===========>");
                        int index = mNotifyList.indexOf(who);
                        if (index != -1) {
                            if (DBG) Log.d(TAG, "TetherModeAlive removing notifyee " + who);
                            mNotifyList.remove(index);
                            if (mNotifyList.isEmpty()) {
                                Xlog.d(TAG, "[MSM_TetherModeAlive] CMD_TETHER_MODE_UNREQUESTED is empty");
                                turnOffMasterTetherSettings(); // transitions appropriately
                            }else {
                                if (DBG) {
                                    Log.d(TAG, "TetherModeAlive still has " + mNotifyList.size() +
                                            " live requests:");
                                    for (Object o : mNotifyList) Log.d(TAG, "  " + o);
                                }
                            }
                        }else {
                           Log.e(TAG, "TetherModeAliveState UNREQUESTED has unknown who: " + who);
                        }
                        /** M: for bug solving, ALPS00331223 */
                        if (who.mUsb){
                            mUnTetherDone = true;
                            Xlog.d(TAG,"[MSM_TetherModeAliveState] sendTetherStateChangedBroadcast");
                            sendTetherStateChangedBroadcast();
                        }

                        Xlog.d(TAG, "[MSM_TetherModeAlive] CMD_TETHER_MODE_UNREQUESTED <===========");
                        break;
                    case CMD_UPSTREAM_CHANGED:
                        // need to try DUN immediately if Wifi goes down
                        mTryCell = !WAIT_FOR_NETWORK_TO_SETTLE;
                        chooseUpstreamType(mTryCell);
                        mTryCell = !mTryCell;
                        break;
                    case CMD_CELL_CONNECTION_RENEW:
                        // make sure we're still using a requested connection - may have found
                        // wifi or something since then.
                        if (mCurrentConnectionSequence == message.arg1) {
                            if (VDBG) {
                                Log.d(TAG, "[MSM_TetherModeAlive]["+mName+"] renewing mobile connection - requeuing for another " +
                                        CELL_CONNECTION_RENEW_MS + "ms");
                            }
                            turnOnUpstreamMobileConnection(mMobileApnReserved);
                        }
                        break;
                    case CMD_RETRY_UPSTREAM:
                        chooseUpstreamType(mTryCell);
                        mTryCell = !mTryCell;
                        break;
                    default:
                        retValue = false;
                        break;
                }
                return retValue;
            }
        }

        class ErrorState extends State {
            int mErrorNotification;
            @Override
            public boolean processMessage(Message message) {
                Xlog.d(TAG, "[MSM_Error]["+mName+"] processMessage what=" + message.what);
                boolean retValue = true;
                switch (message.what) {
                    case CMD_TETHER_MODE_REQUESTED:
                        TetherInterfaceSM who = (TetherInterfaceSM)message.obj;
                        who.sendMessage(mErrorNotification);
                        break;
                    default:
                       retValue = false;
                }
                return retValue;
            }
            void notify(int msgType) {
                mErrorNotification = msgType;
                for (Object o : mNotifyList) {
                    TetherInterfaceSM sm = (TetherInterfaceSM)o;
                    sm.sendMessage(msgType);
                }
            }

        }
        class SetIpForwardingEnabledErrorState extends ErrorState {
            @Override
            public void enter() {
                Log.e(TAG, "[MSM_Error]["+mName+"] setIpForwardingEnabled");
                notify(TetherInterfaceSM.CMD_IP_FORWARDING_ENABLE_ERROR);
                /** M: for bug solving */
                transitionTo(mInitialState);
            }
        }

        class SetIpForwardingDisabledErrorState extends ErrorState {
            @Override
            public void enter() {
                Log.e(TAG, "[MSM_Error]["+mName+"] setIpForwardingDisabled");
                notify(TetherInterfaceSM.CMD_IP_FORWARDING_DISABLE_ERROR);
                /** M: for bug solving */
                transitionTo(mInitialState);
            }
        }

        class StartTetheringErrorState extends ErrorState {
            @Override
            public void enter() {
                Log.e(TAG, "[MSM_Error]["+mName+"] startTethering");
                notify(TetherInterfaceSM.CMD_START_TETHERING_ERROR);
                try {
                    /** M: ipv6 tethering @{ */
                    if (isIpv6MasterSmOn()) {
                        if (MASTERSM_IPV4.equals(mName)) {
                            mNMService.setIpForwardingEnabled(false); }
                        else if (MASTERSM_IPV6.equals(mName)) {
                            mNMService.setIpv6ForwardingEnabled(false); }
                    } else {
                    /** @} */
                        mNMService.setIpForwardingEnabled(false);
                    }
                } catch (Exception e) {}
                /** M: for bug solving */
                transitionTo(mInitialState);
            }
        }

        class StopTetheringErrorState extends ErrorState {
            @Override
            public void enter() {
                Log.e(TAG, "[MSM_Error]["+mName+"] stopTethering");
                notify(TetherInterfaceSM.CMD_STOP_TETHERING_ERROR);
                try {
                    /** M: ipv6 tethering @{ */
                    if (isIpv6MasterSmOn()) {
                        if (MASTERSM_IPV4.equals(mName)) {
                            mNMService.setIpForwardingEnabled(false); }
                        else if (MASTERSM_IPV6.equals(mName)) {
                            mNMService.setIpv6ForwardingEnabled(false); }
                    } else {
                    /** @} */
                        mNMService.setIpForwardingEnabled(false);
                    }
                } catch (Exception e) {}
                /** M: for bug solving */
                transitionTo(mInitialState);
            }
        }

        class SetDnsForwardersErrorState extends ErrorState {
            @Override
            public void enter() {
                Log.e(TAG, "[MSM_Error]["+mName+"] setDnsForwarders");
                notify(TetherInterfaceSM.CMD_SET_DNS_FORWARDERS_ERROR);
                try {
                    mNMService.stopTethering();
                } catch (Exception e) {}
                try {
                    /** M: ipv6 tethering @{ */
                    if (isIpv6MasterSmOn()) {
                        if (MASTERSM_IPV4.equals(mName)) {
                            mNMService.setIpForwardingEnabled(false); }
                        if (MASTERSM_IPV6.equals(mName)) {
                            mNMService.setIpv6ForwardingEnabled(false); }
                    } else {
                    /** @} */
                        mNMService.setIpForwardingEnabled(false);
                    }
                } catch (Exception e) {}
                /** M: for bug solving */
                transitionTo(mInitialState);
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mContext.checkCallingOrSelfPermission(
                android.Manifest.permission.DUMP) != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission Denial: can't dump ConnectivityService.Tether " +
                    "from from pid=" + Binder.getCallingPid() + ", uid=" +
                    Binder.getCallingUid());
                    return;
        }

        synchronized (mPublicSync) {
            pw.println("mUpstreamIfaceTypes: ");
            for (Integer netType : mUpstreamIfaceTypes) {
                pw.println(" " + netType);
            }

            pw.println();
            pw.println("Tether state:");
            for (Object o : mIfaces.values()) {
                pw.println(" "+o);
            }
        }
        pw.println();
        return;
    }

    /** M: Hotspot Manager @{ */
    public void setUsbInternetEnable(boolean enable) {
        Xlog.d(TAG, "setUsbInternetEnable:" + enable);
        mUsbInternetEnable = enable;
        setUsbInternetEnable( enable, USB_INTERNET_SYSTEM_DEFAULT );
    }

    public void setUsbInternetEnable(boolean enable, int system_type) {
        Xlog.d(TAG, "setUsbInternetEnable:" + enable + " system_type: "+system_type);
        mUsbInternetEnable = enable;
        if(enable)
        {
            mUsbInternetSystemType = system_type;
            mUsbInternetDnsServers[0] = USB_INTERNET_DNS_SERVER1[mUsbInternetSystemType];
            mUsbInternetGateway = USB_INTERNET_DNS_SERVER1[mUsbInternetSystemType];
        }
    }

    private void broadcastDataConnectionStateChanged(int state) {
        /** Two receivers for this Intent, UsbDataStateTracker and WirelessSettings.
         *  if UsbInternet not enable, don't broadcast.(otherwise Usb Tethering will triger this Intent)
         *  if UsbInternet is set from enable to disable, UsbDataStateTracker will not receive this Intent
         *  due to mUsbInternetEnable is false. so we still broadcast if state is DISCONNECTED
         */

        if(mUsbInternetState == state){
            Xlog.d(TAG, "Skip the state:" + state);
            return;
        }
        mUsbInternetState = state;


        if (!mUsbInternetEnable && state == USB_INTERNET_CONNECTED) {
            Xlog.d(TAG, "broadcastDataConnectionStateChanged ignore:" + state);
        } else {
            Xlog.d(TAG, "broadcastDataConnectionStateChanged:" + state);
            Intent intent = new Intent("mediatek.intent.action.USB_DATA_STATE");
           /**  DISCONNEDTED 0
            *   CONNECTING   1
            *   CONNECTED    2
            *   SUSPENDED    3
            */

            //Configure link property for USB/RDNIS interface
            //Add default gateway property for VPN. ALPS00388310/ALPS00552341.
            //This route will be cleared in routing table while configureUsbIface
            LinkProperties linkProperties = new LinkProperties();
            linkProperties.setInterfaceName(mUsbIface);
            try{
                InetAddress gwAddr = InetAddress.parseNumericAddress(mUsbInternetGateway);
                RouteInfo route = new RouteInfo(gwAddr);
                linkProperties.addRoute(route);
                intent.putExtra(PhoneConstants.DATA_LINK_PROPERTIES_KEY, linkProperties);
            }catch(Exception e){
                e.printStackTrace();
            }

            intent.putExtra(PhoneConstants.STATE_KEY, DefaultPhoneNotifier.convertDataState(state).toString());
            intent.putExtra(PhoneConstants.DATA_APN_KEY, "internet");
            intent.putExtra(PhoneConstants.DATA_APN_TYPE_KEY, "usbinternet");
            intent.putExtra(ConnectivityManager.USB_INTERNET_SYSTEM_KEY, mUsbInternetSystemType);

            mContext.sendStickyBroadcast(intent);
        }
    }

    private void broadcastReadyforUsbInternetConfig() {
        if (!mUsbInternetEnable) {
            Xlog.d(TAG, "broadcastReadyforUsbInternetConfig ignore");
        } else {
            Xlog.d(TAG, "broadcastReadyforUsbInternetConfig");
            Intent intent = new Intent(ConnectivityManager.READY_FOR_USBINTERNET);
            mContext.sendBroadcast(intent);
        }
    }
    /** @} */
}
